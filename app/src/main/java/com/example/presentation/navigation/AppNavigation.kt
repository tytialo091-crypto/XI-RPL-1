package com.example.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.EduPinjamApp
import com.example.data.datastore.SessionState
import com.example.data.remote.FirestoreInventoryRepository
import com.example.domain.model.UserRole
import com.example.presentation.screens.BorrowScreen
import com.example.presentation.screens.DashboardScreen
import com.example.presentation.screens.HistoryReportScreen
import com.example.presentation.screens.InventoryScreen
import com.example.presentation.screens.ItemDetailScreen
import com.example.presentation.screens.ItemFormScreen
import com.example.presentation.screens.LoginScreen
import com.example.presentation.screens.ProfileSettingsScreen
import com.example.presentation.screens.QrScannerScreen
import com.example.presentation.screens.RegisterScreen
import com.example.presentation.screens.SplashScreen
import com.example.presentation.viewmodel.AuthViewModel
import com.example.presentation.viewmodel.CloudSyncViewModel
import com.example.presentation.viewmodel.CloudSyncViewModelFactory
import com.example.presentation.viewmodel.DashboardViewModel
import com.example.presentation.viewmodel.EduPinjamViewModelFactory
import com.example.presentation.viewmodel.InventoryViewModel
import com.example.presentation.viewmodel.LoanViewModel
import com.example.presentation.viewmodel.ProfileViewModel
import com.example.ui.theme.DarkSlateText
import com.example.ui.theme.DeepSkyBlue
import com.example.ui.theme.LightSkyBlue
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateSecondaryText
import com.example.utils.GoogleAuthHelper
import com.example.utils.GoogleAuthHelper.authStateFlow
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.serialization.Serializable

// ==================== RUTE NAVIGASI TYPE-SAFE ====================

@Serializable object SplashRoute
@Serializable object LoginRoute
@Serializable object RegisterRoute
@Serializable object DashboardRoute
@Serializable object InventoryRoute
@Serializable data class ItemDetailRoute(val barangId: Long)
@Serializable data class ItemFormRoute(val barangId: Long = -1L)
@Serializable data class BorrowRoute(val preselectedBarangId: Long = -1L)
@Serializable object HistoryRoute
@Serializable object QrScannerRoute
@Serializable object ProfileRoute

private data class BottomTabItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: Any,
    val testTag: String
)

private val bottomTabs = listOf(
    BottomTabItem("Beranda", Icons.Filled.Home, Icons.Outlined.Home, DashboardRoute, "nav_beranda"),
    BottomTabItem("Inventaris", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, InventoryRoute, "nav_inventaris"),
    BottomTabItem("Pinjam", Icons.Filled.SwapHoriz, Icons.Outlined.SwapHoriz, BorrowRoute(), "nav_pinjam"),
    BottomTabItem("Riwayat", Icons.Filled.History, Icons.Outlined.History, HistoryRoute, "nav_riwayat"),
    BottomTabItem("Profil", Icons.Filled.Person, Icons.Outlined.Person, ProfileRoute, "nav_profil")
)

/**
 * Pengelola navigasi utama aplikasi EDU-PINJAM dengan Auth Gating Google Sign-In
 * sebelum mengaktifkan ViewModel dan listener Cloud Firestore.
 */
