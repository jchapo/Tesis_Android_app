package com.example.android_app.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ordermanagement.DetailCard
import com.google.firebase.firestore.FirebaseFirestore

data class Cliente(
    val uid: String,
    val empresa: String
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrderScreen(
    onBackClick: () -> Unit = {},
    onSaveClick: (Order) -> Unit = {}
) {
    var client by remember { mutableStateOf("") }
    var pickupDistrict by remember { mutableStateOf("") }
    var recipient by remember { mutableStateOf("") }
    var deliveryDistrict by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }
    var length by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var deliveryDate by remember { mutableStateOf("") }

    val volume = remember(height, width, length) {
        val h = height.toFloatOrNull() ?: 0f
        val w = width.toFloatOrNull() ?: 0f
        val l = length.toFloatOrNull() ?: 0f
        if (h > 0 && w > 0 && l > 0) "${(h * w * l).toInt()} cm³" else ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Nuevo Pedido",
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
                actions = { Spacer(modifier = Modifier.width(48.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            AddOrderBottomActions(
                onCancel = onBackClick,
                onSave = {
                    val newOrder = Order(
                        id = "#NEW-${(1000..9999).random()}",
                        status = OrderStatus.PENDING,
                        client = client,
                        recipient = recipient,
                        route = "$pickupDistrict → $deliveryDistrict",
                        deliveryInfo = "Entrega programada: $deliveryDate",
                        driverInfo = "Sin motorizado asignado"
                    )
                    onSaveClick(newOrder)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            val districtOptions = listOf(
                "Ate (Lima)",
                "Barranco (Lima)",
                "Bellavista (Callao)",
                "Breña (Lima)",
                "Callao (Callao)",
                "Carabayllo (Lima)",
                "Carmen de la Legua (Callao)",
                "Cercado de Lima (Lima)",
                "Chorrillos (Lima)",
                "Comas (Lima)",
                "El Agustino (Lima)",
                "Huachipa (Ate, Lima)",
                "Independencia (Lima)",
                "Jesús María (Lima)",
                "La Molina (Lima)",
                "La Perla (Callao)",
                "La Punta (Callao)",
                "La Victoria (Lima)",
                "Lince (Lima)",
                "Los Olivos (Lima)",
                "Lurín (Lima)",
                "Magdalena del Mar (Lima)",
                "Mi Perú (Callao)",
                "Miraflores (Lima)",
                "Oquendo (Callao)",
                "Pueblo Libre (Lima)",
                "Puente Piedra (Lima)",
                "Rímac (Lima)",
                "San Borja (Lima)",
                "San Isidro (Lima)",
                "San Juan de Lurigancho (Lima)",
                "San Juan de Miraflores (Lima)",
                "San Luis (Lima)",
                "San Martín de Porres (Lima)",
                "San Miguel (Lima)",
                "Santa Anita (Lima)",
                "Santa Clara (Ate, Lima)",
                "Santa Rosa (Callao)",
                "Surco (Lima)",
                "Surquillo (Lima)",
                "Ventanilla (Callao)",
                "Villa El Salvador (Lima)",
                "Villa María del Triunfo (Lima)"
            )
            val clientes = rememberClientes()
            var selectedCliente by remember { mutableStateOf("") }

            DetailCard(title = "Remitente") {
                SearchableDropdownField(
                    label = "Cliente / Proveedor",
                    options = clientes.map { it.empresa },
                    selectedOption = selectedCliente,
                    onOptionSelected = { selectedCliente = it }
                )
                DropdownField(
                    label = "Distrito de Recojo",
                    options = districtOptions,
                    selectedOption = pickupDistrict,
                    onOptionSelected = { pickupDistrict = it }
                )
            }

            DetailCard(title = "Destinatario") {
                LabeledTextField(label = "Nombre del Destinatario", value = recipient, onValueChange = { recipient = it })
                DropdownField(
                    label = "Distrito de Entrega",
                    options = districtOptions,
                    selectedOption = deliveryDistrict,
                    onOptionSelected = { deliveryDistrict = it }
                )
            }

            DetailCard(title = "Paquete") {
                Text("Dimensiones (cm)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledTextField(placeholder = "Alto", value = height, onValueChange = { height = it }, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number)
                    LabeledTextField(placeholder = "Ancho", value = width, onValueChange = { width = it }, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number)
                    LabeledTextField(placeholder = "Largo", value = length, onValueChange = { length = it }, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number)
                }
                if (volume.isNotEmpty()) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Volumen estimado: $volume") },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF197FE6).copy(alpha = 0.1f))
                    )
                }
                LabeledTextField(label = "Monto (S/)", value = amount, onValueChange = { amount = it }, keyboardType = KeyboardType.Number)
            }

            DetailCard(title = "Entrega") {
                LabeledTextField(label = "Fecha de Entrega", value = deliveryDate, onValueChange = { deliveryDate = it }, keyboardType = KeyboardType.Text)
            }

            DetailCard(title = "Fotos del Paquete") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Gray.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "Añadir foto", tint = Color.Gray)
                        Text("Añadir fotos aquí", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}


@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(1.dp, Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun LabeledTextField(
    label: String? = null,
    placeholder: String? = null,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = modifier) {
        label?.let {
            Text(it, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { placeholder?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true
        )
    }
}

@Composable
fun AddOrderBottomActions(
    onCancel: () -> Unit,
    onSave: () -> Unit
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
                onClick = onCancel,
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
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Cancelar", fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = onSave,
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
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Guardar", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF197FE6),
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.2f)
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableDropdownField(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(selectedOption) }

    val filteredOptions = options.filter {
        it.contains(query, ignoreCase = true)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                expanded = true
            },
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF197FE6),
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.2f)
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            filteredOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        query = option
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}


@Composable
fun rememberClientes(): List<Cliente> {
    var clientes by remember { mutableStateOf<List<Cliente>>(emptyList()) }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .whereEqualTo("rol", "cliente") // o "cliente" si usas ese rol
            .get()
            .addOnSuccessListener { result ->
                val lista = result.documents.mapNotNull { doc ->
                    val empresa = doc.getString("empresa") ?: return@mapNotNull null
                    val uid = doc.getString("uid") ?: doc.id
                    Cliente(uid, "$empresa")
                }
                clientes = lista
            }
    }

    return clientes
}













@Preview(showBackground = true)
@Composable
fun AddOrderScreenPreview() {
    MaterialTheme {
        AddOrderScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSectionCard() {
    MaterialTheme {
        SectionCard(title = "Sección de Prueba") {
            Text("Contenido de ejemplo")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLabeledTextFieldWithLabel() {
    MaterialTheme {
        LabeledTextField(
            label = "Nombre",
            value = "Juan Pérez",
            onValueChange = {},
            keyboardType = KeyboardType.Text
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLabeledTextFieldWithPlaceholder() {
    MaterialTheme {
        LabeledTextField(
            placeholder = "Ingrese nombre",
            value = "",
            onValueChange = {},
            keyboardType = KeyboardType.Text
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPackagePhotoBox() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddAPhoto, contentDescription = "Añadir foto", tint = Color.Gray)
                Text("Añadir fotos o arrastrar aquí", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}
