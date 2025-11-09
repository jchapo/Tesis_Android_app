package com.example.android_app.Orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OrdersUiState(
    val isLoading: Boolean = true,
    val orders: List<Order> = emptyList(),
    val filteredOrders: List<Order> = emptyList(),
    val selectedFilter: OrderStatus? = null,
    val searchQuery: String = "",
    val error: String? = null
)

class OrderManagementViewModel(
    private val repository: `OrderRepository.kt` = `OrderRepository.kt`()
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    /**
     * Carga todos los pedidos desde Firebase
     */
    fun loadOrders() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.getAllOrders()
                .onSuccess { orders ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        orders = orders,
                        filteredOrders = applyFilters(orders)
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Error al cargar pedidos"
                    )
                }
        }
    }

    /**
     * Filtra por estado
     */
    fun filterByStatus(status: OrderStatus?) {
        _uiState.value = _uiState.value.copy(selectedFilter = status)
        _uiState.value = _uiState.value.copy(
            filteredOrders = applyFilters(_uiState.value.orders)
        )
    }

    /**
     * Busca pedidos
     */
    fun searchOrders(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        _uiState.value = _uiState.value.copy(
            filteredOrders = applyFilters(_uiState.value.orders)
        )
    }

    /**
     * Aplica todos los filtros
     */
    private fun applyFilters(orders: List<Order>): List<Order> {
        var filtered = orders

        // Filtrar por estado
        filtered = repository.filterByStatus(filtered, _uiState.value.selectedFilter)

        // Filtrar por búsqueda
        filtered = repository.searchOrders(filtered, _uiState.value.searchQuery)

        return filtered
    }

    /**
     * Limpia errores
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Refresca los pedidos (pull to refresh)
     */
    fun refresh() {
        loadOrders()
    }
}