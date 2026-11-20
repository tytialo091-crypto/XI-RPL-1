package com.example.presentation.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.DailyLoanPoint
import com.example.domain.model.IndikatorStok
import com.example.domain.model.InventoryStatusCounts
import com.example.domain.model.KategoriBarang
import com.example.domain.model.KondisiBarang
import com.example.domain.model.PeminjamanDetail
import com.example.domain.model.StatusPeminjaman
import com.example.ui.theme.ChartBarPrimary
import com.example.ui.theme.ChartBarSecondary
import com.example.ui.theme.ChartTrack
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
import com.example.utils.QrCodeUtils

/**
 * Tombol utama EDU-PINJAM berwarna Deep Sky Blue / Sky Blue dengan teks putih dan sudut 12dp.
 */
@Composable
fun SkyPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    testTag: String = "sky_primary_button"
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DeepSkyBlue,
            contentColor = PureWhite,
            disabledContainerColor = SkyBorder,
            disabledContentColor = SlateSecondaryText
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = modifier
            .height(50.dp)
            .testTag(testTag)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Card dasar bernuansa Light Sky Blue (#E0F2FE) atau Putih dengan sudut 16dp dan bayangan lembut.
 */
@Composable
fun EduCard(
    modifier: Modifier = Modifier,
    containerColor: Color = PureWhite,
    borderColor: Color = SkyBorder,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        content()
    }
}

/**
 * Mengembalikan ikon Material sesuai kategori barang inventaris sekolah.
 */
fun categoryIcon(kategori: String): ImageVector = when (KategoriBarang.fromLabel(kategori)) {
    KategoriBarang.ALAT_TULIS -> Icons.Filled.Edit
    KategoriBarang.BUKU -> Icons.Filled.MenuBook
    KategoriBarang.ELEKTRONIK -> Icons.Filled.Devices
    KategoriBarang.OLAHRAGA -> Icons.Filled.SportsBasketball
    KategoriBarang.LAB -> Icons.Filled.Science
    KategoriBarang.MUSIK -> Icons.Filled.MusicNote
    KategoriBarang.LAINNYA -> Icons.Filled.Inventory2
}

/**
 * Thumbnail foto barang dengan Coil atau ilustrasi ikon kategori bernuansa Sky Blue.
 */
@Composable
fun BarangThumbnail(
    fotoUri: String?,
    kategori: String,
    contentDescription: String,
    size: Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(LightSkyBlue, Color(0xFFBAE6FD))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!fotoUri.isNullOrBlank()) {
            AsyncImage(
                model = Uri.parse(fotoUri),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = categoryIcon(kategori),
                contentDescription = contentDescription,
                tint = DeepSkyBlue,
                modifier = Modifier.size(size * 0.48f)
            )
        }
    }
}

/**
 * Badge indikator ketersediaan stok: Hijau (Tersedia), Kuning (Sisa Sedikit), Merah (Habis).
 */
