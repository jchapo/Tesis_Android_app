package com.example.android_app.features.orders.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_app.features.orders.data.Order
import com.example.android_app.features.orders.data.OrderRepository
import com.example.android_app.features.orders.data.OrderStatus
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
    private val repository: OrderRepository = OrderRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

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

    fun filterByStatus(status: OrderStatus?) {
        _uiState.value = _uiState.value.copy(selectedFilter = status)
        _uiState.value = _uiState.value.copy(
            filteredOrders = applyFilters(_uiState.value.orders)
        )
    }

    fun searchOrders(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        _uiState.value = _uiState.value.copy(
            filteredOrders = applyFilters(_uiState.value.orders)
        )
    }

    private fun applyFilters(orders: List<Order>): List<Order> {
        var filtered = orders
        filtered = repository.filterByStatus(filtered, _uiState.value.selectedFilter)
        filtered = repository.searchOrders(filtered, _uiState.value.searchQuery)
        return filtered
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