@Composable
fun AppNavigation(
    factory: EduPinjamViewModelFactory,
    sessionState: SessionState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val authStateFlow = remember { Firebase.auth.authStateFlow() }
    val firebaseUser by authStateFlow.collectAsStateWithLifecycle(
        initialValue = Firebase.auth.currentUser
    )

    val authViewModel: AuthViewModel = viewModel(factory = factory)

    // Coba auto sign-in senyap untuk pengguna yang pernah masuk dengan Google
    LaunchedEffect(Unit) {
        GoogleAuthHelper.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { user ->
                if (!sessionState.isLoggedIn) {
                    authViewModel.completeGoogleSession(
                        displayName = user.displayName,
                        email = user.email,
                        role = sessionState.userRole
                    )
                }
            },
            onUnauthenticated = {},
            scope = scope
        )
    }

    val activeUser = firebaseUser
    if (activeUser == null) {
        // Pengguna belum terautentikasi: hanya tampilkan alur Splash / Login / Register
        UnauthenticatedNavGraph(
            authViewModel = authViewModel,
            credentialManager = credentialManager
        )
    } else {
        // Kunci seluruh tree dan ViewModel dengan UID Google aktif agar listener terisolasi bersih
        key(activeUser.uid) {
            AuthenticatedNavGraph(
                firebaseUser = activeUser,
                factory = factory,
                sessionState = sessionState,
                credentialManager = credentialManager
            )
        }
    }
}

@Composable
private fun UnauthenticatedNavGraph(
    authViewModel: AuthViewModel,
    credentialManager: CredentialManager
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authUi by authViewModel.uiState.collectAsStateWithLifecycle()

    fun launchGoogleSignIn(role: UserRole) {
        authViewModel.setLoading(true)
        GoogleAuthHelper.onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { user ->
                authViewModel.completeGoogleSession(
                    displayName = user.displayName,
                    email = user.email,
                    role = role
                )
            },
            onAuthError = { errorMsg ->
                authViewModel.setError(errorMsg)
            },
            onAuthCancelled = {
                authViewModel.setLoading(false)
            },
            scope = scope
        )
    }

    NavHost(
        navController = navController,
        startDestination = SplashRoute
    ) {
        composable<SplashRoute> {
            SplashScreen(
                isInitialized = authUi.isInitialized,
                isFirebaseAuthenticated = false,
                onNavigateToLogin = {
                    navController.navigate(LoginRoute) {
                        popUpTo<SplashRoute> { inclusive = true }
                    }
                }
            )
        }

        composable<LoginRoute> {
            LoginScreen(
                uiState = authUi,
                onGoogleSignIn = { selectedRole ->
                    launchGoogleSignIn(selectedRole)
                },
                onLogin = { id, pass, role ->
                    authViewModel.login(id, pass, role) {
                        // Setelah akun lokal terverifikasi, wajib autentikasi Google untuk Cloud Firestore
                        if (Firebase.auth.currentUser == null) {
                            launchGoogleSignIn(role)
                        }
                    }
                },
                onNavigateToRegister = {
                    authViewModel.clearMessage()
                    navController.navigate(RegisterRoute)
                },
                onClearError = { authViewModel.clearMessage() }
            )
        }

        composable<RegisterRoute> {
            RegisterScreen(
                uiState = authUi,
                onRegister = { nama, nis, kelas, noHp, pass ->
                    authViewModel.registerSiswa(nama, nis, kelas, noHp, pass) {
                        if (Firebase.auth.currentUser == null) {
                            launchGoogleSignIn(UserRole.SISWA)
                        }
                    }
                },
                onBack = { navController.popBackStack() },
                onClearError = { authViewModel.clearMessage() }
            )
        }
    }
}

