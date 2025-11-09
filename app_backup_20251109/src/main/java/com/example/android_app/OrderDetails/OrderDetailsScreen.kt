package com.example.ordermanagement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.android_app.Orders.InfoRow
import com.example.android_app.Orders.Order
import com.example.android_app.Orders.OrderStatus
import com.example.android_app.Orders.StatusBadge
import com.example.android_app.Orders.getSampleOrders

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    order: Order = getSampleOrders()[1],
    onBackClick: () -> Unit = {},
    onEditOrder: () -> Unit = {},
    onUpdateStatus: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Detalles del Pedido",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    Spacer(modifier = Modifier.width(48.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            OrderDetailsBottomActions(
                onEditOrder = onEditOrder,
                onUpdateStatus = onUpdateStatus
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Order ID and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    order.id,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(order.status)
            }

            // Motorizado Asignado
            DetailCard(title = "Motorizado Asignado") {
                CourierInfo(order.driverInfo ?: "Sin motorizado asignado")
            }

            // Información General
            DetailCard(title = "Información General") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailInfoRow(label = "Cliente", value = order.client)
                    DetailInfoRow(label = "Destinatario", value = order.recipient)
                }
            }

            // Ruta
            DetailCard(title = "Ruta") {
                InfoRow(
                    icon = Icons.Default.Route,
                    text = order.route
                )
            }

            // Detalles del Paquete
            DetailCard(title = "Detalles del Paquete") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailInfoRow(label = "Dimensiones", value = "10 x 20 x 15 cm")
                    DetailInfoRow(label = "Volumen", value = "3,000 cm³")
                    DetailInfoRow(label = "Peso", value = "2.5 kg")
                }
            }

            // Cronología
            DetailCard(title = "Cronología") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailInfoRow(
                        label = "Fecha creación",
                        value = "25 Oct 2023, 10:00 AM"
                    )
                    DetailInfoRow(
                        label = "Entrega programada",
                        value = order.deliveryInfo
                    )
                }
            }

            // Finanzas
            DetailCard(title = "Finanzas") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailInfoRow(label = "Monto del servicio", value = "S/ 25.00")
                    DetailInfoRow(label = "Estado de pago", value = "Pagado", valueColor = Color(0xFF10B981))
                }
            }

            // Fotos del Paquete
            DetailCard(title = "Fotos del Paquete") {
                PhotosGrid(
                    photos = listOf(
                        "https://picsum.photos/200/200",
                        "https://picsum.photos/201/201",
                        "https://picsum.photos/202/202",
                        "https://picsum.photos/203/203"
                    )
                )
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun DetailCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun CourierInfo(driverName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFF197FE6).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = "Motorizado",
                modifier = Modifier.size(32.dp),
                tint = Color(0xFF197FE6)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                driverName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (driverName != "Sin motorizado asignado") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFB923C)
                    )
                    Text(
                        "4.8",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        if (driverName != "Sin motorizado asignado") {
            IconButton(
                onClick = { /* Call action */ },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF10B981).copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = "Llamar",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun DetailInfoRow(
    label: String,
    value: String,
    valueColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            value,
            fontSize = 14.sp,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun PhotosGrid(photos: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        photos.chunked(2).forEach { rowPhotos ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowPhotos.forEach { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "Foto del paquete",
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                }
                // Si la fila tiene solo 1 foto, agregar espacio vacío
                if (rowPhotos.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun OrderDetailsBottomActions(
    onEditOrder: () -> Unit,
    onUpdateStatus: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onEditOrder,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF197FE6)
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF197FE6))
                )
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Editar", fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = onUpdateStatus,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF197FE6),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.Default.Update,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Actualizar", fontWeight = FontWeight.Medium)
            }
        }
    }
}

// Previews
@Preview(name = "Order Details - Light", showBackground = true)
@Composable
fun OrderDetailsScreenPreview() {
    MaterialTheme {
        OrderDetailsScreen()
    }
}

@Preview(name = "Order Details - Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OrderDetailsScreenDarkPreview() {
    MaterialTheme {
        OrderDetailsScreen()
    }
}

@Preview(name = "Order Details - Pending", showBackground = true)
@Composable
fun OrderDetailsPendingPreview() {
    MaterialTheme {
        OrderDetailsScreen(
            order = Order(
                id = "#12345",
                status = OrderStatus.PENDING,
                client = "Cliente A",
                recipient = "Juan Pérez",
                route = "Surquillo → Miraflores",
                deliveryInfo = "Entrega: 25 Oct, 3:00 PM",
                driverInfo = "Sin motorizado asignado"
            )
        )
    }
}

@Preview(name = "Order Details - In Route", showBackground = true)
@Composable
fun OrderDetailsInRoutePreview() {
    MaterialTheme {
        OrderDetailsScreen(
            order = Order(
                id = "#12346",
                status = OrderStatus.IN_ROUTE,
                client = "Proveedor B",
                recipient = "María López",
                route = "Lince → San Isidro",
                deliveryInfo = "Entrega: 25 Oct, 4:30 PM",
                driverInfo = "Motorizado: Carlos V."
            )
        )
    }
}

@Preview(name = "Courier Card - Assigned", showBackground = true)
@Composable
fun CourierInfoAssignedPreview() {
    MaterialTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                CourierInfo("Carlos Vega")
            }
        }
    }
}

@Preview(name = "Courier Card - Not Assigned", showBackground = true)
@Composable
fun CourierInfoNotAssignedPreview() {
    MaterialTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                CourierInfo("Sin motorizado asignado")
            }
        }
    }
}