package com.ibem.pedagogo.data.parser

// Pure-Kotlin COR (Certificate of Registration) text parser after ML Kit OCR.
// Zero Android/Room imports - headless-testable from scratch/ (CorParserSmoke),
// then reused by the Confirm screen. OCR is never perfect; the UI lets the
// student confirm/fix in ~10 seconds. The parser's job: tolerant, consistent,
// never crash on garbage.
import java.util.regex.Pattern
import kotlin.Pair

data class ParsedCorSlot(
    val subjectCode: String,
    val title: String,
    val room: String,
    val days: String,               // normalized, e.g. "MWF" / "TTH" / "M"
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val category: String,           // LECTURE | FIELD_STUDY | DEMO_TEACHING | PREP_TIME
    val prepOffsetMinutes: Int = 30
)

data class CorParseResult(
    val slots: List<ParsedCorSlot>,
    val rowsSeen: Int,
    val skippedLines: Int,
    val warnings: List<String>
)

object CorTextParser {

    private val CODE_RE = Pattern.compile(
        "\\b[A-Z]{1,6}[-. ]?[A-Z]?\\d{1,4}\\b"
    )
    private val TIME_RE = Pattern.compile(
        "(\\d{1,2})[:.](\\d{2})\\s*(AM|PM)?\\s*[-\\u2013\\u2014to]\\s*" +
            "(\\d{1,2})[:.](\\d{2})\\s*(AM|PM)?"
    )
    private val ROOM_RE = Pattern.compile(
        "(?:(?:Rm|RM|Room|Bldg|Building)\\.?\\s*[A-Z]?\\d{1,4}[A-Z]?|\\b[A-Z]\\d{2,4}\\b)"
    )
    private val DAY_COMBOS = Pattern.compile(
        "(?:MTWThF|MTWTThF|MTWTF|MWThF|TWThF|TThF|MTThF|MWF|TTh|ThF|ThS|MW|Th|MF|WF|TW|TF|MTh)\\b"
    )
    private val DAY_SINGLE = Pattern.compile(
        "\\b([MTWFS])(?=\\s*\\d{1,2}[:.])"
    )
    private val NOISE_RE = Pattern.compile(
        "(?i)\\b(certificate|certification|registrar|name|student id|id no|address|status|" +
            "program|curriculum|prerequisite|signature|adviser|advisor|semester|school year|" +
            "remarks|date printed|total units|pre.req)\\b|^\\s*\\d+\\s*$"
    )

    /** 24h normalization. PM shifts <12 by +12; 12:xx AM maps to 00; bare hours stay. */
    private fun normalize(h: Int, m: Int, suffix: String?): Pair<Int, Int> {
        val s = suffix?.uppercase()
        if (s == "AM") return Pair(if (h == 12) 0 else h, m)
        if (s == "PM") return Pair(if (h < 12) h + 12 else h, m)
        // Bare hours: treat 1..6 as AM (evening CORs rarely start 1-6am); the
        // Confirm screen is the safety net for the weird edge cases.
        return Pair(h, m)
    }

    /** Canonical day letters, order-preserving, duplicates removed. "Th" -> "TTH". */
    private fun dayLetters(daysRaw: String): String {
        var s = daysRaw.uppercase().replace("TH", "TTH")
        val sb = StringBuilder()
        for (i in 0 until s.length) {
            val ch = s[i].toString()
            if ("MTWFS".contains(ch) && !sb.toString().contains(ch)) sb.append(ch)
        }
        return sb.toString()
    }