@Composable
private fun AuthenticatedNavGraph(
    firebaseUser: FirebaseUser,
    factory: EduPinjamViewModelFactory,
    sessionState: SessionState,
    credentialManager: CredentialManager
) {
    val context = LocalContext.current
    val app = context.applicationContext as EduPinjamApp
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val firestoreRepository = remember(firebaseUser.uid, app) {
        FirestoreInventoryRepository(app)
    }
    val cloudFactory = remember(firebaseUser.uid, app) {
        CloudSyncViewModelFactory(
            firestoreRepository = firestoreRepository,
            localRepository = app.repository,
            database = app.database,
            currentUserId = firebaseUser.uid
        )
    }

    val cloudSyncViewModel: CloudSyncViewModel = viewModel(
        key = "cloud_${firebaseUser.uid}",
        factory = cloudFactory
    )
    val dashboardViewModel: DashboardViewModel = viewModel(
        key = "dash_${firebaseUser.uid}",
        factory = factory
    )
    val inventoryViewModel: InventoryViewModel = viewModel(
        key = "inv_${firebaseUser.uid}",
        factory = factory
    )
    val loanViewModel: LoanViewModel = viewModel(
        key = "loan_${firebaseUser.uid}",
        factory = factory
    )
    val profileViewModel: ProfileViewModel = viewModel(
        key = "prof_${firebaseUser.uid}",
        factory = factory
    )

    // Pastikan sesi lokal DataStore sinkron saat login otomatis Google
    LaunchedEffect(firebaseUser.uid) {
        if (!sessionState.isLoggedIn) {
            app.repository.completeGoogleSession(
                displayName = firebaseUser.displayName,
                email = firebaseUser.email,
                role = sessionState.userRole
            )
        }
    }

    val cloudState by cloudSyncViewModel.cloudState.collectAsStateWithLifecycle()
    val isSyncingCloud by cloudSyncViewModel.isSyncingCloud.collectAsStateWithLifecycle()
    val cloudFeedback by cloudSyncViewModel.cloudFeedback.collectAsStateWithLifecycle()

    val showMainNavigation = currentDestination?.let { dest ->
        dest.hasRoute<DashboardRoute>() ||
            dest.hasRoute<InventoryRoute>() ||
            dest.hasRoute<BorrowRoute>() ||
            dest.hasRoute<HistoryRoute>() ||
            dest.hasRoute<ProfileRoute>()
    } == true

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showMainNavigation && !useNavigationRail) {
                    NavigationBar(
                        containerColor = PureWhite,
                        contentColor = DeepSkyBlue,
                        tonalElevation = 4.dp,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        bottomTabs.forEach { tab ->
                            val selected = when (tab.route) {
                                is DashboardRoute -> currentDestination?.hasRoute<DashboardRoute>() == true
                                is InventoryRoute -> currentDestination?.hasRoute<InventoryRoute>() == true
                                is BorrowRoute -> currentDestination?.hasRoute<BorrowRoute>() == true
                                is HistoryRoute -> currentDestination?.hasRoute<HistoryRoute>() == true
                                is ProfileRoute -> currentDestination?.hasRoute<ProfileRoute>() == true
                                else -> false
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DeepSkyBlue,
                                    selectedTextColor = DeepSkyBlue,
                                    indicatorColor = LightSkyBlue,
                                    unselectedIconColor = SlateSecondaryText,
                                    unselectedTextColor = SlateSecondaryText
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showMainNavigation && useNavigationRail) {
                    NavigationRail(
                        containerColor = PureWhite,
                        contentColor = DeepSkyBlue
                    ) {
                        bottomTabs.forEach { tab ->
                            val selected = when (tab.route) {
                                is DashboardRoute -> currentDestination?.hasRoute<DashboardRoute>() == true
                                is InventoryRoute -> currentDestination?.hasRoute<InventoryRoute>() == true
                                is BorrowRoute -> currentDestination?.hasRoute<BorrowRoute>() == true
                                is HistoryRoute -> currentDestination?.hasRoute<HistoryRoute>() == true
                                is ProfileRoute -> currentDestination?.hasRoute<ProfileRoute>() == true
                                else -> false
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, color = if (selected) DeepSkyBlue else DarkSlateText) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = DeepSkyBlue,
                                    selectedTextColor = DeepSkyBlue,
                                    indicatorColor = LightSkyBlue,
                                    unselectedIconColor = SlateSecondaryText,
                                    unselectedTextColor = SlateSecondaryText
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = DashboardRoute
                    ) {
                        composable<DashboardRoute> {
                            val dashUi by dashboardViewModel.uiState.collectAsStateWithLifecycle()
                            DashboardScreen(
                                session = sessionState,
                                uiState = dashUi,
                                cloudState = cloudState,
                                isSyncingCloud = isSyncingCloud,
                                cloudFeedback = cloudFeedback,
                                firebaseUserEmail = firebaseUser.email,
                                onSyncToCloud = cloudSyncViewModel::syncNowToCloud,
                                onAddBarang = { navController.navigate(ItemFormRoute(-1L)) },
                                onPinjamBarang = { navigateTopLevel(BorrowRoute()) },
                                onScanQr = { navController.navigate(QrScannerRoute) },
                                onOpenInventory = {
                                    inventoryViewModel.setStockFilter(null)
                                    navigateTopLevel(InventoryRoute)
                                },
                                onOpenInventoryWithFilter = { stockFilter ->
                                    inventoryViewModel.setStockFilter(stockFilter)
                                    navigateTopLevel(InventoryRoute)
                                },
                                onOpenHistory = { navigateTopLevel(HistoryRoute) },
                                onTriggerSync = dashboardViewModel::triggerSyncNow,
                                onOpenSyncSettings = { navigateTopLevel(ProfileRoute) },
                                onReturnLoan = { peminjamanId, kondisi, fotoUri, catatan ->
                                    loanViewModel.kembalikanBarang(peminjamanId, kondisi, fotoUri, catatan)
                                }
                            )
                        }

                        composable<InventoryRoute> {
                            val invUi by inventoryViewModel.uiState.collectAsStateWithLifecycle()
                            InventoryScreen(
                                session = sessionState,
                                uiState = invUi,
                                onSearchChange = inventoryViewModel::setSearchQuery,
                                onCategorySelect = inventoryViewModel::setCategory,
                                onStockFilterSelect = inventoryViewModel::setStockFilter,
                                onSortSelect = inventoryViewModel::setSortOption,
                                onItemClick = { id -> navController.navigate(ItemDetailRoute(id)) },
                                onAddBarang = { navController.navigate(ItemFormRoute(-1L)) },
                                onScanQr = { navController.navigate(QrScannerRoute) }
                            )
                        }

                        composable<ItemDetailRoute> { backStackEntry ->
                            val args = backStackEntry.toRoute<ItemDetailRoute>()
                            val barang by inventoryViewModel.observeBarangDetail(args.barangId)
                                .collectAsStateWithLifecycle(initialValue = null)
                            val history by inventoryViewModel.observeBarangHistory(args.barangId)
                                .collectAsStateWithLifecycle(initialValue = emptyList())

                            ItemDetailScreen(
                                session = sessionState,
                                barang = barang,
                                history = history,
                                onBack = { navController.popBackStack() },
                                onEdit = { id -> navController.navigate(ItemFormRoute(id)) },
                                onDelete = { entity ->
                                    inventoryViewModel.deleteBarang(entity) {
                                        navController.popBackStack()
                                    }
                                },
                                onPinjamBarang = { id ->
                                    navController.navigate(BorrowRoute(preselectedBarangId = id))
                                }
                            )
                        }

                        composable<ItemFormRoute> { backStackEntry ->
                            val args = backStackEntry.toRoute<ItemFormRoute>()
                            val existing by inventoryViewModel.observeBarangDetail(args.barangId)
                                .collectAsStateWithLifecycle(initialValue = null)
                            val invUi by inventoryViewModel.uiState.collectAsStateWithLifecycle()

                            ItemFormScreen(
                                existingBarang = if (args.barangId > 0L) existing else null,
                                errorMessage = invUi.errorMessage,
                                onSave = { entity ->
                                    inventoryViewModel.saveBarang(entity) {
                                        navController.popBackStack()
                                    }
                                },
                                onBack = { navController.popBackStack() },
                                onClearError = { inventoryViewModel.clearFeedback() }
                            )
                        }

                        composable<BorrowRoute> { backStackEntry ->
                            val args = backStackEntry.toRoute<BorrowRoute>()
                            val loanUi by loanViewModel.uiState.collectAsStateWithLifecycle()

                            BorrowScreen(
                                session = sessionState,
                                uiState = loanUi,
                                preselectedBarangId = args.preselectedBarangId.takeIf { it > 0L },
                                onPinjam = { barangId, siswaId, petugasId, jumlah, tglPinjam, tglKembali, keperluan ->
                                    loanViewModel.pinjamBarang(
                                        barangId = barangId,
                                        siswaId = siswaId,
                                        petugasId = petugasId,
                                        jumlah = jumlah,
                                        tanggalPinjam = tglPinjam,
                                        tanggalKembali = tglKembali,
                                        keperluan = keperluan
                                    )
                                },
                                onKembalikan = { id, kondisi, fotoUri, catatan ->
                                    loanViewModel.kembalikanBarang(id, kondisi, fotoUri, catatan)
                                },
                                onScanQr = { navController.navigate(QrScannerRoute) },
                                onClearFeedback = { loanViewModel.clearFeedback() }
                            )
                        }

                        composable<HistoryRoute> {
                            val loanUi by loanViewModel.uiState.collectAsStateWithLifecycle()
                            HistoryReportScreen(
                                uiState = loanUi,
                                onFilterSelect = loanViewModel::setFilter,
                                onSearchChange = loanViewModel::setSearchQuery,
                                onKembalikan = { id, kondisi, fotoUri, catatan ->
                                    loanViewModel.kembalikanBarang(id, kondisi, fotoUri, catatan)
                                },
                                onFetchH1Loans = { loanViewModel.getLoansNeedingH1Reminder() }
                            )
                        }

                        composable<QrScannerRoute> {
                            val loanUi by loanViewModel.uiState.collectAsStateWithLifecycle()
                            QrScannerScreen(
                                activeLoans = loanUi.activeLoans,
                                onLookupKode = { kode -> loanViewModel.findBarangByKode(kode) },
                                onNavigateToPinjam = { barangId ->
                                    navController.navigate(BorrowRoute(preselectedBarangId = barangId)) {
                                        popUpTo<QrScannerRoute> { inclusive = true }
                                    }
                                },
                                onNavigateToDetail = { barangId ->
                                    navController.navigate(ItemDetailRoute(barangId = barangId)) {
                                        popUpTo<QrScannerRoute> { inclusive = true }
                                    }
                                },
                                onKembalikan = { id, kondisi, fotoUri, catatan ->
                                    loanViewModel.kembalikanBarang(id, kondisi, fotoUri, catatan)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<ProfileRoute> {
                            val profileUi by profileViewModel.uiState.collectAsStateWithLifecycle()
                            ProfileSettingsScreen(
                                uiState = profileUi,
                                firebaseUserEmail = firebaseUser.email,
                                cloudState = cloudState,
                                isSyncingCloud = isSyncingCloud,
                                cloudFeedback = cloudFeedback,
                                onSyncToCloud = cloudSyncViewModel::syncNowToCloud,
                                onClearCloudFeedback = cloudSyncViewModel::clearCloudFeedback,
                                onUpdatePhoto = profileViewModel::updatePhoto,
                                onAddPetugas = profileViewModel::addPetugas,
                                onDeletePetugas = profileViewModel::deletePetugas,
                                onChangePassword = profileViewModel::changePassword,
                                onSelectTema = profileViewModel::setTemaTerang,
                                onSelectBahasa = profileViewModel::setBahasa,
                                onToggleReminder = profileViewModel::setReminderEnabled,
                                onSaveSyncServerUrl = profileViewModel::saveSyncServerUrl,
                                onToggleAutoSync = profileViewModel::setAutoSyncEnabled,
                                onToggleWifiOnlySync = profileViewModel::setWifiOnlySync,
                                onTriggerSyncNow = profileViewModel::triggerSyncNow,
                                onExportBackupJson = { profileViewModel.exportBackupJson() },
                                onRestoreBackupJson = profileViewModel::restoreBackupJson,
                                onLogout = {
                                    profileViewModel.logout {
                                        GoogleAuthHelper.signOutGoogle(
                                            credentialManager = credentialManager,
                                            scope = scope
                                        )
                                    }
                                },
                                onClearFeedback = { profileViewModel.clearFeedback() }
                            )
                        }
                    }
                }
            }
        }
    }
}
