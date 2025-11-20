package com.example.ordermanagement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.android_app.orders.*
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Chat

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.TextButton



// Tipos de usuario en el sistema
enum class UserRole {
    ADMIN,      // Administrador: puede Editar, Cancelar, Entregar
    DRIVER,     // Motorizado: puede Cancelar, Entregar
    CLIENT      // Cliente: puede Editar
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    order: Order,
    userRole: UserRole = UserRole.ADMIN, // Por defecto es admin para compatibilidad
    onBackClick: () -> Unit = {},
    onEditOrder: () -> Unit = {},
    onCancelOrder: () -> Unit = {},
    onPickupOrder: () -> Unit = {},
    onDeliverOrder: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var orderDetails by remember { mutableStateOf<OrderDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showCancelDialog by remember { mutableStateOf(false) }

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
            // Solo mostrar la barra de acciones si el pedido NO está cancelado
            if (order.status != OrderStatus.CANCELED) {
                OrderDetailsBottomActions(
                    userRole = userRole,
                    isPickedUp = orderDetails?.isPickedUp ?: false,
                    onEditOrder = onEditOrder,
                    onCancelOrder = { showCancelDialog = true },
                    onPickupOrder = onPickupOrder,
                    onDeliverOrder = onDeliverOrder
                )
            }
        }
    ) { padding ->
        // ✅ Renderizado progresivo: mostramos datos básicos de inmediato
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
                        val displayId = if (id.length > 13) {
                            "# ${id.takeLast(13)}"
                        } else {
                            id
                        }

                        Text(
                            text = displayId,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Badge ocupa 1/3 del ancho
                    StatusBadge(
                        order.status,
                        modifier = Modifier.weight(1f)
                    )
                }


                // Motorizado de Recojo
                if (order.status != OrderStatus.CANCELED) {
                    DetailCard(title = "Motorizado de Recojo") {
                        if (isLoading) {
                            CourierInfo(
                                driverName = "Cargando...",
                                driverRating = null,
                                driverPhone = null
                            )
                        } else {
                            CourierInfo(
                                driverName = orderDetails?.pickupDriverName ?: "Sin motorizado asignado",
                                driverRating = orderDetails?.pickupDriverRating,
                                driverPhone = orderDetails?.pickupDriverPhone
                            )
                        }
                    }
                }

                // Motorizado de Entrega (solo si ya fue recogido o está en ruta)
                if (order.status != OrderStatus.CANCELED &&
                    (orderDetails?.isPickedUp == true || order.status == OrderStatus.DELIVERED)) {
                    DetailCard(title = "Motorizado de Entrega") {
                        if (isLoading) {
                            CourierInfo(
                                driverName = "Cargando...",
                                driverRating = null,
                                driverPhone = null
                            )
                        } else {
                            CourierInfo(
                                driverName = orderDetails?.deliveryDriverName ?: "Sin motorizado asignado",
                                driverRating = orderDetails?.deliveryDriverRating,
                                driverPhone = orderDetails?.deliveryDriverPhone
                            )
                        }
                    }
                }

                // Información General - Muestra datos básicos inmediatamente
                DetailCard(title = "Información General") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Información del Cliente
                        DetailInfoRow(label = "Cliente", value = order.client)
                        if (isLoading) {
                            ContactInfoRowSkeleton(label = "Teléfono Cliente")
                        } else {
                            orderDetails?.customerPhone?.let { phone ->
                                ContactInfoRow(label = "Teléfono Cliente", phoneNumber = phone)
                            }
                        }

                        // Separador visual
                        if (orderDetails?.customerPhone != null) {
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        // Información del Destinatario
                        DetailInfoRow(label = "Destinatario", value = order.recipient)
                        if (isLoading) {
                            ContactInfoRowSkeleton(label = "Teléfono Destinatario")
                        } else {
                            orderDetails?.recipientPhone?.let { phone ->
                                ContactInfoRow(label = "Teléfono Destinatario", phoneNumber = phone)
                            }
                        }
                    }
                }

                // Ruta
                DetailCard(title = "Ruta") {
                    orderDetails?.let { details ->
                        RouteInfoRow(
                            routeText = order.route,
                            pickupLat = details.pickupLat,
                            pickupLng = details.pickupLng,
                            deliveryLat = details.deliveryLat,
                            deliveryLng = details.deliveryLng
                        )
                    } ?: InfoRow(
                        icon = Icons.Default.Route,
                        text = order.route
                    )
                }


            // Detalles del Paquete - Skeleton mientras carga
                DetailCard(title = "Detalles del Paquete") {
                    if (isLoading) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SkeletonLoader(width = 200.dp, height = 20.dp)
                            SkeletonLoader(width = 180.dp, height = 20.dp)
                            SkeletonLoader(width = 160.dp, height = 20.dp)
                        }
                    } else {
                        orderDetails?.let { details ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                DetailInfoRow(label = "Dimensiones", value = details.dimensions)
                                DetailInfoRow(label = "Volumen", value = details.volume)
                                DetailInfoRow(label = "Peso", value = details.weight)
                            }
                        }
                    }
                }

                // Cronología - Skeleton mientras carga
                DetailCard(title = "Cronología") {
                    if (isLoading) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SkeletonLoader(width = 220.dp, height = 20.dp)
                            SkeletonLoader(width = 200.dp, height = 20.dp)
                        }
                    } else {
                        orderDetails?.let { details ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                DetailInfoRow(label = "Fecha creación", value = details.createdAt)
                                DetailInfoRow(label = "Entrega programada", value = details.scheduledDelivery)
                                details.pickedUpAt?.let { pickedUpDate ->
                                    DetailInfoRow(label = "Fecha de recogida", value = pickedUpDate)
                                }
                                details.deliveredAt?.let { deliveredDate ->
                                    DetailInfoRow(label = "Fecha de entrega", value = deliveredDate)
                                }
                            }
                        }
                    }
                }

                // Finanzas - Skeleton mientras carga
                DetailCard(title = "Finanzas") {
                    if (isLoading) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SkeletonLoader(width = 150.dp, height = 20.dp)
                            SkeletonLoader(width = 150.dp, height = 20.dp)
                            SkeletonLoader(width = 150.dp, height = 20.dp)
                            SkeletonLoader(width = 130.dp, height = 20.dp)
                        }
                    } else {
                        orderDetails?.let { details ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Si se cobra, mostrar desglose completo
                                if (details.shouldCharge) {
                                    DetailInfoRow(label = "Monto base", value = details.baseAmount)
                                    DetailInfoRow(label = "Comisión", value = details.commission)
                                    DetailInfoRow(label = "Monto total", value = details.serviceAmount)
                                } else {
                                    // Si no se cobra, solo mostrar monto total
                                    DetailInfoRow(label = "Monto total", value = details.serviceAmount)
                                }
                                DetailInfoRow(
                                    label = "Estado de pago",
                                    value = details.paymentStatus,
                                    valueColor = details.paymentStatusColor
                                )
                            }
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

    // Diálogo de cancelación
    if (showCancelDialog && orderDetails != null) {
        CancelOrderDialog(
            orderDetails = orderDetails!!,
            onDismiss = { showCancelDialog = false },
            onConfirm = { shouldCharge, chargeAmount ->
                val db = FirebaseFirestore.getInstance()
                val updates = hashMapOf<String, Any>(
                    "fechas.anulacion" to com.google.firebase.Timestamp.now(),
                    "asignacion.recojo.estado" to "cancelado",
                    "asignacion.entrega.estado" to "cancelado",
                    "pago.montoTotal" to chargeAmount
                )

                db.collection("pedidos")
                    .document(order.id)
                    .update(updates)
                    .addOnSuccessListener {
                        showCancelDialog = false
                        onCancelOrder()
                        onBackClick()
                    }
                    .addOnFailureListener { e ->
                        Log.e("OrderDetailsScreen", "Error al cancelar pedido: ${e.message}")
                        showCancelDialog = false
                    }
            }
        )
    }
}

