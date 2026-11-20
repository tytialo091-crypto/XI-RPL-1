package com.example.data.remote

import com.example.data.local.entity.SyncQueueEntity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Hasil pengiriman batch sinkronisasi ke server inventaris sekolah.
 */
sealed class SyncPushResult {
    data class Success(val syncedCount: Int, val serverMessage: String) : SyncPushResult()
    data class RetryableError(val reason: String) : SyncPushResult()
    data class PermanentError(val reason: String) : SyncPushResult()
}

/**
 * Klien HTTP berbasis OkHttp untuk mengirim batch perubahan data inventaris ke server REST.
 */
class InventorySyncClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun isValidServerUrl(url: String): Boolean {
        val trimmed = url.trim()
        return trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
    }

    /**
     * Mengirim daftar perubahan tertunda ke endpoint server dalam format JSON.
     */
    fun pushSyncBatch(serverUrl: String, tasks: List<SyncQueueEntity>): SyncPushResult {
        if (!isValidServerUrl(serverUrl)) {
            return SyncPushResult.PermanentError("Format URL server tidak valid (harus diawali http:// atau https://).")
        }

        val payload = JSONObject().apply {
            put("app", "EDU-PINJAM")
            put("sentAt", System.currentTimeMillis())
            put("totalChanges", tasks.size)
            val changesArr = JSONArray()
            tasks.forEach { task ->
                changesArr.put(
                    JSONObject().apply {
                        put("queueId", task.id)
                        put("entityType", task.entityType)
                        put("entityId", task.entityId)
                        put("kodeReferensi", task.kodeReferensi)
                        put("operation", task.operation)
                        put("createdAt", task.createdAt)
                        put("data", runCatching { JSONObject(task.payloadJson) }.getOrElse { task.payloadJson })
                    }
                )
            }
            put("changes", changesArr)
        }

        val request = Request.Builder()
            .url(serverUrl.trim())
            .post(payload.toString().toRequestBody(jsonMediaType))
            .addHeader("Accept", "application/json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                when {
                    response.isSuccessful -> {
                        SyncPushResult.Success(
                            syncedCount = tasks.size,
                            serverMessage = "HTTP ${response.code}: ${tasks.size} perubahan berhasil disinkronkan."
                        )
                    }
                    response.code in 500..599 || response.code == 408 || response.code == 429 -> {
                        SyncPushResult.RetryableError("Server sibuk/error (HTTP ${response.code}). Akan dicoba ulang otomatis.")
                    }
                    else -> {
                        SyncPushResult.PermanentError("Ditolak server (HTTP ${response.code}): ${bodyText.take(120)}")
                    }
                }
            }
        } catch (e: IOException) {
            SyncPushResult.RetryableError("Koneksi jaringan gagal: ${e.message ?: "Timeout"}")
        } catch (e: Exception) {
            SyncPushResult.PermanentError("Kesalahan tak terduga: ${e.message ?: "Unknown"}")
        }
    }
}
