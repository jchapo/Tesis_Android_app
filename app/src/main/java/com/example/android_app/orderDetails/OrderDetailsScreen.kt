package com.example.ordermanagement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.android_app.orders.*
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    order: Order,
    onBackClick: () -> Unit = {},
    onEditOrder: () -> Unit = {},
    onUpdateStatus: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var orderDetails by remember { mutableStateOf<OrderDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Cargar detalles completos del pedido
    LaunchedEffect(order.id) {
        val db = FirebaseFirestore.getInstance()
        db.collection("pedidos")
            .document(order.id)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    orderDetails = transformToOrderDetails(document.id, document.data)
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ID ocupa 2/3 del ancho
                    Column(
                        modifier = Modifier
                            .weight(2f)
                            .padding(end = 8.dp)
                    ) {
                        val id = order.id
                        val firstPart = id.take(10)
                        val secondPart = if (id.length > 10) id.drop(10) else ""

                        Text(
                            text = firstPart,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                        if (secondPart.isNotEmpty()) {
                            Text(
                                text = secondPart,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Badge ocupa 1/3 del ancho
                    StatusBadge(
                        order.status,
                        modifier = Modifier.weight(1f)
                    )
                }


                // Motorizado Asignado
                DetailCard(title = "Motorizado Asignado") {
                    CourierInfo(
                        driverName = order.driverInfo ?: "Sin motorizado asignado",
                        driverRating = orderDetails?.driverRating,
                        driverPhone = orderDetails?.driverPhone
                    )
                }

                // Información General
                DetailCard(title = "Información General") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailInfoRow(label = "Cliente", value = order.client)
                        orderDetails?.customerPhone?.let { phone ->
                            DetailInfoRow(label = "Teléfono", value = phone)
                        }
                        DetailInfoRow(label = "Destinatario", value = order.recipient)
                        orderDetails?.recipientPhone?.let { phone ->
                            DetailInfoRow(label = "Teléfono", value = phone)
                        }
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
                if (orderDetails != null) {
                    DetailCard(title = "Detalles del Paquete") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DetailInfoRow(
                                label = "Dimensiones",
                                value = orderDetails!!.dimensions
                            )
                            DetailInfoRow(
                                label = "Volumen",
                                value = orderDetails!!.volume
                            )
                            DetailInfoRow(
                                label = "Peso",
                                value = orderDetails!!.weight
                            )
                        }
                    }
                }

                // Cronología
                if (orderDetails != null) {
                    DetailCard(title = "Cronología") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DetailInfoRow(
                                label = "Fecha creación",
                                value = orderDetails!!.createdAt
                            )
                            DetailInfoRow(
                                label = "Entrega programada",
                                value = orderDetails!!.scheduledDelivery
                            )
                            orderDetails!!.deliveredAt?.let { deliveredDate ->
                                DetailInfoRow(
                                    label = "Fecha de entrega",
                                    value = deliveredDate
                                )
                            }
                        }
                    }
                }

                // Finanzas
                if (orderDetails != null) {
                    DetailCard(title = "Finanzas") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DetailInfoRow(
                                label = "Monto del servicio",
                                value = orderDetails!!.serviceAmount
                            )
                            DetailInfoRow(
                                label = "Estado de pago",
                                value = orderDetails!!.paymentStatus,
                                valueColor = orderDetails!!.paymentStatusColor
                            )
                        }
                    }
                }

                // Fotos del Paquete
                if (orderDetails?.photos?.isNotEmpty() == true) {
                    DetailCard(title = "Fotos del Paquete") {
                        PhotosGrid(photos = orderDetails!!.photos)
                    }
                }

                Spacer(Modifier.height(80.dp))
            }
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
fun CourierInfo(
    driverName: String,
    driverRating: Float? = null,
    driverPhone: String? = null
) {
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
            if (driverName != "Sin motorizado asignado" && driverRating != null) {
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
                        String.format("%.1f", driverRating),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        if (driverName != "Sin motorizado asignado" && driverPhone != null) {
            IconButton(
                onClick = { /* TODO: Implementar llamada */ },
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


@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewFullOrderDetailsScreen() {
    // Datos de ejemplo para simular un pedido completo
    val sampleOrder = Order(
        id = "ORD-12345786587658765758765865876587657657",
        status = OrderStatus.PENDING,
        client = "Cliente A",
        recipient = "Juan Pérez",
        route = "Surquillo → Miraflores",
        deliveryInfo = "Entrega: 25 Oct, 3:00 PM",
        driverInfo = "Sin motorizado asignado"
    )

    // Simulación de detalles adicionales del pedido
    val sampleOrderDetails = OrderDetails(
        order = sampleOrder,
        driverRating = 4.8f,
        driverPhone = "987654321",
        customerPhone = "912345678",
        recipientPhone = "976543210",
        dimensions = "30x20x15 cm",
        volume = "9,000 cm³",
        weight = "2.5 kg",
        createdAt = "10 Nov 2025 - 10:00 AM",
        scheduledDelivery = "11 Nov 2025 - 3:00 PM",
        deliveredAt = null,
        serviceAmount = "S/ 25.00",
        paymentStatus = "Pagado",
        paymentStatusColor = Color(0xFF10B981),
        photos = listOf(
            "data:image/png;base64,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",
            "data:image/png;base64,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",
        )
    )

    // Contenedor para la vista previa
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            // Simula el contenido principal sin usar Firestore
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
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                bottomBar = {
                    OrderDetailsBottomActions(
                        onEditOrder = {},
                        onUpdateStatus = {}
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ID ocupa 2/3 del ancho
                        Text(
                            sampleOrder.id,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .weight(2f) // 2 partes del total (de 3)
                                .padding(end = 8.dp),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Badge ocupa 1/3 del ancho
                        StatusBadge(
                            sampleOrder.status,
                            modifier = Modifier.weight(1f)
                        )
                    }



                    DetailCard(title = "Motorizado Asignado") {
                        CourierInfo(
                            driverName = sampleOrder.driverInfo ?: "Sin motorizado asignado",
                            driverRating = sampleOrderDetails.driverRating,
                            driverPhone = sampleOrderDetails.driverPhone
                        )
                    }

                    DetailCard(title = "Información General") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DetailInfoRow(label = "Cliente", value = sampleOrder.client)
                            DetailInfoRow(label = "Teléfono", value = sampleOrderDetails.customerPhone.toString())
                            DetailInfoRow(label = "Destinatario", value = sampleOrder.recipient)
                            DetailInfoRow(label = "Teléfono", value = sampleOrderDetails.recipientPhone.toString())
                        }
                    }

                    DetailCard(title = "Ruta") {
                        InfoRow(icon = Icons.Default.Route, text = sampleOrder.route)
                    }

                    DetailCard(title = "Detalles del Paquete") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DetailInfoRow(label = "Dimensiones", value = sampleOrderDetails.dimensions)
                            DetailInfoRow(label = "Volumen", value = sampleOrderDetails.volume)
                            DetailInfoRow(label = "Peso", value = sampleOrderDetails.weight)
                        }
                    }

                    DetailCard(title = "Cronología") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DetailInfoRow(label = "Fecha creación", value = sampleOrderDetails.createdAt)
                            DetailInfoRow(label = "Entrega programada", value = sampleOrderDetails.scheduledDelivery)
                        }
                    }

                    DetailCard(title = "Finanzas") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DetailInfoRow(
                                label = "Monto del servicio",
                                value = sampleOrderDetails.serviceAmount
                            )
                            DetailInfoRow(
                                label = "Estado de pago",
                                value = sampleOrderDetails.paymentStatus,
                                valueColor = sampleOrderDetails.paymentStatusColor
                            )
                        }
                    }

                    DetailCard(title = "Fotos del Paquete") {
                        PhotosGrid(photos = sampleOrderDetails.photos)
                    }

                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

