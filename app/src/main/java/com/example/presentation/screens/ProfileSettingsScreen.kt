package com.example.presentation.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.entity.UserEntity
import com.example.domain.model.BahasaApp
import com.example.domain.model.TemaTerang
import com.example.domain.model.UserRole
import com.example.presentation.components.EduCard
import com.example.presentation.components.LoanStatusBadge
import com.example.presentation.viewmodel.CloudInventorySnapshot
import com.example.presentation.viewmodel.CloudUiState
import com.example.presentation.viewmodel.ProfileUiState
import com.example.ui.theme.DarkSlateText
import com.example.ui.theme.DeepSkyBlue
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LightSkyBlue
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SkyBluePrimary
import com.example.ui.theme.SkyBorder
import com.example.ui.theme.SlateSecondaryText
import com.example.ui.theme.StockAmberBg
import com.example.ui.theme.StockAmberText
import com.example.ui.theme.StockGreenBg
import com.example.ui.theme.StockGreenText
import com.example.ui.theme.StockRedBg
import com.example.ui.theme.StockRedText
import com.example.utils.DateUtils
import com.example.utils.NotificationUtils
import kotlinx.coroutines.launch

/**
 * Halaman Profil Pengguna (Siswa/Petugas/Admin), Kelola Petugas, Ganti Password,
 * Pengaturan Tema Terang, Bahasa, Notifikasi H-1, dan Backup Data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    uiState: ProfileUiState,
    firebaseUserEmail: String? = null,
    cloudState: CloudUiState<CloudInventorySnapshot> = CloudUiState.Loading,
    isSyncingCloud: Boolean = false,
    cloudFeedback: String? = null,
    onSyncToCloud: () -> Unit = {},
    onClearCloudFeedback: () -> Unit = {},
    onUpdatePhoto: (Long, String?) -> Unit,
    onAddPetugas: (nama: String, username: String, noHp: String, password: String, onDone: () -> Unit) -> Unit,
    onDeletePetugas: (UserEntity) -> Unit,
    onChangePassword: (userId: Long, oldPass: String, newPass: String, onDone: () -> Unit) -> Unit,
    onSelectTema: (TemaTerang) -> Unit,
    onSelectBahasa: (BahasaApp) -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    onSaveSyncServerUrl: (String) -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onToggleWifiOnlySync: (Boolean) -> Unit,
    onTriggerSyncNow: () -> Unit,
    onExportBackupJson: suspend () -> String,
    onRestoreBackupJson: (String) -> Unit,
    onLogout: () -> Unit,
    onClearFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val user = uiState.currentUser
    val isAdmin = uiState.session.userRole == UserRole.ADMIN
    val isSiswa = uiState.session.userRole == UserRole.SISWA

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showAddPetugasDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage, cloudFeedback) {
        val msg = cloudFeedback ?: uiState.successMessage ?: uiState.errorMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            onClearFeedback()
            onClearCloudFeedback()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && user != null) {
            onUpdatePhoto(user.id, uri.toString())
        }
    }

    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = onExportBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "Backup data JSON berhasil disimpan!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal backup: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val backupRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!json.isNullOrBlank()) {
                    onRestoreBackupJson(json)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membaca file backup: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onToggleReminder(granted)
        NotificationUtils.scheduleDailyReminderCheck(context, granted)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Profil & Pengaturan") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSkyBlue,
                    titleContentColor = PureWhite
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card Profil Utama
            item {
                EduCard(containerColor = LightSkyBlue, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(PureWhite)
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .testTag("profile_avatar_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!user?.fotoUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = Uri.parse(user?.fotoUri),
                                        contentDescription = "Foto profil ${user?.nama}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "Avatar profil",
                                        tint = DeepSkyBlue,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = DeepSkyBlue
                                ) {
                                    Text(
                                        text = uiState.session.userRole.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PureWhite,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = user?.nama ?: uiState.session.userName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = DarkSlateText
                                )
                                Text(
                                    text = if (isSiswa) {
                                        "NIS: ${user?.nis ?: "-"} • Kelas ${user?.kelas ?: "-"}"
                                    } else {
                                        "Username: ${user?.username ?: "-"} • ${user?.kelas ?: "Staff"}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateSecondaryText
                                )
                                if (!user?.noHp.isNullOrBlank()) {
                                    Text(
                                        text = "HP: ${user?.noHp}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlateSecondaryText
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PureWhite,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Total Pinjam", style = MaterialTheme.typography.labelSmall, color = SlateSecondaryText)
                                    Text(
                                        text = "${uiState.myLoans.size}",
                                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMono),
                                        color = DeepSkyBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PureWhite,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Sedang Dipinjam", style = MaterialTheme.typography.labelSmall, color = SlateSecondaryText)
                                    Text(
                                        text = "${uiState.myLoans.count { it.masihAktif }}",
                                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMono),
                                        color = DeepSkyBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, DeepSkyBlue),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = DeepSkyBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Foto Profil", color = DeepSkyBlue, style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = { showChangePasswordDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, DeepSkyBlue),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("change_password_button")
                            ) {
                                Icon(Icons.Filled.Key, contentDescription = null, tint = DeepSkyBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ganti Sandi", color = DeepSkyBlue, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // Riwayat Peminjaman Pribadi (untuk Siswa atau Pengguna saat ini)
            if (uiState.myLoans.isNotEmpty()) {
                item {
                    Text(
                        text = "Riwayat Peminjaman Saya",
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkSlateText
                    )
                }
                items(uiState.myLoans.take(4), key = { "my_${it.peminjaman.id}" }) { detail ->
                    EduCard(containerColor = PureWhite, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = detail.barang?.namaBarang ?: "Barang",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DarkSlateText
                                )
                                Text(
                                    text = "${detail.peminjaman.jumlah} unit • ${DateUtils.formatTanggalPendek(detail.peminjaman.tanggalPinjam)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateSecondaryText
                                )
                            }
                            LoanStatusBadge(status = detail.statusTampil)
                        }
                    }
                }
            }

            // Panel Admin: Kelola Akun Petugas
            if (isAdmin) {
                item {
                    EduCard(containerColor = PureWhite, modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = DeepSkyBlue)
                                    Text("Kelola Akun Petugas", style = MaterialTheme.typography.titleMedium, color = DarkSlateText)
                                }
                                TextButton(
                                    onClick = { showAddPetugasDialog = true },
                                    modifier = Modifier.testTag("add_petugas_button")
                                ) {
                                    Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = DeepSkyBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tambah", color = DeepSkyBlue, fontWeight = FontWeight.Bold)
                                }
                            }

                            uiState.petugasList.forEach { petugas ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = LightSkyBlue,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(petugas.nama, style = MaterialTheme.typography.titleSmall, color = DarkSlateText)
                                            Text("Username: ${petugas.username} • ${petugas.noHp}", style = MaterialTheme.typography.bodySmall, color = SlateSecondaryText)
                                        }
                                        IconButton(onClick = { onDeletePetugas(petugas) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Hapus petugas", tint = StockRedText)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Pengaturan Tema Terang, Bahasa, Notifikasi H-1, dan Backup Data
            item {
                EduCard(containerColor = PureWhite, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Pengaturan Aplikasi", style = MaterialTheme.typography.titleMedium, color = DarkSlateText)

                        // Tema Terang
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Palette, contentDescription = null, tint = DeepSkyBlue)
                            Text("Ganti Tema Terang", style = MaterialTheme.typography.titleSmall, color = DarkSlateText)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TemaTerang.entries.forEach { tema ->
                                FilterChip(
                                    selected = uiState.session.temaTerang == tema,
                                    onClick = { onSelectTema(tema) },
                                    label = { Text(tema.label, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DeepSkyBlue,
                                        selectedLabelColor = PureWhite,
                                        containerColor = LightSkyBlue
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Bahasa Indonesia
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Language, contentDescription = null, tint = DeepSkyBlue)
                            Text("Bahasa Aplikasi", style = MaterialTheme.typography.titleSmall, color = DarkSlateText)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BahasaApp.entries.forEach { bahasa ->
                                FilterChip(
                                    selected = uiState.session.bahasaApp == bahasa,
                                    onClick = { onSelectBahasa(bahasa) },
                                    label = { Text(bahasa.label, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DeepSkyBlue,
                                        selectedLabelColor = PureWhite,
                                        containerColor = LightSkyBlue
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Notifikasi Pengingat H-1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = DeepSkyBlue)
                                Column {
                                    Text("Pengingat H-1 Jatuh Tempo", style = MaterialTheme.typography.titleSmall, color = DarkSlateText)
                                    Text("Kirim notifikasi sebelum batas kembali", style = MaterialTheme.typography.bodySmall, color = SlateSecondaryText)
                                }
                            }
                            Switch(
                                checked = uiState.session.reminderEnabled,
                                onCheckedChange = { checked ->
                                    if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        !NotificationUtils.hasNotificationPermission(context)
                                    ) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        onToggleReminder(checked)
                                        NotificationUtils.scheduleDailyReminderCheck(context, checked)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PureWhite,
                                    checkedTrackColor = DeepSkyBlue
                                )
                            )
                        }

                        // Backup & Pulihkan Data JSON
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    backupExportLauncher.launch("Backup_EduPinjam_${System.currentTimeMillis()}.json")
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SkyBluePrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("backup_data_button")
                            ) {
                                Icon(Icons.Filled.Backup, contentDescription = null, tint = DeepSkyBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backup Data", color = DeepSkyBlue, style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = {
                                    backupRestoreLauncher.launch(arrayOf("application/json"))
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SkyBluePrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("restore_data_button")
                            ) {
                                Icon(Icons.Filled.Restore, contentDescription = null, tint = DeepSkyBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pulihkan", color = DeepSkyBlue, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // Manajer Sinkronisasi Latar Belakang (Room + WorkManager)
            item {
                val sync = uiState.syncStatus
                var serverUrlInput by remember(sync.syncServerUrl) { mutableStateOf(sync.syncServerUrl) }

                EduCard(containerColor = PureWhite, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.CloudSync, contentDescription = null, tint = DeepSkyBlue)
                                Text(
                                    text = "Sinkronisasi Data (WorkManager)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DarkSlateText
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (sync.isOnline) StockGreenBg else StockAmberBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (sync.isOnline) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                                        contentDescription = null,
                                        tint = if (sync.isOnline) StockGreenText else StockAmberText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (sync.isOnline) "Online" else "Offline",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (sync.isOnline) StockGreenText else StockAmberText,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = LightSkyBlue,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Cloud Firestore: ${firebaseUserEmail ?: "Akun Google Aktif"}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DeepSkyBlue
                                )
                                val cloudSummary = when (cloudState) {
                                    is CloudUiState.Loading -> "Memuat data Cloud Firestore..."
                                    is CloudUiState.Success -> "Tersimpan di Cloud: ${cloudState.data.cloudBarang.size} barang • ${cloudState.data.cloudPeminjaman.size} pinjaman"
                                    is CloudUiState.Error -> "Error Cloud: ${cloudState.message}"
                                }
                                Text(
                                    text = cloudSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (cloudState is CloudUiState.Error) StockRedText else DarkSlateText
                                )
                                Text(
                                    text = "Status: ${sync.statusLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateSecondaryText
                                )
                                Text(
                                    text = "Antrean Outbox Room: ${sync.pendingCount} tugas  •  Peminjaman tertunda: ${sync.pendingBorrowingCount}  •  Worker: ${sync.workerState?.name ?: "IDLE"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateSecondaryText
                                )
                                if (sync.lastSyncTimestamp > 0L) {
                                    Text(
                                        text = "Sinkronisasi terakhir: ${DateUtils.formatTanggalLengkap(sync.lastSyncTimestamp)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlateSecondaryText
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onSyncToCloud,
                            enabled = !isSyncingCloud,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepSkyBlue,
                                contentColor = PureWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("profile_cloud_sync_button")
                        ) {
                            Icon(Icons.Filled.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSyncingCloud) "Menyinkronkan ke Cloud Firestore..." else "Sinkronkan Semua Data ke Cloud Firestore",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            value = serverUrlInput,
                            onValueChange = { serverUrlInput = it },
                            label = { Text("URL Endpoint Server Inventaris") },
                            placeholder = { Text("https://server-sekolah.sch.id/api/sync") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sync_server_url_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onSaveSyncServerUrl(serverUrlInput) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, DeepSkyBlue),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_sync_url_button")
                            ) {
                                Text("Simpan URL", color = DeepSkyBlue, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = onTriggerSyncNow,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DeepSkyBlue,
                                    contentColor = PureWhite
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sync_now_button")
                            ) {
                                Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sinkron Sekarang")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sinkronisasi Otomatis Saat Online", style = MaterialTheme.typography.titleSmall, color = DarkSlateText)
                                Text("WorkManager otomatis jalan saat jaringan tersedia", style = MaterialTheme.typography.bodySmall, color = SlateSecondaryText)
                            }
                            Switch(
                                checked = sync.autoSyncEnabled,
                                onCheckedChange = onToggleAutoSync,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PureWhite,
                                    checkedTrackColor = DeepSkyBlue
                                ),
                                modifier = Modifier.testTag("auto_sync_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hanya Gunakan Wi-Fi", style = MaterialTheme.typography.titleSmall, color = DarkSlateText)
                                Text("Hemat kuota seluler saat sinkronisasi latar belakang", style = MaterialTheme.typography.bodySmall, color = SlateSecondaryText)
                            }
                            Switch(
                                checked = sync.wifiOnlySync,
                                onCheckedChange = onToggleWifiOnlySync,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PureWhite,
                                    checkedTrackColor = DeepSkyBlue
                                ),
                                modifier = Modifier.testTag("wifi_only_sync_switch")
                            )
                        }

                        if (sync.recentLogs.isNotEmpty()) {
                            Text(
                                text = "Log Sinkronisasi Terbaru",
                                style = MaterialTheme.typography.labelLarge,
                                color = DarkSlateText
                            )
                            sync.recentLogs.take(3).forEach { log ->
                                val isOk = log.status == "SUCCESS"
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isOk) StockGreenBg.copy(alpha = 0.6f) else StockAmberBg.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${log.status} • ${log.itemsSynced} item",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isOk) StockGreenText else StockAmberText,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = DateUtils.formatTanggalPendek(log.timestamp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SlateSecondaryText
                                            )
                                        }
                                        Text(
                                            text = log.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DarkSlateText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tombol Keluar (Logout)
            item {
                Button(
                    onClick = onLogout,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StockRedBg,
                        contentColor = StockRedText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("logout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Keluar akun")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Keluar dari Akun", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showChangePasswordDialog && user != null) {
        var oldPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            containerColor = PureWhite,
            title = { Text("Ganti Kata Sandi", color = DarkSlateText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = oldPass,
                        onValueChange = { oldPass = it },
                        label = { Text("Kata Sandi Lama") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("Kata Sandi Baru") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onChangePassword(user.id, oldPass, newPass) {
                            showChangePasswordDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepSkyBlue, contentColor = PureWhite)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showAddPetugasDialog) {
        var nama by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var noHp by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddPetugasDialog = false },
            containerColor = PureWhite,
            title = { Text("Tambah Akun Petugas", color = DarkSlateText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nama,
                        onValueChange = { nama = it },
                        label = { Text("Nama Petugas") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noHp,
                        onValueChange = { noHp = it },
                        label = { Text("Nomor HP") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Kata Sandi") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddPetugas(nama, username, noHp, password) {
                            showAddPetugasDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepSkyBlue, contentColor = PureWhite)
                ) {
                    Text("Tambahkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPetugasDialog = false }) { Text("Batal") }
            }
        )
    }
}
