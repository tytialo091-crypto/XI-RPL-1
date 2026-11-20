package com.example.presentation.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.BarangEntity
import com.example.domain.model.KategoriBarang
import com.example.domain.model.KondisiBarang
import com.example.presentation.components.BarangThumbnail
import com.example.presentation.components.EduCard
import com.example.presentation.components.SkyPrimaryButton
import com.example.ui.theme.DarkSlateText
import com.example.ui.theme.DeepSkyBlue
import com.example.ui.theme.LightSkyBlue
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SkyBluePrimary
import com.example.ui.theme.SkyBorder
import com.example.ui.theme.StockRedBg
import com.example.ui.theme.StockRedText

/**
 * Halaman Tambah / Edit Barang Inventaris lengkap dengan Photo Picker dan auto-generate kode barang.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemFormScreen(
    existingBarang: BarangEntity?,
    errorMessage: String?,
    onSave: (BarangEntity) -> Unit,
    onBack: () -> Unit,
    onClearError: () -> Unit
) {
    val isEdit = existingBarang != null
    var namaBarang by remember { mutableStateOf("") }
    var kodeBarang by remember { mutableStateOf("") }
    var selectedKategori by remember { mutableStateOf(KategoriBarang.ELEKTRONIK) }
    var stokTotalText by remember { mutableStateOf("5") }
    var stokTersediaText by remember { mutableStateOf("5") }
    var selectedKondisi by remember { mutableStateOf(KondisiBarang.BAIK) }
    var lokasiRak by remember { mutableStateOf("") }
    var deskripsi by remember { mutableStateOf("") }
    var fotoUri by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(existingBarang?.id) {
        if (existingBarang != null) {
            namaBarang = existingBarang.namaBarang
            kodeBarang = existingBarang.kodeBarang
            selectedKategori = KategoriBarang.fromLabel(existingBarang.kategori)
            stokTotalText = existingBarang.stokTotal.toString()
            stokTersediaText = existingBarang.stokTersedia.toString()
            selectedKondisi = KondisiBarang.fromLabel(existingBarang.kondisi)
            lokasiRak = existingBarang.lokasiRak
            deskripsi = existingBarang.deskripsi
            fotoUri = existingBarang.fotoUri
        } else if (kodeBarang.isBlank()) {
            kodeBarang = "BRG-${(100..999).random()}"
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            fotoUri = uri.toString()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Barang" else "Tambah Barang Baru") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("item_form_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSkyBlue,
                    titleContentColor = PureWhite,
                    navigationIconContentColor = PureWhite
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EduCard(containerColor = PureWhite, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Foto Barang dengan Photo Picker
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        BarangThumbnail(
                            fotoUri = fotoUri,
                            kategori = selectedKategori.label,
                            contentDescription = "Foto barang",
                            size = 76.dp
                        )
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SkyBluePrimary),
                            modifier = Modifier.testTag("pick_item_photo_button")
                        ) {
                            Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = DeepSkyBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (fotoUri == null) "Pilih Foto Barang" else "Ganti Foto",
                                color = DeepSkyBlue
                            )
                        }
                    }

                    OutlinedTextField(
                        value = namaBarang,
                        onValueChange = { namaBarang = it; onClearError() },
                        label = { Text("Nama Barang") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepSkyBlue,
                            unfocusedBorderColor = SkyBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_nama_barang_input")
                    )

                    OutlinedTextField(
                        value = kodeBarang,
                        onValueChange = { kodeBarang = it; onClearError() },
                        label = { Text("Kode Barang (Untuk QR Code)") },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val prefix = selectedKategori.label.take(3).uppercase()
                                    kodeBarang = "$prefix-${(100..999).random()}"
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoFixHigh,
                                    contentDescription = "Buat kode otomatis",
                                    tint = DeepSkyBlue
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepSkyBlue,
                            unfocusedBorderColor = SkyBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_kode_barang_input")
                    )

                    Text("Kategori Barang", style = MaterialTheme.typography.labelLarge, color = DarkSlateText)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KategoriBarang.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedKategori == cat,
                                onClick = { selectedKategori = cat },
                                label = { Text(cat.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DeepSkyBlue,
                                    selectedLabelColor = PureWhite,
                                    containerColor = LightSkyBlue
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = stokTotalText,
                            onValueChange = { value ->
                                val digits = value.filter { it.isDigit() }
                                stokTotalText = digits
                                if (!isEdit) stokTersediaText = digits
                                onClearError()
                            },
                            label = { Text("Stok Total") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DeepSkyBlue,
                                unfocusedBorderColor = SkyBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("form_stok_total_input")
                        )

                        OutlinedTextField(
                            value = stokTersediaText,
                            onValueChange = { value ->
                                stokTersediaText = value.filter { it.isDigit() }
                                onClearError()
                            },
                            label = { Text("Stok Tersedia") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DeepSkyBlue,
                                unfocusedBorderColor = SkyBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("form_stok_tersedia_input")
                        )
                    }

                    Text("Kondisi Barang", style = MaterialTheme.typography.labelLarge, color = DarkSlateText)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KondisiBarang.entries.forEach { kondisi ->
                            FilterChip(
                                selected = selectedKondisi == kondisi,
                                onClick = { selectedKondisi = kondisi },
                                label = { Text(kondisi.label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DeepSkyBlue,
                                    selectedLabelColor = PureWhite,
                                    containerColor = LightSkyBlue
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = lokasiRak,
                        onValueChange = { lokasiRak = it; onClearError() },
                        label = { Text("Lokasi Rak / Ruang") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepSkyBlue,
                            unfocusedBorderColor = SkyBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_lokasi_rak_input")
                    )

                    OutlinedTextField(
                        value = deskripsi,
                        onValueChange = { deskripsi = it },
                        label = { Text("Deskripsi / Kelengkapan (Opsional)") },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepSkyBlue,
                            unfocusedBorderColor = SkyBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StockRedBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = StockRedText,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    SkyPrimaryButton(
                        text = if (isEdit) "Simpan Perubahan" else "Simpan Barang",
                        onClick = {
                            val total = stokTotalText.toIntOrNull() ?: 0
                            val tersedia = stokTersediaText.toIntOrNull() ?: 0
                            onSave(
                                BarangEntity(
                                    id = existingBarang?.id ?: 0L,
                                    kodeBarang = kodeBarang,
                                    namaBarang = namaBarang,
                                    kategori = selectedKategori.label,
                                    stokTotal = total,
                                    stokTersedia = tersedia,
                                    kondisi = selectedKondisi.label,
                                    fotoUri = fotoUri,
                                    lokasiRak = lokasiRak,
                                    deskripsi = deskripsi,
                                    createdAt = existingBarang?.createdAt ?: System.currentTimeMillis()
                                )
                            )
                        },
                        icon = Icons.Filled.Save,
                        testTag = "form_save_barang_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
