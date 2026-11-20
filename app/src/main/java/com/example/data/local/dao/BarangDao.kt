package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.BarangEntity
import com.example.domain.model.InventoryStatusCounts
import kotlinx.coroutines.flow.Flow

/**
 * DAO untuk mengelola data barang inventaris sekolah.
 */
@Dao
interface BarangDao {
    @Query("SELECT * FROM barang ORDER BY namaBarang ASC")
    fun getAllBarang(): Flow<List<BarangEntity>>

    @Query(
        """
        SELECT
            COUNT(*) AS totalItems,
            COALESCE(SUM(CASE WHEN stokTersedia > 2 THEN 1 ELSE 0 END), 0) AS availableItems,
            COALESCE(SUM(CASE WHEN stokTersedia BETWEEN 1 AND 2 THEN 1 ELSE 0 END), 0) AS lowStockItems,
            COALESCE(SUM(CASE WHEN stokTersedia <= 0 THEN 1 ELSE 0 END), 0) AS outOfStockItems,
            COALESCE(SUM(stokTersedia), 0) AS totalUnitsAvailable,
            COALESCE(SUM(stokTotal), 0) AS totalUnits
        FROM barang
        """
    )
    fun observeInventoryStatusCounts(): Flow<InventoryStatusCounts>

    @Query("SELECT * FROM barang ORDER BY namaBarang ASC")
    suspend fun getAllBarangSnapshot(): List<BarangEntity>

    @Query("SELECT * FROM barang WHERE id = :id LIMIT 1")
    fun observeBarangById(id: Long): Flow<BarangEntity?>

    @Query("SELECT * FROM barang WHERE id = :id LIMIT 1")
    suspend fun getBarangById(id: Long): BarangEntity?

    @Query("SELECT * FROM barang WHERE LOWER(kodeBarang) = LOWER(:kode) LIMIT 1")
    suspend fun getBarangByKode(kode: String): BarangEntity?

    @Query("SELECT COUNT(*) FROM barang")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBarang(barang: BarangEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(daftarBarang: List<BarangEntity>)

    @Update
    suspend fun updateBarang(barang: BarangEntity)

    @Delete
    suspend fun deleteBarang(barang: BarangEntity)

    @Query("DELETE FROM barang WHERE id = :id")
    suspend fun deleteBarangById(id: Long)

    // Mengurangi stok tersedia secara atomik jika stok mencukupi
    @Query("UPDATE barang SET stokTersedia = stokTersedia - :jumlah WHERE id = :barangId AND stokTersedia >= :jumlah")
    suspend fun kurangiStok(barangId: Long, jumlah: Int): Int

    // Menambah kembali stok tersedia saat pengembalian barang
    @Query("UPDATE barang SET stokTersedia = MIN(stokTotal, stokTersedia + :jumlah), syncStatus = 'PENDING_SYNC', updatedAt = :updatedAt WHERE id = :barangId")
    suspend fun tambahStok(barangId: Long, jumlah: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE barang SET syncStatus = 'SYNCED', lastSyncedAt = :syncedAt WHERE id IN (:barangIds)")
    suspend fun markBarangSynced(barangIds: List<Long>, syncedAt: Long)

    @Query("UPDATE barang SET syncStatus = :status, updatedAt = :updatedAt WHERE id = :barangId")
    suspend fun updateSyncStatus(barangId: Long, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM barang WHERE syncStatus != 'SYNCED'")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("DELETE FROM barang")
    suspend fun clearAll()
}
