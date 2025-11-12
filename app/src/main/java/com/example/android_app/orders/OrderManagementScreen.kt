package com.example.android_app.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Data Models
data class Order(
    val id: String,
    val status: OrderStatus,
    val client: String,
    val recipient: String,
    val route: String,
    val deliveryInfo: String,
    val driverInfo: String? = null
)

enum class OrderStatus(
    val displayName: String,
    val backgroundColor: Color,
    val textColor: Color
) {
    PENDING("Pendiente", Color(0xFFFB923C).copy(alpha = 0.2f), Color(0xFFFB923C)),
    IN_ROUTE("En Ruta", Color(0xFF3B82F6).copy(alpha = 0.2f), Color(0xFF3B82F6)),
    DELIVERED("Entregado", Color(0xFF10B981).copy(alpha = 0.2f), Color(0xFF10B981)),
    CANCELED("Cancelado", Color(0xFFEF4444).copy(alpha = 0.2f), Color(0xFFEF4444))
}

// Main Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderManagementScreen(
    orders: List<Order> = getSampleOrders(),
    selectedFilter: OrderStatus? = null,
    onFilterChange: (OrderStatus?) -> Unit = {},
    onSearchQuery: (String) -> Unit = {},
    onViewOrder: (Order) -> Unit = {},
    onAssignDriver: (Order) -> Unit = {},
    onEditOrder: (Order) -> Unit = {},
    onAddOrder: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    // ✅ Conteo de pedidos por estado
    val totalOrders = orders.size
    val pendingCount = orders.count { it.status == OrderStatus.PENDING }
    val inRouteCount = orders.count { it.status == OrderStatus.IN_ROUTE }
    val deliveredCount = orders.count { it.status == OrderStatus.DELIVERED }
    val canceledCount = orders.count { it.status == OrderStatus.CANCELED }

    Scaffold(
        topBar = {
            OrderTopAppBar(
                searchQuery = searchQuery,
                onSearchQueryChange = {
                    searchQuery = it
                    onSearchQuery(it)
                },
                selectedFilter = selectedFilter,
                onFilterChange = onFilterChange,
                totalOrders = totalOrders,
                pendingCount = pendingCount,
                inRouteCount = inRouteCount,
                deliveredCount = deliveredCount,
                canceledCount = canceledCount
            )
        }
        //},
        // COMENTA O ELIMINA ESTE BLOQUE COMPLETO:
        // floatingActionButton = {
        //     FloatingActionButton(
        //         onClick = onAddOrder,
        //         containerColor = Color(0xFF197FE6),
        //         contentColor = Color.White
        //     ) {
        //         Icon(Icons.Default.Add, contentDescription = "Agregar pedido")
        //     }
        // }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(orders) { order ->
                OrderCard(
                    order = order,
                    onViewClick = { onViewOrder(order) },
                    onAssignClick = { onAssignDriver(order) },
                    onEditClick = { onEditOrder(order) }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTopAppBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: OrderStatus?,
    onFilterChange: (OrderStatus?) -> Unit,
    totalOrders: Int,
    pendingCount: Int,
    inRouteCount: Int,
    deliveredCount: Int,
    canceledCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    "Gestión de Pedidos",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            navigationIcon = {
                IconButton(onClick = { /* Menu action */ }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            },
            actions = {
                Spacer(modifier = Modifier.width(48.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { onFilterChange(null) },
                label = { Text("Todos ($totalOrders)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF197FE6),
                    selectedLabelColor = Color.White
                )
            )

            OrderStatus.entries.forEach { status ->
                val count = when (status) {
                    OrderStatus.PENDING -> pendingCount
                    OrderStatus.IN_ROUTE -> inRouteCount
                    OrderStatus.DELIVERED -> deliveredCount
                    OrderStatus.CANCELED -> canceledCount
                }

                FilterChip(
                    selected = selectedFilter == status,
                    onClick = { onFilterChange(status) },
                    label = { Text("${status.displayName} ($count)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF197FE6),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        HorizontalDivider(thickness = 1.dp, color = Color.Gray.copy(alpha = 0.1f))
    }
}

@Composable
fun OrderCard(
    order: Order,
    onViewClick: () -> Unit,
    onAssignClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Card Content
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        order.id,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    StatusBadge(order.status)
                }

                // Client & Recipient
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    InfoText(label = "Cliente:", value = order.client)
                    InfoText(label = "Destinatario:", value = order.recipient)
                }

                // Route
                InfoRow(
                    icon = Icons.Default.Route,
                    text = order.route
                )

                // Delivery Info
                InfoRow(
                    icon = when (order.status) {
                        OrderStatus.DELIVERED -> Icons.Default.EventAvailable
                        else -> Icons.Default.CalendarToday
                    },
                    text = order.deliveryInfo
                )

                // Driver Info
                order.driverInfo?.let { driver ->
                    InfoRow(
                        icon = Icons.Default.TwoWheeler,
                        text = driver,
                        textColor = when {
                            driver.contains("Sin motorizado") -> order.status.textColor
                            driver.contains("Dirección incorrecta") -> Color(0xFFEF4444)
                            else -> null
                        }
                    )
                } ?: run {
                    if (order.status == OrderStatus.PENDING) {
                        InfoRow(
                            icon = Icons.Default.Person,
                            text = "Sin motorizado asignado",
                            textColor = Color(0xFFFB923C)
                        )
                    }
                }
            }

            // Action Buttons
            HorizontalDivider(thickness = 1.dp, color = Color.Gray.copy(alpha = 0.1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                when (order.status) {
                    OrderStatus.PENDING -> {
                        ActionButton(
                            text = "Ver",
                            icon = Icons.Default.Visibility,
                            onClick = onViewClick,
                            modifier = Modifier.weight(1f),
                            isPrimary = false
                        )
                        VerticalDivider(thickness = 1.dp, color = Color.Gray.copy(alpha = 0.1f))
                        ActionButton(
                            text = "Asignar",
                            icon = Icons.Default.PersonAdd,
                            onClick = onAssignClick,
                            modifier = Modifier.weight(1f),
                            isPrimary = true
                        )
                    }
                    OrderStatus.CANCELED -> {
                        ActionButton(
                            text = "Editar",
                            icon = Icons.Default.Edit,
                            onClick = onEditClick,
                            modifier = Modifier.fillMaxWidth(),
                            isPrimary = false
                        )
                    }
                    else -> {
                        ActionButton(
                            text = "Ver Detalles",
                            icon = Icons.Default.Visibility,
                            onClick = onViewClick,
                            modifier = Modifier.fillMaxWidth(),
                            isPrimary = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: OrderStatus, modifier: Modifier = Modifier) {
    Row(
        modifier = Modifier
            .background(
                status.backgroundColor,
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(status.textColor)
        )
        Text(
            status.displayName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = status.textColor
        )
    }
}

@Composable
fun InfoText(label: String, value: String) {
    Text(
        buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(label)
            }
            append(" ")
            withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                append(value)
            }
        },
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun InfoRow(
    icon: ImageVector,
    text: String,
    textColor: Color? = null
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = textColor ?: MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(text)
                }
            },
            fontSize = 14.sp,
            color = textColor ?: MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(0.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (isPrimary) Color(0xFF197FE6) else Color.Transparent,
            contentColor = if (isPrimary) Color.White else Color(0xFF197FE6)
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// Sample Data
fun getSampleOrders() = listOf(
    Order(
        id = "#12345",
        status = OrderStatus.PENDING,
        client = "Cliente A",
        recipient = "Juan Pérez",
        route = "Surquillo → Miraflores",
        deliveryInfo = "Entrega: 25 Oct, 3:00 PM",
        driverInfo = "Sin motorizado asignado"
    ),
    Order(
        id = "#12346",
        status = OrderStatus.IN_ROUTE,
        client = "Proveedor B",
        recipient = "María López",
        route = "Lince → San Isidro",
        deliveryInfo = "Entrega: 25 Oct, 4:30 PM",
        driverInfo = "Motorizado: Carlos V."
    ),
    Order(
        id = "#12347",
        status = OrderStatus.DELIVERED,
        client = "Tienda C",
        recipient = "Ana Torres",
        route = "San Borja → La Molina",
        deliveryInfo = "Entregado: 24 Oct, 11:15 AM",
        driverInfo = null
    ),
    Order(
        id = "#12348",
        status = OrderStatus.CANCELED,
        client = "E-commerce D",
        recipient = "Luis Rojas",
        route = "Callao → Pueblo Libre",
        deliveryInfo = "Dirección incorrecta",
        driverInfo = "Dirección incorrecta"
    )
)

// Previews
@Preview(name = "Light Mode", showBackground = true)
@Composable
fun OrderManagementScreenPreview() {
    MaterialTheme {
        OrderManagementScreen()
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OrderManagementScreenDarkPreview() {
    MaterialTheme {
        OrderManagementScreen()
    }
}

@Preview(name = "Order Card - Pending", showBackground = true)
@Composable
fun OrderCardPendingPreview() {
    MaterialTheme {
        OrderCard(
            order = Order(
                id = "#12345",
                status = OrderStatus.PENDING,
                client = "Cliente A",
                recipient = "Juan Pérez",
                route = "Surquillo → Miraflores",
                deliveryInfo = "Entrega: 25 Oct, 3:00 PM",
                driverInfo = "Sin motorizado asignado"
            ),
            onViewClick = {},
            onAssignClick = {},
            onEditClick = {}
        )
    }
}

@Preview(name = "Order Card - In Route", showBackground = true)
@Composable
fun OrderCardInRoutePreview() {
    MaterialTheme {
        OrderCard(
            order = Order(
                id = "#12346",
                status = OrderStatus.IN_ROUTE,
                client = "Proveedor B",
                recipient = "María López",
                route = "Lince → San Isidro",
                deliveryInfo = "Entrega: 25 Oct, 4:30 PM",
                driverInfo = "Motorizado: Carlos V."
            ),
            onViewClick = {},
            onAssignClick = {},
            onEditClick = {}
        )
    }
}

@Preview(name = "Order Card - Delivered", showBackground = true)
@Composable
fun OrderCardDeliveredPreview() {
    MaterialTheme {
        OrderCard(
            order = Order(
                id = "#12347",
                status = OrderStatus.DELIVERED,
                client = "Tienda C",
                recipient = "Ana Torres",
                route = "San Borja → La Molina",
                deliveryInfo = "Entregado: 24 Oct, 11:15 AM",
                driverInfo = null
            ),
            onViewClick = {},
            onAssignClick = {},
            onEditClick = {}
        )
    }
}

@Preview(name = "Order Card - CANCELED", showBackground = true)
@Composable
fun OrderCardCANCELEDPreview() {
    MaterialTheme {
        OrderCard(
            order = Order(
                id = "#12348",
                status = OrderStatus.CANCELED,
                client = "E-commerce D",
                recipient = "Luis Rojas",
                route = "Callao → Pueblo Libre",
                deliveryInfo = "Dirección incorrecta",
                driverInfo = "Dirección incorrecta"
            ),
            onViewClick = {},
            onAssignClick = {},
            onEditClick = {}
        )
    }
}