package com.ibem.pedagogo.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,              // e.g. "ED 204", "FS 1", "ENG 301"
    val title: String,             // e.g. "Facilitating Learner-Centered Teaching"
    val instructor: String = "",   // e.g. "Prof. Santos / Cooperating Teacher"
    val section: String = "",      // e.g. "BSEd-Eng 3A"
    val colorHex: String = "#3B6347", // Pedagogical accent color
    val prepOffsetMinutes: Int = 30,  // Configurable prep offset (survives traffic/prep)
    val category: String = "LECTURE"  // "LECTURE", "FIELD_STUDY", "DEMO_TEACHING", "PREP_TIME"
)

