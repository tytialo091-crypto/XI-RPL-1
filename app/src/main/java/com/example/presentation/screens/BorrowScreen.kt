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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.datastore.SessionState
import com.example.data.local.entity.BarangEntity
import com.example.data.local.entity.UserEntity
import com.example.domain.model.KondisiBarang
import com.example.domain.model.PeminjamanDetail
import com.example.domain.model.UserRole
import com.example.presentation.components.BarangThumbnail
import com.example.presentation.components.EduCard
import com.example.presentation.components.EmptyStateBox
import com.example.presentation.components.LoanStatusBadge
import com.example.presentation.components.ReturnLoanDialog
import com.example.presentation.components.SkyPrimaryButton
import com.example.presentation.components.StockStatusBadge
import com.example.presentation.viewmodel.LoanUiState
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
import com.example.utils.DateUtils

/**
 * Halaman Peminjaman & Pengembalian Barang (Simpan Pinjam Inventaris).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowScreen(
    session: SessionState,
    uiState: LoanUiState,
    preselectedBarangId: Long?,
    onPinjam: (
        barangId: Long,
        siswaId: Long,
        petugasId: Long?,
        jumlah: Int,
        tanggalPinjam: Long,
        tanggalKembali: Long,
        keperluan: String
    ) -> Unit,
    onKembalikan: (peminjamanId: Long, kondisi: KondisiBarang, fotoUri: String?, catatan: String) -> Unit,
    onScanQr: () -> Unit,
    onClearFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        val msg = uiState.successMessage ?: uiState.errorMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            onClearFeedback()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Simpan Pinjam Barang") },
                actions = {
                    IconButton(
                        onClick = onScanQr,
                        modifier = Modifier.testTag("borrow_scan_qr_button")
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan QR")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSkyBlue,
                    titleContentColor = PureWhite,
                    actionIconContentColor = PureWhite
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
                    text = { Text("Form Pinjam", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_form_pinjam")
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Text(
                            text = "Kembalikan (${uiState.activeLoans.size})",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    modifier = Modifier.testTag("tab_kembalikan")
                )
            }

            if (activeTab == 0) {
                BorrowFormTab(
                    session = session,
                    barangList = uiState.allBarang,
                    siswaList = uiState.allSiswa,
                    preselectedBarangId = preselectedBarangId,
                    isProcessing = uiState.isProcessing,
                    onSubmit = onPinjam,
                    onScanQr = onScanQr
                )
            } else {
                ReturnActiveLoansTab(
                    activeLoans = uiState.activeLoans,
                    onKembalikan = onKembalikan
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BorrowFormTab(
    session: SessionState,
    barangList: List<BarangEntity>,
    siswaList: List<UserEntity>,
    preselectedBarangId: Long?,
    isProcessing: Boolean,
    onSubmit: (Long, Long, Long?, Int, Long, Long, String) -> Unit,
    onScanQr: () -> Unit
) {
    val isSiswa = session.userRole == UserRole.SISWA
    var selectedSiswa by remember(siswaList, session.userId) {
        mutableStateOf(
            if (isSiswa) siswaList.find { it.id == session.userId }
            else siswaList.firstOrNull()
        )
    }
    var selectedBarang by remember(barangList, preselectedBarangId) {
        mutableStateOf(
            if (preselectedBarangId != null && preselectedBarangId > 0L) {
                barangList.find { it.id == preselectedBarangId }
            } else {
                barangList.firstOrNull { it.stokTersedia > 0 } ?: barangList.firstOrNull()
            }
        )
    }

    LaunchedEffect(preselectedBarangId, barangList) {
        if (preselectedBarangId != null && preselectedBarangId > 0L) {
            barangList.find { it.id == preselectedBarangId }?.let {
                selectedBarang = it
            }
        }
    }

    var jumlah by remember { mutableIntStateOf(1) }
    var tanggalPinjam by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var tanggalKembali by remember { mutableLongStateOf(DateUtils.tambahHari(System.currentTimeMillis(), 2)) }
    var keperluan by remember { mutableStateOf("") }

    var showSiswaPicker by remember { mutableStateOf(false) }
    var showBarangPicker by remember { mutableStateOf(false) }
    var showDatePickerPinjam by remember { mutableStateOf(false) }
    var showDatePickerKembali by remember { mutableStateOf(false) }

    val stokTersedia = selectedBarang?.stokTersedia ?: 0
    val stokHabis = selectedBarang != null && stokTersedia <= 0
    val jumlahMelebihiStok = selectedBarang != null && jumlah > stokTersedia
    val canSubmit = selectedSiswa != null &&
        selectedBarang != null &&
        !stokHabis &&
        !jumlahMelebihiStok &&
        jumlah > 0 &&
        keperluan.isNotBlank() &&
        !isProcessing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Card Pilih Siswa
        EduCard(containerColor = PureWhite, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("1. Data Peminjam (Siswa)", style = MaterialTheme.typography.titleMedium, color = DarkSlateText)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LightSkyBlue,
                    border = BorderStroke(1.dp, SkyBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isSiswa) { showSiswaPicker = true }
                        .testTag("select_siswa_box")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = DeepSkyBlue)
                            Column {
                                Text(
                                    text = selectedSiswa?.nama ?: "Pilih Siswa Peminjam",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DarkSlateText
                                )
                                Text(
                                    text = selectedSiswa?.let { "NIS ${it.nis} • ${it.kelas}" } ?: "Ketuk untuk memilih siswa",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateSecondaryText
                                )
                            }
                        }
                        if (!isSiswa) {
                            Text("Ubah", color = DeepSkyBlue, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Card Pilih Barang & Validasi Stok
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
                    Text("2. Pilih Barang Inventaris", style = MaterialTheme.typography.titleMedium, color = DarkSlateText)
                    TextButton(onClick = onScanQr) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = DeepSkyBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan QR", color = DeepSkyBlue)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LightSkyBlue,
                    border = BorderStroke(1.dp, SkyBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBarangPicker = true }
                        .testTag("select_barang_box")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (selectedBarang != null) {
                            BarangThumbnail(
                                fotoUri = selectedBarang!!.fotoUri,
                                kategori = selectedBarang!!.kategori,
                                contentDescription = selectedBarang!!.namaBarang,
                                size = 54.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedBarang!!.namaBarang,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DarkSlateText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${selectedBarang!!.kodeBarang} • ${selectedBarang!!.lokasiRak}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateSecondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                StockStatusBadge(stokTersedia = selectedBarang!!.stokTersedia)
                            }
                        } else {
                            Icon(Icons.Filled.Inventory2, contentDescription = null, tint = DeepSkyBlue)
                            Text("Ketuk untuk memilih barang", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                if (stokHabis) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StockRedBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = StockRedText)
                            Text(
                                text = "Stok barang ini sedang habis (0 unit). Pilih barang lain atau tunggu pengembalian.",
                                style = MaterialTheme.typography.bodySmall,
                                color = StockRedText
                            )
                        }
                    }
                }

                // Stepper Jumlah
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Jumlah Dipinjam", style = MaterialTheme.typography.titleSmall, color = DarkSlateText)
                        Text(
                            text = "Maksimal tersedia: $stokTersedia unit",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSecondaryText
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { if (jumlah > 1) jumlah-- },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("qty_minus_button")
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Kurangi jumlah", tint = DeepSkyBlue)
                        }
                        Text(
                            text = jumlah.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                            color = DarkSlateText,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(
                            onClick = { if (jumlah < stokTersedia) jumlah++ },
                            enabled = jumlah < stokTersedia,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("qty_plus_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Tambah jumlah", tint = DeepSkyBlue)
                        }
                    }
                }
            }
        }

        // Card Tanggal & Keperluan
        EduCard(containerColor = PureWhite, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("3. Jadwal & Keperluan", style = MaterialTheme.typography.titleMedium, color = DarkSlateText)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LightSkyBlue,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePickerPinjam = true }
                            .testTag("date_pinjam_picker")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = DeepSkyBlue, modifier = Modifier.size(14.dp))
                                Text("Tgl Pinjam", style = MaterialTheme.typography.labelSmall, color = SlateSecondaryText)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = DateUtils.formatTanggalPendek(tanggalPinjam),
                                style = MaterialTheme.typography.titleSmall,
                                color = DeepSkyBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LightSkyBlue,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePickerKembali = true }
                            .testTag("date_kembali_picker")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = DeepSkyBlue, modifier = Modifier.size(14.dp))
                                Text("Tgl Kembali", style = MaterialTheme.typography.labelSmall, color = SlateSecondaryText)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = DateUtils.formatTanggalPendek(tanggalKembali),
                                style = MaterialTheme.typography.titleSmall,
                                color = DeepSkyBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = keperluan,
                    onValueChange = { keperluan = it },
                    label = { Text("Keperluan Peminjaman") },
                    placeholder = { Text("Contoh: Praktikum Biologi kelas XI IPA 1") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepSkyBlue,
                        unfocusedBorderColor = SkyBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("borrow_keperluan_input")
                )

                Text(
                    text = "Catatan: Denda keterlambatan otomatis Rp 1.000 / hari setelah jatuh tempo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateSecondaryText
                )

                SkyPrimaryButton(
                    text = if (isProcessing) "Menyimpan..." else "Pinjam Barang Sekarang",
                    onClick = {
                        val b = selectedBarang ?: return@SkyPrimaryButton
                        val s = selectedSiswa ?: return@SkyPrimaryButton
                        val petugasId = if (!isSiswa) session.userId else null
                        onSubmit(b.id, s.id, petugasId, jumlah, tanggalPinjam, tanggalKembali, keperluan)
                        keperluan = ""
                        jumlah = 1
                    },
                    enabled = canSubmit,
                    icon = Icons.Filled.SwapHoriz,
                    testTag = "submit_pinjam_button",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Dialog Pilih Siswa
    if (showSiswaPicker) {
        var q by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSiswaPicker = false },
            containerColor = PureWhite,
            title = { Text("Pilih Siswa Peminjam", color = DarkSlateText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = q,
                        onValueChange = { q = it },
                        placeholder = { Text("Cari nama atau NIS...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        val list = siswaList.filter {
                            q.isBlank() || it.nama.contains(q, true) || it.nis.contains(q, true)
                        }
                        items(list, key = { it.id }) { siswa ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedSiswa?.id == siswa.id) LightSkyBlue else PureWhite,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSiswa = siswa
                                        showSiswaPicker = false
                                    }
                                    .padding(vertical = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(siswa.nama, style = MaterialTheme.typography.titleSmall, color = DarkSlateText)
                                    Text("NIS ${siswa.nis} • ${siswa.kelas}", style = MaterialTheme.typography.bodySmall, color = SlateSecondaryText)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSiswaPicker = false }) { Text("Tutup", color = DeepSkyBlue) }
            }
        )
    }

    // Dialog Pilih Barang
    if (showBarangPicker) {
        var q by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBarangPicker = false },
            containerColor = PureWhite,
            title = { Text("Pilih Barang Inventaris", color = DarkSlateText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = q,
                        onValueChange = { q = it },
                        placeholder = { Text("Cari nama atau kode...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyColumn(modifier = Modifier.height(280.dp)) {
                        val list = barangList.filter {
                            q.isBlank() || it.namaBarang.contains(q, true) || it.kodeBarang.contains(q, true)
                        }
                        items(list, key = { it.id }) { barang ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedBarang?.id == barang.id) LightSkyBlue else PureWhite,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedBarang = barang
                                        jumlah = if (barang.stokTersedia > 0) 1 else 0
                                        showBarangPicker = false
                                    }
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(barang.namaBarang, style = MaterialTheme.typography.titleSmall, color = DarkSlateText)
                                        Text("${barang.kodeBarang} • ${barang.kategori}", style = MaterialTheme.typography.bodySmall, color = SlateSecondaryText)
                                    }
                                    StockStatusBadge(stokTersedia = barang.stokTersedia)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBarangPicker = false }) { Text("Tutup", color = DeepSkyBlue) }
            }
        )
    }

    if (showDatePickerPinjam) {
        val state = rememberDatePickerState(initialSelectedDateMillis = tanggalPinjam)
        DatePickerDialog(
            onDismissRequest = { showDatePickerPinjam = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { tanggalPinjam = it }
                        showDatePickerPinjam = false
                    }
                ) { Text("Pilih", color = DeepSkyBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerPinjam = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showDatePickerKembali) {
        val state = rememberDatePickerState(initialSelectedDateMillis = tanggalKembali)
        DatePickerDialog(
            onDismissRequest = { showDatePickerKembali = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { tanggalKembali = it }
                        showDatePickerKembali = false
                    }
                ) { Text("Pilih", color = DeepSkyBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerKembali = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun ReturnActiveLoansTab(
    activeLoans: List<PeminjamanDetail>,
    onKembalikan: (peminjamanId: Long, kondisi: KondisiBarang, fotoUri: String?, catatan: String) -> Unit
) {
    var selectedLoan by remember { mutableStateOf<PeminjamanDetail?>(null) }

    if (activeLoans.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStateBox(
                title = "Semua Barang Telah Kembali",
                subtitle = "Tidak ada peminjaman aktif saat ini."
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(activeLoans, key = { it.peminjaman.id }) { detail ->
                EduCard(containerColor = PureWhite, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            BarangThumbnail(
                                fotoUri = detail.barang?.fotoUri,
                                kategori = detail.barang?.kategori ?: "Lainnya",
                                contentDescription = detail.barang?.namaBarang ?: "Barang",
                                size = 56.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = detail.barang?.namaBarang ?: "Barang",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DarkSlateText
                                )
                                Text(
                                    text = "${detail.peminjam?.nama ?: "Siswa"} (${detail.peminjam?.kelas ?: "-"}) • ${detail.peminjaman.jumlah} unit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateSecondaryText
                                )
                                Text(
                                    text = "Jatuh tempo: ${DateUtils.formatTanggalPendek(detail.peminjaman.tanggalKembaliRencana)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateSecondaryText
                                )
                            }
                            LoanStatusBadge(status = detail.statusTampil)
                        }

                        if (detail.isTerlambatLive) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = StockRedBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Terlambat ${detail.hariTerlambatLive} hari • Estimasi denda: ${DateUtils.formatRupiah(detail.estimasiDendaLive)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StockRedText,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Button(
                            onClick = { selectedLoan = detail },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepSkyBlue,
                                contentColor = PureWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("return_loan_button_${detail.peminjaman.id}")
                        ) {
                            Icon(Icons.Filled.AssignmentReturn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kembalikan + Upload Foto Kondisi")
                        }
                    }
                }
            }
        }
    }

    selectedLoan?.let { target ->
        ReturnLoanDialog(
            detail = target,
            onDismiss = { selectedLoan = null },
            onConfirmReturn = { kondisi, fotoUri, catatan ->
                onKembalikan(target.peminjaman.id, kondisi, fotoUri, catatan)
                selectedLoan = null
            }
        )
    }
}
