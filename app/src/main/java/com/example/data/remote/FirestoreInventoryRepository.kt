package com.example.data.remote

import android.content.Context
import androidx.room.withTransaction
import com.example.R
import com.example.data.local.EduPinjamDatabase
import com.example.data.local.entity.BarangEntity
import com.example.data.local.entity.PeminjamanEntity
import com.example.data.local.entity.UserEntity
import com.example.domain.model.PeminjamanDetail
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Repository Cloud Firestore berbasis database ID khusus aplikasi (`R.string.firestore_database_id`)
 * dengan isolasi data per pengguna (`/users/{userId}/...`).
 */
class FirestoreInventoryRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val validDocIdRegex = Regex("^[a-zA-Z0-9_\\-]+$")

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Pengguna wajib masuk dengan Google sebelum mengakses Cloud Firestore.")
    }

    private fun sanitizeDocId(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        require(cleaned.isNotBlank() && validDocIdRegex.matches(cleaned)) {
            "ID dokumen tidak valid."
        }
        return cleaned
    }

    /**
     * Mengamati daftar inventaris Cloud Firestore secara real-time untuk pengguna aktif.
     */
    fun observeBarang(userId: String): Flow<List<CloudBarang>> = flow {
        require(userId.isNotBlank()) { "User ID kosong." }
        val path = "users/$userId/barang"
        emitAll(
            db.collection("users").document(userId).collection("barang")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudBarang::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Mengamati daftar peminjaman Cloud Firestore secara real-time untuk pengguna aktif.
     */
    fun observePeminjaman(userId: String): Flow<List<CloudPeminjaman>> = flow {
        require(userId.isNotBlank()) { "User ID kosong." }
        val path = "users/$userId/peminjaman"
        emitAll(
            db.collection("users").document(userId).collection("peminjaman")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudPeminjaman::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Memastikan dokumen profil `/users/{userId}` dibuat atau diperbarui di Cloud Firestore.
     */
    suspend fun ensureUserProfile(
        nama: String,
        email: String?,
        nis: String? = null,
        kelas: String? = null,
        noHp: String? = null
    ): Result<Unit> {
        var docPath = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val snap = docRef.get().await()
            val cleanNama = nama.trim().take(80).ifBlank { "Pengguna EDU-PINJAM" }
            if (!snap.exists()) {
                @Suppress("UNCHECKED_CAST")
                val createPayload = mapOf(
                    "userId" to uid,
                    "nama" to cleanNama,
                    "email" to email?.trim()?.take(120)?.takeIf { it.isNotEmpty() },
                    "nis" to nis?.trim()?.take(40)?.takeIf { it.isNotEmpty() },
                    "kelas" to kelas?.trim()?.take(60)?.takeIf { it.isNotEmpty() },
                    "noHp" to noHp?.trim()?.take(30)?.takeIf { it.isNotEmpty() },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                docRef.set(createPayload).await()
            } else {
                @Suppress("UNCHECKED_CAST")
                val updatePayload = mapOf(
                    "nama" to cleanNama,
                    "email" to email?.trim()?.take(120)?.takeIf { it.isNotEmpty() },
                    "nis" to nis?.trim()?.take(40)?.takeIf { it.isNotEmpty() },
                    "kelas" to kelas?.trim()?.take(60)?.takeIf { it.isNotEmpty() },
                    "noHp" to noHp?.trim()?.take(30)?.takeIf { it.isNotEmpty() },
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Membuat atau memperbarui dokumen barang inventaris di `/users/{userId}/barang/{docId}`.
     */
    suspend fun upsertBarang(barang: BarangEntity): Result<Unit> {
        var docPath = "users/unauthenticated/barang"
        return try {
            val uid = requireUserId()
            val docId = sanitizeDocId("barang_${barang.id}")
            val docRef = db.collection("users").document(uid)
                .collection("barang").document(docId)
            docPath = docRef.path

            val cloudModel = CloudBarang(
                userId = uid,
                kodeBarang = barang.kodeBarang,
                namaBarang = barang.namaBarang,
                kategori = barang.kategori,
                stokTotal = barang.stokTotal.toLong(),
                stokTersedia = barang.stokTersedia.toLong(),
                kondisi = barang.kondisi,
                lokasiRak = barang.lokasiRak,
                deskripsi = barang.deskripsi.takeIf { it.isNotBlank() },
                fotoUri = barang.fotoUri?.takeIf { it.isNotBlank() }
            )

            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(cloudModel.toCreateMap(uid)).await()
            } else {
                docRef.update(cloudModel.toUpdateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Menghapus dokumen barang di Cloud Firestore berdasarkan ID Room.
     */
    suspend fun deleteBarang(barangId: Long): Result<Unit> {
        var docPath = "users/unauthenticated/barang"
        return try {
            val uid = requireUserId()
            val docId = sanitizeDocId("barang_$barangId")
            val docRef = db.collection("users").document(uid)
                .collection("barang").document(docId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Menyimpan atau memperbarui transaksi peminjaman ke `/users/{userId}/peminjaman/{docId}`.
     */
    suspend fun upsertPeminjaman(
        peminjaman: PeminjamanEntity,
        barang: BarangEntity,
        peminjam: UserEntity?
    ): Result<Unit> {
        var docPath = "users/unauthenticated/peminjaman"
        return try {
            val uid = requireUserId()
            val barangDocId = sanitizeDocId("barang_${barang.id}")
            val docId = sanitizeDocId("pinjam_${peminjaman.id}")

            // Pastikan dokumen barang induk tersedia di Firestore sesuai aturan exists()
            val barangRef = db.collection("users").document(uid)
                .collection("barang").document(barangDocId)
            if (!barangRef.get().await().exists()) {
                upsertBarang(barang).getOrThrow()
            }

            val docRef = db.collection("users").document(uid)
                .collection("peminjaman").document(docId)
            docPath = docRef.path

            val snap = docRef.get().await()
            if (!snap.exists()) {
                val model = CloudPeminjaman(
                    userId = uid,
                    kodePeminjaman = peminjaman.kodePeminjaman,
                    barangDocId = barangDocId,
                    kodeBarang = barang.kodeBarang,
                    namaBarang = barang.namaBarang,
                    peminjamNama = peminjam?.nama ?: "Siswa",
                    peminjamNis = peminjam?.nis?.takeIf { it.isNotBlank() },
                    peminjamKelas = peminjam?.kelas?.takeIf { it.isNotBlank() },
                    jumlah = peminjaman.jumlah.toLong(),
                    tanggalPinjam = peminjaman.tanggalPinjam,
                    tanggalKembaliRencana = peminjaman.tanggalKembaliRencana,
                    tanggalKembaliAktual = peminjaman.tanggalKembaliAktual,
                    status = peminjaman.status,
                    denda = peminjaman.denda,
                    kondisiKembali = if (peminjaman.status in listOf("Dikembalikan", "Rusak")) barang.kondisi.take(40) else null,
                    keperluan = peminjaman.keperluan.takeIf { it.isNotBlank() },
                    catatan = peminjaman.catatanPengembalian.takeIf { it.isNotBlank() },
                    fotoKondisiUri = peminjaman.fotoPengembalianUri?.takeIf { it.isNotBlank() }
                )
                docRef.set(model.toCreateMap(uid)).await()
            } else {
                val existingStatus = snap.getString("status") ?: "Dipinjam"
                if (existingStatus in listOf("Dipinjam", "Terlambat")) {
                    val targetStatus = if (peminjaman.status in listOf("Dipinjam", "Dikembalikan", "Terlambat", "Rusak")) {
                        peminjaman.status
                    } else {
                        "Dipinjam"
                    }
                    if (targetStatus in listOf("Dikembalikan", "Rusak")) {
                        @Suppress("UNCHECKED_CAST")
                        val returnUpdate = mapOf(
                            "status" to targetStatus,
                            "tanggalKembaliAktual" to (peminjaman.tanggalKembaliAktual ?: System.currentTimeMillis()),
                            "denda" to peminjaman.denda.coerceAtLeast(0L),
                            "kondisiKembali" to barang.kondisi.take(40).takeIf { it.isNotBlank() },
                            "catatan" to peminjaman.catatanPengembalian.take(300).takeIf { it.isNotBlank() },
                            "fotoKondisiUri" to peminjaman.fotoPengembalianUri?.take(500)?.takeIf { it.isNotBlank() },
                            "updatedAt" to FieldValue.serverTimestamp()
                        ).filterValues { it != null } as Map<String, Any>
                        docRef.update(returnUpdate).await()
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        val activeUpdate = mapOf(
                            "jumlah" to peminjaman.jumlah.toLong().coerceIn(1L, 10000L),
                            "tanggalKembaliRencana" to peminjaman.tanggalKembaliRencana.coerceAtLeast(1L),
                            "status" to targetStatus,
                            "keperluan" to peminjaman.keperluan.take(300).takeIf { it.isNotBlank() },
                            "catatan" to peminjaman.catatanPengembalian.take(300).takeIf { it.isNotBlank() },
                            "updatedAt" to FieldValue.serverTimestamp()
                        ).filterValues { it != null } as Map<String, Any>
                        docRef.update(activeUpdate).await()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Menyinkronkan seluruh data inventaris dan peminjaman dari Room ke Cloud Firestore.
     */
    suspend fun syncAllLocalToCloud(
        barangList: List<BarangEntity>,
        peminjamanDetails: List<PeminjamanDetail>
    ): Result<Int> {
        return try {
            val uid = requireUserId()
            val user = auth.currentUser
            ensureUserProfile(
                nama = user?.displayName ?: "Pengguna EDU-PINJAM",
                email = user?.email
            ).getOrThrow()

            var syncedCount = 0
            for (barang in barangList) {
                upsertBarang(barang).getOrThrow()
                syncedCount++
            }
            for (detail in peminjamanDetails) {
                val master = detail.barang ?: continue
                upsertPeminjaman(detail.peminjaman, master, detail.peminjam).getOrThrow()
                syncedCount++
            }
            Result.success(syncedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mengisi Cloud Firestore awal saat koleksi cloud pengguna masih kosong atau menarik item cloud baru ke Room.
     */
    suspend fun bootstrapCloudFromLocalOrPull(database: EduPinjamDatabase): Result<Int> {
        var listPath = "users/unauthenticated/barang"
        return try {
            val uid = requireUserId()
            val user = auth.currentUser
            ensureUserProfile(
                nama = user?.displayName ?: "Pengguna EDU-PINJAM",
                email = user?.email
            ).getOrThrow()

            val barangCol = db.collection("users").document(uid).collection("barang")
            listPath = barangCol.path
            val cloudSnap = barangCol.get().await()
            val cloudItems = cloudSnap.toObjects(
                CloudBarang::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )

            val barangDao = database.barangDao()
            val localItems = barangDao.getAllBarangSnapshot()

            if (cloudItems.isEmpty() && localItems.isNotEmpty()) {
                var pushed = 0
                for (local in localItems) {
                    upsertBarang(local).getOrThrow()
                    pushed++
                }
                val now = System.currentTimeMillis()
                if (localItems.isNotEmpty()) {
                    barangDao.markBarangSynced(localItems.map { it.id }, now)
                }
                return Result.success(pushed)
            }

            // Sinkronkan dokumen cloud yang belum ada secara lokal berdasarkan kodeBarang
            val localByCode = localItems.associateBy { it.kodeBarang.lowercase() }
            var pulled = 0
            database.withTransaction {
                for (cloud in cloudItems) {
                    val existing = localByCode[cloud.kodeBarang.lowercase()]
                    if (existing == null) {
                        barangDao.insertBarang(
                            BarangEntity(
                                kodeBarang = cloud.kodeBarang,
                                namaBarang = cloud.namaBarang,
                                kategori = cloud.kategori,
                                stokTotal = cloud.stokTotal.toInt(),
                                stokTersedia = cloud.stokTersedia.toInt(),
                                kondisi = cloud.kondisi,
                                lokasiRak = cloud.lokasiRak,
                                deskripsi = cloud.deskripsi ?: "",
                                fotoUri = cloud.fotoUri,
                                syncStatus = "SYNCED",
                                updatedAt = System.currentTimeMillis(),
                                lastSyncedAt = System.currentTimeMillis()
                            )
                        )
                        pulled++
                    }
                }
            }
            Result.success(pulled)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, listPath)
            Result.failure(e)
        }
    }
}
