package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.datastore.SessionState
import com.example.data.local.entity.UserEntity
import com.example.data.repository.EduPinjamRepository
import com.example.data.sync.DataSyncStatus
import com.example.domain.model.BahasaApp
import com.example.domain.model.PeminjamanDetail
import com.example.domain.model.TemaTerang
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State UI untuk halaman Profil, Kelola Petugas, Sinkronisasi Data, dan Pengaturan Aplikasi.
 */
data class ProfileUiState(
    val session: SessionState = SessionState(),
    val currentUser: UserEntity? = null,
    val myLoans: List<PeminjamanDetail> = emptyList(),
    val petugasList: List<UserEntity> = emptyList(),
    val syncStatus: DataSyncStatus = DataSyncStatus(),
    val successMessage: String? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel profil pengguna, kelola akun petugas, pengaturan tema/bahasa, backup/restore, dan sinkronisasi WorkManager.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val repository: EduPinjamRepository
) : ViewModel() {

    private val _feedback = MutableStateFlow<Pair<String?, String?>>(null to null)

    private val currentUserFlow = repository.sessionState.flatMapLatest { session ->
        if (session.userId > 0L) repository.observeUser(session.userId) else flowOf(null)
    }

    private val myLoansFlow = repository.sessionState.flatMapLatest { session ->
        if (session.userId > 0L) repository.observePeminjamanByUser(session.userId) else flowOf(emptyList())
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        repository.sessionState,
        currentUserFlow,
        myLoansFlow,
        repository.allPetugas,
        repository.syncStatus,
        _feedback
    ) { args ->
        val session = args[0] as SessionState
        val user = args[1] as UserEntity?
        @Suppress("UNCHECKED_CAST")
        val myLoans = args[2] as List<PeminjamanDetail>
        @Suppress("UNCHECKED_CAST")
        val petugas = args[3] as List<UserEntity>
        val syncStatus = args[4] as DataSyncStatus
        @Suppress("UNCHECKED_CAST")
        val feedback = args[5] as Pair<String?, String?>

        ProfileUiState(
            session = session,
            currentUser = user,
            myLoans = myLoans,
            petugasList = petugas,
            syncStatus = syncStatus,
            successMessage = feedback.first,
            errorMessage = feedback.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    fun updatePhoto(userId: Long, uri: String?) {
        viewModelScope.launch {
            repository.updateProfilePhoto(userId, uri)
            _feedback.value = "Foto profil berhasil diperbarui." to null
        }
    }

    fun addPetugas(nama: String, username: String, noHp: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repository.addPetugas(nama, username, noHp, password)
            result.fold(
                onSuccess = {
                    _feedback.value = "Akun petugas $nama berhasil ditambahkan." to null
                    onSuccess()
                },
                onFailure = { error ->
                    _feedback.value = null to (error.message ?: "Gagal menambahkan akun petugas.")
                }
            )
        }
    }

    fun deletePetugas(user: UserEntity) {
        viewModelScope.launch {
            val result = repository.deleteUser(user)
            result.fold(
                onSuccess = {
                    _feedback.value = "Akun petugas ${user.nama} telah dihapus." to null
                },
                onFailure = { error ->
                    _feedback.value = null to (error.message ?: "Gagal menghapus akun.")
                }
            )
        }
    }

    fun changePassword(userId: Long, oldPass: String, newPass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repository.updatePassword(userId, oldPass, newPass)
            result.fold(
                onSuccess = {
                    _feedback.value = "Kata sandi berhasil diperbarui." to null
                    onSuccess()
                },
                onFailure = { error ->
                    _feedback.value = null to (error.message ?: "Gagal mengganti kata sandi.")
                }
            )
        }
    }

    fun setTemaTerang(tema: TemaTerang) {
        viewModelScope.launch {
            repository.setTemaTerang(tema)
            _feedback.value = "Tema diubah ke ${tema.label}." to null
        }
    }

    fun setBahasa(bahasa: BahasaApp) {
        viewModelScope.launch {
            repository.setBahasa(bahasa)
            _feedback.value = "Pengaturan bahasa disimpan: ${bahasa.label}." to null
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setReminderEnabled(enabled)
            _feedback.value = if (enabled) {
                "Pengingat H-1 jatuh tempo diaktifkan."
            } else {
                "Pengingat H-1 jatuh tempo dinonaktifkan."
            } to null
        }
    }

    fun saveSyncServerUrl(url: String) {
        viewModelScope.launch {
            val clean = url.trim()
            if (clean.isNotEmpty() && !clean.startsWith("http://", true) && !clean.startsWith("https://", true)) {
                _feedback.value = null to "URL server harus diawali http:// atau https://"
                return@launch
            }
            repository.updateSyncServerUrl(clean)
            _feedback.value = if (clean.isBlank()) {
                "URL server sinkronisasi dikosongkan."
            } else {
                "URL server sinkronisasi disimpan."
            } to null
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoSyncEnabled(enabled)
            _feedback.value = if (enabled) {
                "Sinkronisasi latar belakang otomatis diaktifkan."
            } else {
                "Sinkronisasi otomatis dinonaktifkan."
            } to null
        }
    }

    fun setWifiOnlySync(wifiOnly: Boolean) {
        viewModelScope.launch {
            repository.setWifiOnlySync(wifiOnly)
            _feedback.value = if (wifiOnly) {
                "Sinkronisasi hanya dijalankan saat terhubung Wi-Fi."
            } else {
                "Sinkronisasi diizinkan melalui semua jaringan."
            } to null
        }
    }

    fun triggerSyncNow() {
        viewModelScope.launch {
            repository.triggerSyncNow()
            _feedback.value = "Tugas sinkronisasi WorkManager dijadwalkan." to null
        }
    }

    suspend fun exportBackupJson(): String = repository.exportBackupJson()

    fun restoreBackupJson(json: String) {
        viewModelScope.launch {
            val result = repository.restoreBackupJson(json)
            result.fold(
                onSuccess = { msg -> _feedback.value = msg to null },
                onFailure = { err -> _feedback.value = null to (err.message ?: "Gagal memulihkan data.") }
            )
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            onLoggedOut()
        }
    }

    fun clearFeedback() {
        _feedback.value = null to null
    }
}
