package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.UserEntity
import com.example.utils.PasswordUtil
import kotlinx.coroutines.flow.Flow

/**
 * DAO untuk mengelola akun Admin, Petugas, dan Siswa.
 */
@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY nama ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY id ASC")
    suspend fun getAllUsersSnapshot(): List<UserEntity>

    @Query("SELECT * FROM users WHERE role = :role ORDER BY nama ASC")
    fun getUsersByRole(role: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeUserById(id: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    /**
     * Mencari user berdasarkan username/NIS dan role, lalu membandingkan hash password di kode Kotlin,
     * bukan langsung di SQL query ( lebih aman menghindari exposure password di query).
     */
    @Query("SELECT * FROM users WHERE (LOWER(username) = LOWER(:identifier) OR LOWER(nis) = LOWER(:identifier)) AND role = :role LIMIT 1")
    suspend fun findByUsernameOrNisAndRole(username: String, role: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) OR (nis != '' AND LOWER(nis) = LOWER(:username)) LIMIT 1")
    suspend fun findByUsernameOrNis(username: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET password = :hash WHERE id = :userId")
    suspend fun updatePassword(userId: Long, hash: String)

    @Query("UPDATE users SET fotoUri = :fotoUri WHERE id = :userId")
    suspend fun updateFoto(userId: Long, fotoUri: String?)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clearAll()

    /**
     * Login with password verification in Kotlin code (not in SQL).
     * Returns user if credentials are valid, null otherwise.
     */
    suspend fun login(identifier: String, password: String, role: String): UserEntity? {
        val user = findByUsernameOrNisAndRole(identifier, role)
        return if (user != null) {
            PasswordUtil.verifyPassword(password, user.password) ? user : null
        } else {
            null
        }
    }
}
