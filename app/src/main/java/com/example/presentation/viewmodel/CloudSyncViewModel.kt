package com.example.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.EduPinjamDatabase
import com.example.data.local.entity.SyncLogEntity
import com.example.data.remote.CloudBarang
import com.example.data.remote.CloudPeminjaman
import com.example.data.remote.FirestoreInventoryRepository
import com.example.data.repository.EduPinjamRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State UI terpadu untuk observasi Cloud Firestore secara real-time.
 */
sealed interface CloudUiState<out T> {
    data object Loading : CloudUiState<Nothing>
    data class Success<T>(val data: T) : CloudUiState<T>
    data class Error(val message: String) : CloudUiState<Nothing>
}

data class CloudInventorySnapshot(
    val cloudBarang: List<CloudBarang> = emptyList(),
    val cloudPeminjaman: List<CloudPeminjaman> = emptyList()
)

/**
 * ViewModel Cloud Firestore yang di-key dengan `currentUserId` terautentikasi Google Sign-In.
 */
class CloudSyncViewModel(
    private val firestoreRepository: FirestoreInventoryRepository,
    private val localRepository: EduPinjamRepository,
    private val database: EduPinjamDatabase,
    private val currentUserId: String
) : ViewModel() {

    private val _isSyncingCloud = MutableStateFlow(false)
    val isSyncingCloud: StateFlow<Boolean> = _isSyncingCloud.asStateFlow()

    private val _cloudFeedback = MutableStateFlow<String?>(null)
    val cloudFeedback: StateFlow<String?> = _cloudFeedback.asStateFlow()

    val cloudState: StateFlow<CloudUiState<CloudInventorySnapshot>> = combine(
        firestoreRepository.observeBarang(currentUserId),
        firestoreRepository.observePeminjaman(currentUserId)
    ) { barangList, peminjamanList ->
        CloudInventorySnapshot(
            cloudBarang = barangList,
            cloudPeminjaman = peminjamanList
        )
    }
        .map<CloudInventorySnapshot, CloudUiState<CloudInventorySnapshot>> { CloudUiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Gagal mengamati Cloud Firestore", rootError)
            emit(CloudUiState.Error(rootError.message ?: "Gagal terhubung ke Cloud Firestore."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = CloudUiState.Loading
        )

    init {
        viewModelScope.launch {
            localRepository.ensureSeeded()
            firestoreRepository.bootstrapCloudFromLocalOrPull(database)
        }
    }

    fun syncNowToCloud() {
        viewModelScope.launch {
            _isSyncingCloud.value = true
            _cloudFeedback.value = null
            val barang = localRepository.allBarang.first()
            val loans = localRepository.allPeminjamanDetail.first()
            val result = firestoreRepository.syncAllLocalToCloud(barang, loans)
            result.fold(
                onSuccess = { count ->
                    val now = System.currentTimeMillis()
                    if (barang.isNotEmpty()) {
                        database.barangDao().markBarangSynced(barang.map { it.id }, now)
                    }
                    if (loans.isNotEmpty()) {
                        database.peminjamanDao().markPeminjamanSynced(loans.map { it.peminjaman.id }, now)
                    }
                    val pendingQueueIds = database.syncDao().getPendingItems().map { it.id }
                    if (pendingQueueIds.isNotEmpty()) {
                        database.syncDao().markAsSynced(pendingQueueIds, now)
                    }
                    database.syncDao().insertLog(
                        SyncLogEntity(
                            timestamp = now,
                            status = "SUCCESS",
                            itemsSynced = count,
                            serverUrl = "Cloud Firestore",
                            message = "$count dokumen berhasil disinkronkan ke Cloud Firestore."
                        )
                    )
                    _cloudFeedback.value = "$count data berhasil disinkronkan ke Cloud Firestore."
                },
                onFailure = { error ->
                    database.syncDao().insertLog(
                        SyncLogEntity(
                            timestamp = System.currentTimeMillis(),
                            status = "FAILED",
                            itemsSynced = 0,
                            serverUrl = "Cloud Firestore",
                            message = error.localizedMessage ?: "Sinkronisasi Cloud Firestore gagal."
                        )
                    )
                    _cloudFeedback.value = error.localizedMessage ?: "Gagal menyinkronkan ke Cloud Firestore."
                }
            )
            _isSyncingCloud.value = false
        }
    }

    fun clearCloudFeedback() {
        _cloudFeedback.value = null
    }

    private companion object {
        const val TAG = "CloudSyncVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
