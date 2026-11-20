package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.BarangEntity
import com.example.data.local.entity.PeminjamanEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Relasi gabungan transaksi peminjaman dengan barang dan pengguna peminjam.
 */
data class PeminjamanWithRelations(
    @Embedded val peminjaman: PeminjamanEntity,
    @Relation(
        parentColumn = "barangId",
        entityColumn = "id"
    )
    val barang: BarangEntity?,
    @Relation(
        parentColumn = "userId",
        entityColumn = "id"
    )
    val peminjam: UserEntity?
)

/**
 * DAO untuk mengelola transaksi simpan pinjam barang inventaris sekolah.
 */
@Dao
interface PeminjamanDao {
    @Transaction
    @Query("SELECT * FROM peminjaman ORDER BY tanggalPinjam DESC")
    fun getAllPeminjamanWithDetail(): Flow<List<PeminjamanWithRelations>>

    @Query("SELECT * FROM peminjaman ORDER BY id ASC")
    suspend fun getAllPeminjamanSnapshot(): List<PeminjamanEntity>

    @Transaction
    @Query("SELECT * FROM peminjaman WHERE barangId = :barangId ORDER BY tanggalPinjam DESC")
    fun getPeminjamanByBarang(barangId: Long): Flow<List<PeminjamanWithRelations>>

    @Transaction
    @Query("SELECT * FROM peminjaman WHERE userId = :userId ORDER BY tanggalPinjam DESC")
    fun getPeminjamanByUser(userId: Long): Flow<List<PeminjamanWithRelations>>

    @Transaction
    @Query("SELECT * FROM peminjaman WHERE id = :id LIMIT 1")
    suspend fun getPeminjamanDetailById(id: Long): PeminjamanWithRelations?

    @Query("SELECT * FROM peminjaman WHERE id = :id LIMIT 1")
    suspend fun getPeminjamanById(id: Long): PeminjamanEntity?

    @Query("SELECT * FROM peminjaman WHERE kodePeminjaman = :kode LIMIT 1")
    suspend fun getPeminjamanByKode(kode: String): PeminjamanEntity?

    @Query("SELECT COUNT(*) FROM peminjaman")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPeminjaman(peminjaman: PeminjamanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(daftarPeminjaman: List<PeminjamanEntity>)

    @Update
    suspend fun updatePeminjaman(peminjaman: PeminjamanEntity)

    @Query("SELECT COUNT(*) FROM peminjaman WHERE syncStatus != 'SYNCED'")
    fun observePendingSyncCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM peminjaman WHERE syncStatus != 'SYNCED' ORDER BY tanggalPinjam ASC")
    suspend fun getPendingSyncPeminjaman(): List<PeminjamanWithRelations>

    @Query("UPDATE peminjaman SET syncStatus = 'SYNCED', lastSyncedAt = :syncedAt WHERE id IN (:ids)")
    suspend fun markPeminjamanSynced(ids: List<Long>, syncedAt: Long)

    @Query("UPDATE peminjaman SET syncStatus = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM peminjaman")
    suspend fun clearAll()
}
