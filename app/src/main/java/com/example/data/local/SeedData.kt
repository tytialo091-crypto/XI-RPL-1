package com.example.data.local

import com.example.data.local.entity.BarangEntity
import com.example.data.local.entity.PeminjamanEntity
import com.example.data.local.entity.UserEntity
import com.example.domain.model.KategoriBarang
import com.example.domain.model.KondisiBarang
import com.example.domain.model.StatusPeminjaman
import com.example.domain.model.UserRole
import com.example.utils.PasswordUtil

/**
 * Data awal (seed) untuk aplikasi EDU-PINJAM:
 * - Akun Admin, Petugas, dan 4 Siswa demo
 * - 15 Barang inventaris sekolah lengkap
 * - Riwayat peminjaman 7 hari terakhir (termasuk pengingat H-1 dan keterlambatan)
 */
object SeedData {

    fun initialUsers(): List<UserEntity> {
        val now = System.currentTimeMillis()
        val hashedPassword123 = PasswordUtil.hashPassword("123")
        return listOf(
            UserEntity(
                id = 1L,
                nama = "Bapak Hendra Wijaya",
                username = "admin",
                password = hashedPassword123,
                role = UserRole.ADMIN.name,
                nis = "",
                kelas = "Kepala Sarpras",
                noHp = "081234567890",
                createdAt = now
            ),
            UserEntity(
                id = 2L,
                nama = "Ibu Sari Rahmawati",
                username = "petugas",
                password = hashedPassword123,
                role = UserRole.PETUGAS.name,
                nis = "",
                kelas = "Petugas Inventaris",
                noHp = "081298765432",
                createdAt = now
            ),
            UserEntity(
                id = 3L,
                nama = "Rizky Pratama",
                username = "2024001",
                password = hashedPassword123,
                role = UserRole.SISWA.name,
                nis = "2024001",
                kelas = "XI IPA 1",
                noHp = "085611222333",
                createdAt = now
            ),
            UserEntity(
                id = 4L,
                nama = "Aisyah Putri",
                username = "2024002",
                password = hashedPassword123,
                role = UserRole.SISWA.name,
                nis = "2024002",
                kelas = "XI IPA 1",
                noHp = "085722333444",
                createdAt = now
            ),
            UserEntity(
                id = 5L,
                nama = "Dimas Aryo",
                username = "2024003",
                password = hashedPassword123,
                role = UserRole.SISWA.name,
                nis = "2024003",
                kelas = "XI IPS 1",
                noHp = "085833444555",
                createdAt = now
            ),
            UserEntity(
                id = 6L,
                nama = "Nabila Zahra",
                username = "2024004",
                password = hashedPassword123,
                role = UserRole.SISWA.name,
                nis = "2024004",
                kelas = "XI IPS 1",
                noHp = "085944555666",
                createdAt = now
            ),
            UserEntity(
                id = 7L,
                nama = "Bintang Arya Hafidsyah",
                username = "id bintang",
                password = PasswordUtil.hashPassword("alokepep098"),
                role = UserRole.ADMIN.name,
                nis = "",
                kelas = "XI RPL 1",
                noHp = "081299887766",
                createdAt = now
            )
        )
    }

