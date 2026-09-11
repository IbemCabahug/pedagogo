package com.ibem.pedagogo.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "class_slots",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"])]
)
data class ClassSlot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val dayOfWeek: Int,            // 1 = Monday, 2 = Tuesday, ... 7 = Sunday
    val startHour: Int,            // 0 - 23
    val startMinute: Int,          // 0 - 59
    val endHour: Int,
    val endMinute: Int,
    val room: String,              // e.g. "Room 402", "CS Lab 3"
    val isLab: Boolean = false
)
