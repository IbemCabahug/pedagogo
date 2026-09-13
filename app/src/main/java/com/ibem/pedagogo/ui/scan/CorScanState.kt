package com.ibem.pedagogo.ui.scan

import com.ibem.pedagogo.data.parser.CorParseResult

// State machine for the COR scan wedge:
// Capture -> Scanning -> Confirm (& Fix) -> Saving -> Done.
enum class CorScanPhase {
    CAPTURE,
    SCANNING,
    CONFIRM,
    SAVING,
    DONE
}

// One human-editable row on the Confirm screen. The parser's raw slot is
// exploded into per-day ClassSlot drafts; the student edits fields (~10s)
// before saving to Room + AlarmScheduler.
data class CorDraftSlot(
    val id: Int,
    var subjectCode: String,
    var title: String,
    var room: String,
    var dayOfWeek: Int,          // 1 = Monday ... 7 = Sunday
    var startHour: Int,
    var startMinute: Int,
    var endHour: Int,
    var endMinute: Int,
    var category: String,
    var included: Boolean = true
)

data class CorScanState(
    val phase: CorScanPhase = CorScanPhase.CAPTURE,
    val parseResult: CorParseResult? = null,
    val drafts: List<CorDraftSlot> = emptyList(),
    val error: String? = null,
    val savedCount: Int = 0,
    val alarmedCount: Int = 0,
    val pageCount: Int = 0
)
