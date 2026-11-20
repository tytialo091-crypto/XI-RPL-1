package com.example.data.sync

import android.content.Context
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.datastore.SessionManager
import com.example.data.local.EduPinjamDatabase
import com.example.data.local.entity.BarangEntity
import com.example.data.local.entity.PeminjamanEntity
import com.example.data.local.entity.SyncLogEntity
import com.example.data.local.entity.UserEntity
import com.example.data.remote.FirestoreInventoryRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Kontrak pengunggah transaksi peminjaman ke Cloud Firestore agar mudah diuji secara unit test.
 */
fun interface BorrowingCloudUploader {
    suspend fun uploadBorrowing(
        peminjaman: PeminjamanEntity,
        barang: BarangEntity,
        peminjam: UserEntity?
    ): kotlin.Result<Unit>
}

/**
 * Worker WorkManager khusus untuk mengunggah catatan peminjaman tertunda (`PENDING_SYNC`)
 * dari Room ke Cloud Firestore secara otomatis ketika perangkat kembali terhubung internet.
 */
class BorrowingSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = EduPinjamDatabase.getInstance(applicationContext)
        val sessionManager = SessionManager(applicationContext)

        val isSignedIn = runCatching { Firebase.auth.currentUser != null }.getOrDefault(false)
        if (!isSignedIn) {
            val hasPending = database.syncDao().getPendingItemsByType(ENTITY_TYPE_PEMINJAMAN).isNotEmpty() ||
                database.peminjamanDao().getPendingSyncPeminjaman().isNotEmpty()
            return@withContext if (hasPending) {
                database.syncDao().insertLog(
                    SyncLogEntity(
                        timestamp = System.currentTimeMillis(),
                        status = "SKIPPED",
                        itemsSynced = 0,
                        serverUrl = "Cloud Firestore",
                        message = "Menunggu login Google sebelum mengunggah catatan peminjaman tertunda."
                    )
                )
                Result.retry()
            } else {
                Result.success(workDataOf(KEY_UPLOADED_COUNT to 0))
            }
        }

        val firestoreRepo = runCatching { FirestoreInventoryRepository(applicationContext) }.getOrNull()
            ?: return@withContext Result.retry()

        runBorrowingSync(
            database = database,
            sessionManager = sessionManager,
            uploader = BorrowingCloudUploader { peminjaman, barang, peminjam ->
                firestoreRepo.upsertPeminjaman(peminjaman, barang, peminjam)
            }
        )
    }

    companion object {
        const val ENTITY_TYPE_PEMINJAMAN = "PEMINJAMAN"
        const val KEY_UPLOADED_COUNT = "uploaded_borrowing_count"
        const val KEY_ERROR_MESSAGE = "error_message"

        /**
         * Logika inti sinkronisasi catatan peminjaman tertunda yang dapat diuji langsung di Robolectric.
         */
        suspend fun runBorrowingSync(
            database: EduPinjamDatabase,
            sessionManager: SessionManager,
            uploader: BorrowingCloudUploader
        ): Result {
            val syncDao = database.syncDao()
            val peminjamanDao = database.peminjamanDao()
            val now = System.currentTimeMillis()

            val pendingQueue = syncDao.getPendingItemsByType(ENTITY_TYPE_PEMINJAMAN)
            val pendingLoans = peminjamanDao.getPendingSyncPeminjaman()

            val candidateIds = linkedSetOf<Long>().apply {
                pendingLoans.forEach { add(it.peminjaman.id) }
                pendingQueue.forEach { add(it.entityId) }
            }

            if (candidateIds.isEmpty()) {
                return Result.success(workDataOf(KEY_UPLOADED_COUNT to 0))
            }

            val allQueueIds = pendingQueue.map { it.id }
            if (allQueueIds.isNotEmpty()) {
                syncDao.markAsInProgress(allQueueIds, now)
            }

            val succeededQueueIds = mutableListOf<Long>()
            val failedQueueIds = mutableListOf<Long>()
            val uploadedBorrowingIds = mutableListOf<Long>()
            val failedBorrowingIds = mutableListOf<Long>()
            var lastError: String? = null

            for (borrowingId in candidateIds) {
                val queueIdsForRecord = pendingQueue.filter { it.entityId == borrowingId }.map { it.id }
                val detail = peminjamanDao.getPeminjamanDetailById(borrowingId)
                val barang = detail?.barang
                if (detail == null || barang == null) {
                    succeededQueueIds.addAll(queueIdsForRecord)
                    continue
                }

                val uploadResult = try {
                    uploader.uploadBorrowing(detail.peminjaman, barang, detail.peminjam)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    kotlin.Result.failure(e)
                }

                if (uploadResult.isSuccess) {
                    uploadedBorrowingIds.add(borrowingId)
                    succeededQueueIds.addAll(queueIdsForRecord)
                } else {
                    failedBorrowingIds.add(borrowingId)
                    failedQueueIds.addAll(queueIdsForRecord)
                    lastError = uploadResult.exceptionOrNull()?.localizedMessage
                        ?: "Gagal mengunggah catatan peminjaman ke Cloud Firestore."
                }
            }

            val finishedAt = System.currentTimeMillis()
            database.withTransaction {
                if (succeededQueueIds.isNotEmpty()) {
                    syncDao.markAsSynced(succeededQueueIds.distinct(), finishedAt)
                }
                if (uploadedBorrowingIds.isNotEmpty()) {
                    peminjamanDao.markPeminjamanSynced(uploadedBorrowingIds.distinct(), finishedAt)
                }
                if (failedQueueIds.isNotEmpty()) {
                    syncDao.markAsFailed(
                        failedQueueIds.distinct(),
                        lastError ?: "Gagal mengunggah peminjaman.",
                        finishedAt
                    )
                }
                failedBorrowingIds.distinct().forEach { id ->
                    peminjamanDao.updateSyncStatus(id, "FAILED", finishedAt)
                }

                if (lastError == null) {
                    syncDao.insertLog(
                        SyncLogEntity(
                            timestamp = finishedAt,
                            status = "SUCCESS",
                            itemsSynced = uploadedBorrowingIds.size,
                            serverUrl = "Cloud Firestore",
                            message = "Cloud Firestore: ${uploadedBorrowingIds.size} catatan peminjaman berhasil diunggah."
                        )
                    )
                } else {
                    syncDao.insertLog(
                        SyncLogEntity(
                            timestamp = finishedAt,
                            status = "FAILED",
                            itemsSynced = uploadedBorrowingIds.size,
                            serverUrl = "Cloud Firestore",
                            message = lastError
                        )
                    )
                }
            }

            return if (lastError == null) {
                sessionManager.setLastSyncTimestamp(finishedAt)
                Result.success(workDataOf(KEY_UPLOADED_COUNT to uploadedBorrowingIds.size))
            } else {
                Result.retry()
            }
        }
    }
}
