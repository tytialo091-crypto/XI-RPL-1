package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.EduPinjamRepository
import com.example.data.sync.DataSyncStatus
import com.example.domain.model.DailyLoanPoint
import com.example.domain.model.DashboardStats
import com.example.domain.model.InventoryStatusCounts
import com.example.domain.model.PeminjamanDetail
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State UI lengkap untuk halaman Beranda / Dashboard.
 */
data class DashboardUiState(
    val stats: DashboardStats = DashboardStats(),
    val inventoryStatusCounts: InventoryStatusCounts = InventoryStatusCounts(),
    val chartPoints: List<DailyLoanPoint> = emptyList(),
    val recentLoans: List<PeminjamanDetail> = emptyList(),
    val h1ReminderLoans: List<PeminjamanDetail> = emptyList(),
    val syncStatus: DataSyncStatus = DataSyncStatus(),
    val isLoading: Boolean = true
)

/**
 * ViewModel Dashboard untuk menyajikan ringkasan inventaris, grafik 7 hari terakhir, pengingat H-1, dan status sinkronisasi.
 */
class DashboardViewModel(
    private val repository: EduPinjamRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.allBarang,
        repository.allPeminjamanDetail,
        repository.syncStatus,
        repository.inventoryStatusCounts
    ) { barangList, loans, syncStatus, statusCounts ->
        val now = System.currentTimeMillis()
        val satuHari = 24L * 60L * 60L * 1000L

        val totalJenis = barangList.size
        val totalUnit = barangList.sumOf { it.stokTotal }
        val tersediaUnit = barangList.sumOf { it.stokTersedia }
        val dipinjamUnit = loans.filter { it.masihAktif }.sumOf { it.peminjaman.jumlah }
        val terlambatCount = loans.count { it.masihAktif && it.isTerlambatLive }
        val batasH1 = now + satuHari + (12L * 60L * 60L * 1000L)
        val h1Loans = loans.filter { it.masihAktif && it.peminjaman.tanggalKembaliRencana <= batasH1 }

        val todayStart = DateUtils.awalHari(now)
        val points = (6 downTo 0).map { offset ->
            val dayStart = todayStart - offset * satuHari
            val dayEnd = DateUtils.akhirHari(dayStart)
            val count = loans.count { it.peminjaman.tanggalPinjam in dayStart..dayEnd }
            DailyLoanPoint(
                labelHari = DateUtils.formatNamaHariSingkat(dayStart),
                tanggalEpoch = dayStart,
                jumlahPeminjaman = count
            )
        }

        DashboardUiState(
            stats = DashboardStats(
                totalJenisBarang = totalJenis,
                totalUnitBarang = totalUnit,
                barangTersediaUnit = tersediaUnit,
                barangDipinjamUnit = dipinjamUnit,
                peminjamanTerlambatCount = terlambatCount,
                pengingatH1Count = h1Loans.size
            ),
            inventoryStatusCounts = statusCounts,
            chartPoints = points,
            recentLoans = loans.take(6),
            h1ReminderLoans = h1Loans,
            syncStatus = syncStatus,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun triggerSyncNow() {
        viewModelScope.launch {
            repository.triggerSyncNow()
        }
    }
}
