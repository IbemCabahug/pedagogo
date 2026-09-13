package com.ibem.pedagogo.ui.screens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ibem.pedagogo.ui.scan.CorDraftSlot
import com.ibem.pedagogo.ui.scan.CorScanViewModel


val COR_DAY_NAMES = listOf("", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

// The blueprint's whole trick: OCR is never perfect, so the human confirms
// in ~10 seconds. Checkbox = include, Fix = edit inline, trash = drop.
@Composable
fun CorConfirmContent(
    modifier: Modifier = Modifier,
    vm: CorScanViewModel,
    onRetake: () -> Unit
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val included = state.drafts.count { it.included }
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "Check what we found — fix anything wrong, then save.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "$included of ${state.drafts.size} rows selected" +
                    (state.parseResult?.let { " · ${it.skippedLines} lines skipped" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
        }
        items(state.drafts, key = { it.id }) { draft ->
            CorDraftCard(
                draft = draft,
                onToggle = { vm.toggleIncluded(draft.id) },
                onDelete = { vm.removeDraft(draft.id) },
                onEdit = { fn -> vm.updateDraft(draft.id, fn) }
            )
        }
        item {
            Button(
                onClick = { vm.saveConfirmed(context) },
                enabled = included > 0,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Text("  Save $included to my schedule", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onRetake,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Retake photo") }
            if (state.error != null) {
                Text(
                    state.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}


@Composable
fun CorDraftCard(
    draft: CorDraftSlot,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: ((CorDraftSlot) -> CorDraftSlot) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (draft.included)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = draft.included, onCheckedChange = { onToggle() })
                Column(Modifier.weight(1f)) {
                    Text(
                        "${draft.subjectCode} · ${COR_DAY_NAMES.getOrElse(draft.dayOfWeek) { "Mon" }} " +
                            "${corTwo(draft.startHour)}:${corTwo(draft.startMinute)}–" +
                            "${corTwo(draft.endHour)}:${corTwo(draft.endMinute)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        draft.title.ifBlank { "Tap Fix to add a title" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Done" else "Fix")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove row")
                }
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CorField(
                        value = draft.subjectCode,
                        label = "Code",
                        modifier = Modifier.weight(1f),
                        onChange = { v -> onEdit { d -> d.copy(subjectCode = v.uppercase()) } }
                    )
                    CorField(
                        value = draft.room,
                        label = "Room",
                        modifier = Modifier.weight(1f),
                        onChange = { v -> onEdit { d -> d.copy(room = v) } }
                    )
                }
                Spacer(Modifier.height(8.dp))
                CorTimeFields(draft = draft, onEdit = onEdit)
            }
        }
    }
}


fun corTwo(n: Int) = n.toString().padStart(2, '0')

fun parseHm(v: String): Pair<Int, Int>? {
    val digits = v.filter { it.isDigit() }
    if (digits.length < 3 || digits.length > 4) return null
    val h: Int
    val m: Int
    if (digits.length == 3) { h = digits.substring(0, 1).toInt(); m = digits.substring(1).toInt() }
    else { h = digits.substring(0, 2).toInt(); m = digits.substring(2).toInt() }
    if (h !in 0..23 || m !in 0..59) return null
    return h to m
}

@Composable
fun CorTimeFields(
    draft: CorDraftSlot,
    onEdit: ((CorDraftSlot) -> CorDraftSlot) -> Unit
) {
    CorField(
        value = draft.title,
        label = "Title",
        modifier = Modifier.fillMaxWidth(),
        onChange = { v -> onEdit { d -> d.copy(title = v) } }
    )
    Spacer(Modifier.height(8.dp))
    DayPickerRow(
        selected = draft.dayOfWeek,
        onPick = { v -> onEdit { d -> d.copy(dayOfWeek = v) } }
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CorField(
            value = "${corTwo(draft.startHour)}:${corTwo(draft.startMinute)}",
            label = "Start HH:MM",
            modifier = Modifier.weight(1f),
            onChange = { v ->
                parseHm(v)?.let { (h, m) ->
                    onEdit { d -> d.copy(startHour = h, startMinute = m) }
                }
            }
        )
        CorField(
            value = "${corTwo(draft.endHour)}:${corTwo(draft.endMinute)}",
            label = "End HH:MM",
            modifier = Modifier.weight(1f),
            onChange = { v ->
                parseHm(v)?.let { (h, m) ->
                    onEdit { d -> d.copy(endHour = h, endMinute = m) }
                }
            }
        )
    }
}

@Composable
fun CorField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        modifier = modifier
    )
}

@Composable
fun DayPickerRow(selected: Int, onPick: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (d in 1..7) {
            if (d == selected) {
                Button(
                    onClick = { onPick(d) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) { Text(COR_DAY_NAMES[d], fontWeight = FontWeight.Bold) }
            } else {
                OutlinedButton(
                    onClick = { onPick(d) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) { Text(COR_DAY_NAMES[d]) }
            }
        }
    }
}