@Composable
fun StockStatusBadge(
    stokTersedia: Int,
    modifier: Modifier = Modifier
) {
    val indikator = IndikatorStok.dariStok(stokTersedia)
    val (bg, fg) = when (indikator) {
        IndikatorStok.TERSEDIA -> StockGreenBg to StockGreenText
        IndikatorStok.SISA_SEDIKIT -> StockAmberBg to StockAmberText
        IndikatorStok.HABIS -> StockRedBg to StockRedText
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Text(
                text = if (stokTersedia > 0) "${indikator.label} ($stokTersedia)" else indikator.label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Komponen visual Dashboard untuk menampilkan jumlah barang inventaris berdasarkan status
 * stok Room Database menggunakan chip berwarna (Hijau = Tersedia, Kuning = Sisa Sedikit, Merah = Habis).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InventoryStatusChipsCard(
    counts: InventoryStatusCounts,
    onChipClick: (IndikatorStok?) -> Unit,
    modifier: Modifier = Modifier
) {
    EduCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("inventory_status_chips_card"),
        containerColor = PureWhite
    ) {
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
                        Icon(
                            imageVector = Icons.Filled.Inventory2,
                            contentDescription = "Ringkasan Status Inventaris",
                            tint = DeepSkyBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Status Stok Inventaris",
                            style = MaterialTheme.typography.titleMedium,
                            color = DarkSlateText
                        )
                        Text(
                            text = "${counts.totalUnitsAvailable} dari ${counts.totalUnits} unit tersedia • Room Live",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSecondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = LightSkyBlue
                ) {
                    Text(
                        text = "${counts.totalItems} Jenis",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
                        color = DeepSkyBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Bilah proporsi distribusi status stok secara real-time
            if (counts.totalItems > 0) {
                val totalF = counts.totalItems.toFloat()
                val greenW = (counts.availableItems / totalF).coerceAtLeast(0f)
                val amberW = (counts.lowStockItems / totalF).coerceAtLeast(0f)
                val redW = (counts.outOfStockItems / totalF).coerceAtLeast(0f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(LightSkyBlue),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (greenW > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(greenW)
                                .fillMaxHeight()
                                .background(StockGreenText)
                        )
                    }
                    if (amberW > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(amberW)
                                .fillMaxHeight()
                                .background(StockAmberText)
                        )
                    }
                    if (redW > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(redW)
                                .fillMaxHeight()
                                .background(StockRedText)
                        )
                    }
                }
            }

            // Chip jumlah inventaris per status ketersediaan
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InventoryCountStatusChip(
                    label = "Tersedia",
                    count = counts.availableItems,
                    bgColor = StockGreenBg,
                    contentColor = StockGreenText,
                    icon = Icons.Filled.CheckCircle,
                    onClick = { onChipClick(IndikatorStok.TERSEDIA) },
                    testTag = "chip_status_tersedia"
                )
                InventoryCountStatusChip(
                    label = "Sisa Sedikit",
                    count = counts.lowStockItems,
                    bgColor = StockAmberBg,
                    contentColor = StockAmberText,
                    icon = Icons.Filled.Warning,
                    onClick = { onChipClick(IndikatorStok.SISA_SEDIKIT) },
                    testTag = "chip_status_sisa_sedikit"
                )
                InventoryCountStatusChip(
                    label = "Habis",
                    count = counts.outOfStockItems,
                    bgColor = StockRedBg,
                    contentColor = StockRedText,
                    icon = Icons.Filled.Cancel,
                    onClick = { onChipClick(IndikatorStok.HABIS) },
                    testTag = "chip_status_habis"
                )
                InventoryCountStatusChip(
                    label = "Semua",
                    count = counts.totalItems,
                    bgColor = LightSkyBlue,
                    contentColor = DeepSkyBlue,
                    icon = Icons.Filled.Inventory2,
                    onClick = { onChipClick(null) },
                    testTag = "chip_status_semua"
                )
            }
        }
    }
}

@Composable
private fun InventoryCountStatusChip(
    label: String,
    count: Int,
    bgColor: Color,
    contentColor: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.25f)),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "$label: $count barang",
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = PureWhite.copy(alpha = 0.85f)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold
                    ),
                    color = contentColor,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Badge status transaksi peminjaman.
 */
