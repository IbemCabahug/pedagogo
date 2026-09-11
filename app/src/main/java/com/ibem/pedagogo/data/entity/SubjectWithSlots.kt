package com.ibem.pedagogo.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class SubjectWithSlots(
    @Embedded val subject: Subject,
    @Relation(
        parentColumn = "id",
        entityColumn = "subjectId"
    )
    val slots: List<ClassSlot>
)
