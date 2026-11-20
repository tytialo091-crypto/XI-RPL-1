package com.example.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.entity.BarangEntity
import com.example.domain.model.KondisiBarang
import com.example.domain.model.PeminjamanDetail
import com.example.presentation.components.BarangThumbnail
import com.example.presentation.components.EduCard
import com.example.presentation.components.ReturnLoanDialog
import com.example.presentation.components.SkyPrimaryButton
import com.example.presentation.components.StockStatusBadge
import com.example.ui.theme.DarkSlateText
import com.example.ui.theme.DeepSkyBlue
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LightSkyBlue
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SkyBluePrimary
import com.example.ui.theme.SkyBorder
import com.example.ui.theme.SlateSecondaryText
import com.example.ui.theme.StockRedBg
import com.example.ui.theme.StockRedText
import com.example.utils.QrCodeAnalyzer
import com.example.utils.QrCodeUtils
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Halaman Scan QR Code menggunakan CameraX + ZXing dengan fallback galeri dan input kode manual.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    activeLoans: List<PeminjamanDetail>,
    onLookupKode: suspend (String) -> BarangEntity?,
    onNavigateToPinjam: (Long) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onKembalikan: (Long, KondisiBarang, String?, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var torchEnabled by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var manualKode by remember { mutableStateOf("") }
    var scannedKode by remember { mutableStateOf<String?>(null) }
    var matchedBarang by remember { mutableStateOf<BarangEntity?>(null) }
    var lookupError by remember { mutableStateOf<String?>(null) }
    var loanToReturn by remember { mutableStateOf<PeminjamanDetail?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val galleryQrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val decoded = QrCodeUtils.decodeQrFromUri(context, uri)
            if (decoded != null) {
                scannedKode = decoded
                manualKode = decoded
            } else {
                Toast.makeText(context, "QR Code tidak terbaca pada gambar tersebut.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(scannedKode) {
        val kode = scannedKode ?: return@LaunchedEffect
        val found = onLookupKode(kode)
        matchedBarang = found
        lookupError = if (found == null) "Kode \"$kode\" tidak terdaftar di inventaris." else null
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    val activeLoanForMatched = remember(matchedBarang, activeLoans) {
        val targetId = matchedBarang?.id ?: return@remember null
        activeLoans.firstOrNull { it.peminjaman.barangId == targetId }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Scan QR Barang") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("qr_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                actions = {
                    if (hasCameraPermission) {
                        IconButton(
                            onClick = {
                                torchEnabled = !torchEnabled
                                boundCamera?.cameraControl?.enableTorch(torchEnabled)
                            }
                        ) {
                            Icon(
                                imageVector = if (torchEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                contentDescription = "Lampu kilat"
                            )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Area Kamera CameraX + ZXing Analyzer
            EduCard(containerColor = LightSkyBlue, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    try {
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }
                                        val analysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()
                                            .also { imageAnalysis ->
                                                imageAnalysis.setAnalyzer(
                                                    cameraExecutor,
                                                    QrCodeAnalyzer { decoded ->
                                                        scannedKode = decoded
                                                        manualKode = decoded
                                                    }
                                                )
                                            }
                                        cameraProvider.unbindAll()
                                        boundCamera = cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            analysis
                                        )
                                    } catch (_: Exception) {
                                        // Kamera tidak tersedia pada perangkat/emulator tertentu
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        // Bingkai target QR Sky Blue
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .border(3.dp, SkyBluePrimary, RoundedCornerShape(18.dp))
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = null,
                                tint = DeepSkyBlue,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Izin kamera diperlukan untuk memindai QR Code barang secara langsung.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkSlateText
                            )
                            SkyPrimaryButton(
                                text = "Izinkan Kamera",
                                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                testTag = "grant_camera_button"
                            )
                        }
                    }
                }
            }

            // Pilih gambar QR dari galeri + Input manual kode barang
            EduCard(containerColor = PureWhite, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Cari atau Pindai Kode Barang",
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkSlateText
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualKode,
                            onValueChange = { manualKode = it },
                            label = { Text("Kode Barang (misal: ELK-001)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DeepSkyBlue,
                                unfocusedBorderColor = SkyBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("qr_manual_code_input")
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    val found = onLookupKode(manualKode)
                                    matchedBarang = found
                                    lookupError = if (found == null) "Kode \"$manualKode\" tidak ditemukan." else null
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepSkyBlue, contentColor = PureWhite),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("qr_manual_lookup_button")
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = "Cari kode")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                galleryQrLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SkyBluePrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.ImageSearch, contentDescription = null, tint = DeepSkyBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("QR dari Galeri", color = DeepSkyBlue, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // Chip kode cepat untuk simulasi pemindaian
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ELK-001", "ELK-002", "OLR-001").forEach { sampleCode ->
                            AssistChip(
                                onClick = {
                                    manualKode = sampleCode
                                    scannedKode = sampleCode
                                },
                                label = {
                                    Text(
                                        sampleCode,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(containerColor = LightSkyBlue),
                                border = BorderStroke(1.dp, SkyBorder)
                            )
                        }
                    }
                }
            }

            if (!lookupError.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StockRedBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = lookupError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = StockRedText,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // Hasil Barang Terdeteksi dari QR
            matchedBarang?.let { barang ->
                EduCard(
                    containerColor = LightSkyBlue,
                    borderColor = DeepSkyBlue,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            BarangThumbnail(
                                fotoUri = barang.fotoUri,
                                kategori = barang.kategori,
                                contentDescription = barang.namaBarang,
                                size = 60.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = DeepSkyBlue, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = barang.kodeBarang,
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
                                        color = DeepSkyBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = barang.namaBarang,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DarkSlateText
                                )
                                StockStatusBadge(stokTersedia = barang.stokTersedia)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SkyPrimaryButton(
                                text = "Pinjam Cepat",
                                onClick = { onNavigateToPinjam(barang.id) },
                                enabled = barang.stokTersedia > 0,
                                icon = Icons.Filled.SwapHoriz,
                                testTag = "qr_quick_borrow_button",
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = { onNavigateToDetail(barang.id) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, DeepSkyBlue),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                            ) {
                                Icon(Icons.Filled.Info, contentDescription = null, tint = DeepSkyBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Detail", color = DeepSkyBlue)
                            }
                        }

                        if (activeLoanForMatched != null) {
                            Button(
                                onClick = { loanToReturn = activeLoanForMatched },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PureWhite,
                                    contentColor = DeepSkyBlue
                                ),
                                border = BorderStroke(1.dp, DeepSkyBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.AssignmentReturn, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kembalikan (${activeLoanForMatched.peminjam?.nama ?: "Siswa"})")
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
                Toast.makeText(context, "Pengembalian berhasil diproses!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
