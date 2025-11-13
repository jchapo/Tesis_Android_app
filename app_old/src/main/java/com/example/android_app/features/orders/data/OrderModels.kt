package com.example.android_app.features.orders.data

import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp

// Modelos Firebase
data class OrderFirestore(
    val id: String = "",
    val proveedor: Proveedor? = null,
    val destinatario: Destinatario? = null,
    val paquete: Paquete? = null,
    val pago: Pago? = null,
    val asignacion: Asignacion? = null,
    val fechas: Fechas? = null
)

data class Proveedor(
    val nombre: String = "",
    val direccion: Direccion? = null
)

data class Destinatario(
    val nombre: String = "",
    val direccion: Direccion? = null
)

data class Direccion(
    val distrito: String = "",
    val referencia: String = ""
)

data class Paquete(
    val dimensiones: Dimensiones? = null
)

data class Dimensiones(
    val largo: Double = 0.0,
    val ancho: Double = 0.0,
    val alto: Double = 0.0
)

data class Pago(
    val seCobra: Boolean = false,
    val monto: Long = 0,
    val montoTotal: Long = 0
)

data class Asignacion(
    val recojo: AsignacionDetalle? = null,
    val entrega: AsignacionDetalle? = null
)

data class AsignacionDetalle(
    val motorizadoNombre: String = "",
    val estado: String = ""
)

data class Fechas(
    val creacion: Timestamp? = null,
    val entregaProgramada: Timestamp? = null,
    val entrega: Timestamp? = null,
    val anulacion: Timestamp? = null
)

// Modelo UI (el que ya tienes)
data class Order(
    val id: String,
    val status: OrderStatus,
    val client: String,
    val recipient: String,
    val route: String,
    val deliveryInfo: String,
    val driverInfo: String? = null,
    val rawData: OrderFirestore? = null
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
