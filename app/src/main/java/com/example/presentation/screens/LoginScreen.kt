package com.example.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.UserRole
import com.example.presentation.components.EduCard
import com.example.presentation.components.SkyPrimaryButton
import com.example.presentation.viewmodel.AuthUiState
import com.example.ui.theme.DarkSlateText
import com.example.ui.theme.DeepSkyBlue
import com.example.ui.theme.LightSkyBlue
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SkyBluePrimary
import com.example.ui.theme.SkyBorder
import com.example.ui.theme.SlateSecondaryText
import com.example.ui.theme.StockRedBg
import com.example.ui.theme.StockRedText

/**
 * Halaman Login dengan pilihan peran Admin, Petugas, dan Siswa serta akses cepat akun demo.
 */
@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onGoogleSignIn: (role: UserRole) -> Unit,
    onLogin: (identifier: String, password: String, role: UserRole) -> Unit,
    onNavigateToRegister: () -> Unit,
    onClearError: () -> Unit
) {
    var selectedRole by remember { mutableStateOf(UserRole.ADMIN) }
    var identifier by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("123") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(LightSkyBlue, PureWhite, PureWhite)
                )
            )
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Header Banner Ilustrasi Inventaris Sekolah
        EduCard(
            containerColor = LightSkyBlue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Image(
                    painter = painterResource(id = R.drawable.img_school_banner),
                    contentDescription = "Ilustrasi perlengkapan inventaris sekolah",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "EDU-PINJAM",
                        style = MaterialTheme.typography.headlineSmall,
                        color = DeepSkyBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "SMKS BRIGJEND KATAMSO 2 by Bintang Arya Hafidsyah",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateSecondaryText
                    )
                    Text(
                        text = "Masuk untuk mengelola atau meminjam inventaris perlengkapan sekolah.",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateSecondaryText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        EduCard(
            containerColor = PureWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Pilih Peran Akun",
                    style = MaterialTheme.typography.titleMedium,
                    color = DarkSlateText
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UserRole.entries.forEach { role ->
                        val icon = when (role) {
                            UserRole.ADMIN -> Icons.Filled.AdminPanelSettings
                            UserRole.PETUGAS -> Icons.Filled.Badge
                            UserRole.SISWA -> Icons.Filled.School
                        }
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = {
                                selectedRole = role
                                onClearError()
                                when (role) {
                                    UserRole.ADMIN -> {
                                        identifier = "admin"
                                        password = "123"
                                    }
                                    UserRole.PETUGAS -> {
                                        identifier = "petugas"
                                        password = "123"
                                    }
                                    UserRole.SISWA -> {
                                        identifier = "2024001"
                                        password = "123"
                                    }
                                }
                            },
                            label = { Text(role.label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DeepSkyBlue,
                                selectedLabelColor = PureWhite,
                                selectedLeadingIconColor = PureWhite
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("role_chip_${role.name.lowercase()}")
                        )
                    }
                }

                // Tombol utama Masuk dengan Google untuk akses Cloud Firestore
                Button(
                    onClick = {
                        onClearError()
                        onGoogleSignIn(selectedRole)
                    },
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepSkyBlue,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("google_signin_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudDone,
                        contentDescription = "Masuk dengan Google",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = if (uiState.isLoading) "Menghubungkan Google..." else "Masuk dengan Google (Cloud)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = SkyBorder)
                    Text(
                        text = "atau verifikasi akun lokal",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateSecondaryText
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = SkyBorder)
                }

                OutlinedTextField(
                    value = identifier,
                    onValueChange = {
                        identifier = it
                        onClearError()
                    },
                    label = {
                        Text(if (selectedRole == UserRole.SISWA) "NIS Siswa" else "Username")
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = DeepSkyBlue)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepSkyBlue,
                        unfocusedBorderColor = SkyBorder
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_identifier_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        onClearError()
                    },
                    label = { Text("Kata Sandi") },
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = DeepSkyBlue)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Tampilkan kata sandi",
                                tint = SlateSecondaryText
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepSkyBlue,
                        unfocusedBorderColor = SkyBorder
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input")
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
                    text = if (uiState.isLoading) "Memproses..." else "Verifikasi Lokal & Masuk Google",
                    onClick = { onLogin(identifier, password, selectedRole) },
                    enabled = !uiState.isLoading,
                    testTag = "login_submit_button",
                    modifier = Modifier.fillMaxWidth()
                )

                // Akses cepat akun bawaan untuk uji coba langsung
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Akun Uji Coba Bawaan (Sandi: 123):",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateSecondaryText
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                selectedRole = UserRole.ADMIN
                                identifier = "admin"
                                password = "123"
                            },
                            label = { Text("admin", style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = LightSkyBlue),
                            border = BorderStroke(1.dp, SkyBluePrimary)
                        )
                        AssistChip(
                            onClick = {
                                selectedRole = UserRole.PETUGAS
                                identifier = "petugas"
                                password = "123"
                            },
                            label = { Text("petugas", style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = LightSkyBlue),
                            border = BorderStroke(1.dp, SkyBluePrimary)
                        )
                        AssistChip(
                            onClick = {
                                selectedRole = UserRole.SISWA
                                identifier = "2024001"
                                password = "123"
                            },
                            label = { Text("NIS 2024001", style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = LightSkyBlue),
                            border = BorderStroke(1.dp, SkyBluePrimary)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Siswa belum memiliki akun?",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateSecondaryText
            )
            TextButton(
                onClick = onNavigateToRegister,
                modifier = Modifier.testTag("go_to_register_button")
            ) {
                Text(
                    text = "Daftar Siswa Baru",
                    color = DeepSkyBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
