package com.example.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.datastore.SessionState
import com.example.domain.model.IndikatorStok
import com.example.domain.model.KondisiBarang
import com.example.domain.model.PeminjamanDetail
import com.example.domain.model.UserRole
import com.example.presentation.components.BarangThumbnail
import com.example.presentation.components.EduCard
import com.example.presentation.components.EmptyStateBox
import com.example.presentation.components.InventoryStatusChipsCard
import com.example.presentation.components.LoanStatusBadge
import com.example.presentation.components.ReturnLoanDialog
import com.example.presentation.components.SevenDayLoanChart
import com.example.presentation.viewmodel.CloudInventorySnapshot
import com.example.presentation.viewmodel.CloudUiState
import com.example.presentation.viewmodel.DashboardUiState
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

/**
 * Halaman Beranda / Dashboard Admin, Petugas, dan Siswa.
 */
@Composable
fun DashboardScreen(
    session: SessionState,
    uiState: DashboardUiState,
    cloudState: CloudUiState<CloudInventorySnapshot> = CloudUiState.Loading,
    isSyncingCloud: Boolean = false,
    cloudFeedback: String? = null,
    firebaseUserEmail: String? = null,
    onSyncToCloud: () -> Unit = {},
    onAddBarang: () -> Unit,
    onPinjamBarang: () -> Unit,
    onScanQr: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenInventoryWithFilter: (IndikatorStok?) -> Unit,
    onOpenHistory: () -> Unit,
    onTriggerSync: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    onReturnLoan: (peminjamanId: Long, kondisi: KondisiBarang, fotoUri: String?, catatan: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var loanToReturn by remember { mutableStateOf<PeminjamanDetail?>(null) }
    val isStaff = session.userRole == UserRole.ADMIN || session.userRole == UserRole.PETUGAS

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Selamat Datang Deep Sky Blue
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSkyBlue),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(DeepSkyBlue, SkyBluePrimary)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = PureWhite.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = "EDU-PINJAM • ${session.userRole.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Halo, ${session.userName.ifBlank { "Pengguna" }}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = PureWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isStaff) {
                                    "Pantau stok barang & peminjaman sekolah hari ini."
                                } else {
                                    "Pinjam perlengkapan sekolah mudah & tepat waktu."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = LightSkyBlue
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Image(
                            painter = painterResource(id = R.drawable.img_launcher_fg),
                            contentDescription = "Ikon EDU-PINJAM",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }
                }
            }
        }

        // Status Database Cloud Firestore & Sinkronisasi Latar Belakang (Room + WorkManager)
        item {
            val sync = uiState.syncStatus
            val syncIcon = when {
                !sync.isOnline -> Icons.Filled.CloudOff
                cloudState is CloudUiState.Error -> Icons.Filled.Warning
                sync.pendingCount > 0 || isSyncingCloud -> Icons.Filled.CloudSync
                else -> Icons.Filled.CloudDone
            }
            val syncTint = when {
                !sync.isOnline -> StockAmberText
                cloudState is CloudUiState.Error -> StockRedText
                sync.pendingCount > 0 || isSyncingCloud -> DeepSkyBlue
                else -> StockGreenText
            }
            EduCard(
                containerColor = PureWhite,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSyncSettings() }
                    .testTag("dashboard_sync_status_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(LightSkyBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSyncingCloud || cloudState is CloudUiState.Loading) {
                                    CircularProgressIndicator(
                                        color = DeepSkyBlue,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = syncIcon,
                                        contentDescription = "Status sinkronisasi Cloud Firestore",
                                        tint = syncTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = if (isSyncingCloud) "Menyinkronkan ke Cloud..." else sync.statusLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DarkSlateText
                                )
                                val cloudSubtitle = when (cloudState) {
                                    is CloudUiState.Loading -> "Menghubungkan Cloud Firestore..."
                                    is CloudUiState.Success -> "Firestore: ${cloudState.data.cloudBarang.size} barang • ${cloudState.data.cloudPeminjaman.size} pinjaman (${firebaseUserEmail ?: "Google"})"
                                    is CloudUiState.Error -> cloudState.message
                                }
                                Text(
                                    text = cloudSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (cloudState is CloudUiState.Error) StockRedText else SlateSecondaryText,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                onTriggerSync()
                                onSyncToCloud()
                            },
                            enabled = !isSyncingCloud,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, SkyBluePrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("dashboard_sync_now_button")
                        ) {
                            Text(
                                text = "Sinkron Cloud",
                                style = MaterialTheme.typography.labelSmall,
                                color = DeepSkyBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!cloudFeedback.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LightSkyBlue,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = cloudFeedback,
                                style = MaterialTheme.typography.labelSmall,
                                color = DeepSkyBlue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Pengingat Jatuh Tempo H-1
        if (uiState.h1ReminderLoans.isNotEmpty()) {
            item {
                EduCard(
                    containerColor = StockAmberBg,
                    borderColor = StockAmberText.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = "Pengingat H-1",
                                tint = StockAmberText
                            )
                            Column {
                                Text(
                                    text = "${uiState.h1ReminderLoans.size} Peminjaman Jatuh Tempo H-1 / Terlambat",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DarkSlateText
                                )
                                Text(
                                    text = "Ketuk tombol lonceng untuk kirim notifikasi pengingat.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateSecondaryText
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                NotificationUtils.showH1ReminderNotification(
                                    context,
                                    uiState.h1ReminderLoans
                                )
                            },
                            modifier = Modifier.testTag("dashboard_send_reminder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = "Kirim notifikasi pengingat H-1",
                                tint = DeepSkyBlue
                            )
                        }
                    }
                }
            }
        }

        // Ringkasan Status Inventaris dengan Chip Berwarna (Room Database Live)
        item {
            InventoryStatusChipsCard(
                counts = uiState.inventoryStatusCounts,
                onChipClick = onOpenInventoryWithFilter
            )
        }

        // 4 Card Statistik Utama (Total Barang, Tersedia, Dipinjam, Terlambat)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardStatCard(
                        title = "Total Barang",
                        value = "${uiState.stats.totalJenisBarang}",
                        subtitle = "${uiState.stats.totalUnitBarang} total unit",
                        icon = Icons.Filled.Inventory2,
                        cardBg = LightSkyBlue,
                        accentColor = DeepSkyBlue,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenInventory() }
                            .testTag("stat_total_barang")
                    )
                    DashboardStatCard(
                        title = "Barang Tersedia",
                        value = "${uiState.stats.barangTersediaUnit}",
                        subtitle = "Unit siap dipinjam",
                        icon = Icons.Filled.CheckCircle,
                        cardBg = StockGreenBg,
                        accentColor = StockGreenText,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenInventory() }
                            .testTag("stat_barang_tersedia")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardStatCard(
                        title = "Barang Dipinjam",
                        value = "${uiState.stats.barangDipinjamUnit}",
                        subtitle = "Sedang digunakan",
                        icon = Icons.Filled.ShoppingBag,
                        cardBg = LightSkyBlue,
                        accentColor = DeepSkyBlue,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenHistory() }
                            .testTag("stat_barang_dipinjam")
                    )
                    DashboardStatCard(
                        title = "Barang Terlambat",
                        value = "${uiState.stats.peminjamanTerlambatCount}",
                        subtitle = "Perlu ditindaklanjuti",
                        icon = Icons.Filled.Warning,
                        cardBg = if (uiState.stats.peminjamanTerlambatCount > 0) StockRedBg else LightSkyBlue,
                        accentColor = if (uiState.stats.peminjamanTerlambatCount > 0) StockRedText else DeepSkyBlue,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenHistory() }
                            .testTag("stat_barang_terlambat")
                    )
                }
            }
        }

        // Tombol Cepat: Tambah Barang, Pinjam Barang, Scan QR
        item {
            EduCard(containerColor = PureWhite, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Aksi Cepat",
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkSlateText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionTile(
                            label = if (isStaff) "Tambah Barang" else "Inventaris",
                            icon = if (isStaff) Icons.Filled.AddBox else Icons.Filled.Inventory2,
                            onClick = if (isStaff) onAddBarang else onOpenInventory,
                            testTag = "quick_add_barang_button",
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            label = "Pinjam Barang",
                            icon = Icons.Filled.SwapHoriz,
                            onClick = onPinjamBarang,
                            testTag = "quick_pinjam_button",
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            label = "Scan QR",
                            icon = Icons.Filled.QrCodeScanner,
                            onClick = onScanQr,
                            testTag = "quick_scan_qr_button",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Grafik Mini Peminjaman 7 Hari Terakhir
        item {
            SevenDayLoanChart(points = uiState.chartPoints)
        }

        // Header Daftar Peminjaman Terbaru
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Peminjaman Terbaru",
                    style = MaterialTheme.typography.titleMedium,
                    color = DarkSlateText
                )
                TextButton(
                    onClick = onOpenHistory,
                    modifier = Modifier.testTag("dashboard_see_all_history")
                ) {
                    Text("Lihat Semua", color = DeepSkyBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (uiState.recentLoans.isEmpty()) {
            item {
                EduCard(containerColor = PureWhite, modifier = Modifier.fillMaxWidth()) {
                    EmptyStateBox(
                        title = "Belum Ada Transaksi Peminjaman",
                        subtitle = "Mulai catat peminjaman barang sekolah lewat tombol Pinjam Barang."
                    )
                }
            }
        } else {
            items(uiState.recentLoans, key = { it.peminjaman.id }) { detail ->
                RecentLoanCard(
                    detail = detail,
                    onKembalikanClick = { loanToReturn = detail }
                )
            }
        }
    }

    loanToReturn?.let { target ->
        ReturnLoanDialog(
            detail = target,
            onDismiss = { loanToReturn = null },
            onConfirmReturn = { kondisi, fotoUri, catatan ->
                onReturnLoan(target.peminjaman.id, kondisi, fotoUri, catatan)
                loanToReturn = null
            }
        )
    }
}

@Composable
private fun DashboardStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    cardBg: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = DarkSlateText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(PureWhite.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold
                ),
                color = accentColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SlateSecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DeepSkyBlue,
            contentColor = PureWhite
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        modifier = modifier
            .height(74.dp)
            .testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentLoanCard(
    detail: PeminjamanDetail,
    onKembalikanClick: () -> Unit
) {
    EduCard(containerColor = PureWhite, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BarangThumbnail(
                    fotoUri = detail.barang?.fotoUri,
                    kategori = detail.barang?.kategori ?: "Lainnya",
                    contentDescription = detail.barang?.namaBarang ?: "Barang",
                    size = 54.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = detail.barang?.namaBarang ?: "Barang Terhapus",
                        style = MaterialTheme.typography.titleSmall,
                        color = DarkSlateText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${detail.peminjam?.nama ?: "Siswa"} (${detail.peminjam?.kelas ?: "-"}) • ${detail.peminjaman.jumlah} unit",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateSecondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = SlateSecondaryText,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Kembali: ${DateUtils.formatTanggalPendek(detail.peminjaman.tanggalKembaliRencana)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateSecondaryText
                        )
                    }
                }
                LoanStatusBadge(status = detail.statusTampil)
            }

            if (detail.masihAktif) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (detail.isTerlambatLive) {
                        Text(
                            text = "Terlambat ${detail.hariTerlambatLive} hr • Denda ${DateUtils.formatRupiah(detail.estimasiDendaLive)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = StockRedText,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Kode: ${detail.peminjaman.kodePeminjaman}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                            color = SlateSecondaryText
                        )
                    }
                    OutlinedButton(
                        onClick = onKembalikanClick,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DeepSkyBlue),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("dashboard_return_button_${detail.peminjaman.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AssignmentReturn,
                            contentDescription = "Kembalikan barang",
                            tint = DeepSkyBlue,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Kembalikan",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepSkyBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
