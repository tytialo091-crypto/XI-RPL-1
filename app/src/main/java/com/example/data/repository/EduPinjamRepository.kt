package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.datastore.SessionManager
import com.example.data.datastore.SessionState
import com.example.data.local.EduPinjamDatabase
import com.example.data.local.SeedData
import com.example.data.local.entity.BarangEntity
import com.example.data.local.entity.PeminjamanEntity
import com.example.data.local.entity.UserEntity
import com.example.data.sync.DataSyncManager
import com.example.data.sync.DataSyncStatus
import com.example.domain.model.BahasaApp
import com.example.domain.model.InventoryStatusCounts
import com.example.domain.model.KondisiBarang
import com.example.domain.model.PeminjamanDetail
import com.example.domain.model.StatusPeminjaman
import com.example.domain.model.TemaTerang
import com.example.domain.model.UserRole
import com.example.utils.PasswordUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository utama EDU-PINJAM yang menjembatani Room Database, DataStore, DataSyncManager, dan ViewModel.
 */
class EduPinjamRepository(
    private val database: EduPinjamDatabase,
    private val sessionManager: SessionManager,
    private val dataSyncManager: DataSyncManager? = null
) {
    private val barangDao = database.barangDao()
    private val userDao = database.userDao()
    private val peminjamanDao = database.peminjamanDao()
    private val seedMutex = Mutex()
    private var seeded = false

    val sessionState: Flow<SessionState> = sessionManager.sessionFlow
    val syncStatus: Flow<DataSyncStatus> = dataSyncManager?.syncStatusFlow ?: flowOf(DataSyncStatus())

    val allBarang: Flow<List<BarangEntity>> = barangDao.getAllBarang()
    val inventoryStatusCounts: Flow<InventoryStatusCounts> = barangDao.observeInventoryStatusCounts()
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val allSiswa: Flow<List<UserEntity>> = userDao.getUsersByRole(UserRole.SISWA.name)
    val allPetugas: Flow<List<UserEntity>> = userDao.getUsersByRole(UserRole.PETUGAS.name)

    val allPeminjamanDetail: Flow<List<PeminjamanDetail>> = combine(
        peminjamanDao.getAllPeminjamanWithDetail(),
        sessionManager.sessionFlow
    ) { list, session ->
        val now = System.currentTimeMillis()
        list.map { rel ->
            PeminjamanDetail(
                peminjaman = rel.peminjaman,
                barang = rel.barang,
                peminjam = rel.peminjam,
                dendaPerHari = session.dendaPerHari,
                waktuSekarang = now
            )
        }
    }

    /**
     * Memastikan database terisi 15 data barang awal, akun demo, dan riwayat awal sekali saja.
     */
    suspend fun ensureSeeded() {
        if (seeded) return
        seedMutex.withLock {
            if (seeded) return
            database.withTransaction {
                if (userDao.countAll() == 0) {
                    userDao.insertAll(SeedData.initialUsers())
                }
                if (barangDao.countAll() == 0) {
                    barangDao.insertAll(SeedData.initialBarang())
                }
                if (peminjamanDao.countAll() == 0) {
                    peminjamanDao.insertAll(SeedData.initialPeminjaman())
                }
            }
            seeded = true
        }
    }

    // ==================== OTENTIKASI & PENGGUNA ====================

    suspend fun login(identifier: String, password: String, role: UserRole): Result<UserEntity> {
        ensureSeeded()
        val trimmedId = identifier.trim()
        val trimmedPass = password.trim()
        if (trimmedId.isEmpty() || trimmedPass.isEmpty()) {
            return Result.failure(IllegalArgumentException("Username/NIS dan kata sandi wajib diisi."))
        }
        val user = userDao.login(trimmedId, trimmedPass, role.name)
            ?: return Result.failure(IllegalArgumentException("Akun tidak ditemukan atau kata sandi salah untuk peran ${role.label}."))
        sessionManager.saveLoginSession(user.id, UserRole.fromString(user.role), user.nama)
        return Result.success(user)
    }

    /**
     * Menyimpan sesi lokal setelah pengguna berhasil autentikasi Google Sign-In.
     * Security: Kalau email tidak cocok dengan akun lokal, jangan beri peran apa pun.
     * - Untuk siswa: buat akun baru.
     * - Untuk Admin/Petugas: tolak (hanya bisa lewat login lokal).
     */
    suspend fun completeGoogleSession(
        displayName: String?,
        email: String?,
        role: UserRole
    ): Result<UserEntity> {
        ensureSeeded()
        val cleanName = displayName?.trim()?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore('@')?.takeIf { it.isNotBlank() }
            ?: "Pengguna Google"
        val identifier = email?.trim()?.takeIf { it.isNotBlank() } ?: cleanName

        val existingByIdentifier = userDao.findByUsernameOrNis(identifier)

        val matchedUser = when {
            existingByIdentifier != null && existingByIdentifier.role == role.name -> {
                existingByIdentifier
            }
            role == UserRole.SISWA -> {
                val newId = userDao.insertUser(
                    UserEntity(
                        nama = cleanName,
                        username = identifier,
                        password = PasswordUtil.hashPassword("google_omitted"),
                        role = UserRole.SISWA.name,
                        nis = identifier,
                        kelas = "Umum",
                        noHp = ""
                    )
                )
                userDao.getUserById(newId)!!
            }
            else -> {
                return Result.failure(
                    IllegalArgumentException(
                        "Akun Google dengan peran $role.label tidak didukung. " +
                        "Silakan login menggunakan akun lokal (username & password)."
                    )
                )
            }
        }

        sessionManager.saveLoginSession(matchedUser.id, UserRole.fromString(matchedUser.role), matchedUser.nama)
        return Result.success(matchedUser)
    }

    suspend fun registerSiswa(
        nama: String,
        nis: String,
        kelas: String,
        noHp: String,
        password: String
    ): Result<UserEntity> {
        ensureSeeded()
        if (nama.isBlank() || nis.isBlank() || kelas.isBlank() || noHp.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Semua kolom pendaftaran siswa wajib dilengkapi."))
        }
        val existing = userDao.findByUsernameOrNis(nis.trim())
        if (existing != null) {
            return Result.failure(IllegalStateException("NIS ${nis.trim()} sudah terdaftar sebelumnya."))
        }
        val hashedPass = PasswordUtil.hashPassword(password)
        val newUser = UserEntity(
            nama = nama.trim(),
            username = nis.trim(),
            password = hashedPass,
            role = UserRole.SISWA.name,
            nis = nis.trim(),
            kelas = kelas.trim(),
            noHp = noHp.trim()
        )
        val id = userDao.insertUser(newUser)
        val savedUser = newUser.copy(id = id)
        sessionManager.saveLoginSession(savedUser.id, UserRole.SISWA, savedUser.nama)
        return Result.success(savedUser)
    }

    suspend fun addPetugas(
        nama: String,
        username: String,
        noHp: String,
        password: String
    ): Result<Long> {
        ensureSeeded()
        if (nama.isBlank() || username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama, username, dan kata sandi petugas wajib diisi."))
        }
        if (userDao.findByUsernameOrNis(username.trim()) != null) {
            return Result.failure(IllegalStateException("Username ${username.trim()} sudah digunakan."))
        }
        val hashedPass = PasswordUtil.hashPassword(password)
        val id = userDao.insertUser(
            UserEntity(
                nama = nama.trim(),
                username = username.trim(),
                password = hashedPass,
                role = UserRole.PETUGAS.name,
                kelas = "Petugas Inventaris",
                noHp = noHp.trim()
            )
        )
        return Result.success(id)
    }

    suspend fun deleteUser(user: UserEntity): Result<Unit> {
        if (user.role == UserRole.ADMIN.name) {
            return Result.failure(IllegalStateException("Akun Admin utama tidak dapat dihapus."))
        }
        userDao.deleteUser(user)
        return Result.success(Unit)
    }

    suspend fun updatePassword(userId: Long, oldPass: String, newPass: String): Result<Unit> {
        val user = userDao.getUserById(userId)
            ?: return Result.failure(IllegalStateException("Pengguna tidak ditemukan."))
        val trimmedOld = oldPass.trim()
        val trimmedNew = newPass.trim()
        if (!PasswordUtil.verifyPassword(trimmedOld, user.password)) {
            return Result.failure(IllegalArgumentException("Kata sandi lama tidak sesuai."))
        }
        if (trimmedNew.length < 3) {
            return Result.failure(IllegalArgumentException("Kata sandi baru minimal 3 karakter."))
        }
        userDao.updatePassword(userId, PasswordUtil.hashPassword(trimmedNew))
        return Result.success(Unit)
    }

    suspend fun updateProfilePhoto(userId: Long, fotoUri: String?) {
        userDao.updateFoto(userId, fotoUri)
    }

    fun observeUser(userId: Long): Flow<UserEntity?> = userDao.observeUserById(userId)

    suspend fun logout() {
        sessionManager.clearSession()
    }

    // ==================== INVENTARIS BARANG ====================

    fun observeBarangById(barangId: Long): Flow<BarangEntity?> = barangDao.observeBarangById(barangId)

    suspend fun getBarangByKode(kode: String): BarangEntity? {
        ensureSeeded()
        return barangDao.getBarangByKode(kode.trim())
    }

    fun observePeminjamanByBarang(barangId: Long): Flow<List<PeminjamanDetail>> =
        combine(
            peminjamanDao.getPeminjamanByBarang(barangId),
            sessionManager.sessionFlow
        ) { list, session ->
            val now = System.currentTimeMillis()
            list.map { rel ->
                PeminjamanDetail(rel.peminjaman, rel.barang, rel.peminjam, session.dendaPerHari, now)
            }
        }

    fun observePeminjamanByUser(userId: Long): Flow<List<PeminjamanDetail>> =
        combine(
            peminjamanDao.getPeminjamanByUser(userId),
            sessionManager.sessionFlow
        ) { list, session ->
            val now = System.currentTimeMillis()
            list.map { rel ->
                PeminjamanDetail(rel.peminjaman, rel.barang, rel.peminjam, session.dendaPerHari, now)
            }
        }

    suspend fun saveBarang(barang: BarangEntity): Result<Long> {
        ensureSeeded()
        if (barang.namaBarang.isBlank() || barang.kodeBarang.isBlank() || barang.lokasiRak.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama barang, kode barang, dan lokasi rak wajib diisi."))
        }
        if (barang.stokTotal < 0 || barang.stokTersedia < 0 || barang.stokTersedia > barang.stokTotal) {
            return Result.failure(IllegalArgumentException("Jumlah stok tersedia tidak boleh melebihi stok total."))
        }

        val duplicate = barangDao.getBarangByKode(barang.kodeBarang.trim())
        if (duplicate != null && duplicate.id != barang.id) {
            return Result.failure(IllegalStateException("Kode barang ${barang.kodeBarang} sudah digunakan barang lain."))
        }

        val now = System.currentTimeMillis()
        val cleanBarang = barang.copy(
            kodeBarang = barang.kodeBarang.trim().uppercase(Locale.getDefault()),
            namaBarang = barang.namaBarang.trim(),
            lokasiRak = barang.lokasiRak.trim(),
            deskripsi = barang.deskripsi.trim(),
            syncStatus = "PENDING_SYNC",
            updatedAt = now
        )
        return if (cleanBarang.id == 0L) {
            val id = barangDao.insertBarang(cleanBarang)
            dataSyncManager?.enqueueBarangSync(cleanBarang.copy(id = id), "CREATE")
            Result.success(id)
        } else {
            barangDao.updateBarang(cleanBarang)
            dataSyncManager?.enqueueBarangSync(cleanBarang, "UPDATE")
            Result.success(cleanBarang.id)
        }
    }

    suspend fun deleteBarang(barang: BarangEntity): Result<Unit> {
        ensureSeeded()
        barangDao.getBarangById(barang.id)?.let { current ->
            val activeLoans = peminjamanDao.getPeminjamanByBarang(current.id).first()
            if (activeLoans.any { it.peminjaman.status == "DIPINJAM" || it.peminjaman.status == "TERLAMBAT" }) {
                return Result.failure(IllegalStateException("Barang tidak dapat dihapus karena masih ada peminjaman aktif."))
            }
        }
        dataSyncManager?.enqueueBarangSync(barang, "DELETE")
        barangDao.deleteBarang(barang)
        return Result.success(Unit)
    }

    // ==================== TRANSAKSI SIMPAN PINJAM ====================

    /**
     * Memproses peminjaman barang dengan validasi stok otomatis dan pengurangan stok atomik.
     */
    suspend fun pinjamBarang(
        barangId: Long,
        siswaId: Long,
        petugasId: Long?,
        jumlah: Int,
        tanggalPinjam: Long,
        tanggalKembaliRencana: Long,
        keperluan: String
    ): Result<Long> {
        ensureSeeded()
        if (jumlah <= 0) {
            return Result.failure(IllegalArgumentException("Jumlah peminjaman minimal 1 unit."))
        }
        if (keperluan.isBlank()) {
            return Result.failure(IllegalArgumentException("Keperluan peminjaman wajib diisi."))
        }
        if (tanggalKembaliRencana < tanggalPinjam) {
            return Result.failure(IllegalArgumentException("Tanggal kembali tidak boleh lebih awal dari tanggal pinjam."))
        }

        return try {
            // Semua operasi dalam satu transaksi atomic
            val newId = database.withTransaction {
                val barang = barangDao.getBarangById(barangId)
                    ?: throw IllegalStateException("Data barang tidak ditemukan.")
                if (barang.stokTersedia <= 0) {
                    throw IllegalStateException("Stok ${barang.namaBarang} sedang habis (0 tersedia). Tidak dapat dipinjam.")
                }
                if (jumlah > barang.stokTersedia) {
                    throw IllegalStateException("Stok ${barang.namaBarang} tidak mencukupi (tersedia ${barang.stokTersedia} unit).")
                }

                // Kurangi stok atomik via DAO (cek stokTersedia >= jumlah di dalam query)
                val affectedRows = barangDao.kurangiStok(barangId, jumlah)
                if (affectedRows == 0) {
                    throw IllegalStateException("Gagal memperbarui stok barang.")
                }

                val dateTag = SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date())
                var kodePinjam: String? = null
                repeat(5) {
                    val randomSuffix = (100..999).random()
                    val candidate = "PJM-$dateTag-$randomSuffix"
                    // Cek bentrok sebelum insert, ulangi kalau duplikat
                    val exists = database.peminjamanDao().getPeminjamanByKode(candidate) != null
                    if (!exists) {
                        kodePinjam = candidate
                    }
                }

                val kode = kodePinjam ?: "PJM-$dateTag-${(100..999).random()}-${(100..999).random()}"
                val insertedId = database.peminjamanDao().insertPeminjaman(
                    PeminjamanEntity(
                        kodePeminjaman = kode,
                        barangId = barangId,
                        userId = siswaId,
                        petugasId = petugasId,
                        jumlah = jumlah,
                        tanggalPinjam = tanggalPinjam,
                        tanggalKembaliRencana = tanggalKembaliRencana,
                        keperluan = keperluan.trim(),
                        status = StatusPeminjaman.DIPINJAM.label,
                        denda = 0L
                    )
                )
                insertedId
            }
            // Update sync status setelah transaksi selesai
            barangDao.updateSyncStatus(barangId, "PENDING_SYNC")
            barangDao.getBarangById(barangId)?.let { updatedBarang ->
                dataSyncManager?.enqueueBarangSync(updatedBarang, "UPDATE")
            }
            database.peminjamanDao().getPeminjamanById(newId)?.let { savedLoan ->
                dataSyncManager?.enqueuePeminjamanSync(savedLoan, "CREATE")
            }
            Result.success(newId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Memproses pengembalian barang, menambah stok kembali, menghitung denda otomatis Rp 1.000/hari,
     * serta menyimpan foto bukti kondisi barang.
     */
    suspend fun kembalikanBarang(
        peminjamanId: Long,
        kondisiAkhir: KondisiBarang,
        fotoKondisiUri: String?,
        catatan: String
    ): Result<PeminjamanEntity> {
        ensureSeeded()
        return try {
            val updated = database.withTransaction {
                val existing = database.peminjamanDao().getPeminjamanById(peminjamanId)
                    ?: throw IllegalStateException("Transaksi peminjaman tidak ditemukan.")
                if (existing.status == StatusPeminjaman.DIKEMBALIKAN.label ||
                    existing.status == StatusPeminjaman.RUSAK.label
                ) {
                    throw IllegalStateException("Barang pada transaksi ini sudah dikembalikan.")
                }

                val now = System.currentTimeMillis()
                val dendaPerHari = sessionManager.sessionFlow.first().dendaPerHari
                val satuHariMillis = 24L * 60L * 60L * 1000L
                val selisih = now - existing.tanggalKembaliRencana
                val hariTerlambat = if (selisih > 0L) {
                    ((selisih + satuHariMillis - 1L) / satuHariMillis).coerceAtLeast(1L)
                } else {
                    0L
                }
                val totalDenda = hariTerlambat * dendaPerHari

                val statusAkhir = if (kondisiAkhir == KondisiBarang.RUSAK_BERAT || kondisiAkhir == KondisiBarang.RUSAK_RINGAN) {
                    StatusPeminjaman.RUSAK.label
                } else {
                    StatusPeminjaman.DIKEMBALIKAN.label
                }

                // Tambahkan stok kembali hanya jika kondisi akhir BAIK
                if (kondisiAkhir == KondisiBarang.BAIK) {
                    barangDao.tambahStok(existing.barangId, existing.jumlah)
                }

                // Jika kondisi barang berubah rusak, perbarui kondisi pada master barang
                if (kondisiAkhir != KondisiBarang.BAIK) {
                    barangDao.getBarangById(existing.barangId)?.let { master ->
                        barangDao.updateBarang(master.copy(kondisi = kondisiAkhir.label))
                    }
                }

                val updatedLoan = existing.copy(
                    tanggalKembaliAktual = now,
                    status = statusAkhir,
                    denda = totalDenda,
                    fotoPengembalianUri = fotoKondisiUri,
                    catatanPengembalian = catatan.ifBlank { "Kondisi saat kembali: ${kondisiAkhir.label}" },
                    syncStatus = "PENDING_SYNC",
                    updatedAt = now
                )
                database.peminjamanDao().updatePeminjaman(updatedLoan)
                updatedLoan
            }
            barangDao.getBarangById(updated.barangId)?.let { updatedBarang ->
                dataSyncManager?.enqueueBarangSync(updatedBarang, "UPDATE")
            }
            dataSyncManager?.enqueuePeminjamanSync(updated, "UPDATE")
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== SINKRONISASI LATAR BELAKANG ====================

    suspend fun triggerSyncNow() {
        ensureSeeded()
        val pending = database.syncDao().getPendingItems()
        if (pending.isEmpty()) {
            val unsynced = barangDao.getAllBarangSnapshot().filter { it.syncStatus != "SYNCED" }
            unsynced.forEach { item ->
                dataSyncManager?.enqueueBarangSync(item, "UPDATE")
            }
        }
        val queuedBorrowingIds = pending
            .filter { it.entityType == "PEMINJAMAN" }
            .map { it.entityId }
            .toSet()
        database.peminjamanDao().getPendingSyncPeminjaman()
            .filter { it.peminjaman.id !in queuedBorrowingIds }
            .forEach { rel ->
                dataSyncManager?.enqueuePeminjamanSync(rel.peminjaman, "CREATE")
            }
        val wifiOnly = sessionManager.sessionFlow.first().wifiOnlySync
        dataSyncManager?.enqueueImmediateSync(wifiOnly)
        dataSyncManager?.enqueuePendingBorrowingUpload(wifiOnly)
    }

    suspend fun updateSyncServerUrl(url: String) {
        dataSyncManager?.setSyncServerUrl(url) ?: sessionManager.setSyncServerUrl(url)
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        dataSyncManager?.setAutoSyncEnabled(enabled) ?: sessionManager.setAutoSyncEnabled(enabled)
    }

    suspend fun setWifiOnlySync(wifiOnly: Boolean) {
        dataSyncManager?.setWifiOnlySync(wifiOnly) ?: sessionManager.setWifiOnlySync(wifiOnly)
    }

    /**
     * Mengambil daftar peminjaman yang jatuh tempo besok (H-1) atau sudah lewat jatuh tempo.
     */
    suspend fun getLoansNeedingH1Reminder(): List<PeminjamanDetail> {
        ensureSeeded()
        val now = System.currentTimeMillis()
        val satuHari = 24L * 60L * 60L * 1000L
        val batasBesok = now + satuHari + (12L * 60L * 60L * 1000L)
        val all = allPeminjamanDetail.first()
        return all.filter { detail ->
            detail.masihAktif && detail.peminjaman.tanggalKembaliRencana <= batasBesok
        }
    }

    // ==================== PENGATURAN & BACKUP ====================

    suspend fun setTemaTerang(tema: TemaTerang) = sessionManager.setTemaTerang(tema)
    suspend fun setBahasa(bahasa: BahasaApp) = sessionManager.setBahasa(bahasa)
    suspend fun setReminderEnabled(enabled: Boolean) = sessionManager.setReminderEnabled(enabled)

    /**
     * Mengekspor seluruh data Room Database ke format JSON untuk backup lokal.
     */
    suspend fun exportBackupJson(): String {
        ensureSeeded()
        val users = userDao.getAllUsersSnapshot()
        val barang = barangDao.getAllBarangSnapshot()
        val peminjaman = peminjamanDao.getAllPeminjamanSnapshot()

        val root = JSONObject()
        root.put("app", "EDU-PINJAM")
        root.put("exportedAt", System.currentTimeMillis())

        val usersArr = JSONArray()
        users.forEach { u ->
            usersArr.put(JSONObject().apply {
                put("id", u.id)
                put("nama", u.nama)
                put("username", u.username)
                // SECURITY: Jangan pernah sertakan password/ hash di backup JSON
                put("role", u.role)
                put("nis", u.nis)
                put("kelas", u.kelas)
                put("noHp", u.noHp)
                put("fotoUri", u.fotoUri ?: "")
                put("createdAt", u.createdAt)
            })
        }
        root.put("users", usersArr)

        val barangArr = JSONArray()
        barang.forEach { b ->
            barangArr.put(JSONObject().apply {
                put("id", b.id)
                put("kodeBarang", b.kodeBarang)
                put("namaBarang", b.namaBarang)
                put("kategori", b.kategori)
                put("stokTotal", b.stokTotal)
                put("stokTersedia", b.stokTersedia)
                put("kondisi", b.kondisi)
                put("fotoUri", b.fotoUri ?: "")
                put("lokasiRak", b.lokasiRak)
                put("deskripsi", b.deskripsi)
                put("createdAt", b.createdAt)
            })
        }
        root.put("barang", barangArr)

        val pinjamArr = JSONArray()
        peminjaman.forEach { p ->
            pinjamArr.put(JSONObject().apply {
                put("id", p.id)
                put("kodePeminjaman", p.kodePeminjaman)
                put("barangId", p.barangId)
                put("userId", p.userId)
                put("petugasId", p.petugasId ?: -1L)
                put("jumlah", p.jumlah)
                put("tanggalPinjam", p.tanggalPinjam)
                put("tanggalKembaliRencana", p.tanggalKembaliRencana)
                put("tanggalKembaliAktual", p.tanggalKembaliAktual ?: -1L)
                put("keperluan", p.keperluan)
                put("status", p.status)
                put("denda", p.denda)
                put("fotoPengembalianUri", p.fotoPengembalianUri ?: "")
                put("catatanPengembalian", p.catatanPengembalian)
            })
        }
        root.put("peminjaman", pinjamArr)
        return root.toString(2)
    }

    /**
     * Memulihkan data dari file JSON backup.
     * Security: Batasi akses hanya untuk Admin. Jangan simpan/hash password di backup JSON.
     * clearAllQueue() dilakukan di dalam transaksi restore.
     */
    suspend fun restoreBackupJson(jsonString: String): Result<String> {
        // Batasi hanya Admin yang boleh restore
        val session = sessionManager.sessionFlow.first()
        if (session.role != UserRole.ADMIN.name) {
            return Result.failure(
                SecurityException("Hanya Admin yang dapat memulihkan data backup.")
            )
        }
        return try {
            val root = JSONObject(jsonString)
            if (root.optString("app") != "EDU-PINJAM") {
                return Result.failure(IllegalArgumentException("Format file backup bukan milik EDU-PINJAM."))
            }
            val usersArr = root.getJSONArray("users")
            val barangArr = root.getJSONArray("barang")
            val pinjamArr = root.getJSONArray("peminjaman")

            val users = buildList {
                for (i in 0 until usersArr.length()) {
                    val o = usersArr.getJSONObject(i)
                    add(
                        UserEntity(
                            id = o.getLong("id"),
                            nama = o.getString("nama"),
                            username = o.getString("username"),
                            // SECURITY: Backup JSON tidak menyimpan password/hash, gunakan hash default
                            password = PasswordUtil.hashPassword("restored_user"),
                            role = o.getString("role"),
                            nis = o.optString("nis", ""),
                            kelas = o.optString("kelas", ""),
                            noHp = o.optString("noHp", ""),
                            fotoUri = o.optString("fotoUri", "").ifBlank { null },
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val barang = buildList {
                for (i in 0 until barangArr.length()) {
                    val o = barangArr.getJSONObject(i)
                    add(
                        BarangEntity(
                            id = o.getLong("id"),
                            kodeBarang = o.getString("kodeBarang"),
                            namaBarang = o.getString("namaBarang"),
                            kategori = o.getString("kategori"),
                            stokTotal = o.getInt("stokTotal"),
                            stokTersedia = o.getInt("stokTersedia"),
                            kondisi = o.getString("kondisi"),
                            fotoUri = o.optString("fotoUri", "").ifBlank { null },
                            lokasiRak = o.getString("lokasiRak"),
                            deskripsi = o.optString("deskripsi", ""),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val peminjaman = buildList {
                for (i in 0 until pinjamArr.length()) {
                    val o = pinjamArr.getJSONObject(i)
                    val petugas = o.optLong("petugasId", -1L)
                    val kembaliAktual = o.optLong("tanggalKembaliAktual", -1L)
                    add(
                        PeminjamanEntity(
                            id = o.getLong("id"),
                            kodePeminjaman = o.getString("kodePeminjaman"),
                            barangId = o.getLong("barangId"),
                            userId = o.getLong("userId"),
                            petugasId = if (petugas > 0L) petugas else null,
                            jumlah = o.getInt("jumlah"),
                            tanggalPinjam = o.getLong("tanggalPinjam"),
                            tanggalKembaliRencana = o.getLong("tanggalKembaliRencana"),
                            tanggalKembaliAktual = if (kembaliAktual > 0L) kembaliAktual else null,
                            keperluan = o.getString("keperluan"),
                            status = o.getString("status"),
                            denda = o.optLong("denda", 0L),
                            fotoPengembalianUri = o.optString("fotoPengembalianUri", "").ifBlank { null },
                            catatanPengembalian = o.optString("catatanPengembalian", "")
                        )
                    )
                }
            }

            database.withTransaction {
                database.syncDao().clearAllQueue()
                peminjamanDao.clearAll()
                barangDao.clearAll()
                userDao.clearAll()
                userDao.insertAll(users)
                barangDao.insertAll(barang)
                peminjamanDao.insertAll(peminjaman)
            }
            Result.success("Berhasil memulihkan ${barang.size} barang, ${users.size} akun, dan ${peminjaman.size} transaksi.")
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Gagal membaca file backup: ${e.message}"))
        }
    }
}
