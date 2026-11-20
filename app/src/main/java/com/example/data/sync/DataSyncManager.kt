package com.example.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.data.datastore.SessionManager
import com.example.data.local.EduPinjamDatabase
import com.example.data.local.entity.BarangEntity
import com.example.data.local.entity.PeminjamanEntity
import com.example.data.local.entity.SyncLogEntity
import com.example.data.local.entity.SyncQueueEntity
import com.example.utils.NetworkMonitor
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Ringkasan status sinkronisasi Room + WorkManager + Cloud Firestore secara real-time untuk UI.
 */
data class DataSyncStatus(
    val isOnline: Boolean = false,
    val isCloudConnected: Boolean = false,
    val pendingCount: Int = 0,
    val pendingBorrowingCount: Int = 0,
    val unsyncedBarangCount: Int = 0,
    val workerState: WorkInfo.State? = null,
    val workerError: String? = null,
    val recentLogs: List<SyncLogEntity> = emptyList(),
    val autoSyncEnabled: Boolean = true,
    val wifiOnlySync: Boolean = false,
    val syncServerUrl: String = "",
    val lastSyncTimestamp: Long = 0L
) {
    val statusLabel: String
        get() = when {
            workerState == WorkInfo.State.RUNNING -> "Menyinkronkan ke Cloud..."
            !isOnline && (pendingCount > 0 || pendingBorrowingCount > 0) -> {
                val total = maxOf(pendingCount, pendingBorrowingCount)
                "Offline • $total menunggu jaringan"
            }
            !isOnline -> "Offline"
            pendingBorrowingCount > 0 && isCloudConnected -> "$pendingBorrowingCount peminjaman menunggu unggah"
            pendingCount > 0 && isCloudConnected && workerState == WorkInfo.State.ENQUEUED -> "Menunggu antrean Cloud ($pendingCount)"
            pendingCount > 0 && !isCloudConnected && syncServerUrl.isBlank() -> "$pendingCount menunggu Cloud / URL server"
            pendingCount > 0 && workerState == WorkInfo.State.ENQUEUED -> "Menunggu antrean ($pendingCount)"
            pendingCount > 0 -> "$pendingCount perubahan belum sinkron"
            isCloudConnected -> "Tersinkronisasi ke Cloud Firestore"
            else -> "Tersinkronisasi"
        }
}

/**
 * Manajer Sinkronisasi Data (Data Sync Manager) menggunakan Room dan WorkManager.
 * Mengantrekan perubahan inventaris dan catatan peminjaman ke Room outbox lalu menjadwalkan
 * WorkManager (`InventorySyncWorker` & `BorrowingSyncWorker`) dengan kendala jaringan
 * (`NetworkType.CONNECTED` / `UNMETERED`) agar otomatis terunggah saat koneksi kembali aktif.
 */
