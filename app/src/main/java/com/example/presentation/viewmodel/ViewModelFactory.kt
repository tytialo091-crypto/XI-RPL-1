package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.EduPinjamDatabase
import com.example.data.remote.FirestoreInventoryRepository
import com.example.data.repository.EduPinjamRepository

/**
 * Factory untuk membuat ViewModel dengan injeksi EduPinjamRepository secara bersih.
 */
class EduPinjamViewModelFactory(
    private val repository: EduPinjamRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(repository) as T
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(repository) as T
            modelClass.isAssignableFrom(InventoryViewModel::class.java) -> InventoryViewModel(repository) as T
            modelClass.isAssignableFrom(LoanViewModel::class.java) -> LoanViewModel(repository) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(repository) as T
            else -> throw IllegalArgumentException("ViewModel tidak dikenal: ${modelClass.name}")
        }
    }
}

/**
 * Factory untuk membuat CloudSyncViewModel yang terisolasi per UID pengguna Google.
 */
class CloudSyncViewModelFactory(
    private val firestoreRepository: FirestoreInventoryRepository,
    private val localRepository: EduPinjamRepository,
    private val database: EduPinjamDatabase,
    private val currentUserId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CloudSyncViewModel::class.java)) {
            return CloudSyncViewModel(
                firestoreRepository = firestoreRepository,
                localRepository = localRepository,
                database = database,
                currentUserId = currentUserId
            ) as T
        }
        throw IllegalArgumentException("ViewModel tidak dikenal: ${modelClass.name}")
    }
}
