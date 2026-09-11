package com.ibem.pedagogo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ibem.pedagogo.data.entity.ClassSlot
import com.ibem.pedagogo.data.entity.Subject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubjectScreen(
    onBack: () -> Unit,
    onSave: (Subject, List<ClassSlot>) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var instructor by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("LECTURE") }
    var selectedColor by remember { mutableStateOf("#3B6347") } // Default Sage Green
    var prepOffset by remember { mutableIntStateOf(30) } // Default 30 min buffer

    var startHour by remember { mutableStateOf("08") }
    var startMin by remember { mutableStateOf("00") }
    var endHour by remember { mutableStateOf("10") }
    var endMin by remember { mutableStateOf("00") }
    var dayOfWeek by remember { mutableIntStateOf(1) } // 1 = Monday

    val days = listOf(
        1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun"
    )

    val categories = listOf(
        "LECTURE" to "Lecture & Theory",
        "FIELD_STUDY" to "Field Study (FS)",
        "DEMO_TEACHING" to "Demo Teaching",
        "PREP_TIME" to "Lesson & IMs Prep"
    )

    val colorPalette = listOf(
        "#3B6347" to "Sage Green",
        "#2F6F80" to "River Teal",
        "#BF5F3E" to "Terracotta",
        "#D98326" to "Warm Amber",
        "#755B8C" to "Dusty Lavender",
        "#37474F" to "Slate Chalkboard"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Add Course or Session", fontWeight = FontWeight.Bold)
                        Text(
                            "Plan with peace of mind",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Category Selector
            Text(
                text = "Session Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { (catKey, catLabel) ->
                    FilterChip(
                        selected = selectedCategory == catKey,
                        onClick = { selectedCategory = catKey },
                        label = { Text(catLabel) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Subject Code & Title
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Subject Code") },
                placeholder = { Text("e.g. ED 204, FS 1, ENG 301") },
                leadingIcon = { Icon(Icons.Outlined.Class, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Course Title or Focus") },
                placeholder = { Text("e.g. Facilitating Learner-Centered Teaching") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = instructor,
                onValueChange = { instructor = it },
                label = { Text("Instructor / Cooperating Teacher (Optional)") },
                placeholder = { Text("e.g. Dr. Santos / CT Mrs. Cruz") },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = room,
                onValueChange = { room = it },
                label = { Text("Assigned Room or Partner School") },
                placeholder = { Text("e.g. Room 304, Mabolo Elementary Grade 5-A") },
                leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Day of Week
            Text(
                text = "Meeting Day",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEach { (dNumber, dLabel) ->
                    val isSelected = dayOfWeek == dNumber
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { dayOfWeek = dNumber },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dLabel,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Time Selection
            Text(
                text = "Class Hours (24-Hour Format)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startHour,
                    onValueChange = { if (it.length <= 2) startHour = it },
                    label = { Text("Start (HH)") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = startMin,
                    onValueChange = { if (it.length <= 2) startMin = it },
                    label = { Text("Start (MM)") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = endHour,
                    onValueChange = { if (it.length <= 2) endHour = it },
                    label = { Text("End (HH)") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = endMin,
                    onValueChange = { if (it.length <= 2) endMin = it },
                    label = { Text("End (MM)") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            // Prep Buffer Offset
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Alarm,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Calm Prep Buffer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "$prepOffset minutes before",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "A gentle heads-up to gather your visual aids, lesson plan, and arrive centered.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 30, 45, 60).forEach { mins ->
                        FilterChip(
                            selected = prepOffset == mins,
                            onClick = { prepOffset = mins },
                            label = { Text("${mins}m") },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Calm Theme Accent
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Subject Accent Color",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    colorPalette.forEach { (hex, _) ->
                        val colorInt = android.graphics.Color.parseColor(hex)
                        val composeColor = Color(colorInt)
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(composeColor)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = {
                    if (code.isNotBlank() && title.isNotBlank()) {
                        val s = Subject(
                            code = code.trim(),
                            title = title.trim(),
                            instructor = instructor.trim(),
                            colorHex = selectedColor,
                            prepOffsetMinutes = prepOffset,
                            category = selectedCategory
                        )
                        val slot = ClassSlot(
                            subjectId = 0,
                            dayOfWeek = dayOfWeek,
                            startHour = startHour.toIntOrNull() ?: 8,
                            startMinute = startMin.toIntOrNull() ?: 0,
                            endHour = endHour.toIntOrNull() ?: 10,
                            endMinute = endMin.toIntOrNull() ?: 0,
                            room = room.trim().ifEmpty { "TBA" }
                        )
                        onSave(s, listOf(slot))
                    }
                },
                enabled = code.isNotBlank() && title.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Save Course & Schedule Calm Alert",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
