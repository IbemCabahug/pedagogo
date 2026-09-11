package com.ibem.pedagogo.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ibem.pedagogo.PedagogoApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            val app = context.applicationContext as PedagogoApp
            val scheduler = AlarmScheduler(context)

            // Reschedule all class alarms upon reboot
            CoroutineScope(Dispatchers.IO).launch {
                val slots = app.database.scheduleDao().getAllSlotDetailsList()
                for (slot in slots) {
                    scheduler.scheduleAlarmForSlot(slot)
                }
            }
        }
    }
}
