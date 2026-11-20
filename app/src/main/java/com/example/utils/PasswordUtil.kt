package com.example.utils

import androidx.annotation.VisibleForTesting
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException

/**
 * Utilitas keamanan kata sandi menggunakan PBKDF2 dengan SHA-256.
 * Menggunakan salt acak 16-byte dan 100.000 iterasi (OWASP recommended minimum).
 */
object PasswordUtil {

    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH = 256 // bits
    private const val SALT_LENGTH = 16 // bytes
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val DELIMITER = ":"

    /**
     * Menghasilkan hash PBKDF2 untuk kata sandi plaintext.
     * Format output: "saltBase64:hashBase64:iterations"
     */
    fun hashPassword(password: String): String {
        val salt = generateSalt()
        val hash = pbkdf2(password, salt, ITERATIONS)
        return "${android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP)}$DELIMITER${android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)}$DELIMITER$ITERATIONS"
    }

    /**
     * Memverifikasi kata sandi plaintext terhadap hash yang tersimpan.
     */
    fun verifyPassword(password: String, storedHash: String): Boolean {
        return try {
            val parts = storedHash.split(DELIMITER)
            if (parts.size != 3) return false
            val salt = android.util.Base64.decode(parts[0], android.util.Base64.NO_WRAP)
            val hash = android.util.Base64.decode(parts[1], android.util.Base64.NO_WRAP)
            val iterations = parts[2].toIntOrNull() ?: ITERATIONS
            val computedHash = pbkdf2(password, salt, iterations)
            constantTimeEquals(hash, computedHash)
        } catch (e: Exception) {
            false
        }
    }

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }

    private fun pbkdf2(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    /**
     * Perbandingan waktu konstan untuk mencegah timing attack.
     */
    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i] xor b[i])
        }
        return result == 0
    }

    /**
     * Menghasilkan hash standar untuk akun seed (password: "123").
     * Digunakan saat inisialisasi data awal agar konsisten.
     */
    @VisibleForTesting
    fun getDefaultSeedHash(): String {
        return hashPassword("123")
    }
}
