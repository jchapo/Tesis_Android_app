package com.example.android_app.orders

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

sealed class Screen(val route: String) {
    object OrderList : Screen("order_list")
    object OrderDetails : Screen("order_details/{orderId}") {
        fun createRoute(orderId: String) = "order_details/$orderId"
    }
    object AssignDriver : Screen("assign_driver/{orderId}") {
        fun createRoute(orderId: String) = "assign_driver/$orderId"
    }
    object AddOrder : Screen("add_order")
}

class OrderViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _filteredOrders = MutableStateFlow<List<Order>>(emptyList())
    val filteredOrders: StateFlow<List<Order>> = _filteredOrders.asStateFlow()

    private val _selectedOrder = MutableStateFlow<Order?>(null)
    val selectedOrder: StateFlow<Order?> = _selectedOrder.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedFilter = MutableStateFlow<OrderStatus?>(null)
    val selectedFilter: StateFlow<OrderStatus?> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadActiveOrders()
    }

    fun loadActiveOrders() {
        _isLoading.value = true
        _error.value = null

        db.collection("pedidos")
            .whereEqualTo("cicloOperativo.cerradoPorAdmin", false)
            .orderBy("fechas.creacion", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                _isLoading.value = false
                if (e != null) {
                    _error.value = "Error al cargar pedidos: ${e.message}"
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val ordersList = snapshot.documents.mapNotNull { doc ->
                        transformFirestoreDoc(doc.id, doc.data)
                    }
                    _orders.value = ordersList
                    applyFilters()
                }
            }
    }

    fun getOrderById(orderId: String) {
        _isLoading.value = true
        _error.value = null

        db.collection("pedidos")
            .document(orderId)
            .get()
            .addOnSuccessListener { document ->
                _isLoading.value = false
                if (document.exists()) {
                    _selectedOrder.value = transformFirestoreDoc(document.id, document.data)
                } else {
                    _error.value = "Pedido no encontrado"
                }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _error.value = "Error al cargar el pedido: ${e.message}"
            }
    }

    private fun transformFirestoreDoc(docId: String, data: Map<String, Any>?): Order? {
        if (data == null) return null

        try {
            val status = when {
                (data["fechas"] as? Map<*, *>)?.get("entrega") != null -> OrderStatus.DELIVERED
                (data["fechas"] as? Map<*, *>)?.get("anulacion") != null -> OrderStatus.CANCELED
                (data["asignacion"] as? Map<*, *>)?.let { asignacion ->
                    (asignacion["recojo"] as? Map<*, *>)?.get("estado") == "completada" ||
                            (asignacion["entrega"] as? Map<*, *>)?.get("estado") == "en_camino"
                } == true -> OrderStatus.IN_ROUTE
                else -> OrderStatus.PENDING
            }

            val proveedor = data["proveedor"] as? Map<*, *>
            val cliente = proveedor?.get("nombre") as? String ?: "Cliente Desconocido"

            val destinatario = data["destinatario"] as? Map<*, *>
            val recipiente = destinatario?.get("nombre") as? String ?: "Destinatario Desconocido"

            val proveedorDir = proveedor?.get("direccion") as? Map<*, *>
            val destinatarioDir = destinatario?.get("direccion") as? Map<*, *>
            val pickupDistrict = proveedorDir?.get("distrito") as? String ?: "-"
            val deliveryDistrict = destinatarioDir?.get("distrito") as? String ?: "-"
            val route = "$pickupDistrict → $deliveryDistrict"

            val fechas = data["fechas"] as? Map<*, *>
            val deliveryInfo = when (status) {
                OrderStatus.DELIVERED -> {
                    val fecha = fechas?.get("entrega")
                    "Entregado: ${formatDate(fecha)}"
                }
                OrderStatus.CANCELED -> {
                    val motivo = destinatarioDir?.get("observaciones") as? String ?: "Cancelado"
                    motivo
                }
                else -> {
                    val fechaProgramada = fechas?.get("entregaProgramada")
                    "Entrega: ${formatDate(fechaProgramada)}"
                }
            }

            val asignacion = data["asignacion"] as? Map<*, *>
            val driverInfo = when {
                status == OrderStatus.PENDING -> "Sin motorizado asignado"
                status == OrderStatus.CANCELED -> null
                else -> {
                    val entrega = asignacion?.get("entrega") as? Map<*, *>
                    val recojo = asignacion?.get("recojo") as? Map<*, *>
                    val motorizadoNombre = (entrega?.get("motorizadoNombre")
                        ?: recojo?.get("motorizadoNombre")) as? String
                    if (motorizadoNombre != null) {
                        "Motorizado: $motorizadoNombre"
                    } else {
                        "Sin motorizado asignado"
                    }
                }
            }

            return Order(
                id = (data["id"] as? String) ?: docId,
                status = status,
                client = cliente,
                recipient = recipiente,
                route = route,
                deliveryInfo = deliveryInfo,
                driverInfo = driverInfo
            )
        } catch (e: Exception) {
            println("❌ Error transformando documento: ${e.message}")
            return null
        }
    }

    private fun formatDate(timestamp: Any?): String {
        if (timestamp == null) return "-"
        return try {
            val date = when (timestamp) {
                is com.google.firebase.Timestamp -> timestamp.toDate()
                is Date -> timestamp
                else -> return "-"
            }
            val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale("es", "PE"))
            formatter.format(date)
        } catch (e: Exception) {
            "-"
        }
    }

    fun applyFilters() {
        var filtered = _orders.value

        _selectedFilter.value?.let { status ->
            filtered = filtered.filter { it.status == status }
        }

        val query = _searchQuery.value.lowercase().trim()
        if (query.isNotEmpty()) {
            filtered = filtered.filter { order ->
                order.id.lowercase().contains(query) ||
                        order.client.lowercase().contains(query) ||
                        order.recipient.lowercase().contains(query) ||
                        order.route.lowercase().contains(query)
            }
        }

        _filteredOrders.value = filtered
    }

    fun setFilter(status: OrderStatus?) {
        _selectedFilter.value = status
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSelectedOrder() {
        _selectedOrder.value = null
    }
}

class OrderManagementActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                OrderManagementApp()
            }
        }
    }
}

@Composable
fun OrderManagementApp(
    viewModel: OrderViewModel = viewModel()
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.OrderList.route
    ) {
        composable(Screen.OrderList.route) {
            OrderListScreen(
                viewModel = viewModel,
                navController = navController
            )
        }

        composable(
            route = Screen.OrderDetails.route,
            arguments = listOf(
                navArgument("orderId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderDetailsScreenContainer(
                viewModel = viewModel,
                navController = navController,
                orderId = orderId
            )
        }

        composable(
            route = Screen.AssignDriver.route,
            arguments = listOf(
                navArgument("orderId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""

            AssignDriverScreenContainer(
                pedidoId = orderId,
                onBackClick = { navController.popBackStack() },
                onAssignClick = { motorizadoId, motorizadoNombre, motorizadoApellido, ruta, detalleRuta ->

                    val db = FirebaseFirestore.getInstance()
                    val rutaNombre = listOfNotNull(ruta, detalleRuta)
                        .filter { it.isNotBlank() }
                        .joinToString(" - ")
                    val nombreCompleto = "$motorizadoNombre $motorizadoApellido"

                    val updates = hashMapOf<String, Any>(
                        "asignacion.recojo.motorizadoUid" to motorizadoId,
                        "asignacion.recojo.motorizadoNombre" to nombreCompleto,
                        "asignacion.recojo.rutaNombre" to rutaNombre,
                        "asignacion.recojo.asignadaEn" to com.google.firebase.Timestamp.now(),
                        "asignacion.recojo.estado" to "asignado"
                    )

                    db.collection("pedidos")
                        .document(orderId)
                        .update(updates)
                        .addOnSuccessListener {
                            Log.d("OrderManagement", "✅ Motorizado $nombreCompleto asignado exitosamente")
                            navController.popBackStack()
                        }
                        .addOnFailureListener { e ->
                            Log.e("OrderManagement", "❌ Error al asignar motorizado: ${e.message}")
                            navController.popBackStack()
                        }
                }
            )
        }

        composable(route = Screen.AddOrder.route) {
            AddOrderScreen(
                onBackClick = { navController.popBackStack() },
                onSaveClick = { newOrder ->
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun OrderListScreen(
    viewModel: OrderViewModel,
    navController: NavHostController
) {
    val filteredOrders by viewModel.filteredOrders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    error?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    if (isLoading && filteredOrders.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        OrderManagementScreen(
            orders = filteredOrders,
            selectedFilter = selectedFilter,
            onFilterChange = { status -> viewModel.setFilter(status) },
            onSearchQuery = { query -> viewModel.setSearchQuery(query) },
            onViewOrder = { order ->
                navController.navigate(Screen.OrderDetails.createRoute(order.id))
            },
            onAssignDriver = { order ->
                navController.navigate(Screen.AssignDriver.createRoute(order.id))
            },
            onEditOrder = { order ->
                println("Editar pedido: ${order.id}")
            },
            onAddOrder = {
                navController.navigate(Screen.AddOrder.route)
            }
        )
    }
}

@Composable
fun AssignDriverScreenContainer(
    pedidoId: String,
    tipoAsignacion: TipoAsignacion = TipoAsignacion.RECOJO,
    onBackClick: () -> Unit = {},
    onAssignClick: (String, String, String, String?, String?) -> Unit = { _, _, _, _, _ -> }
) {
    val motorizados = rememberMotorizados()
    var selectedId by remember { mutableStateOf<String?>(null) }
    val isLoading = motorizados.isEmpty()

    AssignDriverScreen(
        pedidoId = pedidoId,
        tipoAsignacion = tipoAsignacion,
        motorizados = motorizados,
        selectedMotorizadoId = selectedId,
        onMotorizadoSelected = { newId ->
            selectedId = newId
        },
        onAssignClick = {
            selectedId?.let { id ->
                val motorizado = motorizados.find { it.uid == id }
                if (motorizado != null) {
                    onAssignClick(id, motorizado.nombre, motorizado.apellido, motorizado.ruta, motorizado.detalleRuta)
                }
            }
        },
        onBackClick = onBackClick,
        isLoading = isLoading
    )
}

@Composable
fun OrderDetailsScreenContainer(
    viewModel: OrderViewModel,
    navController: NavHostController,
    orderId: String
) {
    val selectedOrder by viewModel.selectedOrder.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(orderId) {
        viewModel.getOrderById(orderId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSelectedOrder()
        }
    }

    error?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = {
                viewModel.clearError()
                navController.popBackStack()
            },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearError()
                    navController.popBackStack()
                }) {
                    Text("OK")
                }
            }
        )
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        selectedOrder?.let { order ->
            com.example.ordermanagement.OrderDetailsScreen(
                order = order,
                onBackClick = {
                    navController.popBackStack()
                },
                onEditOrder = {
                    println("Editar pedido: ${order.id}")
                },
                onUpdateStatus = {
                    println("Actualizar estado: ${order.id}")
                }
            )
        }
    }
}