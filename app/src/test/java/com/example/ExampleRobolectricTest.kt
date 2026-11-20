package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.data.datastore.SessionManager
import com.example.data.local.EduPinjamDatabase
import com.example.data.local.entity.BarangEntity
import com.example.data.remote.InventorySyncClient
import com.example.data.repository.EduPinjamRepository
import com.example.data.sync.BorrowingCloudUploader
import com.example.data.sync.BorrowingSyncWorker
import com.example.data.sync.DataSyncManager
import com.example.data.sync.InventorySyncWorker
import com.example.domain.model.KondisiBarang
import com.example.domain.model.UserRole
import com.example.presentation.viewmodel.InventoryViewModel
import com.example.utils.NetworkMonitor
import com.example.utils.QrCodeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class ExampleRobolectricTest {

    @Test
    fun `app name matches EDU-PINJAM`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("EDU-PINJAM", appName)
    }

    @Test
    fun `seed data populates 15 items and validates borrow return flow`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, EduPinjamDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val sessionManager = SessionManager(context)
        val repo = EduPinjamRepository(db, sessionManager)

        repo.ensureSeeded()
        val items = repo.allBarang.first()
        assertEquals(15, items.size)

        val loginRes = repo.login("admin", "123", UserRole.ADMIN)
        assertTrue(loginRes.isSuccess)

        val outOfStock = items.first { it.kodeBarang == "ELK-004" }
        val failBorrow = repo.pinjamBarang(
            barangId = outOfStock.id,
            siswaId = 3L,
            petugasId = 1L,
            jumlah = 1,
            tanggalPinjam = System.currentTimeMillis(),
            tanggalKembaliRencana = System.currentTimeMillis() + 86_400_000L,
            keperluan = "Uji stok habis"
        )
        assertTrue(failBorrow.isFailure)

        val laptop = items.first { it.kodeBarang == "ELK-002" }
        val pinjamRes = repo.pinjamBarang(
            barangId = laptop.id,
            siswaId = 3L,
            petugasId = 1L,
            jumlah = 2,
            tanggalPinjam = System.currentTimeMillis() - 4 * 86_400_000L,
            tanggalKembaliRencana = System.currentTimeMillis() - 2 * 86_400_000L,
            keperluan = "Praktikum Informatika"
        )
        assertTrue(pinjamRes.isSuccess)
        val afterBorrow = repo.allBarang.first().first { it.id == laptop.id }
        assertEquals(laptop.stokTersedia - 2, afterBorrow.stokTersedia)

        val returnRes = repo.kembalikanBarang(
            peminjamanId = pinjamRes.getOrThrow(),
            kondisiAkhir = KondisiBarang.BAIK,
            fotoKondisiUri = null,
            catatan = "Baik"
        )
        assertTrue(returnRes.isSuccess)
        assertTrue(returnRes.getOrThrow().denda >= 2000L)

        val afterReturn = repo.allBarang.first().first { it.id == laptop.id }
        assertEquals(laptop.stokTersedia, afterReturn.stokTersedia)

        val qrBitmap = QrCodeUtils.generateQrBitmap("ELK-001", 256)
        assertNotNull(qrBitmap)
        db.close()
    }

    @Test
    fun `data sync manager enqueues room outbox and syncs via workmanager when network available`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)

        val db = Room.inMemoryDatabaseBuilder(context, EduPinjamDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val sessionManager = SessionManager(context)
        val networkMonitor = NetworkMonitor(context)
        val syncManager = DataSyncManager(context, db, sessionManager, networkMonitor)
        val repo = EduPinjamRepository(db, sessionManager, syncManager)

        repo.ensureSeeded()
        repo.updateSyncServerUrl("https://sync.edupinjam.sch.id/api/sync")

        // Simpan barang baru -> harus masuk outbox sync_queue Room dan men-schedule WorkManager
        val saveResult = repo.saveBarang(
            BarangEntity(
                kodeBarang = "SYNC-001",
                namaBarang = "Tablet Praktikum",
                kategori = "Elektronik",
                stokTotal = 5,
                stokTersedia = 5,
                kondisi = "Baik",
                lokasiRak = "Rak Sync 1"
            )
        )
        assertTrue(saveResult.isSuccess)
        val newBarangId = saveResult.getOrThrow()

        val pendingBefore = db.syncDao().getPendingItems()
        assertEquals(1, pendingBefore.size)
        assertEquals("CREATE", pendingBefore.first().operation)

        // Pastikan WorkRequest terdaftar di WorkManager dengan constraint NetworkType.CONNECTED
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(DataSyncManager.UNIQUE_IMMEDIATE_SYNC)
            .get()
        assertTrue(workInfos.isNotEmpty())
        assertEquals(NetworkType.CONNECTED, workInfos.first().constraints.requiredNetworkType)

        // Jalankan logika Worker dengan OkHttp interceptor 200 OK
        val testHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{\"ok\":true}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
        val workerResult = InventorySyncWorker.runSync(
            database = db,
            sessionManager = sessionManager,
            syncClient = InventorySyncClient(testHttpClient)
        )
        assertTrue(workerResult is ListenableWorker.Result.Success)

        // Pastikan antrean outbox kosong dan barang tertandai SYNCED
        assertTrue(db.syncDao().getPendingItems().isEmpty())
        val syncedBarang = db.barangDao().getBarangById(newBarangId)
        assertEquals("SYNCED", syncedBarang?.syncStatus)
        assertNotNull(syncedBarang?.lastSyncedAt)
        assertTrue(db.syncDao().observeRecentLogs().first().isNotEmpty())

        db.close()
    }

    @Test
    fun `room inventory status counts reflect available low stock and out of stock items`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, EduPinjamDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val sessionManager = SessionManager(context)
        val repo = EduPinjamRepository(db, sessionManager)

        repo.ensureSeeded()
        val items = repo.allBarang.first()
        val initialCounts = repo.inventoryStatusCounts.first()

        assertEquals(items.size, initialCounts.totalItems)
        assertEquals(items.count { it.stokTersedia > 2 }, initialCounts.availableItems)
        assertEquals(items.count { it.stokTersedia in 1..2 }, initialCounts.lowStockItems)
        assertEquals(items.count { it.stokTersedia <= 0 }, initialCounts.outOfStockItems)

        // Pinjam salah satu barang yang stoknya > 2 hingga habis (stokTersedia = 0)
        val target = items.first { it.stokTersedia > 2 }
        val pinjamRes = repo.pinjamBarang(
            barangId = target.id,
            siswaId = 3L,
            petugasId = 1L,
            jumlah = target.stokTersedia,
            tanggalPinjam = System.currentTimeMillis(),
            tanggalKembaliRencana = System.currentTimeMillis() + 86_400_000L,
            keperluan = "Uji chip status Room"
        )
        assertTrue(pinjamRes.isSuccess)

        val afterBorrowCounts = repo.inventoryStatusCounts.first()
        assertEquals(initialCounts.availableItems - 1, afterBorrowCounts.availableItems)
        assertEquals(initialCounts.outOfStockItems + 1, afterBorrowCounts.outOfStockItems)

        // Kembalikan barang -> kembali masuk kategori Tersedia
        val returnRes = repo.kembalikanBarang(
            peminjamanId = pinjamRes.getOrThrow(),
            kondisiAkhir = KondisiBarang.BAIK,
            fotoKondisiUri = null,
            catatan = "Kembali"
        )
        assertTrue(returnRes.isSuccess)

        val afterReturnCounts = repo.inventoryStatusCounts.first()
        assertEquals(initialCounts.availableItems, afterReturnCounts.availableItems)
        assertEquals(initialCounts.outOfStockItems, afterReturnCounts.outOfStockItems)

        db.close()
    }

    @Test
    fun `borrowing sync worker schedules with network constraint and uploads pending borrowing records`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)

        val db = Room.inMemoryDatabaseBuilder(context, EduPinjamDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val sessionManager = SessionManager(context)
        val networkMonitor = NetworkMonitor(context)
        val syncManager = DataSyncManager(context, db, sessionManager, networkMonitor)
        val repo = EduPinjamRepository(db, sessionManager, syncManager)

        repo.ensureSeeded()
        val laptop = repo.allBarang.first().first { it.kodeBarang == "ELK-002" }

        // Buat peminjaman baru -> harus tercatat PENDING_SYNC dan menjadwalkan BorrowingSyncWorker
        val pinjamRes = repo.pinjamBarang(
            barangId = laptop.id,
            siswaId = 3L,
            petugasId = 1L,
            jumlah = 1,
            tanggalPinjam = System.currentTimeMillis(),
            tanggalKembaliRencana = System.currentTimeMillis() + 86_400_000L,
            keperluan = "Uji Worker Peminjaman"
        )
        assertTrue(pinjamRes.isSuccess)
        val newLoanId = pinjamRes.getOrThrow()

        val savedLoan = db.peminjamanDao().getPeminjamanById(newLoanId)
        assertNotNull(savedLoan)
        assertEquals("PENDING_SYNC", savedLoan?.syncStatus)

        val pendingBorrowingTasks = db.syncDao().getPendingItemsByType(BorrowingSyncWorker.ENTITY_TYPE_PEMINJAMAN)
        assertEquals(1, pendingBorrowingTasks.size)
        assertEquals("CREATE", pendingBorrowingTasks.first().operation)

        // Pastikan BorrowingSyncWorker terdaftar di WorkManager dengan constraint NetworkType.CONNECTED
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(DataSyncManager.UNIQUE_BORROWING_SYNC)
            .get()
        assertTrue(workInfos.isNotEmpty())
        assertEquals(NetworkType.CONNECTED, workInfos.first().constraints.requiredNetworkType)

        // Simulasi kegagalan jaringan -> Worker harus mengembalikan Result.retry()
        val retryResult = BorrowingSyncWorker.runBorrowingSync(
            database = db,
            sessionManager = sessionManager,
            uploader = BorrowingCloudUploader { _, _, _ ->
                kotlin.Result.failure(IllegalStateException("Network temporarily unavailable"))
            }
        )
        assertTrue(retryResult is ListenableWorker.Result.Retry)
        assertEquals("FAILED", db.peminjamanDao().getPeminjamanById(newLoanId)?.syncStatus)

        // Simulasi koneksi internet kembali aktif -> semua peminjaman tertunda terunggah dan berstatus SYNCED
        val uploadedIds = mutableListOf<Long>()
        val successResult = BorrowingSyncWorker.runBorrowingSync(
            database = db,
            sessionManager = sessionManager,
            uploader = BorrowingCloudUploader { peminjaman, _, _ ->
                uploadedIds.add(peminjaman.id)
                kotlin.Result.success(Unit)
            }
        )
        assertTrue(successResult is ListenableWorker.Result.Success)
        assertTrue(uploadedIds.contains(newLoanId))
        assertTrue(db.peminjamanDao().getPendingSyncPeminjaman().isEmpty())
        assertTrue(db.syncDao().getPendingItemsByType(BorrowingSyncWorker.ENTITY_TYPE_PEMINJAMAN).isEmpty())

        val syncedLoan = db.peminjamanDao().getPeminjamanById(newLoanId)
        assertEquals("SYNCED", syncedLoan?.syncStatus)
        assertNotNull(syncedLoan?.lastSyncedAt)

        db.close()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `inventory search filters items in real time by item name or category`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, EduPinjamDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val viewModelStore = ViewModelStore()
        try {
            val sessionManager = SessionManager(context)
            val repo = EduPinjamRepository(db, sessionManager)
            repo.ensureSeeded()

            val factory = com.example.presentation.viewmodel.EduPinjamViewModelFactory(repo)
            val viewModel = ViewModelProvider(viewModelStore, factory)[InventoryViewModel::class.java]
            val collectJob = backgroundScope.launch(testDispatcher) {
                viewModel.uiState.collect {}
            }

            // Awalnya seluruh 15 barang tampil
            assertEquals(15, viewModel.uiState.first { it.items.isNotEmpty() }.items.size)

            // Filter berdasarkan nama barang (case-insensitive)
            viewModel.setSearchQuery("mikroskop")
            val byName = viewModel.uiState.first { it.searchQuery == "mikroskop" }
            assertTrue(byName.items.isNotEmpty())
            assertTrue(byName.items.all { it.namaBarang.contains("mikroskop", ignoreCase = true) })

            // Filter berdasarkan kategori (case-insensitive dengan spasi)
            viewModel.setSearchQuery("  elektronik ")
            val byCategory = viewModel.uiState.first { it.searchQuery == "  elektronik " }
            assertTrue(byCategory.items.isNotEmpty())
            assertTrue(byCategory.items.all { it.kategori.equals("Elektronik", ignoreCase = true) })

            // Hapus kata kunci pencarian -> kembali menampilkan seluruh 15 barang
            viewModel.setSearchQuery("")
            val cleared = viewModel.uiState.first { it.searchQuery.isEmpty() }
            assertEquals(15, cleared.items.size)

            collectJob.cancel()
        } finally {
            viewModelStore.clear()
            db.close()
            Dispatchers.resetMain()
        }
    }
}
