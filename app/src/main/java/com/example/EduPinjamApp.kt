package com.example

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.example.data.datastore.SessionManager
import com.example.data.local.EduPinjamDatabase
import com.example.data.repository.EduPinjamRepository
import com.example.data.sync.DataSyncManager
import com.example.utils.NetworkMonitor
import com.example.utils.NotificationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Kelas Application utama EDU-PINJAM untuk inisialisasi Database, DataStore, DataSyncManager, dan Notifikasi.
 */
class EduPinjamApp : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: EduPinjamDatabase by lazy {
        EduPinjamDatabase.getInstance(this)
    }

    val sessionManager: SessionManager by lazy {
        SessionManager(this)
    }

    val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(this)
    }

    val dataSyncManager: DataSyncManager by lazy {
        DataSyncManager(this, database, sessionManager, networkMonitor)
    }

    val repository: EduPinjamRepository by lazy {
        EduPinjamRepository(database, sessionManager, dataSyncManager)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationUtils.createNotificationChannel(this)
        appScope.launch {
            repository.ensureSeeded()
            val session = repository.sessionState.first()
            NotificationUtils.scheduleDailyReminderCheck(this@EduPinjamApp, session.reminderEnabled)
            dataSyncManager.configurePeriodicSync(
                enabled = session.autoSyncEnabled,
                wifiOnly = session.wifiOnlySync
            )
            dataSyncManager.observeConnectivityAndTriggerPendingBorrowings(appScope)
        }
    }
}
