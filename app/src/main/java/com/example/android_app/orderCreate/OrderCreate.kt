package com.example.android_app.orderCreate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Data Model
data class NewOrderData(
    val senderName: String = "",
    val pickupDistrict: String = "",
    val recipientName: String = "",
    val deliveryDistrict: String = "",
    val height: String = "",
    val width: String = "",
    val length: String = "",
    val amount: String = "",
    val creationDate: String = "",
    val deliveryDate: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    onBackClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onSaveClick: (NewOrderData) -> Unit = {}
) {
    var orderData by remember { mutableStateOf(NewOrderData()) }

    // Calcular volumen automáticamente
    val calculatedVolume = remember(orderData.height, orderData.width, orderData.length) {
        val h = orderData.height.toFloatOrNull() ?: 0f
        val w = orderData.width.toFloatOrNull() ?: 0f
        val l = orderData.length.toFloatOrNull() ?: 0f
        if (h > 0 && w > 0 && l > 0) {
            String.format("%,.0f cm³", h * w * l)
        } else {
            "0 cm³"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Crear Nuevo Pedido",
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
            BottomActionBar(
                onCancelClick = onCancelClick,
                onSaveClick = { onSaveClick(orderData) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Información del Remitente
            item {
                FormSection(title = "Información del Remitente") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        FormTextField(
                            label = "Cliente / Proveedor",
                            value = orderData.senderName,
                            onValueChange = { orderData = orderData.copy(senderName = it) },
                            placeholder = "Ej: Inversiones Tech S.A.C."
                        )
                        FormTextField(
                            label = "Distrito Recojo",
                            value = orderData.pickupDistrict,
                            onValueChange = { orderData = orderData.copy(pickupDistrict = it) },
                            placeholder = "Ej: San Isidro"
                        )
                    }
                }
            }

            // Información del Destinatario
            item {
                FormSection(title = "Información del Destinatario") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        FormTextField(
                            label = "Nombre del Destinatario",
                            value = orderData.recipientName,
                            onValueChange = { orderData = orderData.copy(recipientName = it) },
                            placeholder = "Ej: Maria Rodriguez"
                        )
                        FormTextField(
                            label = "Distrito Entrega",
                            value = orderData.deliveryDistrict,
                            onValueChange = { orderData = orderData.copy(deliveryDistrict = it) },
                            placeholder = "Ej: La Molina"
                        )
                    }
                }
            }

            // Detalles del Paquete
            item {
                FormSection(title = "Detalles del Paquete") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Dimensiones
                        Column {
                            Text(
                                "Dimensiones (cm)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                FormTextField(
                                    value = orderData.height,
                                    onValueChange = { orderData = orderData.copy(height = it) },
                                    placeholder = "Alto",
                                    keyboardType = KeyboardType.Decimal,
                                    modifier = Modifier.weight(1f),
                                    showLabel = false
                                )
                                FormTextField(
                                    value = orderData.width,
                                    onValueChange = { orderData = orderData.copy(width = it) },
                                    placeholder = "Ancho",
                                    keyboardType = KeyboardType.Decimal,
                                    modifier = Modifier.weight(1f),
                                    showLabel = false
                                )
                                FormTextField(
                                    value = orderData.length,
                                    onValueChange = { orderData = orderData.copy(length = it) },
                                    placeholder = "Largo",
                                    keyboardType = KeyboardType.Decimal,
                                    modifier = Modifier.weight(1f),
                                    showLabel = false
                                )
                            }
                        }

                        // Volumen con badge IA
                        Column {
                            Text(
                                "Volumen",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        calculatedVolume,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFF197FE6).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            "Estimado por IA",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF197FE6),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Monto
                        FormTextField(
                            label = "Monto",
                            value = orderData.amount,
                            onValueChange = { orderData = orderData.copy(amount = it) },
                            placeholder = "S/ 0.00",
                            keyboardType = KeyboardType.Decimal
                        )
                    }
                }
            }

            // Fechas
            item {
                FormSection(title = "Fechas") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        FormTextField(
                            label = "Fecha de Creación",
                            value = orderData.creationDate,
                            onValueChange = { orderData = orderData.copy(creationDate = it) },
                            placeholder = "DD/MM/YYYY"
                        )
                        FormTextField(
                            label = "Fecha de Entrega Programada",
                            value = orderData.deliveryDate,
                            onValueChange = { orderData = orderData.copy(deliveryDate = it) },
                            placeholder = "DD/MM/YYYY"
                        )
                    }
                }
            }

            // Fotos del Paquete
            item {
                FormSection(title = "Fotos del Paquete") {
                    PhotoUploadBox()
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun FormSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun FormTextField(
    label: String = "",
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    Column(modifier = modifier) {
        if (showLabel && label.isNotEmpty()) {
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF197FE6),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun PhotoUploadBox() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.AddAPhoto,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Añadir Fotos o arrastrar y soltar",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BottomActionBar(
    onCancelClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    "Cancelar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = onSaveClick,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF197FE6)
                )
            ) {
                Text(
                    "Guardar Pedido",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Previews
@Preview(name = "Create Order Screen - Light", showBackground = true)
@Composable
fun CreateOrderScreenPreview() {
    MaterialTheme {
        CreateOrderScreen()
    }
}

@Preview(
    name = "Create Order Screen - Dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun CreateOrderScreenDarkPreview() {
    MaterialTheme {
        CreateOrderScreen()
    }
}

@Preview(name = "Form Section Preview", showBackground = true)
@Composable
fun FormSectionPreview() {
    MaterialTheme {
        FormSection(title = "Información del Remitente") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FormTextField(
                    label = "Cliente / Proveedor",
                    value = "",
                    onValueChange = {},
                    placeholder = "Ej: Inversiones Tech S.A.C."
                )
            }
        }
    }
}