package com.ibem.pedagogo.data.dao

import androidx.room.*
import com.ibem.pedagogo.data.entity.ClassSlot
import com.ibem.pedagogo.data.entity.Subject
import com.ibem.pedagogo.data.entity.SubjectWithSlots
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Transaction
    @Query("SELECT * FROM subjects ORDER BY code ASC")
    fun getAllSubjectsWithSlots(): Flow<List<SubjectWithSlots>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Long): Subject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Update
    suspend fun updateSubject(subject: Subject)

    @Delete
    suspend fun deleteSubject(subject: Subject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassSlot(slot: ClassSlot): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassSlots(slots: List<ClassSlot>)

    @Query("DELETE FROM class_slots WHERE subjectId = :subjectId")
    suspend fun deleteSlotsForSubject(subjectId: Long)

    @Transaction
    @Query("""
        SELECT class_slots.*, subjects.code as subjectCode, subjects.title as subjectTitle, 
               subjects.colorHex as subjectColor, subjects.prepOffsetMinutes as prepOffset,
               subjects.category as category
        FROM class_slots
        INNER JOIN subjects ON class_slots.subjectId = subjects.id
        WHERE class_slots.dayOfWeek = :dayOfWeek
        ORDER BY class_slots.startHour ASC, class_slots.startMinute ASC
    """)
    fun getSlotsForDay(dayOfWeek: Int): Flow<List<ClassSlotDetail>>

    @Query("""
        SELECT class_slots.*, subjects.code as subjectCode, subjects.title as subjectTitle, 
               subjects.colorHex as subjectColor, subjects.prepOffsetMinutes as prepOffset,
               subjects.category as category
        FROM class_slots
        INNER JOIN subjects ON class_slots.subjectId = subjects.id
    """)
    suspend fun getAllSlotDetailsList(): List<ClassSlotDetail>
}

data class ClassSlotDetail(
    val id: Long,
    val subjectId: Long,
    val dayOfWeek: Int,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val room: String,
    val isLab: Boolean,
    val subjectCode: String,
    val subjectTitle: String,
    val subjectColor: String,
    val prepOffset: Int,
    val category: String = "LECTURE"
)

