package com.example.android_app.orders

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Extensión del modelo Order con datos adicionales para la vista de detalles
 */
data class OrderDetails(
    val order: Order,
    // Detalles del paquete
    val dimensions: String = "-",
    val volume: String = "-",
    val weight: String = "-",
    val photos: List<String> = emptyList(),

    // Cronología
    val createdAt: String = "-",
    val scheduledDelivery: String = "-",
    val deliveredAt: String? = null,

    // Finanzas
    val serviceAmount: String = "S/ 0.00",
    val paymentStatus: String = "Pendiente",
    val paymentStatusColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Gray,

    // Datos de contacto
    val customerPhone: String? = null,
    val recipientPhone: String? = null,

    // Información adicional del motorizado
    val driverRating: Float? = null,
    val driverPhone: String? = null
)

/**
 * Transforma un documento de Firestore a OrderDetails (versión extendida)
 */
fun transformToOrderDetails(docId: String, data: Map<String, Any>?): OrderDetails? {
    if (data == null) return null

    try {
        // Primero crear el Order básico
        val order = transformBasicOrder(docId, data) ?: return null

        // Obtener dimensiones del paquete
        val paquete = data["paquete"] as? Map<*, *>
        val dimensiones = paquete?.get("dimensiones") as? Map<*, *>
        val largo = dimensiones?.get("largo") as? Number
        val ancho = dimensiones?.get("ancho") as? Number
        val alto = dimensiones?.get("alto") as? Number

        val dimensionsStr = if (largo != null && ancho != null && alto != null) {
            "${largo.toInt()} x ${ancho.toInt()} x ${alto.toInt()} cm"
        } else {
            "-"
        }

        // Calcular volumen
        val volumeStr = if (largo != null && ancho != null && alto != null) {
            val vol = largo.toInt() * ancho.toInt() * alto.toInt()
            "${String.format("%,d", vol)} cm³"
        } else {
            "-"
        }

        // Obtener peso
        val peso = paquete?.get("peso") as? Number
        val weightStr = if (peso != null) {
            "${peso.toFloat()} kg"
        } else {
            "-"
        }

        // Obtener fotos
        val fotosUrls = paquete?.get("fotosUrls") as? List<*>
        val photosList = fotosUrls?.mapNotNull { it as? String } ?: emptyList()

        // Obtener fechas
        val fechas = data["fechas"] as? Map<*, *>
        val createdTimestamp = fechas?.get("creacion")
        val scheduledTimestamp = fechas?.get("entregaProgramada")
        val deliveredTimestamp = fechas?.get("entrega")

        val createdAtStr = formatDateTime(createdTimestamp)
        val scheduledDeliveryStr = formatDateTime(scheduledTimestamp)
        val deliveredAtStr = if (deliveredTimestamp != null) formatDateTime(deliveredTimestamp) else null

        // Obtener información de pago
        val pago = data["pago"] as? Map<*, *>
        val montoTotal = pago?.get("montoTotal") as? Number
        val seCobra = pago?.get("seCobra") as? Boolean ?: false

        val serviceAmountStr = if (seCobra && montoTotal != null) {
            "S/ ${String.format("%.2f", montoTotal.toDouble())}"
        } else {
            "S/ 0.00"
        }

        // Estado de pago
        val pagado = pago?.get("pagado") as? Boolean ?: false
        val paymentStatusStr = if (pagado) "Pagado" else "Pendiente"
        val paymentColor = if (pagado) {
            androidx.compose.ui.graphics.Color(0xFF10B981)
        } else {
            androidx.compose.ui.graphics.Color(0xFFFB923C)
        }

        // Obtener teléfonos
        val proveedor = data["proveedor"] as? Map<*, *>
        val destinatario = data["destinatario"] as? Map<*, *>
        val customerPhone = proveedor?.get("telefono") as? String
        val recipientPhone = destinatario?.get("telefono") as? String

        // Información del motorizado
        val asignacion = data["asignacion"] as? Map<*, *>
        val entrega = asignacion?.get("entrega") as? Map<*, *>
        val recojo = asignacion?.get("recojo") as? Map<*, *>

        val motorizadoTelefono = (entrega?.get("motorizadoTelefono")
            ?: recojo?.get("motorizadoTelefono")) as? String

        // Rating simulado (puedes obtenerlo de tu base de datos si lo tienes)
        val driverRating = if (order.driverInfo != null &&
            order.driverInfo != "Sin motorizado asignado") {
            4.8f
        } else {
            null
        }

        return OrderDetails(
            order = order,
            dimensions = dimensionsStr,
            volume = volumeStr,
            weight = weightStr,
            photos = photosList,
            createdAt = createdAtStr,
            scheduledDelivery = scheduledDeliveryStr,
            deliveredAt = deliveredAtStr,
            serviceAmount = serviceAmountStr,
            paymentStatus = paymentStatusStr,
            paymentStatusColor = paymentColor,
            customerPhone = customerPhone,
            recipientPhone = recipientPhone,
            driverRating = driverRating,
            driverPhone = motorizadoTelefono
        )

    } catch (e: Exception) {
        println("❌ Error transformando a OrderDetails: ${e.message}")
        return null
    }
}

/**
 * Versión básica de transformación para el listado
 */
private fun transformBasicOrder(docId: String, data: Map<String, Any>): Order? {
    try {
        // Determinar el estado del pedido
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
        val destinatario = data["destinatario"] as? Map<*, *>
        val cliente = proveedor?.get("nombre") as? String ?: "Cliente Desconocido"
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
        println("❌ Error en transformBasicOrder: ${e.message}")
        return null
    }
}

/**
 * Formatea fecha y hora completa
 */
private fun formatDateTime(timestamp: Any?): String {
    if (timestamp == null) return "-"

    return try {
        val date = when (timestamp) {
            is Timestamp -> timestamp.toDate()
            is Date -> timestamp
            else -> return "-"
        }

        val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("es", "PE"))
        formatter.format(date)
    } catch (e: Exception) {
        "-"
    }
}

/**
 * Formatea solo fecha
 */
private fun formatDate(timestamp: Any?): String {
    if (timestamp == null) return "-"

    return try {
        val date = when (timestamp) {
            is Timestamp -> timestamp.toDate()
            is Date -> timestamp
            else -> return "-"
        }

        val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale("es", "PE"))
        formatter.format(date)
    } catch (e: Exception) {
        "-"
    }
}