    fun initialBarang(): List<BarangEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            BarangEntity(
                id = 1L,
                kodeBarang = "BRG-001",
                namaBarang = "Laptop ASUS X412",
                kategori = KategoriBarang.ELEKTRONIK.name,
                stokTotal = 10,
                stokTersedia = 8,
                kondisi = KondisiBarang.BAIK.label,
                lokasiRak = "Rak A - Lemari 1",
                deskripsi = "Laptop ASUS X412RAM 8GB, SSD 256GB, Display 14 inci",
                fotoUri = null,
                createdAt = now
            ),
            BarangEntity(
                id = 2L,
                kodeBarang = "BRG-002",
                namaBarang = "Proyektor Epson X51",
                kategori = KategoriBarang.ELEKTRONIK.name,
                stokTotal = 3,
                stokTersedia = 3,
                kondisi = KondisiBarang.BAIK.label,
                lokasiRak = "Rak A - Lemari 1",
                deskripsi = "Proyektor Epson X51, 3200 lumens, kabel HDMI dan VGA",
                fotoUri = null,
                createdAt = now
            ),
            BarangEntity(
                id = 3L,
                kodeBarang = "BRG-003",
                namaBarang = "Kamera DSLR Canon EOS 1500D",
                kategori = KategoriBarang.ELEKTRONIK.name,
                stokTotal = 4,
                stokTersedia = 4,
                kondisi = KondisiBarang.BAIK.label,
                lokasiRak = "Rak A - Lemari 2",
                deskripsi = "Kamera DSLR Canon EOS 1500D, lensa kit 18-55mm, tas kamera",
                fotoUri = null,
                createdAt = now
            ),
            BarangEntity(
                id = 4L,
                kodeBarang = "BRG-004",
                namaBarang = "Kursi Lipat",
                kategori = KategoriBarang.PERALATAN_RUANGAN.name,
                stokTotal = 50,
                stokTersedia = 50,
                kondisi = KondisiBarang.BAIK.label,
                lokasiRak = "Gudang - Lemari 1",
                deskripsi = "Kursi lipat kayu untuk kegiatan sekolah",
                fotoUri = null,
                createdAt = now
            ),
            BarangEntity(
                id = 5L,
                kodeBarang = "BRG-005",
                namaBarang = "Gitar Akustik",
                kategori = KategoriBarang.MUSIK.name,
                stokTotal = 6,
                stokTersedia = 6,
                kondisi = KondisiBarang.BAIK.label,
                lokasiRak = "Rak B - Lemari 2",
                deskripsi = "Gitar akustik 6 senar, tas gitar, kapo, dan picks",
                fotoUri = null,
                createdAt = now
            ),
            BarangEntity(
                id = 6L,
                kodeBarang = "BRG-006",
                namaBarang = "Buku Atlas Geografi",
                kategori = KategoriBarang.PERLENGKAPAN_DIDAKTIF.name,
                stokTotal = 20,
                stokTersedia = 20,
                kondisi = KondisiBarang.BAIK.label,
                lokasiRak = "Rak C - Lemari 1",
                deskripsi = "Atlas Geografi SMP/MTs kurikulum 2013",
                fotoUri = null,
                createdAt = now
            ),
            BarangEntity(
                id = 7L,
                kodeBarang = "BRG-007",
                namaBarang = "Set Alat Kebersihan",
                kategori = KategoriBarang.PERALATAN_RUANGAN.name,
                stokTotal = 12,
                stokTersedia = 12,
                kondisi = KondisiBarang.BAIK.label,
                lokasiRak = "Gudang - Lemari 1",
                deskripsi = "Set sapu, pel, gayung, dan ember",
                fotoUri = null,
                createdAt = now
            ),
            BarangEntity(
                id = 8L,
                kodeBarang = "BRG-008",
                namaBarang = "Toolkit Dasar",
                kategori = KategoriBarang.ELEKTRONIK.name,
                stokTotal = 8,
                stokTersedia = 8,
                kondisi = KondisiBarang.BAIK.label,
                lokasiRak = "Rak A - Lemari 2",
                deskripsi = "Set obeng, tang, meteran, dan kunci",
                fotoUri = null,
                createdAt = now
            ),
            BarangEntity(
                id = 9L,
                kodeBarang = "BRG-009",
                namaBarang = "Buku Matematika",
                kategori = KategoriBarang.PERLENGKAPAN_DIDAKTIF.name,
                stokTotal = 30,
                stokTersedia = 30,
                kondisi = KondisiBarang.BAIK.label,
                lokasiRak = "Rak C - Lemari 2",
                deskripsi = "Buku Matematika kelas XI IPA",
                fotoUri = null,
                createdAt = now
            ),
            BarangEntity(
                id = 10L,
                kodeBarang = "BRG-010",
                namaBarang = "Set Papan Tulis",
                kategori = KategoriBarang.PERALATAN_RUANGAN.name,
                stokTotal = 15,
                stokTersedia = 15,
                kondisi = KondisiBarang.BAIK.label,
                lokasiRak = "Gudang - Lemari 1",
                deskripsi = "Set penghapus dan kapur papan tulis",
                fotoUri = null,
                createdAt = now
            )
        )
    }

    fun initialPeminjaman(): List<PeminjamanEntity> {
        val now = System.currentTimeMillis()
        val satuHari = 24L * 60L * 60L * 1000L
        return listOf(
            // 1. Peminjaman aktif hari ini, kembali besok
            PeminjamanEntity(
                id = 1L,
                kodePeminjaman = "PJM-2026-001",
                barangId = 1L, // Laptop ASUS
                userId = 3L,   // Rizky Pratama
                petugasId = 1L,
                jumlah = 1,
                tanggalPinjam = now - 1 * satuHari,
                tanggalKembaliRencana = now + 1 * satuHari,
                tanggalKembaliAktual = null,
                keperluan = "Presentasi kelas XI IPA 1",
                status = StatusPeminjaman.DIPINJAM.label,
                denda = 0L,
                catatanPengembalian = ""
            ),
            // 2. Peminjaman aktif sudah lewat jadwal kembali (terlambat 1 hari)
            PeminjamanEntity(
                id = 2L,
                kodePeminjaman = "PJM-2026-002",
                barangId = 1L, // Laptop ASUS
                userId = 4L,   // Aisyah Putri
                petugasId = 1L,
                jumlah = 1,
                tanggalPinjam = now - 2 * satuHari,
                tanggalKembaliRencana = now - 1 * satuHari,
                tanggalKembaliAktual = null,
                keperluan = "Mengerjakan tugas kelompok",
                status = StatusPeminjaman.DIPINJAM.label,
                denda = 0L,
                catatanPengembalian = ""
            ),
            // 3. Peminjaman aktif hari ini, kembali 3 hari lagi
            PeminjamanEntity(
                id = 3L,
                kodePeminjaman = "PJM-2026-003",
                barangId = 2L, // Proyektor Epson
                userId = 5L,   // Dimas Aryo
                petugasId = 2L,
                jumlah = 1,
                tanggalPinjam = now - 1 * satuHari,
                tanggalKembaliRencana = now + 2 * satuHari,
                tanggalKembaliAktual = null,
                keperluan = "Presentasi kelas XI IPS 1",
                status = StatusPeminjaman.DIPINJAM.label,
                denda = 0L,
                catatanPengembalian = ""
            ),
            // 4. Peminjaman aktif, kembali besok
            PeminjamanEntity(
                id = 4L,
                kodePeminjaman = "PJM-2026-004",
                barangId = 5L, // Gitar Akustik
                userId = 6L,   // Nabila Zahra
                petugasId = 2L,
                jumlah = 2,
                tanggalPinjam = now - 1 * satuHari,
                tanggalKembaliRencana = now + 1 * satuHari,
                tanggalKembaliAktual = null,
                keperluan = "Latihan musik untuk acara sekolah",
                status = StatusPeminjaman.DIPINJAM.label,
                denda = 0L,
                catatanPengembalian = ""
            ),
            // 5. Sudah dikembalikan (Laptop)
            PeminjamanEntity(
                id = 5L,
                kodePeminjaman = "PJM-2026-005",
                barangId = 1L,
                userId = 3L,   // Rizky Pratama
                petugasId = 1L,
                jumlah = 1,
                tanggalPinjam = now - 5 * satuHari,
                tanggalKembaliRencana = now - 3 * satuHari,
                tanggalKembaliAktual = now - 3 * satuHari,
                keperluan = "Presentasi kelas XI IPA 1",
                status = StatusPeminjaman.DIKEMBALIKAN.label,
                denda = 0L,
                catatanPengembalian = "Kondisi baik, lengkap dengan charger"
            ),
            // 6. Sudah dikembalikan (Kamera)
            PeminjamanEntity(
                id = 6L,
                kodePeminjaman = "PJM-2026-006",
                barangId = 3L, // Kamera DSLR
                userId = 4L,   // Aisyah Putri
                petugasId = 1L,
                jumlah = 1,
                tanggalPinjam = now - 5 * satuHari,
                tanggalKembaliRencana = now - 4 * satuHari,
                tanggalKembaliAktual = now - 4 * satuHari,
                keperluan = "Praktikum jaringan tumbuhan Biologi",
                status = StatusPeminjaman.DIKEMBALIKAN.label,
                denda = 0L,
                catatanPengembalian = "Kondisi lengkap dan bersih"
            ),
            // 7. Sudah dikembalikan terlambat 1 hari (denda Rp 1.000)
            PeminjamanEntity(
                id = 7L,
                kodePeminjaman = "PJM-2026-007",
                barangId = 5L, // Gitar Akustik
                userId = 5L,   // Dimas Aryo
                petugasId = 1L,
                jumlah = 1,
                tanggalPinjam = now - 4 * satuHari,
                tanggalKembaliRencana = now - 2 * satuHari,
                tanggalKembaliAktual = now - 1 * satuHari,
                keperluan = "Latihan paduan suara upacara Senin",
                status = StatusPeminjaman.DIKEMBALIKAN.label,
                denda = 1000L,
                catatanPengembalian = "Senar baik, terlambat 1 hari"
            ),
            // 8. Sudah dikembalikan (Buku Atlas)
            PeminjamanEntity(
                id = 8L,
                kodePeminjaman = "PJM-2026-008",
                barangId = 6L,
                userId = 3L,   // Rizky Pratama
                petugasId = 2L,
                jumlah = 4,
                tanggalPinjam = now - 3 * satuHari,
                tanggalKembaliRencana = now - 1 * satuHari,
                tanggalKembaliAktual = now - 1 * satuHari,
                keperluan = "Kerja kelompok Geografi kelas XI",
                status = StatusPeminjaman.DIKEMBALIKAN.label,
                denda = 0L,
                catatanPengembalian = "Semua buku utuh"
            )
        )
    }
}
