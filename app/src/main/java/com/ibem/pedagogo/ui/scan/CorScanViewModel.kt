package com.ibem.pedagogo.ui.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.ibem.pedagogo.data.parser.CorTextParser
import com.ibem.pedagogo.data.parser.ParsedCorSlot
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CorSaveReport(
    val subjectsCreated: Int,
    val slotsCreated: Int,
    val alarmsScheduled: Int
)

class CorScanViewModel : ViewModel() {
    private val _state = MutableStateFlow(CorScanState())
    val state: StateFlow<CorScanState> = _state.asStateFlow()
    private val draftIds = AtomicInteger(1)
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    fun recognizeBitmap(bitmap: Bitmap) {
        _state.update { it.copy(phase = CorScanPhase.SCANNING, error = null) }
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

    fun recognizeUri(context: Context, uri: Uri) {
        _state.update { it.copy(phase = CorScanPhase.SCANNING, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()
            withContext(Dispatchers.Main) {
                if (bitmap != null) recognizeBitmap(bitmap)
                else _state.update {
                    it.copy(
                        phase = CorScanPhase.CAPTURE,
                        error = "Could not read that image. Try a clearer photo."
                    )
                }
            }
        }
    }

    fun parsePastedText(raw: String) {
        _state.update { it.copy(phase = CorScanPhase.SCANNING, error = null) }
        onOcrText(raw)
    }
    private fun onOcrText(raw: String) {
        if (raw.isBlank()) {
            _state.update {
                it.copy(
                    phase = CorScanPhase.CAPTURE,
                    error = "No text found. Hold steady, fill the frame with the schedule."
                )
            }
            return
        }
        val result = CorTextParser.parse(raw)
        if (result.slots.isEmpty()) {
            _state.update {
                it.copy(
                    phase = CorScanPhase.CAPTURE,
                    error = "No class rows found (${result.skippedLines} lines skipped). " +
                        "Try a closer photo of the subject list."
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
                error = null
            )
        }
    }

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
                'T' -> { out += 2; out += 4 }
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

    fun backToCapture() {
        _state.update { it.copy(phase = CorScanPhase.CAPTURE, error = null) }
    }

    fun reset() {
        _state.update { CorScanState() }
    }
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
