package com.example.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.datastore.SessionState
import com.example.data.local.entity.BarangEntity
import com.example.domain.model.PeminjamanDetail
import com.example.domain.model.UserRole
import com.example.presentation.components.BarangThumbnail
import com.example.presentation.components.EduCard
import com.example.presentation.components.EmptyStateBox
import com.example.presentation.components.LoanStatusBadge
import com.example.presentation.components.QrCodeImage
import com.example.presentation.components.SkyPrimaryButton
import com.example.presentation.components.StockStatusBadge
import com.example.ui.theme.DarkSlateText
import com.example.ui.theme.DeepSkyBlue
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LightSkyBlue
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SkyBluePrimary
import com.example.ui.theme.SlateSecondaryText
import com.example.ui.theme.StockRedText
import com.example.utils.DateUtils

/**
 * Halaman Detail Barang lengkap dengan foto, stok tersedia, QR Code otomatis, dan riwayat peminjaman barang.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    session: SessionState,
    barang: BarangEntity?,
    history: List<PeminjamanDetail>,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (BarangEntity) -> Unit,
    onPinjamBarang: (Long) -> Unit
) {
    val isStaff = session.userRole == UserRole.ADMIN || session.userRole == UserRole.PETUGAS
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Detail Inventaris") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                actions = {
                    if (isStaff && barang != null) {
                        IconButton(
                            onClick = { onEdit(barang.id) },
                            modifier = Modifier.testTag("detail_edit_button")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit barang")
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.testTag("detail_delete_button")
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Hapus barang")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSkyBlue,
                    titleContentColor = PureWhite,
                    navigationIconContentColor = PureWhite,
                    actionIconContentColor = PureWhite
                )
            )
        }
    ) { innerPadding ->
        if (barang == null) {
            EmptyStateBox(
                title = "Barang Tidak Ditemukan",
                subtitle = "Data barang mungkin telah dihapus.",
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card Foto & Informasi Utama
            item {
                EduCard(containerColor = PureWhite, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            BarangThumbnail(
                                fotoUri = barang.fotoUri,
                                kategori = barang.kategori,
                                contentDescription = barang.namaBarang,
                                size = 90.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = LightSkyBlue
                                ) {
                                    Text(
                                        text = barang.kodeBarang,
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
                                        color = DeepSkyBlue,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = barang.namaBarang,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = DarkSlateText
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                StockStatusBadge(stokTersedia = barang.stokTersedia)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            InfoBoxPill(
                                label = "Stok Tersedia",
                                value = "${barang.stokTersedia} / ${barang.stokTotal} unit",
                                modifier = Modifier.weight(1f)
                            )
                            InfoBoxPill(
                                label = "Kondisi",
                                value = barang.kondisi,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Category, contentDescription = null, tint = DeepSkyBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kategori: ${barang.kategori}", style = MaterialTheme.typography.bodyMedium, color = DarkSlateText)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Place, contentDescription = null, tint = DeepSkyBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Lokasi: ${barang.lokasiRak}", style = MaterialTheme.typography.bodyMedium, color = DarkSlateText)
                        }
                        if (barang.deskripsi.isNotBlank()) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Filled.Info, contentDescription = null, tint = DeepSkyBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(barang.deskripsi, style = MaterialTheme.typography.bodySmall, color = SlateSecondaryText)
                            }
                        }

                        SkyPrimaryButton(
                            text = if (barang.stokTersedia > 0) "Pinjam Barang Ini" else "Stok Habis (Tidak Bisa Dipinjam)",
                            onClick = { onPinjamBarang(barang.id) },
                            enabled = barang.stokTersedia > 0,
                            icon = Icons.Filled.SwapHoriz,
                            testTag = "detail_pinjam_button",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Card QR Code Otomatis
            item {
                EduCard(containerColor = LightSkyBlue, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "QR Code Inventaris Otomatis",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepSkyBlue
                        )
                        Text(
                            text = "Tempelkan pada barang untuk pemindaian cepat",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSecondaryText
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = PureWhite,
                            border = BorderStroke(1.dp, SkyBluePrimary)
                        ) {
                            QrCodeImage(
                                kodeBarang = barang.kodeBarang,
                                size = 165.dp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = barang.kodeBarang,
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                            color = DarkSlateText
                        )
                    }
                }
            }

            // Riwayat Peminjaman Barang Tersebut
            item {
                Text(
                    text = "Riwayat Peminjaman Barang (${history.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = DarkSlateText
                )
            }

            if (history.isEmpty()) {
                item {
                    EduCard(containerColor = PureWhite, modifier = Modifier.fillMaxWidth()) {
                        EmptyStateBox(
                            title = "Belum Pernah Dipinjam",
                            subtitle = "Barang ini belum memiliki riwayat transaksi peminjaman."
                        )
                    }
                }
            } else {
                items(history, key = { it.peminjaman.id }) { detail ->
                    EduCard(containerColor = PureWhite, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = detail.peminjam?.nama ?: "Siswa",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DarkSlateText
                                )
                                Text(
                                    text = "${detail.peminjam?.kelas ?: "-"} • ${detail.peminjaman.jumlah} unit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateSecondaryText
                                )
                                Text(
                                    text = "${DateUtils.formatTanggalPendek(detail.peminjaman.tanggalPinjam)} s/d ${DateUtils.formatTanggalPendek(detail.peminjaman.tanggalKembaliRencana)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateSecondaryText
                                )
                            }
                            LoanStatusBadge(status = detail.statusTampil)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && barang != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = PureWhite,
            title = { Text("Hapus Barang?", color = DarkSlateText) },
            text = {
                Text("Barang \"${barang.namaBarang}\" dan riwayat terkait akan dihapus permanen dari inventaris.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(barang)
                    }
                ) {
                    Text("Hapus", color = StockRedText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal", color = SlateSecondaryText)
                }
            }
        )
    }
}

@Composable
private fun InfoBoxPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = LightSkyBlue
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = SlateSecondaryText
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = DeepSkyBlue,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
