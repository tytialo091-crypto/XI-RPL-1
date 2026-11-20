package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.BarangEntity
import com.example.data.local.entity.UserEntity
import com.example.data.repository.EduPinjamRepository
import com.example.domain.model.KondisiBarang
import com.example.domain.model.MonthlyReportSummary
import com.example.domain.model.PeminjamanDetail
import com.example.domain.model.StatusPeminjaman
import com.example.domain.model.TopItemReport
import com.example.domain.model.TopStudentReport
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Filter riwayat peminjaman.
 */
enum class HistoryFilter(val label: String) {
    SEMUA("Semua"),
    DIPINJAM("Dipinjam"),
    DIKEMBALIKAN("Dikembalikan"),
    TERLAMBAT("Terlambat"),
    RUSAK("Rusak")
}

/**
 * State UI untuk transaksi peminjaman, pengembalian, riwayat, dan laporan bulanan.
 */
data class LoanUiState(
    val allBarang: List<BarangEntity> = emptyList(),
    val allSiswa: List<UserEntity> = emptyList(),
    val allLoans: List<PeminjamanDetail> = emptyList(),
    val filteredLoans: List<PeminjamanDetail> = emptyList(),
    val activeLoans: List<PeminjamanDetail> = emptyList(),
    val selectedFilter: HistoryFilter = HistoryFilter.SEMUA,
    val searchQuery: String = "",
    val reportSummary: MonthlyReportSummary = MonthlyReportSummary(
        bulanLabel = DateUtils.formatBulanTahun(),
        totalTransaksi = 0,
        totalDikembalikan = 0,
        totalTerlambat = 0,
        totalDendaTerkumpul = 0L,
        topBarang = emptyList(),
        topSiswa = emptyList()
    ),
    val isProcessing: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel transaksi simpan pinjam, pengembalian, scan QR lookup, dan laporan bulanan.
 */
class LoanViewModel(
    private val repository: EduPinjamRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(HistoryFilter.SEMUA)
    private val _searchQuery = MutableStateFlow("")
    private val _feedback = MutableStateFlow<Triple<Boolean, String?, String?>>(Triple(false, null, null))

    val uiState: StateFlow<LoanUiState> = combine(
        repository.allBarang,
        repository.allSiswa,
        repository.allPeminjamanDetail,
        _selectedFilter,
        _searchQuery,
        _feedback
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val barang = args[0] as List<BarangEntity>
        @Suppress("UNCHECKED_CAST")
        val siswa = args[1] as List<UserEntity>
        @Suppress("UNCHECKED_CAST")
        val loans = args[2] as List<PeminjamanDetail>
        val filter = args[3] as HistoryFilter
        val query = args[4] as String
        @Suppress("UNCHECKED_CAST")
        val feedback = args[5] as Triple<Boolean, String?, String?>

        val filtered = loans.filter { detail ->
            val statusTampil = detail.statusTampil
            val matchFilter = when (filter) {
                HistoryFilter.SEMUA -> true
                HistoryFilter.DIPINJAM -> statusTampil == StatusPeminjaman.DIPINJAM
                HistoryFilter.DIKEMBALIKAN -> statusTampil == StatusPeminjaman.DIKEMBALIKAN
                HistoryFilter.TERLAMBAT -> statusTampil == StatusPeminjaman.TERLAMBAT
                HistoryFilter.RUSAK -> statusTampil == StatusPeminjaman.RUSAK
            }
            val matchQuery = query.isBlank() ||
                (detail.barang?.namaBarang?.contains(query, ignoreCase = true) == true) ||
                (detail.peminjam?.nama?.contains(query, ignoreCase = true) == true) ||
                detail.peminjaman.kodePeminjaman.contains(query, ignoreCase = true)
            matchFilter && matchQuery
        }

        val activeLoans = loans.filter { it.masihAktif }

        // Laporan bulanan: barang paling sering dipinjam & siswa paling aktif
        val topBarang = loans
            .groupBy { it.peminjaman.barangId }
            .mapNotNull { (barangId, list) ->
                val master = list.firstOrNull()?.barang ?: return@mapNotNull null
                TopItemReport(
                    barangId = barangId,
                    namaBarang = master.namaBarang,
                    kodeBarang = master.kodeBarang,
                    kategori = master.kategori,
                    totalDipinjamKali = list.size,
                    totalUnitDipinjam = list.sumOf { it.peminjaman.jumlah }
                )
            }
            .sortedByDescending { it.totalDipinjamKali }

        val topSiswa = loans
            .groupBy { it.peminjaman.userId }
            .mapNotNull { (userId, list) ->
                val peminjam = list.firstOrNull()?.peminjam ?: return@mapNotNull null
                TopStudentReport(
                    userId = userId,
                    namaSiswa = peminjam.nama,
                    nis = peminjam.nis.ifBlank { peminjam.username },
                    kelas = peminjam.kelas.ifBlank { "-" },
                    totalTransaksi = list.size,
                    totalUnitDipinjam = list.sumOf { it.peminjaman.jumlah }
                )
            }
            .sortedByDescending { it.totalTransaksi }

        val summary = MonthlyReportSummary(
            bulanLabel = DateUtils.formatBulanTahun(),
            totalTransaksi = loans.size,
            totalDikembalikan = loans.count {
                it.statusTampil == StatusPeminjaman.DIKEMBALIKAN || it.statusTampil == StatusPeminjaman.RUSAK
            },
            totalTerlambat = loans.count { it.isTerlambatLive },
            totalDendaTerkumpul = loans.sumOf { it.estimasiDendaLive },
            topBarang = topBarang,
            topSiswa = topSiswa
        )

        LoanUiState(
            allBarang = barang,
            allSiswa = siswa,
            allLoans = loans,
            filteredLoans = filtered,
            activeLoans = activeLoans,
            selectedFilter = filter,
            searchQuery = query,
            reportSummary = summary,
            isProcessing = feedback.first,
            successMessage = feedback.second,
            errorMessage = feedback.third
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LoanUiState()
    )

    fun setFilter(filter: HistoryFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun pinjamBarang(
        barangId: Long,
        siswaId: Long,
        petugasId: Long?,
        jumlah: Int,
        tanggalPinjam: Long,
        tanggalKembali: Long,
        keperluan: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _feedback.value = Triple(true, null, null)
            val result = repository.pinjamBarang(
                barangId = barangId,
                siswaId = siswaId,
                petugasId = petugasId,
                jumlah = jumlah,
                tanggalPinjam = tanggalPinjam,
                tanggalKembaliRencana = tanggalKembali,
                keperluan = keperluan
            )
            result.fold(
                onSuccess = {
                    _feedback.value = Triple(false, "Peminjaman berhasil dicatat dan stok diperbarui!", null)
                    onSuccess()
                },
                onFailure = { error ->
                    _feedback.value = Triple(false, null, error.message ?: "Gagal memproses peminjaman.")
                }
            )
        }
    }

    fun kembalikanBarang(
        peminjamanId: Long,
        kondisi: KondisiBarang,
        fotoUri: String?,
        catatan: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _feedback.value = Triple(true, null, null)
            val result = repository.kembalikanBarang(peminjamanId, kondisi, fotoUri, catatan)
            result.fold(
                onSuccess = { updated ->
                    val dendaInfo = if (updated.denda > 0L) {
                        " Total denda keterlambatan: ${DateUtils.formatRupiah(updated.denda)}."
                    } else {
                        ""
                    }
                    _feedback.value = Triple(false, "Barang berhasil dikembalikan.$dendaInfo", null)
                    onSuccess()
                },
                onFailure = { error ->
                    _feedback.value = Triple(false, null, error.message ?: "Gagal memproses pengembalian.")
                }
            )
        }
    }

    suspend fun findBarangByKode(kode: String): BarangEntity? =
        repository.getBarangByKode(kode)

    suspend fun getLoansNeedingH1Reminder(): List<PeminjamanDetail> =
        repository.getLoansNeedingH1Reminder()

    fun clearFeedback() {
        _feedback.value = Triple(false, null, null)
    }
}
