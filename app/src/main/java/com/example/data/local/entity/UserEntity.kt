package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entitas pengguna (Admin, Petugas, dan Siswa) di Room Database.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val nama: String,
    val username: String, // Username untuk Admin/Petugas atau NIS untuk Siswa
    val password: String,
    val role: String,     // ADMIN, PETUGAS, SISWA
    val nis: String = "",
    val kelas: String = "",
    val noHp: String = "",
    val fotoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