@Composable
fun LoanStatusBadge(
    status: StatusPeminjaman,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (status) {
        StatusPeminjaman.DIPINJAM -> LightSkyBlue to DeepSkyBlue
        StatusPeminjaman.DIKEMBALIKAN -> StockGreenBg to StockGreenText
        StatusPeminjaman.TERLAMBAT -> StockRedBg to StockRedText
        StatusPeminjaman.RUSAK -> StockAmberBg to StockAmberText
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/**
 * Grafik mini peminjaman 7 hari terakhir dengan animasi batang Canvas Sky Blue.
 */
@Composable
fun SevenDayLoanChart(
    points: List<DailyLoanPoint>,
    modifier: Modifier = Modifier
) {
    val maxVal = (points.maxOfOrNull { it.jumlahPeminjaman } ?: 1).coerceAtLeast(1)

    EduCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = PureWhite
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Grafik Peminjaman 7 Hari Terakhir",
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkSlateText
                    )
                    Text(
                        text = "Tren transaksi harian inventaris sekolah",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateSecondaryText
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LightSkyBlue
                ) {
                    Text(
                        text = "${points.sumOf { it.jumlahPeminjaman }} Pinjam",
                        style = MaterialTheme.typography.labelMedium,
                        color = DeepSkyBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                points.forEachIndexed { index, point ->
                    val targetFraction = (point.jumlahPeminjaman.toFloat() / maxVal.toFloat())
                        .coerceIn(0.08f, 1f)
                    val animatedFraction by animateFloatAsState(
                        targetValue = if (point.jumlahPeminjaman == 0) 0.08f else targetFraction,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "chart_bar_$index"
                    )
                    val isToday = index == points.lastIndex

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = point.jumlahPeminjaman.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                            color = if (isToday) DeepSkyBlue else SlateSecondaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Canvas(
                            modifier = Modifier
                                .width(22.dp)
                                .height(82.dp)
                        ) {
                            // Track latar belakang batang
                            drawRoundRect(
                                color = ChartTrack,
                                topLeft = Offset.Zero,
                                size = size,
                                cornerRadius = CornerRadius(12f, 12f)
                            )
                            val barHeight = size.height * animatedFraction
                            val topY = size.height - barHeight
                            drawRoundRect(
                                color = if (isToday) ChartBarPrimary else ChartBarSecondary,
                                topLeft = Offset(0f, topY),
                                size = Size(size.width, barHeight),
                                cornerRadius = CornerRadius(12f, 12f)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = point.labelHari,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) DeepSkyBlue else SlateSecondaryText,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Gambar QR Code dari teks kode barang menggunakan ZXing.
 */
@Composable
fun QrCodeImage(
    kodeBarang: String,
    size: Dp = 180.dp,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(kodeBarang) {
        QrCodeUtils.generateQrBitmap(kodeBarang, 512)
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(PureWhite)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Code untuk $kodeBarang",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = kodeBarang,
                style = MaterialTheme.typography.labelMedium,
                color = DarkSlateText
            )
        }
    }
}

/**
 * Dialog pengembalian barang lengkap dengan perhitungan denda otomatis (Rp 1.000/hari)
 * dan unggah foto kondisi barang menggunakan Android Photo Picker.
 */
@Composable
fun ReturnLoanDialog(
    detail: PeminjamanDetail,
    onDismiss: () -> Unit,
    onConfirmReturn: (kondisi: KondisiBarang, fotoUri: String?, catatan: String) -> Unit
) {
    var selectedKondisi by remember { mutableStateOf(KondisiBarang.BAIK) }
    var fotoKondisiUri by remember { mutableStateOf<String?>(null) }
    var catatan by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            fotoKondisiUri = uri.toString()
        }
    }

    val hariTerlambat = detail.hariTerlambatLive
    val estimasiDenda = detail.estimasiDendaLive

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureWhite,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Kembalikan Barang",
                style = MaterialTheme.typography.titleLarge,
                color = DarkSlateText
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LightSkyBlue
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = detail.barang?.namaBarang ?: "Barang",
                            style = MaterialTheme.typography.titleSmall,
                            color = DarkSlateText
                        )
                        Text(
                            text = "Peminjam: ${detail.peminjam?.nama ?: "-"} • ${detail.peminjaman.jumlah} unit",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSecondaryText
                        )
                        Text(
                            text = "Jatuh tempo: ${DateUtils.formatTanggalPendek(detail.peminjaman.tanggalKembaliRencana)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSecondaryText
                        )
                    }
                }

                // Informasi denda otomatis
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (hariTerlambat > 0L) StockRedBg else StockGreenBg
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (hariTerlambat > 0L) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (hariTerlambat > 0L) StockRedText else StockGreenText
                        )
                        Column {
                            Text(
                                text = if (hariTerlambat > 0L) {
                                    "Terlambat $hariTerlambat hari"
                                } else {
                                    "Pengembalian Tepat Waktu"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = if (hariTerlambat > 0L) StockRedText else StockGreenText
                            )
                            Text(
                                text = if (hariTerlambat > 0L) {
                                    "Denda otomatis: ${DateUtils.formatRupiah(estimasiDenda)} (Rp 1.000/hari)"
                                } else {
                                    "Bebas denda keterlambatan (Rp 0)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hariTerlambat > 0L) StockRedText else StockGreenText
                            )
                        }
                    }
                }

                Text(
                    text = "Kondisi Barang Saat Dikembalikan:",
                    style = MaterialTheme.typography.labelLarge,
                    color = DarkSlateText
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KondisiBarang.entries.forEach { kondisi ->
                        FilterChip(
                            selected = selectedKondisi == kondisi,
                            onClick = { selectedKondisi = kondisi },
                            label = {
                                Text(
                                    text = kondisi.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DeepSkyBlue,
                                selectedLabelColor = PureWhite
                            )
                        )
                    }
                }

                // Unggah foto kondisi barang
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SkyBluePrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_return_photo_button")
                ) {
                    Icon(
                        imageVector = if (fotoKondisiUri == null) Icons.Filled.AddAPhoto else Icons.Filled.CameraAlt,
                        contentDescription = "Unggah foto kondisi barang",
                        tint = DeepSkyBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (fotoKondisiUri == null) "Upload Foto Kondisi Barang" else "Foto Terlampir (Ganti)",
                        color = DeepSkyBlue
                    )
                }

                if (!fotoKondisiUri.isNullOrBlank()) {
                    AsyncImage(
                        model = Uri.parse(fotoKondisiUri),
                        contentDescription = "Pratinjau foto pengembalian",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                OutlinedTextField(
                    value = catatan,
                    onValueChange = { catatan = it },
                    label = { Text("Catatan pengembalian (opsional)") },
                    singleLine = false,
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmReturn(selectedKondisi, fotoKondisiUri, catatan) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepSkyBlue,
                    contentColor = PureWhite
                ),
                modifier = Modifier.testTag("confirm_return_button")
            ) {
                Text("Konfirmasi Kembali")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = SlateSecondaryText)
            }
        }
    )
}

/**
 * State kosong yang bersih dengan ilustrasi bawaan.
 */
@Composable
fun EmptyStateBox(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_inventory),
            contentDescription = title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = DarkSlateText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = SlateSecondaryText,
            textAlign = TextAlign.Center
        )
    }
}