@Composable
fun SkeletonLoader(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    )
}

@Composable
fun ContactInfoRowSkeleton(label: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonLoader(width = 120.dp, height = 18.dp)

            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Skeleton del botón de WhatsApp
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                )

                // Skeleton del botón de llamada
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                )
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


// Componente con lógica de negocio (maneja contexto y permisos)
@Composable
fun ContactInfoRow(
    label: String,
    phoneNumber: String
) {
    val context = LocalContext.current

    // Launcher para solicitar permiso de llamada
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")

        if (isGranted) {
            // Permiso concedido, realizar la llamada
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$cleanNumber")
            }
            context.startActivity(intent)
        } else {
            // Permiso denegado, usar marcador como alternativa
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanNumber")
            }
            context.startActivity(intent)
            android.widget.Toast.makeText(
                context,
                "Permiso de llamada denegado. Se abrirá el marcador.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Funciones de acción
    val onWhatsAppClick: () -> Unit = {
        try {
            var cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")

            Log.d("ContactInfoRow", "WhatsApp - Número recibido: $phoneNumber")
            Log.d("ContactInfoRow", "WhatsApp - Número limpio inicial: $cleanNumber")

            // Si no empieza con +, agregar código de país (ejemplo Perú = +51)
            if (!cleanNumber.startsWith("+")) {
                cleanNumber = "+51$cleanNumber"
                Log.d("ContactInfoRow", "WhatsApp - Número ajustado con código de país: $cleanNumber")
            }

            val pm = context.packageManager
            val availableIntents = mutableListOf<Intent>()

            // Verificar WhatsApp normal
            try {
                pm.getPackageInfo("com.whatsapp", 0)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$cleanNumber")
                    setPackage("com.whatsapp")
                }
                availableIntents.add(intent)
                Log.d("ContactInfoRow", "WhatsApp normal encontrado")
            } catch (e: PackageManager.NameNotFoundException) {
                Log.d("ContactInfoRow", "WhatsApp normal NO encontrado")
            }

            // Verificar WhatsApp Business
            try {
                pm.getPackageInfo("com.whatsapp.w4b", 0)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$cleanNumber")
                    setPackage("com.whatsapp.w4b")
                }
                availableIntents.add(intent)
                Log.d("ContactInfoRow", "WhatsApp Business encontrado")
            } catch (e: PackageManager.NameNotFoundException) {
                Log.d("ContactInfoRow", "WhatsApp Business NO encontrado")
            }

            when {
                availableIntents.isEmpty() -> {
                    Log.d("ContactInfoRow", "Ningún WhatsApp instalado, intentando navegador")
                    val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://wa.me/$cleanNumber")
                    }
                    context.startActivity(browserIntent)
                }
                availableIntents.size == 1 -> {
                    Log.d("ContactInfoRow", "Abriendo WhatsApp directamente")
                    context.startActivity(availableIntents.first())
                }
                else -> {
                    Log.d("ContactInfoRow", "Mostrando selector de WhatsApp")
                    val chooser = Intent.createChooser(
                        availableIntents.first(),
                        "Abrir con WhatsApp"
                    )
                    chooser.putExtra(
                        Intent.EXTRA_INITIAL_INTENTS,
                        availableIntents.drop(1).toTypedArray()
                    )
                    context.startActivity(chooser)
                }
            }

        } catch (e: Exception) {
            Log.e("ContactInfoRow", "Error al abrir WhatsApp", e)
            android.widget.Toast.makeText(
                context,
                "Error al abrir WhatsApp: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    val onCallClick: () -> Unit = {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")

        Log.d("ContactInfoRow", "Dialer - Número recibido: $phoneNumber")
        Log.d("ContactInfoRow", "Dialer - Número limpio: $cleanNumber")

        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) -> {
                try {
                    val intent = Intent(Intent.ACTION_CALL).apply {
                        data = Uri.parse("tel:$cleanNumber")
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(
                        context,
                        "No se pudo iniciar la llamada",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            else -> {
                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            }
        }
    }

    // Usar el componente puro de UI
    ContactInfoRowContent(
        label = label,
        phoneNumber = phoneNumber,
        onWhatsAppClick = onWhatsAppClick,
        onCallClick = onCallClick
    )
}

// Componente puro de UI (sin dependencias de Android, renderizable en Preview)
@Composable
fun ContactInfoRowContent(
    label: String,
    phoneNumber: String,
    onWhatsAppClick: () -> Unit = {},
    onCallClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                phoneNumber,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Botón de WhatsApp
                IconButton(
                    onClick = onWhatsAppClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF25D366).copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "WhatsApp",
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Botón de llamada directa
                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF10B981).copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Llamar",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
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
fun RouteInfoRowContent(
    routeText: String,
    onPickupMapsClick: () -> Unit = {},
    onDeliveryMapsClick: () -> Unit = {}
) {
    // Extraer origen y destino del texto de ruta (ej: "Surquillo → Miraflores")
    val parts = routeText.split("→").map { it.trim() }
    val origin = parts.getOrNull(0) ?: ""
    val destination = parts.getOrNull(1) ?: ""

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // 🔰 Icono de ruta al inicio
        Icon(
            imageVector = Icons.Default.Route,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 🟦 Botón ORIGEN
        Button(
            onClick = { onPickupMapsClick() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4285F4).copy(alpha = 0.1f)
            ),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = origin,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4285F4),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(6.dp))
        }

        // ➡️ Flecha
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 🟩 Botón DESTINO
        Button(
            onClick = { onDeliveryMapsClick() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF10B981).copy(alpha = 0.1f)
            ),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = destination,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10B981),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(6.dp))
        }
    }



}

