package com.ibem.pedagogo.ui.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Color
import android.util.Log
import android.os.Build
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ibem.pedagogo.PedagogoApp
import com.ibem.pedagogo.alarm.AlarmScheduler
import com.ibem.pedagogo.data.entity.ClassSlot
import com.ibem.pedagogo.data.entity.Subject
import com.ibem.pedagogo.data.imageprep.corSampleSize
import com.ibem.pedagogo.data.parser.CorTextParser
import com.ibem.pedagogo.data.parser.ParsedCorSlot
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

data class CorSaveReport(
    val subjectsCreated: Int,
    val slotsCreated: Int,
    val alarmsScheduled: Int
)

// Single-source-of-truth brain for the COR wedge. All intake paths (full-res
// photo, gallery image/screenshot, portal PDF, pasted lines) funnel into the
// same OCR -> parser -> Confirm pipeline. Pages accumulate: raw OCR text from
// every accepted page is re-parsed together until the student saves.
class CorScanViewModel : ViewModel() {
    companion object {
        private const val TAG = "CorScan"
    }

    private val _state = MutableStateFlow(CorScanState())
    val state: StateFlow<CorScanState> = _state.asStateFlow()
    private val draftIds = AtomicInteger(1)
    private val rawTexts = mutableListOf<String>()
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    // Prep (invert dark-mode shots, trim camera overkill) off the UI thread,
    // then run ML Kit on the prepped bitmap.
    fun recognizeBitmap(bitmap: Bitmap) {
        _state.update { it.copy(phase = CorScanPhase.SCANNING, error = null) }
        viewModelScope.launch(Dispatchers.Default) {
            val prepared = runCatching { prepareForOcr(bitmap) }.getOrElse { e ->
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(phase = CorScanPhase.CAPTURE, error = friendly(e)) }
                }
                return@launch
            }
            withContext(Dispatchers.Main) { startOcr(prepared) }
        }
    }

    private fun startOcr(bitmap: Bitmap) {
        val image = runCatching { InputImage.fromBitmap(bitmap, 0) }.getOrElse { e ->
            _state.update { it.copy(phase = CorScanPhase.CAPTURE, error = friendly(e)) }
            return
        }
        recognizer.process(image)
            .addOnSuccessListener { visionText -> onOcrText(visionText.text) }
            .addOnFailureListener { e: Exception ->
                _state.update { it.copy(phase = CorScanPhase.CAPTURE, error = friendly(e)) }
            }
    }

    // Full-res decode, single-read: pull the bytes ONCE, then try bounds +
    // sample decode from the same array (some providers flake on the second
    // stream open), then fall back to ImageDecoder for formats BitmapFactory
    // refuses - HEIC/HEIF album photos above all (the "clearer photo" false
    // positive). Real decode failures log to logcat and name the actual cause.
    fun recognizeUri(context: Context, uri: Uri) {
        _state.update { it.copy(phase = CorScanPhase.SCANNING, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val decoded = runCatching { decodeCapped(context, uri, 4096) }
            withContext(Dispatchers.Main) {
                decoded.fold(
                    onSuccess = { bitmap ->
                        if (bitmap != null) recognizeBitmap(bitmap)
                        else _state.update {
                            it.copy(
                                phase = CorScanPhase.CAPTURE,
                                error = decodeError
                            )
                        }
                    },
                    onFailure = { e ->
                        Log.e(TAG, "decode failed", e)
                        _state.update {
                            it.copy(phase = CorScanPhase.CAPTURE, error = decodeError)
                        }
                    }
                )
            }
        }
    }

    private fun decodeCapped(context: Context, uri: Uri, maxSide: Int): Bitmap? {
        val bytes = context.contentResolver.openInputStream(uri)
            ?.use { it.readBytes() }
            ?: return null
        Log.d(TAG, "intake bytes=" + bytes.size + " type=" + context.contentResolver.getType(uri))
        if (bytes.isEmpty()) return null

        // Pass 1: BitmapFactory from the in-memory bytes (single read).
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val w = bounds.outWidth
        val h = bounds.outHeight
        if (w > 0 && h > 0) {
            val sample = corSampleSize(maxOf(w, h), maxSide)
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            if (decoded != null) return decoded
            Log.w(TAG, "BitmapFactory null for " + w + "x" + h + " sample=" + sample + " - trying ImageDecoder")
        } else {
            Log.w(TAG, "no bounds (unsupported container, likely HEIC) - trying ImageDecoder")
        }

        // Pass 2: ImageDecoder - modern, HEIC-aware (API 28+ only; older
        // devices keep the honest-error path instead of crashing).
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Log.w(TAG, "API < 28 - no ImageDecoder fallback available")
            return null
        }
        val source = ImageDecoder.createSource(bytes)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val size = info.size
            val longSide = maxOf(size.width, size.height)
            if (longSide > maxSide * 2) {
                val scale = (maxSide * 2f) / longSide
                decoder.setTargetSize(
                    (size.width * scale).toInt().coerceAtLeast(1),
                    (size.height * scale).toInt().coerceAtLeast(1)
                )
            }
        }
    }

    private val decodeError: String
        get() = "Could not open that file. If it is a photo, save it as a JPEG " +
            "or take a screenshot of it - then pick that instead."

    // Portal PDF path: framework PdfRenderer (zero new deps) -> white-backed
    // bitmaps -> per-page OCR -> one combined text for the parser.
    fun recognizePdf(context: Context, uri: Uri) {
        _state.update { it.copy(phase = CorScanPhase.SCANNING, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val pages = runCatching { renderPdfPages(context, uri) }.getOrElse { e ->
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(phase = CorScanPhase.CAPTURE, error = friendly(e)) }
                }
                return@launch
            }
            if (pages.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _state.update {
                        it.copy(
                            phase = CorScanPhase.CAPTURE,
                            error = "That PDF has no readable pages. Try the photo or typing option."
                        )
                    }
                }
                return@launch
            }
            val combined = StringBuilder()
            for (page in pages) combined.appendLine(awaitOcr(page))
            withContext(Dispatchers.Main) { onOcrText(combined.toString()) }
        }
    }

    private fun renderPdfPages(context: Context, uri: Uri): List<Bitmap> {
        val out = mutableListOf<Bitmap>()
        val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return out
        val renderer = PdfRenderer(fd)
        try {
            for (index in 0 until renderer.pageCount) {
                val page = renderer.openPage(index)
                try {
                    // Points -> pixels: scale up small PDF pages for OCR,
                    // cap 4x so a letter page lands around ~2400px long side.
                    val scale = (2400f / maxOf(page.width, page.height)).coerceIn(1f, 4f)
                    val w = (page.width * scale).toInt().coerceAtLeast(1)
                    val h = (page.height * scale).toInt().coerceAtLeast(1)
                    val pageBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    pageBitmap.eraseColor(Color.WHITE) // transparent page -> white bg
                    page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    out += pageBitmap
                } finally {
                    page.close()
                }
            }
        } finally {
            renderer.close()
        }
        return out
    }

    // Suspend wrapper around the callback Task so PDF pages OCR sequentially.
    private suspend fun awaitOcr(bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            val image = runCatching { InputImage.fromBitmap(bitmap, 0) }.getOrNull()
            if (image == null) {
                cont.resume("")
                return@suspendCancellableCoroutine
            }
            recognizer.process(image)
                .addOnSuccessListener { cont.resume(it.text) }
                .addOnFailureListener { cont.resume("") }
        }

    // Direct paste path (typed COR lines or screenshot text) - same parser.
    fun parsePastedText(raw: String) {
        _state.update { it.copy(phase = CorScanPhase.SCANNING, error = null) }
        onOcrText(raw)
    }

    private fun onOcrText(raw: String) {
        if (raw.isBlank()) {
            _state.update {
                it.copy(
                    phase = CorScanPhase.CAPTURE,
                    error = if (rawTexts.isEmpty())
                        "No text found. Hold steady, fill the frame with the schedule."
                    else
                        "That page had no readable text. Try again or add the next page."
                )
            }
            return
        }
        rawTexts += raw
        val result = CorTextParser.parse(rawTexts.joinToString("\n"))
        if (result.slots.isEmpty()) {
            _state.update {
                it.copy(
                    phase = CorScanPhase.CAPTURE,
                    error = "No class rows found so far (${result.skippedLines} lines skipped). " +
                        "Add a clearer page, or type the lines instead."
                )
            }
            return
        }
        val drafts = result.slots.flatMap { slot -> explodeDays(slot) }
        _state.update {
            it.copy(
                phase = CorScanPhase.CONFIRM,
                parseResult = result,
                drafts = drafts,
                pageCount = rawTexts.size,
                error = null
            )
        }
    }

    // Day-letter expansion. Parser canonicalizes Tue+Thu ("TTh") to a single
    // "T" (smoke-pinned), so one "T" draft becomes BOTH Tue and Thu rows -
    // the student unchecks the wrong one in ~2 seconds on Confirm.
    private fun explodeDays(slot: ParsedCorSlot): List<CorDraftSlot> {
        val days = daysToInts(slot.days)
        return days.map { day ->
            CorDraftSlot(
                id = draftIds.getAndIncrement(),
                subjectCode = slot.subjectCode,
                title = slot.title.ifBlank { slot.subjectCode },
                room = slot.room.ifBlank { "TBA" },
                dayOfWeek = day,
                startHour = slot.startHour,
                startMinute = slot.startMinute,
                endHour = slot.endHour,
                endMinute = slot.endMinute,
                category = slot.category,
                included = true
            )
        }
    }

    private fun daysToInts(days: String): List<Int> {
        if (days.isBlank()) return listOf(1)
        val out = mutableListOf<Int>()
        for (ch in days.uppercase()) {
            when (ch) {
                'M' -> out += 1
                'T' -> { out += 2; out += 4 } // Tue + Thu (parser collapses TTh)
                'W' -> out += 3
                'F' -> out += 5
                'S' -> out += 6
            }
        }
        return out.distinct().sorted().ifEmpty { listOf(1) }
    }

    fun updateDraft(id: Int, transform: (CorDraftSlot) -> CorDraftSlot) {
        _state.update { s ->
            s.copy(drafts = s.drafts.map { if (it.id == id) transform(it) else it })
        }
    }

    fun toggleIncluded(id: Int) {
        updateDraft(id) { it.copy(included = !it.included) }
    }

    fun removeDraft(id: Int) {
        _state.update { s -> s.copy(drafts = s.drafts.filterNot { it.id == id }) }
    }

    // Back to capture KEEPING accepted pages - the multi-page "add another" path.
    fun addAnotherPage() {
        _state.update { it.copy(phase = CorScanPhase.CAPTURE, error = null) }
    }

    fun backToCapture() {
        _state.update { it.copy(phase = CorScanPhase.CAPTURE, error = null) }
    }

    fun setError(message: String) {
        _state.update { it.copy(phase = CorScanPhase.CAPTURE, error = message) }
    }

    fun reset() {
        rawTexts.clear()
        _state.value = CorScanState()
    }

    // Group drafts by subject code -> Subject row, then ClassSlot rows, then
    // exact alarms via the existing AlarmScheduler. Runs on IO; UI observes DONE.
    fun saveConfirmed(context: Context, onDone: (CorSaveReport) -> Unit = {}) {
        val drafts = _state.value.drafts.filter { it.included }
        if (drafts.isEmpty()) {
            _state.update { it.copy(error = "Nothing selected - tick at least one row.") }
            return
        }
        _state.update { it.copy(phase = CorScanPhase.SAVING, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val app = context.applicationContext as PedagogoApp
            val dao = app.database.scheduleDao()
            val scheduler = AlarmScheduler(context.applicationContext)
            var subjectsCreated = 0
            var slotsCreated = 0
            var alarmsScheduled = 0
            val byCode = drafts.groupBy { it.subjectCode.trim().uppercase() }
            for ((_, rows) in byCode) {
                val first = rows.first()
                val subjectId = dao.insertSubject(
                    Subject(
                        code = first.subjectCode.trim(),
                        title = first.title.trim().ifBlank { first.subjectCode.trim() },
                        instructor = "",
                        section = "",
                        colorHex = "#3B6347",
                        prepOffsetMinutes = 30,
                        category = first.category
                    )
                )
                subjectsCreated += 1
                val entities = rows.map { d ->
                    ClassSlot(
                        subjectId = subjectId,
                        dayOfWeek = d.dayOfWeek.coerceIn(1, 7),
                        startHour = d.startHour.coerceIn(0, 23),
                        startMinute = d.startMinute.coerceIn(0, 59),
                        endHour = d.endHour.coerceIn(0, 23),
                        endMinute = d.endMinute.coerceIn(0, 59),
                        room = d.room.trim().ifBlank { "TBA" }
                    )
                }
                dao.insertClassSlots(entities)
                slotsCreated += entities.size
            }
            val codes = byCode.keys
            val all = dao.getAllSlotDetailsList()
            for (detail in all.filter { it.subjectCode.uppercase() in codes }) {
                runCatching { scheduler.scheduleAlarmForSlot(detail) }
                    .onSuccess { alarmsScheduled += 1 }
            }
            withContext(Dispatchers.Main) {
                val report = CorSaveReport(subjectsCreated, slotsCreated, alarmsScheduled)
                _state.update {
                    it.copy(
                        phase = CorScanPhase.DONE,
                        savedCount = slotsCreated,
                        alarmedCount = alarmsScheduled
                    )
                }
                onDone(report)
            }
        }
    }

    private fun friendly(e: Throwable): String =
        "Scan failed (${e.message ?: "unknown error"}). Try again with a steadier photo."
}
