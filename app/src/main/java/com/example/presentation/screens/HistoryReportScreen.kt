package com.example.presentation.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.domain.model.KondisiBarang
import com.example.domain.model.PeminjamanDetail
import com.example.presentation.components.BarangThumbnail
import com.example.presentation.components.EduCard
import com.example.presentation.components.EmptyStateBox
import com.example.presentation.components.LoanStatusBadge
import com.example.presentation.components.ReturnLoanDialog
import com.example.presentation.viewmodel.HistoryFilter
import com.example.presentation.viewmodel.LoanUiState
import com.example.ui.theme.DarkSlateText
import com.example.ui.theme.DeepSkyBlue
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LightSkyBlue
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SkyBluePrimary
import com.example.ui.theme.SkyBorder
import com.example.ui.theme.SlateSecondaryText
import com.example.ui.theme.StockGreenBg
import com.example.ui.theme.StockGreenText
import com.example.ui.theme.StockRedBg
import com.example.ui.theme.StockRedText
import com.example.utils.DateUtils
import com.example.utils.ExportUtils
import com.example.utils.NotificationUtils
import kotlinx.coroutines.launch

/**
 * Halaman Riwayat Peminjaman & Laporan Bulanan (Export PDF/Excel & Pengingat H-1).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryReportScreen(
    uiState: LoanUiState,
    onFilterSelect: (HistoryFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onKembalikan: (Long, KondisiBarang, String?, String) -> Unit,
    onFetchH1Loans: suspend () -> List<PeminjamanDetail>,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Riwayat & Laporan") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSkyBlue,
                    titleContentColor = PureWhite
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = PureWhite,
                contentColor = DeepSkyBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = DeepSkyBlue
                    )
                }
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Riwayat (${uiState.filteredLoans.size})", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_riwayat")
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Laporan Bulanan", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_laporan")
                )
            }

            if (activeTab == 0) {
                HistoryTab(
                    uiState = uiState,
                    onFilterSelect = onFilterSelect,
                    onSearchChange = onSearchChange,
                    onKembalikan = onKembalikan
                )
            } else {
                MonthlyReportTab(
                    uiState = uiState,
                    onFetchH1Loans = onFetchH1Loans
                )
            }
        }
    }
}

@Composable
private fun HistoryTab(
    uiState: LoanUiState,
    onFilterSelect: (HistoryFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onKembalikan: (Long, KondisiBarang, String?, String) -> Unit
) {
    var loanToReturn by remember { mutableStateOf<PeminjamanDetail?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Cari kode pinjam, barang, atau siswa...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = DeepSkyBlue) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Hapus pencarian")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PureWhite,
                unfocusedContainerColor = PureWhite,
                focusedBorderColor = DeepSkyBlue,
                unfocusedBorderColor = SkyBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(HistoryFilter.entries) { filter ->
                FilterChip(
                    selected = uiState.selectedFilter == filter,
                    onClick = { onFilterSelect(filter) },
                    label = { Text(filter.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DeepSkyBlue,
                        selectedLabelColor = PureWhite,
                        containerColor = LightSkyBlue
                    ),
                    border = BorderStroke(1.dp, SkyBorder),
                    modifier = Modifier.testTag("history_filter_${filter.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.filteredLoans.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStateBox(
                    title = "Belum Ada Riwayat",
                    subtitle = "Belum ada data transaksi peminjaman sesuai filter."
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.filteredLoans, key = { it.peminjaman.id }) { detail ->
                    EduCard(containerColor = PureWhite, modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
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
                                        text = detail.peminjaman.kodePeminjaman,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                                        color = DeepSkyBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = detail.barang?.namaBarang ?: "Barang",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = DarkSlateText
                                    )
                                    Text(
                                        text = "${detail.peminjam?.nama ?: "Siswa"} • ${detail.peminjaman.jumlah} unit",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SlateSecondaryText
                                    )
                                }
                                LoanStatusBadge(status = detail.statusTampil)
                            }

                            Text(
                                text = "Pinjam: ${DateUtils.formatTanggalPendek(detail.peminjaman.tanggalPinjam)}  •  Jatuh Tempo: ${DateUtils.formatTanggalPendek(detail.peminjaman.tanggalKembaliRencana)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateSecondaryText
                            )

                            if (detail.peminjaman.keperluan.isNotBlank()) {
                                Text(
                                    text = "Keperluan: ${detail.peminjaman.keperluan}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DarkSlateText
                                )
                            }

                            if (!detail.peminjaman.fotoPengembalianUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = Uri.parse(detail.peminjaman.fotoPengembalianUri),
                                    contentDescription = "Foto kondisi pengembalian",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }

                            if (detail.estimasiDendaLive > 0L) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = StockRedBg
                                ) {
                                    Text(
                                        text = "Denda keterlambatan: ${DateUtils.formatRupiah(detail.estimasiDendaLive)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = StockRedText,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            if (detail.masihAktif) {
                                OutlinedButton(
                                    onClick = { loanToReturn = detail },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, DeepSkyBlue),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.AssignmentReturn, contentDescription = null, tint = DeepSkyBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Proses Pengembalian", color = DeepSkyBlue)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    loanToReturn?.let { target ->
        ReturnLoanDialog(
            detail = target,
            onDismiss = { loanToReturn = null },
            onConfirmReturn = { kondisi, fotoUri, catatan ->
                onKembalikan(target.peminjaman.id, kondisi, fotoUri, catatan)
                loanToReturn = null
            }
        )
    }
}

@Composable
private fun MonthlyReportTab(
    uiState: LoanUiState,
    onFetchH1Loans: suspend () -> List<PeminjamanDetail>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val summary = uiState.reportSummary

    val pdfSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    ExportUtils.writePdfReport(out, summary, uiState.allLoans)
                }
                Toast.makeText(context, "Laporan PDF berhasil disimpan!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal menyimpan PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val excelSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    val content = ExportUtils.buildExcelXmlReport(summary, uiState.allLoans)
                    out.write(content.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Laporan Excel (.xls) berhasil disimpan!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal menyimpan Excel: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                val loans = onFetchH1Loans()
                if (loans.isEmpty()) {
                    Toast.makeText(context, "Tidak ada peminjaman jatuh tempo H-1 saat ini.", Toast.LENGTH_SHORT).show()
                } else {
                    NotificationUtils.showH1ReminderNotification(context, loans)
                }
            }
        } else {
            Toast.makeText(context, "Izin notifikasi diperlukan untuk pengingat H-1.", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Ringkasan Bulanan
        item {
            EduCard(containerColor = LightSkyBlue, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Laporan Bulanan • ${summary.bulanLabel}",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepSkyBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportMetricPill("Transaksi", "${summary.totalTransaksi}", Modifier.weight(1f))
                        ReportMetricPill("Kembali", "${summary.totalDikembalikan}", Modifier.weight(1f))
                        ReportMetricPill("Terlambat", "${summary.totalTerlambat}", Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PureWhite,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Akumulasi Denda", style = MaterialTheme.typography.bodyMedium, color = SlateSecondaryText)
                            Text(
                                text = DateUtils.formatRupiah(summary.totalDendaTerkumpul),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                                color = DeepSkyBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Ekspor PDF & Excel + Pengingat H-1
        item {
            EduCard(containerColor = PureWhite, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Ekspor Laporan & Pengingat", style = MaterialTheme.typography.titleMedium, color = DarkSlateText)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                pdfSaverLauncher.launch("Laporan_EduPinjam_${System.currentTimeMillis()}.pdf")
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepSkyBlue, contentColor = PureWhite),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_pdf_button")
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan PDF")
                        }

                        Button(
                            onClick = {
                                excelSaverLauncher.launch("Laporan_EduPinjam_${System.currentTimeMillis()}.xls")
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary, contentColor = DarkSlateText),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_excel_button")
                        ) {
                            Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan Excel")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val file = ExportUtils.createShareableReportFile(
                                    context,
                                    "Laporan_EduPinjam.pdf"
                                ) { out ->
                                    ExportUtils.writePdfReport(out, summary, uiState.allLoans)
                                }
                                ExportUtils.shareFile(context, file, "application/pdf", "Bagikan Laporan PDF")
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SkyBluePrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, tint = DeepSkyBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bagikan PDF", color = DeepSkyBlue, style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !NotificationUtils.hasNotificationPermission(context)
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    scope.launch {
                                        val loans = onFetchH1Loans()
                                        if (loans.isEmpty()) {
                                            Toast.makeText(context, "Tidak ada peminjaman jatuh tempo H-1.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            NotificationUtils.showH1ReminderNotification(context, loans)
                                            Toast.makeText(context, "Notifikasi pengingat H-1 dikirim!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SkyBluePrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("trigger_h1_notification_button")
                        ) {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = DeepSkyBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kirim Pengingat H-1", color = DeepSkyBlue, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Barang Paling Sering Dipinjam
        item {
            EduCard(containerColor = PureWhite, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Barang Paling Sering Dipinjam", style = MaterialTheme.typography.titleMedium, color = DarkSlateText)
                    if (summary.topBarang.isEmpty()) {
                        Text("Belum ada data peminjaman.", style = MaterialTheme.typography.bodySmall, color = SlateSecondaryText)
                    } else {
                        val topMax = summary.topBarang.maxOf { it.totalDipinjamKali }.coerceAtLeast(1)
                        summary.topBarang.take(5).forEachIndexed { idx, item ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${idx + 1}. ${item.namaBarang}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DarkSlateText,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${item.totalDipinjamKali}x (${item.totalUnitDipinjam} unit)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
                                        color = DeepSkyBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { item.totalDipinjamKali.toFloat() / topMax.toFloat() },
                                    color = DeepSkyBlue,
                                    trackColor = LightSkyBlue,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Siswa Paling Aktif Meminjam
        item {
            EduCard(containerColor = PureWhite, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Siswa Paling Aktif Meminjam", style = MaterialTheme.typography.titleMedium, color = DarkSlateText)
                    if (summary.topSiswa.isEmpty()) {
                        Text("Belum ada data peminjaman siswa.", style = MaterialTheme.typography.bodySmall, color = SlateSecondaryText)
                    } else {
                        summary.topSiswa.take(5).forEachIndexed { idx, siswa ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (idx == 0) LightSkyBlue else StockGreenBg.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${idx + 1}. ${siswa.namaSiswa}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = DarkSlateText
                                        )
                                        Text(
                                            text = "NIS ${siswa.nis} • ${siswa.kelas}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SlateSecondaryText
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (idx == 0) DeepSkyBlue else StockGreenText
                                    ) {
                                        Text(
                                            text = "${siswa.totalTransaksi}x Pinjam",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PureWhite,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = PureWhite
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMono),
                color = DeepSkyBlue,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = SlateSecondaryText
            )
        }
    }
}
