package com.example.presentation.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.datastore.SessionState
import com.example.data.local.entity.BarangEntity
import com.example.domain.model.IndikatorStok
import com.example.domain.model.KategoriBarang
import com.example.domain.model.KondisiBarang
import com.example.domain.model.SortOption
import com.example.domain.model.UserRole
import com.example.presentation.components.BarangThumbnail
import com.example.presentation.components.EduCard
import com.example.presentation.components.EmptyStateBox
import com.example.presentation.components.QrCodeImage
import com.example.presentation.components.StockStatusBadge
import com.example.presentation.viewmodel.InventoryUiState
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

/**
 * Halaman Data Inventaris dengan pencarian, filter kategori, pengurutan, dan pratinjau QR Code.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    session: SessionState,
    uiState: InventoryUiState,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onStockFilterSelect: (IndikatorStok?) -> Unit,
    onSortSelect: (SortOption) -> Unit,
    onItemClick: (Long) -> Unit,
    onAddBarang: () -> Unit,
    onScanQr: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isStaff = session.userRole == UserRole.ADMIN || session.userRole == UserRole.PETUGAS
    val focusManager = LocalFocusManager.current
    var showSortMenu by remember { mutableStateOf(false) }
    var qrPreviewBarang by remember { mutableStateOf<BarangEntity?>(null) }
    val categories = remember { listOf("Semua") + KategoriBarang.entries.map { it.label } }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Data Inventaris", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "${uiState.items.size} barang ditemukan",
                            style = MaterialTheme.typography.labelSmall,
                            color = LightSkyBlue
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("inventory_sort_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Urutkan inventaris"
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            containerColor = PureWhite
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            fontWeight = if (uiState.sortOption == option) FontWeight.Bold else FontWeight.Normal,
                                            color = if (uiState.sortOption == option) DeepSkyBlue else DarkSlateText
                                        )
                                    },
                                    onClick = {
                                        onSortSelect(option)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onScanQr,
                        modifier = Modifier.testTag("inventory_scan_qr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = "Scan QR barang"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSkyBlue,
                    titleContentColor = PureWhite,
                    actionIconContentColor = PureWhite
                )
            )
        },
        floatingActionButton = {
            if (isStaff) {
                FloatingActionButton(
                    onClick = onAddBarang,
                    containerColor = DeepSkyBlue,
                    contentColor = PureWhite,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("inventory_add_fab")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Tambah Barang Baru"
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search bar di bagian atas layar untuk memfilter nama barang atau kategori secara real-time
            Surface(
                color = LightSkyBlue.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = {
                        Text(
                            text = "Cari nama barang atau kategori...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Cari inventaris",
                            tint = DeepSkyBlue
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    onSearchChange("")
                                    focusManager.clearFocus()
                                },
                                modifier = Modifier.testTag("inventory_search_clear_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Hapus pencarian",
                                    tint = SlateSecondaryText
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = PureWhite,
                        focusedBorderColor = DeepSkyBlue,
                        unfocusedBorderColor = SkyBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("inventory_search_input")
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Chip filter kategori
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = uiState.selectedCategory == category,
                        onClick = { onCategorySelect(category) },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DeepSkyBlue,
                            selectedLabelColor = PureWhite,
                            containerColor = LightSkyBlue,
                            labelColor = DarkSlateText
                        ),
                        border = BorderStroke(1.dp, SkyBorder),
                        modifier = Modifier.testTag("category_chip_$category")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Chip filter status ketersediaan stok
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedStockFilter == null,
                        onClick = { onStockFilterSelect(null) },
                        label = { Text("Semua Stok") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DeepSkyBlue,
                            selectedLabelColor = PureWhite,
                            containerColor = PureWhite,
                            labelColor = SlateSecondaryText
                        ),
                        border = BorderStroke(1.dp, SkyBorder),
                        modifier = Modifier.testTag("stock_filter_chip_all")
                    )
                }
                items(IndikatorStok.entries) { indikator ->
                    val (bg, fg) = when (indikator) {
                        IndikatorStok.TERSEDIA -> StockGreenBg to StockGreenText
                        IndikatorStok.SISA_SEDIKIT -> StockAmberBg to StockAmberText
                        IndikatorStok.HABIS -> StockRedBg to StockRedText
                    }
                    FilterChip(
                        selected = uiState.selectedStockFilter == indikator,
                        onClick = {
                            onStockFilterSelect(if (uiState.selectedStockFilter == indikator) null else indikator)
                        },
                        label = {
                            Text(
                                text = indikator.label,
                                fontWeight = if (uiState.selectedStockFilter == indikator) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = fg,
                            selectedLabelColor = PureWhite,
                            containerColor = bg,
                            labelColor = fg
                        ),
                        border = BorderStroke(1.dp, fg.copy(alpha = 0.35f)),
                        modifier = Modifier.testTag("stock_filter_chip_${indikator.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateBox(
                        title = "Barang Tidak Ditemukan",
                        subtitle = "Coba ubah kata kunci pencarian atau pilih kategori lain."
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.items, key = { it.id }) { barang ->
                        InventoryItemCard(
                            barang = barang,
                            onClick = { onItemClick(barang.id) },
                            onShowQr = { qrPreviewBarang = barang }
                        )
                    }
                }
            }
        }
    }

    // Dialog pratinjau cepat QR Code dari Kode Barang
    qrPreviewBarang?.let { barang ->
        AlertDialog(
            onDismissRequest = { qrPreviewBarang = null },
            containerColor = PureWhite,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "QR Code Barang",
                    style = MaterialTheme.typography.titleLarge,
                    color = DarkSlateText
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = barang.namaBarang,
                        style = MaterialTheme.typography.titleSmall,
                        color = DarkSlateText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = barang.kodeBarang,
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMono),
                        color = DeepSkyBlue
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = LightSkyBlue,
                        border = BorderStroke(1.dp, SkyBluePrimary)
                    ) {
                        QrCodeImage(
                            kodeBarang = barang.kodeBarang,
                            size = 200.dp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pindai QR ini melalui menu Scan QR untuk pinjam atau kembalikan lebih cepat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateSecondaryText
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { qrPreviewBarang = null }) {
                    Text("Tutup", color = DeepSkyBlue, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun InventoryItemCard(
    barang: BarangEntity,
    onClick: () -> Unit,
    onShowQr: () -> Unit
) {
    val kondisiEnum = KondisiBarang.fromLabel(barang.kondisi)
    val (kondisiBg, kondisiFg) = when (kondisiEnum) {
        KondisiBarang.BAIK -> StockGreenBg to StockGreenText
        KondisiBarang.RUSAK_RINGAN -> StockAmberBg to StockAmberText
        KondisiBarang.RUSAK_BERAT -> StockRedBg to StockRedText
    }

    EduCard(
        containerColor = PureWhite,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("inventory_item_card_${barang.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BarangThumbnail(
                fotoUri = barang.fotoUri,
                kategori = barang.kategori,
                contentDescription = barang.namaBarang,
                size = 68.dp
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LightSkyBlue
                    ) {
                        Text(
                            text = barang.kodeBarang,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                            color = DeepSkyBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = barang.kategori,
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateSecondaryText
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = barang.namaBarang,
                    style = MaterialTheme.typography.titleMedium,
                    color = DarkSlateText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = null,
                        tint = SlateSecondaryText,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = barang.lokasiRak,
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateSecondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StockStatusBadge(stokTersedia = barang.stokTersedia)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = kondisiBg
                    ) {
                        Text(
                            text = barang.kondisi,
                            style = MaterialTheme.typography.labelSmall,
                            color = kondisiFg,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = onShowQr,
                modifier = Modifier.testTag("item_qr_button_${barang.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCode2,
                    contentDescription = "Lihat QR Code ${barang.namaBarang}",
                    tint = DeepSkyBlue
                )
            }
        }
    }
}
