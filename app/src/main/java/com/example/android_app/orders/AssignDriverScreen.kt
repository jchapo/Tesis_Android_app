package com.example.android_app.orders

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore

// Modelo de datos
data class Motorizado(
    val uid: String,
    val nombre: String,
    val apellido: String,
    val email: String,
    val telefono: String,
    val vehiculo: Vehiculo?,
    val licencia: String?,
    val estado: String,
    val photoUrl: String? = null,
    val ruta: String? = null,
    val detalleRuta: String? = null,
    val ubicacionActual: String? = null
)

data class Vehiculo(
    val placa: String,
    val modelo: String,
    val color: String
)

enum class TipoAsignacion {
    RECOJO,
    ENTREGA
}


// Pantalla principal
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignDriverScreen(
    pedidoId: String,
    tipoAsignacion: TipoAsignacion = TipoAsignacion.RECOJO,
    motorizados: List<Motorizado> = emptyList(),
    selectedMotorizadoId: String? = null,
    onMotorizadoSelected: (String) -> Unit = {},
    onAssignClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    isLoading: Boolean = false
) {
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    val filteredMotorizados = remember(motorizados, searchQuery, selectedFilter) {
        val filterValue = selectedFilter
        motorizados.filter { motorizado ->
            val matchesSearch = searchQuery.isEmpty() ||
                    motorizado.nombre.contains(searchQuery, ignoreCase = true) ||
                    motorizado.apellido.contains(searchQuery, ignoreCase = true) ||
                    motorizado.vehiculo?.placa?.contains(searchQuery, ignoreCase = true) == true ||
                    motorizado.ruta?.contains(searchQuery, ignoreCase = true) == true ||
                    motorizado.detalleRuta?.contains(searchQuery, ignoreCase = true) == true

            val matchesFilter = filterValue == null ||
                    motorizado.ruta?.contains(filterValue, ignoreCase = true) == true ||
                    motorizado.detalleRuta?.contains(filterValue, ignoreCase = true) == true

            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            AssignDriverTopBar(pedidoId, tipoAsignacion, onBackClick)
        },
        bottomBar = {
            AssignDriverBottomBar(
                enabled = selectedMotorizadoId != null && !isLoading,
                isLoading = isLoading,
                onAssignClick = onAssignClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            SearchAndFilterBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onFilterClick = { showFilterDialog = true },
                activeFilterCount = if (selectedFilter != null) 1 else 0
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "Motorizados Disponibles (${filteredMotorizados.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(12.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF197FE6))
                }
            } else if (filteredMotorizados.isEmpty()) {
                EmptyState("No se encontraron motorizados")
            } else {
                // Usar key para forzar recomposición
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = filteredMotorizados,
                        key = { it.uid } // Importante: usar key única
                    ) { motorizado ->
                        val isSelected = motorizado.uid == selectedMotorizadoId

                        MotorizadoCard(
                            motorizado = motorizado,
                            isSelected = isSelected,
                            onClick = {
                                onMotorizadoSelected(motorizado.uid)
                            }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showFilterDialog) {
        val rutasDisponibles = motorizados.mapNotNull { it.ruta }.distinct().sorted()
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filtrar por ruta", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterOption("Todos", isSelected = selectedFilter == null) {
                        selectedFilter = null
                        showFilterDialog = false
                    }
                    rutasDisponibles.forEach { ruta ->
                        FilterOption(
                            text = ruta,
                            isSelected = selectedFilter == ruta,
                            onClick = {
                                selectedFilter = ruta
                                showFilterDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("Cerrar", color = Color(0xFF197FE6))
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignDriverTopBar(
    pedidoId: String,
    tipoAsignacion: TipoAsignacion,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Asignar Motorizado",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            actions = {
                Spacer(modifier = Modifier.width(48.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // Subtitle
        Text(
            text = "Pedido #$pedidoId - ${if (tipoAsignacion == TipoAsignacion.RECOJO) "Recojo" else "Entrega"}",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        HorizontalDivider(thickness = 1.dp, color = Color.Gray.copy(alpha = 0.1f))
    }
}

@Composable
fun SearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    activeFilterCount: Int = 0
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Buscar motorizado") },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Limpiar búsqueda",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF197FE6),
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.2f)
            )
        )

        // Filter Button
        Box {
            IconButton(
                onClick = onFilterClick,
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Filtrar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Cambio 1: Agregar logs para debugging
@Composable
fun MotorizadoCard(
    motorizado: Motorizado,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    val backgroundColor = if (isSelected) {
        Color(0xFF197FE6).copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderColor = if (isSelected) {
        Color(0xFF197FE6)
    } else {
        Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 0.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            if (motorizado.photoUrl != null) {
                AsyncImage(
                    model = motorizado.photoUrl,
                    contentDescription = "Foto de ${motorizado.nombre}",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF197FE6).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${motorizado.nombre.firstOrNull() ?: ""}${motorizado.apellido.firstOrNull() ?: ""}",
                        color = Color(0xFF197FE6),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${motorizado.nombre} ${motorizado.apellido}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!motorizado.ruta.isNullOrBlank() || !motorizado.detalleRuta.isNullOrBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Route,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = listOfNotNull(motorizado.ruta, motorizado.detalleRuta).joinToString(" - "),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }


                // Vehículo info
                motorizado.vehiculo?.let { vehiculo ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.TwoWheeler,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${vehiculo.placa} - ${vehiculo.modelo}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Selection Indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color(0xFF197FE6) else Color.Gray.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Seleccionado",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AssignDriverBottomBar(
    enabled: Boolean,
    isLoading: Boolean,
    onAssignClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Button(
            onClick = onAssignClick,
            enabled = enabled && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF197FE6),
                contentColor = Color.White,
                disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
                disabledContentColor = Color.White.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    "Asignar Motorizado",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable
fun FilterOption(
    text: String,
    color: Color? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) Color(0xFF197FE6).copy(alpha = 0.1f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF197FE6)
            )
        )
        if (color != null) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray.copy(alpha = 0.5f)
            )
            Text(
                text = message,
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun rememberMotorizados(): List<Motorizado> {
    var motorizados by remember { mutableStateOf<List<Motorizado>>(emptyList()) }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .whereEqualTo("rol", "motorizado")
            .get()
            .addOnSuccessListener { result ->
                motorizados = result.documents.mapNotNull { doc ->
                    val uid = doc.id
                    val nombre = doc.getString("nombre") ?: return@mapNotNull null
                    val apellido = doc.getString("apellido") ?: ""
                    val email = doc.getString("email") ?: ""
                    val telefono = doc.getString("telefono") ?: ""
                    val licencia = doc.getString("licencia")
                    val estado = doc.getString("estado") ?: "active"
                    val photoUrl = doc.getString("photoUrl")
                    val ubicacionActual = doc.getString("ubicacionActual")
                    val ruta = doc.getString("ruta")
                    val detalleRuta = doc.getString("detalleRuta") ?: doc.getString("detallesRuta")

                    val vehiculoMap = doc.get("vehiculo") as? Map<*, *>
                    val vehiculo = vehiculoMap?.let {
                        val placa = it["placa"] as? String ?: ""
                        val modelo = it["modelo"] as? String ?: ""
                        val color = it["color"] as? String ?: ""
                        if (placa.isNotEmpty()) Vehiculo(placa, modelo, color) else null
                    }

                    Motorizado(
                        uid, nombre, apellido, email, telefono,
                        vehiculo, licencia, estado, photoUrl,
                        ruta, detalleRuta, ubicacionActual
                    )
                }
            }
    }

    return motorizados
}

// Sample Data
fun getSampleMotorizados(): List<Motorizado> {
    return listOf(
        Motorizado(
            uid = "1",
            nombre = "Carlos",
            apellido = "Vargas",
            email = "carlos@example.com",
            telefono = "919009366",
            vehiculo = Vehiculo("ABC-123", "Honda Wave", "Rojo"),
            licencia = "L12345",
            estado = "active",
            detalleRuta = "Carabayllo",
            ubicacionActual = "Miraflores"
        ),
        Motorizado(
            uid = "2",
            nombre = "Luis",
            apellido = "Rodriguez",
            email = "luis@example.com",
            telefono = "919009367",
            vehiculo = Vehiculo("DEF-456", "Yamaha FZ", "Azul"),
            licencia = "L12346",
            estado = "active",
            ruta = "Este",
            detalleRuta = "Carabayllo",
            ubicacionActual = "Miraflores"
        ),
        Motorizado(
            uid = "3",
            nombre = "Mario",
            apellido = "Jimenez",
            email = "mario@example.com",
            telefono = "919009368",
            vehiculo = Vehiculo("GHI-789", "Suzuki GN", "Negro"),
            licencia = "L12347",
            estado = "active",
            ruta = "Norte",
            detalleRuta = "Los Olivos"
        )
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewAssignDriverScreen() {
    AssignDriverScreen(
        pedidoId = "12345",
        tipoAsignacion = TipoAsignacion.RECOJO,
        motorizados = getSampleMotorizados(),
        selectedMotorizadoId = "1",
        onMotorizadoSelected = {},
        onAssignClick = {},
        onBackClick = {},
        isLoading = false
    )
}