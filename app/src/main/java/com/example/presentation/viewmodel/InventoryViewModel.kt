package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.BarangEntity
import com.example.data.repository.EduPinjamRepository
import com.example.domain.model.IndikatorStok
import com.example.domain.model.PeminjamanDetail
import com.example.domain.model.SortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State daftar inventaris dengan pencarian, filter kategori, filter status stok, dan pengurutan.
 */
data class InventoryUiState(
    val items: List<BarangEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "Semua",
    val selectedStockFilter: IndikatorStok? = null,
    val sortOption: SortOption = SortOption.NAMA_AZ,
    val message: String? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel manajemen data inventaris barang sekolah (CRUD, filter, sort, detail).
 */
class InventoryViewModel(
    private val repository: EduPinjamRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("Semua")
    private val _selectedStockFilter = MutableStateFlow<IndikatorStok?>(null)
    private val _sortOption = MutableStateFlow(SortOption.NAMA_AZ)
    private val _feedback = MutableStateFlow<Pair<String?, String?>>(null to null)

    val uiState: StateFlow<InventoryUiState> = combine(
        repository.allBarang,
        _searchQuery,
        _selectedCategory,
        _selectedStockFilter,
        _sortOption,
        _feedback
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val allItems = args[0] as List<BarangEntity>
        val query = args[1] as String
        val category = args[2] as String
        val stockFilter = args[3] as IndikatorStok?
        val sort = args[4] as SortOption
        @Suppress("UNCHECKED_CAST")
        val feedback = args[5] as Pair<String?, String?>

        val trimmedQuery = query.trim()
        val filtered = allItems.filter { item ->
            val matchCat = category == "Semua" || item.kategori.equals(category, ignoreCase = true)
            val matchStock = stockFilter == null || IndikatorStok.dariStok(item.stokTersedia) == stockFilter
            val matchQuery = trimmedQuery.isEmpty() ||
                item.namaBarang.contains(trimmedQuery, ignoreCase = true) ||
                item.kategori.contains(trimmedQuery, ignoreCase = true) ||
                item.kodeBarang.contains(trimmedQuery, ignoreCase = true) ||
                item.lokasiRak.contains(trimmedQuery, ignoreCase = true)
            matchCat && matchStock && matchQuery
        }

        val sorted = when (sort) {
            SortOption.NAMA_AZ -> filtered.sortedBy { it.namaBarang.lowercase() }
            SortOption.NAMA_ZA -> filtered.sortedByDescending { it.namaBarang.lowercase() }
            SortOption.STOK_TERBANYAK -> filtered.sortedByDescending { it.stokTersedia }
            SortOption.STOK_SEDIKIT -> filtered.sortedBy { it.stokTersedia }
        }

        InventoryUiState(
            items = sorted,
            searchQuery = query,
            selectedCategory = category,
            selectedStockFilter = stockFilter,
            sortOption = sort,
            message = feedback.first,
            errorMessage = feedback.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InventoryUiState()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setStockFilter(filter: IndikatorStok?) {
        _selectedStockFilter.value = filter
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun observeBarangDetail(barangId: Long): Flow<BarangEntity?> =
        repository.observeBarangById(barangId)

    fun observeBarangHistory(barangId: Long): Flow<List<PeminjamanDetail>> =
        repository.observePeminjamanByBarang(barangId)

    fun saveBarang(barang: BarangEntity, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val result = repository.saveBarang(barang)
            result.fold(
                onSuccess = { id ->
                    _feedback.value = "Data barang berhasil disimpan." to null
                    onSaved(id)
                },
                onFailure = { error ->
                    _feedback.value = null to (error.message ?: "Gagal menyimpan data barang.")
                }
            )
        }
    }

    fun deleteBarang(barang: BarangEntity, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteBarang(barang)
            _feedback.value = "${barang.namaBarang} dihapus dari inventaris." to null
            onDeleted()
        }
    }

    fun clearFeedback() {
        _feedback.value = null to null
    }
}
