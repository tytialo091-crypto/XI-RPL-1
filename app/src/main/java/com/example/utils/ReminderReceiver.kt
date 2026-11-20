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
 * BroadcastReceiver untuk mengecek peminjaman jatuh tempo H-1 di latar belakang.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val app = context.applicationContext as? EduPinjamApp
        if (app == null) {
            pendingResult.finish()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val session = app.repository.sessionState.first()
                if (session.reminderEnabled) {
                    val dueLoans = app.repository.getLoansNeedingH1Reminder()
                    NotificationUtils.showH1ReminderNotification(context, dueLoans)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