@Composable
fun RouteInfoRow(
    routeText: String,
    pickupLat: Double?,
    pickupLng: Double?,
    deliveryLat: Double?,
    deliveryLng: Double?
) {
    val context = LocalContext.current

    val onPickupMapsClick: () -> Unit = {
        if (pickupLat != null && pickupLng != null) {
            try {
                val uri = Uri.parse(
                    "https://www.google.com/maps/search/?api=1&query=$pickupLat,$pickupLng"
                )
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Si Google Maps no está instalado, abrir en el navegador
                val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$pickupLat,$pickupLng")
                val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
                context.startActivity(browserIntent)
            }
        } else {
            android.widget.Toast.makeText(
                context,
                "Coordenadas de recojo no disponibles",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    val onDeliveryMapsClick: () -> Unit = {
        if (deliveryLat != null && deliveryLng != null) {
            try {
                val uri = Uri.parse(
                    "https://www.google.com/maps/search/?api=1&query=$deliveryLat,$deliveryLng"
                )
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Si Google Maps no está instalado, abrir en el navegador
                val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$deliveryLat,$deliveryLng")
                val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
                context.startActivity(browserIntent)
            }
        } else {
            android.widget.Toast.makeText(
                context,
                "Coordenadas de entrega no disponibles",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Reutilizamos el componente puro
    RouteInfoRowContent(
        routeText = routeText,
        onPickupMapsClick = onPickupMapsClick,
        onDeliveryMapsClick = onDeliveryMapsClick
    )
}


@Composable
fun OrderDetailsBottomActions(
    userRole: UserRole,
    isPickedUp: Boolean,
    onEditOrder: () -> Unit,
    onCancelOrder: () -> Unit,
    onPickupOrder: () -> Unit,
    onDeliverOrder: () -> Unit
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
            when (userRole) {
                UserRole.ADMIN -> {
                    // Administrador: Editar, Cancelar, Entregar
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
                        Spacer(Modifier.width(4.dp))
                        Text("Editar", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onCancelOrder,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF4444)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFEF4444))
                        )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Cancel", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }

                    Button(
                        onClick = if (isPickedUp) onDeliverOrder else onPickupOrder,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPickedUp) Color(0xFF10B981) else Color(0xFF005A9C),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (isPickedUp) "Entreg" else "Recoger",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }

                UserRole.DRIVER -> {
                    // Motorizado: Cancelar, Recoger/Entregar
                    OutlinedButton(
                        onClick = onCancelOrder,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF4444)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFEF4444))
                        )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Cancelar", fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = if (isPickedUp) onDeliverOrder else onPickupOrder,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPickedUp) Color(0xFF10B981) else Color(0xFF005A9C),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isPickedUp) "Entregar" else "Recoger",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                UserRole.CLIENT -> {
                    // Cliente: Solo Editar
                    Button(
                        onClick = onEditOrder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF197FE6),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Editar Pedido", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}


@Composable
fun CancelOrderDialog(
    orderDetails: OrderDetails,
    onDismiss: () -> Unit,
    onConfirm: (shouldCharge: Boolean, chargeAmount: Double) -> Unit
) {
    var shouldCharge by remember { mutableStateOf(false) }

    // Extraer el valor numérico de la comisión
    val commissionValue = orderDetails.commission
        .replace("S/", "")
        .replace(",", "")
        .trim()
        .toDoubleOrNull() ?: 0.0

    var chargeAmount by remember { mutableStateOf(commissionValue.toString()) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Cancelar pedido",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "¿Desea cobrar por este pedido cancelado?",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Opciones de cobro
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Opción: No cobrar
                    FilterChip(
                        selected = !shouldCharge,
                        onClick = { shouldCharge = false },
                        label = { Text("No cobrar") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6B7280),
                            selectedLabelColor = Color.White
                        )
                    )

                    // Opción: Cobrar
                    FilterChip(
                        selected = shouldCharge,
                        onClick = { shouldCharge = true },
                        label = { Text("Cobrar") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF10B981),
                            selectedLabelColor = Color.White
                        )
                    )
                }

                // Campo de texto editable si se elige cobrar
                if (shouldCharge) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Monto a cobrar:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = chargeAmount,
                            onValueChange = { newValue ->
                                // Permitir solo números y punto decimal
                                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    chargeAmount = newValue
                                    showError = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("S/") },
                            singleLine = true,
                            isError = showError,
                            supportingText = if (showError) {
                                { Text("Ingrese un monto válido") }
                            } else {
                                { Text("Monto por defecto: comisión (S/ ${String.format("%.2f", commissionValue)})") }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF10B981),
                                focusedLabelColor = Color(0xFF10B981)
                            )
                        )
                    }
                }

                // Resumen
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Resumen de cancelación:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val finalAmount = if (shouldCharge) {
                            chargeAmount.toDoubleOrNull() ?: 0.0
                        } else {
                            0.0
                        }

                        Text(
                            "Monto total final: S/ ${String.format("%.2f", finalAmount)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (finalAmount > 0) Color(0xFF10B981) else Color(0xFF6B7280)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (shouldCharge) {
                        val amount = chargeAmount.toDoubleOrNull()
                        if (amount != null && amount >= 0) {
                            onConfirm(shouldCharge, amount)
                        } else {
                            showError = true
                        }
                    } else {
                        onConfirm(false, 0.0)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444),
                    contentColor = Color.White
                )
            ) {
                Text("Confirmar cancelación")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Preview(name = "Contact Info Row with Buttons", showBackground = true)
@Composable
fun PreviewContactInfoRow() {
    MaterialTheme {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ContactInfoRowContent(
                    label = "Teléfono Cliente",
                    phoneNumber = "912345678"
                )

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )

                ContactInfoRowContent(
                    label = "Teléfono Destinatario",
                    phoneNumber = "976543210"
                )
            }
        }
    }
}

