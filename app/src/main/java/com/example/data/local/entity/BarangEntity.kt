package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entitas tabel barang inventaris sekolah di Room Database dengan metadata sinkronisasi.
 */
@Entity(
    tableName = "barang",
    indices = [Index(value = ["kodeBarang"], unique = true)]
)
data class BarangEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val kodeBarang: String,
    val namaBarang: String,
    val kategori: String,
    val stokTotal: Int,
    val stokTersedia: Int,
    val kondisi: String,
    val fotoUri: String? = null,
    val lokasiRak: String,
    val deskripsi: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING_SYNC", // PENDING_SYNC, SYNCED, FAILED
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long? = null
)