    /** Day tokens (MWF / TTH / F) are not course codes. */
    private val NONCODE_CHARS_RE = Pattern.compile("[0-9 .,-]")
    private fun isDayLikeCode(cand: String): Boolean {
        if (cand.isBlank()) return true
        val letters = NONCODE_CHARS_RE.matcher(cand).replaceAll("").uppercase()
        if (letters.length == 1) return letters[0] in "MTWFS"
        return letters == "MWF" || letters == "TTH" || letters == "MW" ||
            letters == "TH" || letters == "MTWTHF" || letters == "MTWTF" ||
            letters == "TTHF" || letters == "TW" || letters == "MF" ||
            letters == "WF" || letters == "THF" || letters == "THS" || letters == "MTH"
    }
    fun parse(raw: String): CorParseResult {
        val warnings = mutableListOf<String>()
        if (raw == null || raw.isBlank()) {
            return CorParseResult(mutableListOf(), 0, 1, warnings + "Empty input")
        }

        val lines = PATTERN_NL.split(raw.replace("\\u00A0", " ").replace("\\u2019", "'"))
        val slots = mutableListOf<ParsedCorSlot>()
        val seen = java.util.LinkedHashSet<String>()
        var rowsSeen = 0
        var skipped = 0
        var lastCode = ""

        for (lineRaw in lines) {
            val line = lineRaw.trim()
            if (line.isBlank()) continue

            val hasTime = TIME_RE.matcher(line).find()
            val hasCode = CODE_RE.matcher(line).find()
            val isNoise = NOISE_RE.matcher(line).find()

            // Noise lines (headers/footers) are only skipped when they carry no
            // schedule data. A continuation line like "3 units MWF 7:30-9:00"
            // MUST survive this filter because it holds the row's time.
            if (isNoise && !hasTime) { skipped += 1; continue }

            if (!hasTime) {
                // Code-only line: remember it so the next time line can merge.
                if (hasCode) {
                    val cm = CODE_RE.matcher(line)
                    if (cm.find()) lastCode = cm.group()
                } else {
                    skipped += 1
                }
                continue
            }

            val tm = TIME_RE.matcher(line)
            if (!tm.find()) continue
            val shS = tm.group(1) ?: "0"; val smS = tm.group(2) ?: "0"
            val ehS = tm.group(4) ?: "0"; val emS = tm.group(5) ?: "0"
            val (sh, sm) = normalize(shS.toInt(), smS.toInt(), tm.group(3))
            val (eh, em) = normalize(ehS.toInt(), emS.toInt(), tm.group(6))
            if (eh * 60 + em < sh * 60 + sm) { skipped += 1; continue }  // malformed range

            // Prefer a real course code BEFORE the time token (day tokens and
            // rooms are never codes). Otherwise merge the previous line code.
            val timeStart = tm.start()
            var code = lastCode
            val cm = CODE_RE.matcher(line)
            while (cm.find() && cm.end() <= timeStart) {
                val cand = cm.group()
                if (!isDayLikeCode(cand)) { code = cand; break }
            }
            lastCode = ""   // one-shot merge

            val rm = ROOM_RE.matcher(line)
            var room = ""
            if (rm.find()) room = rm.group().trim()

            var daysRaw = ""
            val dm = DAY_COMBOS.matcher(line)
            if (dm.find()) daysRaw = dm.group()
            else {
                val ds = DAY_SINGLE.matcher(line)
                if (ds.find()) daysRaw = ds.group(1) ?: ""
            }
            val days = dayLetters(daysRaw)

            // Title = line minus machine tokens, minus stray digits/units/points.
            var title = line
            title = TIME_RE.matcher(title).replaceAll(" ")
            title = CODE_RE.matcher(title).replaceAll(" ")
            title = ROOM_RE.matcher(title).replaceAll(" ")
            title = DAY_COMBOS.matcher(title).replaceAll(" ")
            title = DAY_SINGLE.matcher(title).replaceAll(" ")
            title = Pattern.compile("(?i)\\b(AM|PM|units?)\\b").matcher(title).replaceAll(" ")
            title = Pattern.compile("\\s\\d+([.,-]\\d+)?\\s").matcher(title).replaceAll(" ")
            title = title.replace("\\u2013", " ").replace("\\u2014", " ").trim()
            title = title.trimStart('-', '.', ':', ' ').trim()

            if (code.isEmpty()) { skipped += 1; continue }

            val key = "$code|$days|$sh:$sm"
            if (seen.contains(key)) continue
            seen.add(key)
            rowsSeen += 1

            slots.add(ParsedCorSlot(
                subjectCode = code,
                title = title,
                room = room,
                days = days,
                startHour = sh,
                startMinute = sm,
                endHour = eh,
                endMinute = em,
                category = inferCategory(code),
                prepOffsetMinutes = 30
            ))
        }
        return CorParseResult(slots, rowsSeen, skipped, warnings)
    }

    fun inferCategory(code: String): String {
        val u = code.uppercase()
        if (u.contains("FS") || u.startsWith("FIELD")) return "FIELD_STUDY"
        if (u.contains("PREP")) return "PREP_TIME"
        if (u.contains("DEMO")) return "DEMO_TEACHING"
        return "LECTURE"
    }

    private val PATTERN_NL = Pattern.compile("\\r?\\n")
}
