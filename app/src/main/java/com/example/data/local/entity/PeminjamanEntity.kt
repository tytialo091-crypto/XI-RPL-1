package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entitas transaksi simpan pinjam barang inventaris sekolah.
 * Perbaikan: Tambahkan indeks unik pada kodePeminjaman (Bug 6) dan ubah onDelete dari CASCADE ke RESTRICT (Bug 4).
 */
@Entity(
    tableName = "peminjaman",
    foreignKeys = [
        ForeignKey(
            entity = BarangEntity::class,
            parentColumns = ["id"],
            childColumns = ["barangId"],
            onDelete = ForeignKey.RESTRICT  // PERBAIKAN: RESTRICT daripada CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.RESTRICT  // PERBAIKAN: RESTRICT daripada CASCADE
        ),
    ],
    indices = [
        Index(value = ["barangId"]),
        Index(value = ["userId"]),
        Index(value = ["kodePeminjaman"], unique = true)  // PERBAIKAN: Indeks unik pada kodePeminjaman
    ]
)
data class PeminjamanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val kodePeminjaman: String,
    val barangId: Long,
    val userId: Long,
    val petugasId: Long? = null,
    val jumlah: Int,
    val tanggalPinjam: Long,
    val tanggalKembaliRencana: Long,
    val tanggalKembaliAktual: Long? = null,
    val keperluan: String,
    val status: String, // Dipinjam, Dikembalikan, Terlambat, Rusak
    val denda: Long = 0L,
    val fotoPengembalianUri: String? = null,
    val catatanPengembalian: String = "",
    val syncStatus: String = "PENDING_SYNC", // PENDING_SYNC, SYNCED, FAILED
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long? = null
)
