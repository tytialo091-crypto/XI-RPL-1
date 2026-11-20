package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.presentation.components.EduCard
import com.example.presentation.components.SkyPrimaryButton
import com.example.presentation.viewmodel.AuthUiState
import com.example.ui.theme.DeepSkyBlue
import com.example.ui.theme.LightSkyBlue
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SkyBorder
import com.example.ui.theme.SlateSecondaryText
import com.example.ui.theme.StockRedBg
import com.example.ui.theme.StockRedText

/**
 * Halaman Pendaftaran Siswa Baru (Nama, NIS, Kelas, No HP, dan Kata Sandi).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    uiState: AuthUiState,
    onRegister: (nama: String, nis: String, kelas: String, noHp: String, password: String) -> Unit,
    onBack: () -> Unit,
    onClearError: () -> Unit
) {
    var nama by remember { mutableStateOf("") }
    var nis by remember { mutableStateOf("") }
    var kelas by remember { mutableStateOf("") }
    var noHp by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Siswa Baru") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("register_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Login"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSkyBlue,
                    titleContentColor = PureWhite,
                    navigationIconContentColor = PureWhite
                )
            )
        },
        containerColor = PureWhite
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PureWhite)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = LightSkyBlue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Registrasi Anggota Perpustakaan & Sarpras",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepSkyBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Lengkapi data identitas siswa untuk meminjam perlengkapan sekolah secara digital.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateSecondaryText
                    )
                }
            }

            EduCard(containerColor = PureWhite, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = nama,
                        onValueChange = { nama = it; onClearError() },
                        label = { Text("Nama Lengkap Siswa") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = DeepSkyBlue) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepSkyBlue,
                            unfocusedBorderColor = SkyBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_nama_input")
                    )

                    OutlinedTextField(
                        value = nis,
                        onValueChange = { nis = it; onClearError() },
                        label = { Text("Nomor Induk Siswa (NIS)") },
                        leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null, tint = DeepSkyBlue) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepSkyBlue,
                            unfocusedBorderColor = SkyBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_nis_input")
                    )

                    OutlinedTextField(
                        value = kelas,
                        onValueChange = { kelas = it; onClearError() },
                        label = { Text("Kelas (contoh: XI IPA 1)") },
                        leadingIcon = { Icon(Icons.Filled.Class, contentDescription = null, tint = DeepSkyBlue) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepSkyBlue,
                            unfocusedBorderColor = SkyBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_kelas_input")
                    )

                    OutlinedTextField(
                        value = noHp,
                        onValueChange = { noHp = it; onClearError() },
                        label = { Text("Nomor HP / WhatsApp") },
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = DeepSkyBlue) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepSkyBlue,
                            unfocusedBorderColor = SkyBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_hp_input")
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; onClearError() },
                        label = { Text("Buat Kata Sandi") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = DeepSkyBlue) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepSkyBlue,
                            unfocusedBorderColor = SkyBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_password_input")
                    )

                    if (!uiState.errorMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StockRedBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = StockRedText,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    SkyPrimaryButton(
                        text = if (uiState.isLoading) "Mendaftarkan..." else "Daftar & Masuk dengan Google",
                        onClick = { onRegister(nama, nis, kelas, noHp, password) },
                        enabled = !uiState.isLoading,
                        testTag = "register_submit_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
