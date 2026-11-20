package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SyncLogEntity
import com.example.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO untuk mengelola antrean sinkronisasi (outbox) dan riwayat log sinkronisasi.
 */
@Dao
interface SyncDao {
    @Query("SELECT * FROM sync_queue WHERE status != 'SYNCED' ORDER BY createdAt ASC")
    fun observePendingQueue(): Flow<List<SyncQueueEntity>>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status != 'SYNCED'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM sync_queue WHERE status != 'SYNCED' ORDER BY createdAt ASC")
    suspend fun getPendingItems(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE entityType = :entityType AND status != 'SYNCED' ORDER BY createdAt ASC")
    suspend fun getPendingItemsByType(entityType: String): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT 20")
    fun observeRecentLogs(): Flow<List<SyncLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueSyncTask(task: SyncQueueEntity): Long

    @Query("UPDATE sync_queue SET status = 'IN_PROGRESS', updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun markAsInProgress(ids: List<Long>, updatedAt: Long)

    @Query("UPDATE sync_queue SET status = 'SYNCED', errorMessage = NULL, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>, updatedAt: Long)

    @Query("UPDATE sync_queue SET status = 'FAILED', retryCount = retryCount + 1, errorMessage = :error, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun markAsFailed(ids: List<Long>, error: String, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SyncLogEntity)

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun clearSyncedQueue()

    @Query("DELETE FROM sync_queue")
    suspend fun clearAllQueue()
}
