package com.ibem.pedagogo.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.ibem.pedagogo.data.dao.ClassSlotDetail
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExact(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Schedules a class-aware exact alarm surviving Doze using setAlarmClock()
     */
    fun scheduleAlarmForSlot(slot: ClassSlotDetail) {
        val nextOccurrence = calculateNextTriggerTime(slot) ?: return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.ibem.pedagogo.ACTION_CLASS_ALARM"
            putExtra("EXTRA_SLOT_ID", slot.id)
            putExtra("EXTRA_SUBJECT_CODE", slot.subjectCode)
            putExtra("EXTRA_SUBJECT_TITLE", slot.subjectTitle)
            putExtra("EXTRA_ROOM", slot.room)
            putExtra("EXTRA_START_HOUR", slot.startHour)
            putExtra("EXTRA_START_MINUTE", slot.startMinute)
            putExtra("EXTRA_CATEGORY", slot.category)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            slot.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Show AlarmClockInfo so the Android status bar displays the active alarm icon
        val showIntent = Intent(context, com.ibem.pedagogo.MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            slot.id.toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(nextOccurrence, showPendingIntent)
        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: SecurityException) {
            // Defensive fallback if permission was revoked in settings
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextOccurrence, pendingIntent)
            }
        }
    }

    fun cancelAlarmForSlot(slotId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.ibem.pedagogo.ACTION_CLASS_ALARM"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            slotId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun calculateNextTriggerTime(slot: ClassSlotDetail): Long? {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            // Calendar: 1 = Sunday, 2 = Monday, ... 7 = Saturday
            // Our DB: 1 = Mon, 2 = Tue, 3 = Wed, 4 = Thu, 5 = Fri, 6 = Sat, 7 = Sun
            val calDay = when (slot.dayOfWeek) {
                1 -> Calendar.MONDAY
                2 -> Calendar.TUESDAY
                3 -> Calendar.WEDNESDAY
                4 -> Calendar.THURSDAY
                5 -> Calendar.FRIDAY
                6 -> Calendar.SATURDAY
                7 -> Calendar.SUNDAY
                else -> Calendar.MONDAY
            }
            set(Calendar.DAY_OF_WEEK, calDay)
            set(Calendar.HOUR_OF_DAY, slot.startHour)
            set(Calendar.MINUTE, slot.startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Subtract preparation offset minutes (e.g. 30 min before class)
            add(Calendar.MINUTE, -slot.prepOffset)
        }

        // If time already passed this week, advance by 7 days to next week
        if (target.before(now)) {
            target.add(Calendar.WEEK_OF_YEAR, 1)
        }

        return target.timeInMillis
    }
}
