package com.ibem.pedagogo.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val classSlotId: Long,
    val subjectCode: String,
    val room: String,
    val triggerTimeMillis: Long,
    val prepOffsetMinutes: Int,
    val isFired: Boolean = false,
    val isDismissed: Boolean = false
)
