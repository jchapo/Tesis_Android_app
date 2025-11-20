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
import com.example.android_app.delivery.DeliveryScreen
import com.example.android_app.pickup.PickupScreen
import com.example.android_app.pickup.OpenCVInitializer

sealed class Screen(val route: String) {
    object OrderList : Screen("order_list")
    object OrderDetails : Screen("order_details/{orderId}") {
        fun createRoute(orderId: String) = "order_details/$orderId"
    }
    object AssignDriver : Screen("assign_driver/{orderId}?type={type}") {
        fun createRoute(orderId: String, type: String = "recojo") = "assign_driver/$orderId?type=$type"
    }
    object AddOrder : Screen("add_order?orderId={orderId}") {
        const val route_base = "add_order"
        fun createRoute(orderId: String? = null) = if (orderId != null) "add_order?orderId=$orderId" else "add_order"
    }
    object Delivery : Screen("delivery/{orderId}") {
        fun createRoute(orderId: String) = "delivery/$orderId"
    }
    object Pickup : Screen("pickup/{orderId}") {
        fun createRoute(orderId: String) = "pickup/$orderId"
    }
}

data class OrderCounts(
    val total: Int = 0,
    val pending: Int = 0,
    val inRoute: Int = 0,
    val delivered: Int = 0,
    val canceled: Int = 0
)

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

    private val _orderCounts = MutableStateFlow(OrderCounts())
    val orderCounts: StateFlow<OrderCounts> = _orderCounts.asStateFlow()

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
                    Log.e("OrderViewModel", "Error loading orders", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val ordersList = snapshot.documents.mapNotNull { doc ->
                        transformFirestoreDoc(doc.id, doc.data)
                    }
                    _orders.value = ordersList
                    updateOrderCounts(ordersList)
                    applyFilters()
                    Log.d("OrderViewModel", "✅ Pedidos actualizados: ${ordersList.size}")
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
                // Si el pedido ya fue entregado
                (data["fechas"] as? Map<*, *>)?.get("entrega") != null -> OrderStatus.DELIVERED

                // Si el pedido fue cancelado
                (data["fechas"] as? Map<*, *>)?.get("anulacion") != null -> OrderStatus.CANCELED

                // Si la entrega está en camino
                (data["asignacion"] as? Map<*, *>)?.let { asignacion ->
                    (asignacion["entrega"] as? Map<*, *>)?.get("estado") == "en_camino"
                } == true -> OrderStatus.IN_ROUTE

                // Si el recojo está completado pero NO tiene motorizado de entrega, volver a PENDING
                (data["asignacion"] as? Map<*, *>)?.let { asignacion ->
                    (asignacion["recojo"] as? Map<*, *>)?.get("estado") == "completada" &&
                            (asignacion["entrega"] as? Map<*, *>)?.get("motorizadoUid") == null
                } == true -> OrderStatus.PENDING

                // Si el recojo está completado y ya tiene motorizado de entrega asignado
                (data["asignacion"] as? Map<*, *>)?.let { asignacion ->
                    (asignacion["recojo"] as? Map<*, *>)?.get("estado") == "completada" &&
                            (asignacion["entrega"] as? Map<*, *>)?.get("motorizadoUid") != null
                } == true -> OrderStatus.IN_ROUTE

                // Si tiene motorizado de entrega asignado (independientemente del recojo)
                (data["asignacion"] as? Map<*, *>)?.let { asignacion ->
                    (asignacion["entrega"] as? Map<*, *>)?.get("motorizadoUid") != null
                } == true -> OrderStatus.IN_ROUTE

                // Si tiene motorizado de recojo asignado pero el recojo aún no está completado
                (data["asignacion"] as? Map<*, *>)?.let { asignacion ->
                    val recojoEstado = (asignacion["recojo"] as? Map<*, *>)?.get("estado") as? String
                    (asignacion["recojo"] as? Map<*, *>)?.get("motorizadoUid") != null &&
                            recojoEstado != "completada"
                } == true -> OrderStatus.IN_ROUTE

                // En cualquier otro caso, está pendiente
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

            // Verificar si el pedido ya fue recogido y si tiene motorizado de entrega
            val isPickedUp = (data["fechas"] as? Map<*, *>)?.get("recojo") != null
            val isPickupCompleted = (asignacion?.get("recojo") as? Map<*, *>)?.get("estado") == "completada"
            val hasDeliveryDriver = (asignacion?.get("entrega") as? Map<*, *>)?.get("motorizadoUid") != null

            return Order(
                id = (data["id"] as? String) ?: docId,
                status = status,
                client = cliente,
                recipient = recipiente,
                route = route,
                deliveryInfo = deliveryInfo,
                driverInfo = driverInfo,
                isPickedUp = isPickedUp,
                isPickupCompleted = isPickupCompleted,
                hasDeliveryDriver = hasDeliveryDriver
            )
        } catch (e: Exception) {
            Log.e("OrderViewModel", "❌ Error transformando documento: ${e.message}", e)
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

    private fun updateOrderCounts(ordersList: List<Order>) {
        _orderCounts.value = OrderCounts(
            total = ordersList.size,
            pending = ordersList.count { it.status == OrderStatus.PENDING },
            inRoute = ordersList.count { it.status == OrderStatus.IN_ROUTE },
            delivered = ordersList.count { it.status == OrderStatus.DELIVERED },
            canceled = ordersList.count { it.status == OrderStatus.CANCELED }
        )
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

        // Inicializar OpenCV para la detección de dimensiones
        OpenCVInitializer.init(this)

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
                navArgument("orderId") { type = NavType.StringType },
                navArgument("type") {
                    type = NavType.StringType
                    defaultValue = "recojo"
                }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            val assignType = backStackEntry.arguments?.getString("type") ?: "recojo"
            val tipoAsignacion = if (assignType == "entrega") TipoAsignacion.ENTREGA else TipoAsignacion.RECOJO

            AssignDriverScreenContainer(
                viewModel = viewModel,
                pedidoId = orderId,
                tipoAsignacion = tipoAsignacion,
                onBackClick = {
                    navController.popBackStack()
                },
                onAssignClick = { motorizadoId, motorizadoNombre, motorizadoApellido, ruta, detalleRuta ->
                    val db = FirebaseFirestore.getInstance()
                    val rutaNombre = listOfNotNull(ruta, detalleRuta)
                        .filter { it.isNotBlank() }
                        .joinToString(" - ")
                    val nombreCompleto = "$motorizadoNombre $motorizadoApellido"

                    // Determinar qué campos actualizar según el tipo de asignación
                    val fieldPrefix = if (tipoAsignacion == TipoAsignacion.ENTREGA) "entrega" else "recojo"

                    val updates = hashMapOf<String, Any>(
                        "asignacion.$fieldPrefix.motorizadoUid" to motorizadoId,
                        "asignacion.$fieldPrefix.motorizadoNombre" to nombreCompleto,
                        "asignacion.$fieldPrefix.rutaNombre" to rutaNombre,
                        "asignacion.$fieldPrefix.asignadaEn" to com.google.firebase.Timestamp.now(),
                        "asignacion.$fieldPrefix.estado" to "asignado"
                    )

                    db.collection("pedidos")
                        .document(orderId)
                        .update(updates)
                        .addOnSuccessListener {
                            Log.d("OrderManagement", "✅ Motorizado $nombreCompleto asignado exitosamente para $fieldPrefix")
                            navController.popBackStack()
                        }
                        .addOnFailureListener { e ->
                            Log.e("OrderManagement", "❌ Error al asignar motorizado: ${e.message}")
                            navController.popBackStack()
                        }
                }
            )
        }

        composable(
            route = Screen.AddOrder.route,
            arguments = listOf(
                navArgument("orderId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId")
            AddOrderScreen(
                orderId = orderId,
                onBackClick = { navController.popBackStack() },
                onSaveClick = { newOrder ->
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Delivery.route,
            arguments = listOf(
                navArgument("orderId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            DeliveryScreenContainer(
                viewModel = viewModel,
                navController = navController,
                orderId = orderId
            )
        }

        composable(
            route = Screen.Pickup.route,
            arguments = listOf(
                navArgument("orderId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            PickupScreenContainer(
                viewModel = viewModel,
                navController = navController,
                orderId = orderId
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
    val orderCounts by viewModel.orderCounts.collectAsState()

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
            orderCounts = orderCounts,
            onFilterChange = { status -> viewModel.setFilter(status) },
            onSearchQuery = { query -> viewModel.setSearchQuery(query) },
            onViewOrder = { order ->
                navController.navigate(Screen.OrderDetails.createRoute(order.id))
            },
            onAssignDriver = { order ->
                navController.navigate(Screen.AssignDriver.createRoute(order.id))
            },
            onAssignDeliveryDriver = { order ->
                // Navegar a AssignDriver pero con tipo ENTREGA
                navController.navigate("assign_driver/${order.id}?type=entrega")
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
    viewModel: OrderViewModel,
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
            Log.d("AssignDriver", "✅ Motorizado seleccionado: $newId")
        },
        onAssignClick = {
            selectedId?.let { id ->
                val motorizado = motorizados.find { it.uid == id }
                if (motorizado != null) {
                    Log.d("AssignDriver", "✅ Asignando motorizado: ${motorizado.nombre} ${motorizado.apellido}")
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
                userRole = com.example.ordermanagement.UserRole.ADMIN, // TODO: Cambiar según el tipo de usuario autenticado
                onBackClick = {
                    navController.popBackStack()
                },
                onEditOrder = {
                    navController.navigate(Screen.AddOrder.createRoute(order.id))
                },
                onCancelOrder = {
                    println("Cancelar pedido: ${order.id}")
                    // TODO: Implementar lógica para cancelar pedido
                },
                onPickupOrder = {
                    navController.navigate(Screen.Pickup.createRoute(order.id))
                },
                onDeliverOrder = {
                    navController.navigate(Screen.Delivery.createRoute(order.id))
                }
            )
        }
    }
}

@Composable
fun PickupScreenContainer(
    viewModel: OrderViewModel,
    navController: NavHostController,
    orderId: String
) {
    var orderDetails by remember { mutableStateOf<com.example.android_app.orders.OrderDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(orderId) {
        val db = FirebaseFirestore.getInstance()
        db.collection("pedidos")
            .document(orderId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    orderDetails = com.example.android_app.orders.transformToOrderDetails(
                        document.id,
                        document.data
                    )
                } else {
                    error = "Pedido no encontrado"
                }
                isLoading = false
            }
            .addOnFailureListener { e ->
                error = "Error al cargar pedido: ${e.message}"
                isLoading = false
            }
    }

    error?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = {
                navController.popBackStack()
            },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = {
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
        orderDetails?.let { details ->
            PickupScreen(
                orderDetails = details,
                onBackClick = {
                    navController.popBackStack()
                },
                onPickupComplete = {
                    // Volver a los detalles del pedido
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun DeliveryScreenContainer(
    viewModel: OrderViewModel,
    navController: NavHostController,
    orderId: String
) {
    var orderDetails by remember { mutableStateOf<com.example.android_app.orders.OrderDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(orderId) {
        val db = FirebaseFirestore.getInstance()
        db.collection("pedidos")
            .document(orderId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    orderDetails = com.example.android_app.orders.transformToOrderDetails(
                        document.id,
                        document.data
                    )
                } else {
                    error = "Pedido no encontrado"
                }
                isLoading = false
            }
            .addOnFailureListener { e ->
                error = "Error al cargar pedido: ${e.message}"
                isLoading = false
            }
    }

    error?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = {
                navController.popBackStack()
            },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = {
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
        orderDetails?.let { details ->
            DeliveryScreen(
                orderDetails = details,
                onBackClick = {
                    navController.popBackStack()
                },
                onDeliveryComplete = {
                    // Volver a la lista de pedidos
                    navController.popBackStack(Screen.OrderList.route, inclusive = false)
                }
            )
        }
    }
}