class DataSyncManager(
    context: Context,
    private val database: EduPinjamDatabase,
    private val sessionManager: SessionManager,
    val networkMonitor: NetworkMonitor
) {
    private val workManager: WorkManager = WorkManager.getInstance(context.applicationContext)
    private val syncDao = database.syncDao()
    private val barangDao = database.barangDao()
    private val peminjamanDao = database.peminjamanDao()

    companion object {
        const val UNIQUE_IMMEDIATE_SYNC = "edu_pinjam_immediate_inventory_sync"
        const val UNIQUE_PERIODIC_SYNC = "edu_pinjam_periodic_inventory_sync"
        const val UNIQUE_BORROWING_SYNC = "edu_pinjam_borrowing_firestore_sync"
        const val UNIQUE_PERIODIC_BORROWING_SYNC = "edu_pinjam_periodic_borrowing_sync"
        const val SYNC_WORK_TAG = "edu_pinjam_sync"
        const val BORROWING_SYNC_WORK_TAG = "edu_pinjam_borrowing_sync"
    }

    /**
     * Memantau gabungan status jaringan, antrean Room, peminjaman tertunda, status WorkManager, log, dan pengaturan.
     */
    val syncStatusFlow: Flow<DataSyncStatus> = combine(
        networkMonitor.isOnline,
        syncDao.observePendingCount(),
        peminjamanDao.observePendingSyncCount(),
        barangDao.observeUnsyncedCount(),
        workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_IMMEDIATE_SYNC),
        workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_BORROWING_SYNC),
        syncDao.observeRecentLogs(),
        sessionManager.sessionFlow
    ) { args ->
        val online = args[0] as Boolean
        val pendingCount = args[1] as Int
        val pendingBorrowingCount = args[2] as Int
        val unsyncedBarang = args[3] as Int
        @Suppress("UNCHECKED_CAST")
        val inventoryWorkInfos = args[4] as List<WorkInfo>
        @Suppress("UNCHECKED_CAST")
        val borrowingWorkInfos = args[5] as List<WorkInfo>
        @Suppress("UNCHECKED_CAST")
        val logs = args[6] as List<SyncLogEntity>
        val session = args[7] as com.example.data.datastore.SessionState

        val activeWork = borrowingWorkInfos.firstOrNull { it.state == WorkInfo.State.RUNNING }
            ?: inventoryWorkInfos.firstOrNull { it.state == WorkInfo.State.RUNNING }
            ?: borrowingWorkInfos.firstOrNull()
            ?: inventoryWorkInfos.firstOrNull()
        val cloudReady = runCatching {
            Firebase.auth.currentUser != null
        }.getOrDefault(false)

        DataSyncStatus(
            isOnline = online,
            isCloudConnected = cloudReady,
            pendingCount = pendingCount,
            pendingBorrowingCount = pendingBorrowingCount,
            unsyncedBarangCount = unsyncedBarang,
            workerState = activeWork?.state,
            workerError = activeWork?.outputData?.getString(BorrowingSyncWorker.KEY_ERROR_MESSAGE)
                ?: activeWork?.outputData?.getString(InventorySyncWorker.KEY_ERROR_MESSAGE),
            recentLogs = logs,
            autoSyncEnabled = session.autoSyncEnabled,
            wifiOnlySync = session.wifiOnlySync,
            syncServerUrl = session.syncServerUrl,
            lastSyncTimestamp = session.lastSyncTimestamp
        )
    }

    /**
     * Memantau perubahan konektivitas; setiap kali perangkat kembali online dan terdapat catatan peminjaman
     * atau antrean yang belum diunggah, langsung menjadwalkan WorkManager ke Firestore.
     */
    fun observeConnectivityAndTriggerPendingBorrowings(scope: CoroutineScope) {
        scope.launch {
            networkMonitor.isOnline
                .collect { online ->
                    if (online) {
                        val session = sessionManager.sessionFlow.first()
                        if (!session.autoSyncEnabled) return@collect

                        val hasPendingBorrowings = peminjamanDao.getPendingSyncPeminjaman().isNotEmpty() ||
                            syncDao.getPendingItemsByType(BorrowingSyncWorker.ENTITY_TYPE_PEMINJAMAN).isNotEmpty()
                        if (hasPendingBorrowings) {
                            enqueuePendingBorrowingUpload(wifiOnly = session.wifiOnlySync)
                        }

                        if (syncDao.getPendingItems().isNotEmpty()) {
                            enqueueImmediateSync(wifiOnly = session.wifiOnlySync)
                        }
                    }
                }
        }
    }

    /**
     * Memasukkan perubahan data barang ke antrean outbox Room dan memicu sinkronisasi latar belakang.
     */
    suspend fun enqueueBarangSync(
        barang: BarangEntity,
        operation: String
    ) {
        val payload = JSONObject().apply {
            put("id", barang.id)
            put("kodeBarang", barang.kodeBarang)
            put("namaBarang", barang.namaBarang)
            put("kategori", barang.kategori)
            put("stokTotal", barang.stokTotal)
            put("stokTersedia", barang.stokTersedia)
            put("kondisi", barang.kondisi)
            put("lokasiRak", barang.lokasiRak)
            put("deskripsi", barang.deskripsi)
            put("updatedAt", barang.updatedAt)
        }.toString()

        syncDao.enqueueSyncTask(
            SyncQueueEntity(
                entityType = "BARANG",
                entityId = barang.id,
                kodeReferensi = barang.kodeBarang,
                operation = operation,
                payloadJson = payload,
                status = "PENDING"
            )
        )

        val session = sessionManager.sessionFlow.first()
        if (session.autoSyncEnabled) {
            enqueueImmediateSync(wifiOnly = session.wifiOnlySync)
        }
    }

    /**
     * Memasukkan catatan peminjaman/pengembalian ke antrean outbox Room dan menjadwalkan
     * `BorrowingSyncWorker` agar otomatis terunggah ke Firestore saat internet tersedia.
     */
    suspend fun enqueuePeminjamanSync(
        peminjaman: PeminjamanEntity,
        operation: String
    ) {
        val payload = JSONObject().apply {
            put("id", peminjaman.id)
            put("kodePeminjaman", peminjaman.kodePeminjaman)
            put("barangId", peminjaman.barangId)
            put("userId", peminjaman.userId)
            put("jumlah", peminjaman.jumlah)
            put("status", peminjaman.status)
            put("tanggalPinjam", peminjaman.tanggalPinjam)
            put("tanggalKembaliRencana", peminjaman.tanggalKembaliRencana)
            put("tanggalKembaliAktual", peminjaman.tanggalKembaliAktual ?: 0L)
            put("denda", peminjaman.denda)
            put("updatedAt", peminjaman.updatedAt)
        }.toString()

        syncDao.enqueueSyncTask(
            SyncQueueEntity(
                entityType = BorrowingSyncWorker.ENTITY_TYPE_PEMINJAMAN,
                entityId = peminjaman.id,
                kodeReferensi = peminjaman.kodePeminjaman,
                operation = operation,
                payloadJson = payload,
                status = "PENDING"
            )
        )

        val session = sessionManager.sessionFlow.first()
        if (session.autoSyncEnabled) {
            enqueuePendingBorrowingUpload(wifiOnly = session.wifiOnlySync)
        }
    }

    /**
     * Menjadwalkan `BorrowingSyncWorker` satu kali untuk mengunggah semua catatan peminjaman tertunda
     * ke Cloud Firestore segera setelah koneksi internet tersedia.
     */
    fun enqueuePendingBorrowingUpload(wifiOnly: Boolean = false) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<BorrowingSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(BORROWING_SYNC_WORK_TAG)
            .build()

        workManager.enqueueUniqueWork(
            UNIQUE_BORROWING_SYNC,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * Menjadwalkan sinkronisasi satu kali yang otomatis berjalan segera setelah jaringan tersedia.
     */
    fun enqueueImmediateSync(wifiOnly: Boolean = false) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<InventorySyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(SYNC_WORK_TAG)
            .build()

        workManager.enqueueUniqueWork(
            UNIQUE_IMMEDIATE_SYNC,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * Mengatur penjadwalan sinkronisasi berkala setiap 15 menit untuk inventaris dan catatan peminjaman
     * saat perangkat terhubung jaringan.
     */
    fun configurePeriodicSync(enabled: Boolean, wifiOnly: Boolean) {
        if (!enabled) {
            workManager.cancelUniqueWork(UNIQUE_PERIODIC_SYNC)
            workManager.cancelUniqueWork(UNIQUE_PERIODIC_BORROWING_SYNC)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<InventorySyncWorker>(
            15,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(SYNC_WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_SYNC,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )

        val periodicBorrowingRequest = PeriodicWorkRequestBuilder<BorrowingSyncWorker>(
            15,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(BORROWING_SYNC_WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_BORROWING_SYNC,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicBorrowingRequest
        )
    }

    suspend fun setSyncServerUrl(url: String) {
        sessionManager.setSyncServerUrl(url)
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        sessionManager.setAutoSyncEnabled(enabled)
        val wifiOnly = sessionManager.sessionFlow.first().wifiOnlySync
        configurePeriodicSync(enabled, wifiOnly)
        if (enabled) {
            enqueueImmediateSync(wifiOnly)
            enqueuePendingBorrowingUpload(wifiOnly)
        }
    }

    suspend fun setWifiOnlySync(wifiOnly: Boolean) {
        sessionManager.setWifiOnlySync(wifiOnly)
        val autoSync = sessionManager.sessionFlow.first().autoSyncEnabled
        configurePeriodicSync(autoSync, wifiOnly)
    }
}
