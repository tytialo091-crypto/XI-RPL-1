package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.BahasaApp
import com.example.domain.model.TemaTerang
import com.example.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "edu_pinjam_prefs")

/**
 * Data state sesi login, pengaturan aplikasi, dan konfigurasi sinkronisasi dari DataStore.
 */
data class SessionState(
    val isLoggedIn: Boolean = false,
    val userId: Long = -1L,
    val userRole: UserRole = UserRole.SISWA,
    val userName: String = "",
    val temaTerang: TemaTerang = TemaTerang.PUTIH_CERAH,
    val bahasaApp: BahasaApp = BahasaApp.INDONESIA,
    val reminderEnabled: Boolean = true,
    val dendaPerHari: Long = 1000L,
    val autoSyncEnabled: Boolean = true,
    val wifiOnlySync: Boolean = false,
    val syncServerUrl: String = "",
    val lastSyncTimestamp: Long = 0L
)

/**
 * Pengelola sesi login dan preferensi pengguna menggunakan Jetpack DataStore.
 */
class SessionManager(private val context: Context) {

    private object Keys {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val USER_ID = longPreferencesKey("user_id")
        val USER_ROLE = stringPreferencesKey("user_role")
        val USER_NAME = stringPreferencesKey("user_name")
        val TEMA_TERANG = stringPreferencesKey("tema_terang")
        val BAHASA_APP = stringPreferencesKey("bahasa_app")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val DENDA_PER_HARI = longPreferencesKey("denda_per_hari")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val WIFI_ONLY_SYNC = booleanPreferencesKey("wifi_only_sync")
        val SYNC_SERVER_URL = stringPreferencesKey("sync_server_url")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
    }

    val sessionFlow: Flow<SessionState> = context.dataStore.data.map { prefs ->
        SessionState(
            isLoggedIn = prefs[Keys.IS_LOGGED_IN] ?: false,
            userId = prefs[Keys.USER_ID] ?: -1L,
            userRole = UserRole.fromString(prefs[Keys.USER_ROLE] ?: UserRole.SISWA.name),
            userName = prefs[Keys.USER_NAME] ?: "",
            temaTerang = TemaTerang.fromName(prefs[Keys.TEMA_TERANG] ?: TemaTerang.PUTIH_CERAH.name),
            bahasaApp = BahasaApp.fromName(prefs[Keys.BAHASA_APP] ?: BahasaApp.INDONESIA.name),
            reminderEnabled = prefs[Keys.REMINDER_ENABLED] ?: true,
            dendaPerHari = prefs[Keys.DENDA_PER_HARI] ?: 1000L,
            autoSyncEnabled = prefs[Keys.AUTO_SYNC_ENABLED] ?: true,
            wifiOnlySync = prefs[Keys.WIFI_ONLY_SYNC] ?: false,
            syncServerUrl = prefs[Keys.SYNC_SERVER_URL] ?: "",
            lastSyncTimestamp = prefs[Keys.LAST_SYNC_TIMESTAMP] ?: 0L
        )
    }

    suspend fun saveLoginSession(userId: Long, role: UserRole, userName: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = true
            prefs[Keys.USER_ID] = userId
            prefs[Keys.USER_ROLE] = role.name
            prefs[Keys.USER_NAME] = userName
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = false
            prefs[Keys.USER_ID] = -1L
            prefs[Keys.USER_ROLE] = UserRole.SISWA.name
            prefs[Keys.USER_NAME] = ""
        }
    }

    suspend fun setTemaTerang(tema: TemaTerang) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TEMA_TERANG] = tema.name
        }
    }

    suspend fun setBahasa(bahasa: BahasaApp) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BAHASA_APP] = bahasa.name
        }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REMINDER_ENABLED] = enabled
        }
    }

    suspend fun setDendaPerHari(nominal: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DENDA_PER_HARI] = nominal.coerceAtLeast(0L)
        }
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_SYNC_ENABLED] = enabled
        }
    }

    suspend fun setWifiOnlySync(wifiOnly: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WIFI_ONLY_SYNC] = wifiOnly
        }
    }

    suspend fun setSyncServerUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SYNC_SERVER_URL] = url.trim()
        }
    }

    suspend fun setLastSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SYNC_TIMESTAMP] = timestamp
        }
    }
}
