package com.example.data.sync

import android.content.Context
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.datastore.SessionManager
import com.example.data.local.EduPinjamDatabase
import com.example.data.local.entity.SyncLogEntity
import com.example.data.remote.FirestoreInventoryRepository
import com.example.data.remote.InventorySyncClient
import com.example.data.remote.SyncPushResult
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Worker WorkManager untuk menyinkronkan antrean data inventaris dari Room ke Cloud Firestore
 * atau server REST secara otomatis di latar belakang ketika koneksi jaringan tersedia.
 */
class InventorySyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = EduPinjamDatabase.getInstance(applicationContext)
        val sessionManager = SessionManager(applicationContext)
        val syncClient = InventorySyncClient()
        val firestoreRepo = if (runCatching { Firebase.auth.currentUser != null }.getOrDefault(false)) {
            runCatching { FirestoreInventoryRepository(applicationContext) }.getOrNull()
        } else {
            null
        }

        runSync(database, sessionManager, syncClient, firestoreRepo)
    }

    companion object {
        const val KEY_SYNCED_COUNT = "synced_count"
        const val KEY_ERROR_MESSAGE = "error_message"

        /**
         * Logika inti sinkronisasi yang dapat diuji langsung di unit/integration test.
         */
        suspend fun runSync(
            database: EduPinjamDatabase,
            sessionManager: SessionManager,
            syncClient: InventorySyncClient,
            firestoreRepository: FirestoreInventoryRepository? = null
        ): Result {
            val syncDao = database.syncDao()
            val barangDao = database.barangDao()
            val now = System.currentTimeMillis()

            val pendingTasks = syncDao.getPendingItems()
            if (pendingTasks.isEmpty()) {
                syncDao.insertLog(
                    SyncLogEntity(
                        timestamp = now,
                        status = "SUCCESS",
                        itemsSynced = 0,
                        serverUrl = if (firestoreRepository != null) "Cloud Firestore" else sessionManager.sessionFlow.first().syncServerUrl,
                        message = "Semua data inventaris sudah mutakhir (0 antrean)."
                    )
                )
                sessionManager.setLastSyncTimestamp(now)
                return Result.success(workDataOf(KEY_SYNCED_COUNT to 0))
            }

            // Prioritaskan sinkronisasi ke Cloud Firestore jika pengguna telah login Google
            if (firestoreRepository != null) {
                val taskIds = pendingTasks.map { it.id }
                syncDao.markAsInProgress(taskIds, now)
                var syncedCount = 0
                val syncedBarangIds = mutableListOf<Long>()
                var lastFailure: String? = null

                val peminjamanDao = database.peminjamanDao()
                val syncedPeminjamanIds = mutableListOf<Long>()

                for (task in pendingTasks) {
                    when (task.entityType) {
                        "BARANG" -> {
                            val res: kotlin.Result<Unit> = if (task.operation == "DELETE") {
                                firestoreRepository.deleteBarang(task.entityId)
                            } else {
                                val entity = barangDao.getBarangById(task.entityId)
                                if (entity != null) {
                                    firestoreRepository.upsertBarang(entity)
                                } else {
                                    kotlin.Result.success(Unit)
                                }
                            }
                            if (res.isSuccess) {
                                syncedCount++
                                if (task.operation != "DELETE") {
                                    syncedBarangIds.add(task.entityId)
                                }
                            } else {
                                lastFailure = res.exceptionOrNull()?.localizedMessage ?: "Sinkronisasi Cloud Firestore gagal."
                            }
                        }
                        "PEMINJAMAN" -> {
                            val detail = peminjamanDao.getPeminjamanDetailById(task.entityId)
                            val barang = detail?.barang
                            val res: kotlin.Result<Unit> = if (detail != null && barang != null) {
                                firestoreRepository.upsertPeminjaman(detail.peminjaman, barang, detail.peminjam)
                            } else {
                                kotlin.Result.success(Unit)
                            }
                            if (res.isSuccess) {
                                syncedCount++
                                if (detail != null) {
                                    syncedPeminjamanIds.add(task.entityId)
                                }
                            } else {
                                lastFailure = res.exceptionOrNull()?.localizedMessage ?: "Sinkronisasi peminjaman Firestore gagal."
                            }
                        }
                        else -> syncedCount++
                    }
                }

                if (lastFailure == null) {
                    val finishedAt = System.currentTimeMillis()
                    database.withTransaction {
                        syncDao.markAsSynced(taskIds, finishedAt)
                        if (syncedBarangIds.isNotEmpty()) {
                            barangDao.markBarangSynced(syncedBarangIds.distinct(), finishedAt)
                        }
                        if (syncedPeminjamanIds.isNotEmpty()) {
                            peminjamanDao.markPeminjamanSynced(syncedPeminjamanIds.distinct(), finishedAt)
                        }
                        syncDao.insertLog(
                            SyncLogEntity(
                                timestamp = finishedAt,
                                status = "SUCCESS",
                                itemsSynced = syncedCount,
                                serverUrl = "Cloud Firestore",
                                message = "Cloud Firestore: $syncedCount perubahan berhasil disinkronkan."
                            )
                        )
                    }
                    sessionManager.setLastSyncTimestamp(finishedAt)
                    return Result.success(workDataOf(KEY_SYNCED_COUNT to syncedCount))
                } else {
                    val failedAt = System.currentTimeMillis()
                    syncDao.markAsFailed(taskIds, lastFailure, failedAt)
                    syncDao.insertLog(
                        SyncLogEntity(
                            timestamp = failedAt,
                            status = "FAILED",
                            itemsSynced = 0,
                            serverUrl = "Cloud Firestore",
                            message = lastFailure
                        )
                    )
                    return Result.retry()
                }
            }

            val serverUrl = sessionManager.sessionFlow.first().syncServerUrl.trim()
            if (serverUrl.isBlank()) {
                val msg = "URL Server Sinkronisasi belum diatur di Pengaturan (${pendingTasks.size} perubahan menunggu)."
                syncDao.insertLog(
                    SyncLogEntity(
                        timestamp = now,
                        status = "SKIPPED",
                        itemsSynced = 0,
                        serverUrl = "-",
                        message = msg
                    )
                )
                return Result.failure(workDataOf(KEY_ERROR_MESSAGE to msg))
            }

            val taskIds = pendingTasks.map { it.id }
            syncDao.markAsInProgress(taskIds, now)

            return when (val pushResult = syncClient.pushSyncBatch(serverUrl, pendingTasks)) {
                is SyncPushResult.Success -> {
                    val finishedAt = System.currentTimeMillis()
                    val barangIds = pendingTasks
                        .filter { it.entityType == "BARANG" }
                        .map { it.entityId }
                        .distinct()

                    database.withTransaction {
                        syncDao.markAsSynced(taskIds, finishedAt)
                        if (barangIds.isNotEmpty()) {
                            barangDao.markBarangSynced(barangIds, finishedAt)
                        }
                        syncDao.insertLog(
                            SyncLogEntity(
                                timestamp = finishedAt,
                                status = "SUCCESS",
                                itemsSynced = pushResult.syncedCount,
                                serverUrl = serverUrl,
                                message = pushResult.serverMessage
                            )
                        )
                    }
                    sessionManager.setLastSyncTimestamp(finishedAt)
                    Result.success(workDataOf(KEY_SYNCED_COUNT to pushResult.syncedCount))
                }

                is SyncPushResult.RetryableError -> {
                    val failedAt = System.currentTimeMillis()
                    syncDao.markAsFailed(taskIds, pushResult.reason, failedAt)
                    syncDao.insertLog(
                        SyncLogEntity(
                            timestamp = failedAt,
                            status = "FAILED",
                            itemsSynced = 0,
                            serverUrl = serverUrl,
                            message = pushResult.reason
                        )
                    )
                    Result.retry()
                }

                is SyncPushResult.PermanentError -> {
                    val failedAt = System.currentTimeMillis()
                    syncDao.markAsFailed(taskIds, pushResult.reason, failedAt)
                    syncDao.insertLog(
                        SyncLogEntity(
                            timestamp = failedAt,
                            status = "FAILED",
                            itemsSynced = 0,
                            serverUrl = serverUrl,
                            message = pushResult.reason
                        )
                    )
                    Result.failure(workDataOf(KEY_ERROR_MESSAGE to pushResult.reason))
                }
            }
        }
    }
}
