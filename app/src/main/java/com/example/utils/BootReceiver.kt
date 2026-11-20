package com.example.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.EduPinjamApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver untuk menjadwalkan pengecekan pengingat H-1 setelah device reboot.
 * Berjalan pada peristiwa BOOT_COMPLETED agar reminder tetap aktif setelah restart.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as? EduPinjamApp ?: return

            // Menjalankan pengecekan pengingat setelah boot
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val session = app.repository.sessionState.first()
                    if (session.reminderEnabled) {
                        val dueLoans = app.repository.getLoansNeedingH1Reminder()
                        if (dueLoans.isNotEmpty()) {
                            NotificationUtils.showH1ReminderNotification(context, dueLoans)
                        }
                    }
                } catch (_: Exception) {
                    // Abaikan error pengecekan saat boot
                }
            }
        }
    }
}
