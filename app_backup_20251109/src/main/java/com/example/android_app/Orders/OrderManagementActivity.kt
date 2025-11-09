package com.example.android_app.Orders

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp


class OrderManagementActivity : ComponentActivity() {

    private val viewModel: OrderManagementViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            YourAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OrderManagementScreenContainer(
                        viewModel = viewModel,
                        onNavigateToCreateOrder = { navigateToCreateOrder() },
                        onNavigateToOrderDetail = { order -> navigateToOrderDetail(order) },
                        onNavigateToAssignDriver = { order -> navigateToAssignDriver(order) },
                        onNavigateToEditOrder = { order -> navigateToEditOrder(order) },
                        onNavigateBack = { finish() }
                    )
                }
            }
        }
    }

    private fun navigateToCreateOrder() {
        //val intent = Intent(this, CreateOrderActivity::class.java)
        //startActivity(intent)
    }

    private fun navigateToOrderDetail(order: Order) {
        // TODO: Crear OrderDetailActivity
        Toast.makeText(this, "Ver detalles de ${order.id}", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToAssignDriver(order: Order) {
        // TODO: Crear AssignDriverActivity
        Toast.makeText(this, "Asignar motorizado a ${order.id}", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToEditOrder(order: Order) {
        // TODO: Crear EditOrderActivity
        Toast.makeText(this, "Editar pedido ${order.id}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun OrderManagementScreenContainer(
    viewModel: OrderManagementViewModel,
    onNavigateToCreateOrder: () -> Unit,
    onNavigateToOrderDetail: (Order) -> Unit,
    onNavigateToAssignDriver: (Order) -> Unit,
    onNavigateToEditOrder: (Order) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar errores en Snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading && uiState.orders.isEmpty() -> {
                    LoadingScreen()
                }
                uiState.orders.isEmpty() && !uiState.isLoading -> {
                    EmptyOrdersScreen(onAddOrder = onNavigateToCreateOrder)
                }
                else -> {
                    // Pantalla principal con datos
                    OrderManagementScreen(
                        orders = uiState.filteredOrders,
                        selectedFilter = uiState.selectedFilter,
                        onFilterChange = { viewModel.filterByStatus(it) },
                        onSearchQuery = { viewModel.searchOrders(it) },
                        onViewOrder = onNavigateToOrderDetail,
                        onAssignDriver = onNavigateToAssignDriver,
                        onEditOrder = onNavigateToEditOrder,
                        onAddOrder = onNavigateToCreateOrder
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Cargando pedidos...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyOrdersScreen(onAddOrder: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Text(
                "No hay pedidos",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Crea tu primer pedido para comenzar a gestionar entregas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onAddOrder,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Crear Primer Pedido")
            }
        }
    }
}