package com.example.domain.model

import com.example.data.local.entity.BarangEntity
import com.example.data.local.entity.PeminjamanEntity
import com.example.data.local.entity.UserEntity

/**
 * Peran pengguna dalam aplikasi EDU-PINJAM.
 */
enum class UserRole(val label: String) {
    ADMIN("Admin"),
    PETUGAS("Petugas"),
    SISWA("Siswa");

    companion object {
        fun fromString(value: String): UserRole =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: SISWA
    }
}

/**
 * Kategori barang inventaris sekolah sesuai spesifikasi.
 */
enum class KategoriBarang(val label: String) {
    ALAT_TULIS("Alat Tulis"),
    BUKU("Buku"),
    ELEKTRONIK("Elektronik"),
    OLAHRAGA("Olahraga"),
    LAB("Lab"),
    MUSIK("Musik"),
    LAINNYA("Lainnya");

    companion object {
        fun fromLabel(label: String): KategoriBarang =
            entries.find { it.label.equals(label, ignoreCase = true) || it.name.equals(label, ignoreCase = true) }
                ?: LAINNYA
    }
}

/**
 * Kondisi fisik barang inventaris.
 */
enum class KondisiBarang(val label: String) {
    BAIK("Baik"),
    RUSAK_RINGAN("Rusak Ringan"),
    RUSAK_BERAT("Rusak Berat");

    companion object {
        fun fromLabel(label: String): KondisiBarang =
            entries.find { it.label.equals(label, ignoreCase = true) || it.name.equals(label, ignoreCase = true) }
                ?: BAIK
    }
}

/**
 * Status transaksi peminjaman.
 */
enum class StatusPeminjaman(val label: String) {
    DIPINJAM("Dipinjam"),
    DIKEMBALIKAN("Dikembalikan"),
    TERLAMBAT("Terlambat"),
    RUSAK("Rusak");

    companion object {
        fun fromLabel(label: String): StatusPeminjaman =
            entries.find { it.label.equals(label, ignoreCase = true) || it.name.equals(label, ignoreCase = true) }
                ?: DIPINJAM
    }
}

/**
 * Indikator warna ketersediaan stok barang.
 */
enum class IndikatorStok(val label: String) {
    TERSEDIA("Tersedia"),
    SISA_SEDIKIT("Sisa Sedikit"),
    HABIS("Habis");

    companion object {
        fun dariStok(stokTersedia: Int): IndikatorStok = when {
            stokTersedia <= 0 -> HABIS
            stokTersedia <= 2 -> SISA_SEDIKIT
            else -> TERSEDIA
        }
    }
}

/**
 * Pilihan pengurutan daftar inventaris.
 */
enum class SortOption(val label: String) {
    NAMA_AZ("Nama A-Z"),
    NAMA_ZA("Nama Z-A"),
    STOK_TERBANYAK("Stok Terbanyak"),
    STOK_SEDIKIT("Stok Sedikit")
}

/**
 * Pilihan tema terang aplikasi (tetap bernuansa Sky Blue).
 */
enum class TemaTerang(val label: String) {
    PUTIH_CERAH("Putih Cerah (Standar)"),
    BIRU_LANGIT_LEMBUT("Biru Langit Lembut");

    companion object {
        fun fromName(name: String): TemaTerang =
            entries.find { it.name == name } ?: PUTIH_CERAH
    }
}

/**
 * Pengaturan gaya bahasa aplikasi (Indonesia utama).
 */
enum class BahasaApp(val label: String) {
    INDONESIA("Bahasa Indonesia"),
    INDONESIA_FORMAL("Bahasa Indonesia (Baku)");

    companion object {
        fun fromName(name: String): BahasaApp =
            entries.find { it.name == name } ?: INDONESIA
    }
}

/**
 * Model gabungan transaksi peminjaman beserta detail barang dan siswa peminjam.
 */
data class PeminjamanDetail(
    val peminjaman: PeminjamanEntity,
    val barang: BarangEntity?,
    val peminjam: UserEntity?,
    val dendaPerHari: Long = 1000L,
    val waktuSekarang: Long = System.currentTimeMillis()
) {
    val statusEnum: StatusPeminjaman
        get() = StatusPeminjaman.fromLabel(peminjaman.status)

    val masihAktif: Boolean
        get() = statusEnum == StatusPeminjaman.DIPINJAM || statusEnum == StatusPeminjaman.TERLAMBAT

    val hariTerlambatLive: Long
        get() {
            val referensiWaktu = peminjaman.tanggalKembaliAktual ?: waktuSekarang
            val selisihMillis = referensiWaktu - peminjaman.tanggalKembaliRencana
            if (selisihMillis <= 0L) return 0L
            val satuHariMillis = 24L * 60L * 60L * 1000L
            return ((selisihMillis + satuHariMillis - 1L) / satuHariMillis).coerceAtLeast(1L)
        }

    val isTerlambatLive: Boolean
        get() = if (masihAktif) {
            waktuSekarang > peminjaman.tanggalKembaliRencana
        } else {
            statusEnum == StatusPeminjaman.TERLAMBAT || peminjaman.denda > 0L
        }

    val estimasiDendaLive: Long
        get() = if (peminjaman.tanggalKembaliAktual != null) {
            peminjaman.denda
        } else {
            hariTerlambatLive * dendaPerHari
        }

    val statusTampil: StatusPeminjaman
        get() = if (masihAktif && isTerlambatLive) StatusPeminjaman.TERLAMBAT else statusEnum
}

/**
 * Ringkasan statistik untuk Dashboard Admin dan Petugas.
 */
data class DashboardStats(
    val totalJenisBarang: Int = 0,
    val totalUnitBarang: Int = 0,
    val barangTersediaUnit: Int = 0,
    val barangDipinjamUnit: Int = 0,
    val peminjamanTerlambatCount: Int = 0,
    val pengingatH1Count: Int = 0
)

/**
 * Ringkasan jumlah jenis barang per status ketersediaan langsung dari Room Database.
 */
data class InventoryStatusCounts(
    val totalItems: Int = 0,
    val availableItems: Int = 0,
    val lowStockItems: Int = 0,
    val outOfStockItems: Int = 0,
    val totalUnitsAvailable: Int = 0,
    val totalUnits: Int = 0
)

/**
 * Titik data grafik mini peminjaman 7 hari terakhir.
 */
data class DailyLoanPoint(
    val labelHari: String,
    val tanggalEpoch: Long,
    val jumlahPeminjaman: Int
)

/**
 * Item laporan barang paling sering dipinjam.
 */
data class TopItemReport(
    val barangId: Long,
    val namaBarang: String,
    val kodeBarang: String,
    val kategori: String,
    val totalDipinjamKali: Int,
    val totalUnitDipinjam: Int
)

/**
 * Item laporan siswa paling aktif meminjam.
 */
data class TopStudentReport(
    val userId: Long,
    val namaSiswa: String,
    val nis: String,
    val kelas: String,
    val totalTransaksi: Int,
    val totalUnitDipinjam: Int
)

/**
 * Ringkasan laporan bulanan.
 */
data class MonthlyReportSummary(
    val bulanLabel: String,
    val totalTransaksi: Int,
    val totalDikembalikan: Int,
    val totalTerlambat: Int,
    val totalDendaTerkumpul: Long,
    val topBarang: List<TopItemReport>,
    val topSiswa: List<TopStudentReport>
)