@Preview(name = "Route Info with Maps Buttons", showBackground = true)
@Composable
fun PreviewRouteInfoRow() {
    MaterialTheme {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                RouteInfoRowContent(
                    routeText = "Surquillo → Miraflores"
                )
            }
        }
    }
}

@Preview(name = "Contact Info Skeleton", showBackground = true)
@Composable
fun PreviewContactInfoSkeleton() {
    MaterialTheme {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ContactInfoRowSkeleton(label = "Teléfono Cliente")

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )

                ContactInfoRowSkeleton(label = "Teléfono Destinatario")
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
        pickupDriverName = "Carlos Rodríguez",
        pickupDriverPhone = "987654321",
        pickupDriverRating = 4.8f,
        deliveryDriverName = "María González",
        deliveryDriverPhone = "976543210",
        deliveryDriverRating = 4.9f,
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
        isPickedUp = true,
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
                        userRole = UserRole.ADMIN, // Puedes cambiar a DRIVER o CLIENT para ver diferentes vistas
                        isPickedUp = false, // Cambiar a true para ver el botón "Entregar"
                        onEditOrder = {},
                        onCancelOrder = {},
                        onPickupOrder = {},
                        onDeliverOrder = {}
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



                    DetailCard(title = "Motorizado de Recojo") {
                        CourierInfo(
                            driverName = sampleOrderDetails.pickupDriverName ?: "Sin motorizado asignado",
                            driverRating = sampleOrderDetails.pickupDriverRating,
                            driverPhone = sampleOrderDetails.pickupDriverPhone
                        )
                    }

                    DetailCard(title = "Motorizado de Entrega") {
                        CourierInfo(
                            driverName = sampleOrderDetails.deliveryDriverName ?: "Sin motorizado asignado",
                            driverRating = sampleOrderDetails.deliveryDriverRating,
                            driverPhone = sampleOrderDetails.deliveryDriverPhone
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

