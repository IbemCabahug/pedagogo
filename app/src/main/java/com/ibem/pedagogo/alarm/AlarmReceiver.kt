package com.ibem.pedagogo.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ibem.pedagogo.MainActivity
import com.ibem.pedagogo.PedagogoApp

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val subjectCode = intent.getStringExtra("EXTRA_SUBJECT_CODE") ?: "Upcoming Course"
        val subjectTitle = intent.getStringExtra("EXTRA_SUBJECT_TITLE") ?: ""
        val room = intent.getStringExtra("EXTRA_ROOM") ?: "TBA"
        val startHour = intent.getIntExtra("EXTRA_START_HOUR", 8)
        val startMin = intent.getIntExtra("EXTRA_START_MINUTE", 0)
        val category = intent.getStringExtra("EXTRA_CATEGORY") ?: "LECTURE"
        val slotId = intent.getLongExtra("EXTRA_SLOT_ID", 0L)

        val timeString = String.format("%02d:%02d", startHour, startMin)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            slotId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val (titlePrefix, supportiveMessage) = when (category) {
            "DEMO_TEACHING" -> Pair(
                "Demo Teaching Day",
                "Starts at $timeString in $room • Stand tall, Teacher. You've got this!"
            )
            "FIELD_STUDY" -> Pair(
                "Field Study Observation",
                "Session at $timeString in $room • Observe with empathy and curiosity."
            )
            "PREP_TIME" -> Pair(
                "Lesson & IMs Prep",
                "Prep buffer for $subjectCode at $timeString • Gather your materials peacefully."
            )
            else -> Pair(
                "Class Prep",
                "Starts at $timeString in $room • Breathe easy, you are ready for your learners."
            )
        }

        val notification = NotificationCompat.Builder(context, PedagogoApp.ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("$titlePrefix: $subjectCode")
            .setContentText(supportiveMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$supportiveMessage\n$subjectTitle"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .build()

        notificationManager.notify(slotId.toInt(), notification)
    }
}
