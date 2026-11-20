package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Antrean perubahan data inventaris (outbox) untuk disinkronkan oleh WorkManager saat jaringan tersedia.
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val entityType: String,      // BARANG atau PEMINJAMAN
    val entityId: Long,
    val kodeReferensi: String,
    val operation: String,       // CREATE, UPDATE, DELETE
    val payloadJson: String,
    val status: String = "PENDING", // PENDING, IN_PROGRESS, SYNCED, FAILED
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Riwayat hasil eksekusi sinkronisasi latar belakang.
 */
@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String,          // SUCCESS, FAILED, SKIPPED
    val itemsSynced: Int,
    val serverUrl: String,
    val message: String
)
