package com.example.utils

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.model.PeminjamanDetail

/**
 * Utilitas notifikasi pengingat H-1 sebelum jatuh tempo peminjaman barang.
 */
object NotificationUtils {
    const val CHANNEL_ID = "edupinjam_reminder_h1"
    private const val CHANNEL_NAME = "Pengingat Jatuh Tempo H-1"
    private const val CHANNEL_DESC = "Notifikasi pengingat pengembalian barang inventaris sekolah H-1 sebelum jatuh tempo"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Menjadwalkan pengecekan pengingat H-1 harian melalui AlarmManager.
     */
    fun scheduleDailyReminderCheck(context: Context, enabled: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            2026,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (enabled) {
            val triggerAt = System.currentTimeMillis() + 60_000L
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                AlarmManager.INTERVAL_HALF_DAY,
                pendingIntent
            )
        } else {
            alarmManager.cancel(pendingIntent)
        }
    }

    /**
     * Menampilkan notifikasi ringkasan peminjaman yang jatuh tempo besok (H-1) atau terlambat.
     */
    fun showH1ReminderNotification(context: Context, loans: List<PeminjamanDetail>): Boolean {
        if (loans.isEmpty()) return false
        if (!hasNotificationPermission(context)) return false

        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            1001,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val first = loans.first()
        val namaBarang = first.barang?.namaBarang ?: "Inventaris Sekolah"
        val namaSiswa = first.peminjam?.nama ?: "Siswa"
        val title = if (loans.size == 1) {
            "Pengingat Pengembalian H-1: $namaBarang"
        } else {
            "Pengingat EDU-PINJAM: ${loans.size} barang mendekati jatuh tempo"
        }

        val body = if (loans.size == 1) {
            "Peminjam $namaSiswa jatuh tempo ${DateUtils.formatTanggalPendek(first.peminjaman.tanggalKembaliRencana)}. Segera kembalikan agar bebas denda."
        } else {
            loans.take(3).joinToString(" • ") {
                "${it.barang?.namaBarang ?: "Barang"} (${it.peminjam?.nama ?: "Siswa"})"
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(3001, notification)
        return true
    }
}
