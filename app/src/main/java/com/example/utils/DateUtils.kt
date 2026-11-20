package com.example.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Utilitas format tanggal, waktu, dan mata uang Rupiah untuk EDU-PINJAM.
 */
object DateUtils {
    private val indonesiaLocale = Locale("id", "ID")

    fun formatTanggalPendek(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", indonesiaLocale)
        return sdf.format(Date(millis))
    }

    fun formatTanggalLengkap(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", indonesiaLocale)
        return sdf.format(Date(millis))
    }

    fun formatBulanTahun(millis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("MMMM yyyy", indonesiaLocale)
        return sdf.format(Date(millis))
    }

    fun formatNamaHariSingkat(millis: Long): String {
        val sdf = SimpleDateFormat("EEE", indonesiaLocale)
        return sdf.format(Date(millis)).take(3)
    }

    fun formatRupiah(nominal: Long): String {
        val formatter = NumberFormat.getCurrencyInstance(indonesiaLocale)
        formatter.maximumFractionDigits = 0
        return formatter.format(nominal).replace("Rp", "Rp ").replace("  ", " ")
    }

    fun awalHari(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun akhirHari(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun tambahHari(millis: Long, jumlahHari: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            add(Calendar.DAY_OF_YEAR, jumlahHari)
        }
        return cal.timeInMillis
    }
}
