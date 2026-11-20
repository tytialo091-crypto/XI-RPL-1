package com.example.data.remote

import android.util.Log
import androidx.annotation.Keep
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

/**
 * Jenis operasi Firestore untuk pelaporan log terstruktur.
 */
enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

/**
 * Mencatat kesalahan Firestore ke Logcat dalam format JSON terstruktur sesuai standar keamanan.
 */
fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

/**
 * Model dokumen profil pengguna di `/users/{userId}`.
 */
@Keep
@IgnoreExtraProperties
data class CloudUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val nama: String = "",
    val email: String? = null,
    val nis: String? = null,
    val kelas: String? = null,
    val noHp: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

/**
 * Model dokumen barang inventaris di `/users/{userId}/barang/{barangId}`.
 */
@Keep
@IgnoreExtraProperties
data class CloudBarang(
    @DocumentId val id: String = "",
    val userId: String = "",
    val kodeBarang: String = "",
    val namaBarang: String = "",
    val kategori: String = "Lainnya",
    val stokTotal: Long = 0L,
    val stokTersedia: Long = 0L,
    val kondisi: String = "Baik",
    val lokasiRak: String = "",
    val deskripsi: String? = null,
    val fotoUri: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> {
        val validKondisi = if (kondisi in listOf("Baik", "Rusak Ringan", "Rusak Berat")) kondisi else "Baik"
        val total = stokTotal.coerceIn(0L, 100000L)
        val tersedia = stokTersedia.coerceIn(0L, total)
        @Suppress("UNCHECKED_CAST")
        return mapOf(
            "userId" to ownerUid,
            "kodeBarang" to kodeBarang.trim().take(50).ifBlank { "BRG-000" },
            "namaBarang" to namaBarang.trim().take(120).ifBlank { "Barang Sekolah" },
            "kategori" to kategori.trim().take(40).ifBlank { "Lainnya" },
            "stokTotal" to total,
            "stokTersedia" to tersedia,
            "kondisi" to validKondisi,
            "lokasiRak" to lokasiRak.trim().take(80).ifBlank { "Gudang Sekolah" },
            "deskripsi" to deskripsi?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
            "fotoUri" to fotoUri?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }

    fun toUpdateMap(): Map<String, Any> {
        val validKondisi = if (kondisi in listOf("Baik", "Rusak Ringan", "Rusak Berat")) kondisi else "Baik"
        val total = stokTotal.coerceIn(0L, 100000L)
        val tersedia = stokTersedia.coerceIn(0L, total)
        @Suppress("UNCHECKED_CAST")
        return mapOf(
            "kodeBarang" to kodeBarang.trim().take(50).ifBlank { "BRG-000" },
            "namaBarang" to namaBarang.trim().take(120).ifBlank { "Barang Sekolah" },
            "kategori" to kategori.trim().take(40).ifBlank { "Lainnya" },
            "stokTotal" to total,
            "stokTersedia" to tersedia,
            "kondisi" to validKondisi,
            "lokasiRak" to lokasiRak.trim().take(80).ifBlank { "Gudang Sekolah" },
            "deskripsi" to deskripsi?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
            "fotoUri" to fotoUri?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }
}

/**
 * Model dokumen transaksi peminjaman di `/users/{userId}/peminjaman/{peminjamanId}`.
 */
@Keep
@IgnoreExtraProperties
data class CloudPeminjaman(
    @DocumentId val id: String = "",
    val userId: String = "",
    val kodePeminjaman: String = "",
    val barangDocId: String = "",
    val kodeBarang: String = "",
    val namaBarang: String = "",
    val peminjamNama: String = "",
    val peminjamNis: String? = null,
    val peminjamKelas: String? = null,
    val jumlah: Long = 1L,
    val tanggalPinjam: Long = 0L,
    val tanggalKembaliRencana: Long = 0L,
    val tanggalKembaliAktual: Long? = null,
    val status: String = "Dipinjam",
    val denda: Long = 0L,
    val kondisiKembali: String? = null,
    val keperluan: String? = null,
    val catatan: String? = null,
    val fotoKondisiUri: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> {
        val validStatus = if (status in listOf("Dipinjam", "Dikembalikan", "Terlambat", "Rusak")) status else "Dipinjam"
        @Suppress("UNCHECKED_CAST")
        return mapOf(
            "userId" to ownerUid,
            "kodePeminjaman" to kodePeminjaman.trim().take(60).ifBlank { "PJM-001" },
            "barangDocId" to barangDocId,
            "kodeBarang" to kodeBarang.trim().take(50).ifBlank { "BRG-000" },
            "namaBarang" to namaBarang.trim().take(120).ifBlank { "Barang Sekolah" },
            "peminjamNama" to peminjamNama.trim().take(80).ifBlank { "Siswa" },
            "peminjamNis" to peminjamNis?.trim()?.take(40)?.takeIf { it.isNotEmpty() },
            "peminjamKelas" to peminjamKelas?.trim()?.take(60)?.takeIf { it.isNotEmpty() },
            "jumlah" to jumlah.coerceIn(1L, 10000L),
            "tanggalPinjam" to tanggalPinjam.coerceAtLeast(1L),
            "tanggalKembaliRencana" to tanggalKembaliRencana.coerceAtLeast(1L),
            "tanggalKembaliAktual" to tanggalKembaliAktual?.takeIf { it > 0L },
            "status" to validStatus,
            "denda" to denda.coerceIn(0L, 100_000_000L),
            "kondisiKembali" to kondisiKembali?.trim()?.take(40)?.takeIf { it.isNotEmpty() },
            "keperluan" to keperluan?.trim()?.take(300)?.takeIf { it.isNotEmpty() },
            "catatan" to catatan?.trim()?.take(300)?.takeIf { it.isNotEmpty() },
            "fotoKondisiUri" to fotoKondisiUri?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }
}
