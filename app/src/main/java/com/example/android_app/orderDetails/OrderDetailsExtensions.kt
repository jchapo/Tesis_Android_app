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
    val pickedUpAt: String? = null,
    val deliveredAt: String? = null,

    // Finanzas
    val shouldCharge: Boolean = true, // Indica si se debe cobrar este pedido
    val baseAmount: String = "S/ 0.00",
    val commission: String = "S/ 0.00",
    val serviceAmount: String = "S/ 0.00",
    val paymentStatus: String = "Pendiente",
    val paymentStatusColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Gray,

    // Datos de contacto
    val customerPhone: String? = null,
    val recipientPhone: String? = null,

    // Información del motorizado de recojo
    val pickupDriverName: String? = null,
    val pickupDriverPhone: String? = null,
    val pickupDriverRating: Float? = null,

    // Información del motorizado de entrega
    val deliveryDriverName: String? = null,
    val deliveryDriverPhone: String? = null,
    val deliveryDriverRating: Float? = null,

    // Coordenadas
    val pickupLat: Double? = null,
    val pickupLng: Double? = null,
    val deliveryLat: Double? = null,
    val deliveryLng: Double? = null,

    // Estado de recojo
    val isPickedUp: Boolean = false
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
        val pickedUpTimestamp = fechas?.get("recojo")
        val deliveredTimestamp = fechas?.get("entrega")

        val createdAtStr = formatDateTime(createdTimestamp)
        val scheduledDeliveryStr = formatDateTime(scheduledTimestamp)
        val pickedUpAtStr = if (pickedUpTimestamp != null) formatDateTime(pickedUpTimestamp) else null
        val deliveredAtStr = if (deliveredTimestamp != null) formatDateTime(deliveredTimestamp) else null

        // Obtener información de pago
        val pago = data["pago"] as? Map<*, *>
        val monto = pago?.get("monto") as? Number
        val comision = pago?.get("comision") as? Number
        val montoTotal = pago?.get("montoTotal") as? Number
        val seCobra = pago?.get("seCobra") as? Boolean ?: false

        val baseAmountStr = if (monto != null) {
            "S/ ${String.format("%.2f", monto.toDouble())}"
        } else {
            "S/ 0.00"
        }

        val commissionStr = if (comision != null) {
            "S/ ${String.format("%.2f", comision.toDouble())}"
        } else {
            "S/ 0.00"
        }

        val serviceAmountStr = if (seCobra && montoTotal != null) {
            "S/ ${String.format("%.2f", montoTotal.toDouble())}"
        } else {
            "S/ 0.00"
        }

        // Estado de pago
        val pagado = pago?.get("pagado") as? Boolean ?: false
        val paymentStatusStr = if (!seCobra) {
            "No cobrar"
        } else if (pagado) {
            "Pagado"
        } else {
            "Pendiente"
        }

        val paymentColor = if (!seCobra) {
            androidx.compose.ui.graphics.Color(0xFF6B7280) // Gris para "No cobrar"
        } else if (pagado) {
            androidx.compose.ui.graphics.Color(0xFF10B981) // Verde para "Pagado"
        } else {
            androidx.compose.ui.graphics.Color(0xFFFB923C) // Naranja para "Pendiente"
        }

        // Obtener teléfonos
        val proveedor = data["proveedor"] as? Map<*, *>
        val destinatario = data["destinatario"] as? Map<*, *>
        val customerPhone = proveedor?.get("telefono") as? String
        val recipientPhone = destinatario?.get("telefono") as? String

        // Información de los motorizados (separados)
        val asignacion = data["asignacion"] as? Map<*, *>
        val recojo = asignacion?.get("recojo") as? Map<*, *>
        val entrega = asignacion?.get("entrega") as? Map<*, *>

        // Motorizado de recojo
        val pickupDriverName = recojo?.get("motorizadoNombre") as? String
        val pickupDriverPhone = recojo?.get("motorizadoTelefono") as? String
        val pickupDriverRating = if (pickupDriverName != null) 4.8f else null

        // Motorizado de entrega
        val deliveryDriverName = entrega?.get("motorizadoNombre") as? String
        val deliveryDriverPhone = entrega?.get("motorizadoTelefono") as? String
        val deliveryDriverRating = if (deliveryDriverName != null) 4.8f else null

        // Coordenadas del proveedor (punto de recojo)
        val proveedorDir = proveedor?.get("direccion") as? Map<*, *>
        val proveedorCoords = proveedorDir?.get("coordenadas") as? Map<*, *>
        val pickupLat = (proveedorCoords?.get("latitud") as? Number)?.toDouble()
        val pickupLng = (proveedorCoords?.get("longitud") as? Number)?.toDouble()

        // Coordenadas del destinatario (punto de entrega)
        val destinatarioDir = destinatario?.get("direccion") as? Map<*, *>
        val destinatarioCoords = destinatarioDir?.get("coordenadas") as? Map<*, *>
        val deliveryLat = (destinatarioCoords?.get("latitud") as? Number)?.toDouble()
        val deliveryLng = (destinatarioCoords?.get("longitud") as? Number)?.toDouble()

        // Verificar si el pedido ya fue recogido
        val isPickedUp = pickedUpTimestamp != null

        return OrderDetails(
            order = order,
            dimensions = dimensionsStr,
            volume = volumeStr,
            weight = weightStr,
            photos = photosList,
            createdAt = createdAtStr,
            scheduledDelivery = scheduledDeliveryStr,
            pickedUpAt = pickedUpAtStr,
            deliveredAt = deliveredAtStr,
            shouldCharge = seCobra,
            baseAmount = baseAmountStr,
            commission = commissionStr,
            serviceAmount = serviceAmountStr,
            paymentStatus = paymentStatusStr,
            paymentStatusColor = paymentColor,
            customerPhone = customerPhone,
            recipientPhone = recipientPhone,
            pickupDriverName = pickupDriverName,
            pickupDriverPhone = pickupDriverPhone,
            pickupDriverRating = pickupDriverRating,
            deliveryDriverName = deliveryDriverName,
            deliveryDriverPhone = deliveryDriverPhone,
            deliveryDriverRating = deliveryDriverRating,
            pickupLat = pickupLat,
            pickupLng = pickupLng,
            deliveryLat = deliveryLat,
            deliveryLng = deliveryLng,
            isPickedUp = isPickedUp
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
        val fechas = data["fechas"] as? Map<*, *>
        val asignacion = data["asignacion"] as? Map<*, *>

        // Obtener información de asignaciones
        val recojo = asignacion?.get("recojo") as? Map<*, *>
        val entrega = asignacion?.get("entrega") as? Map<*, *>

        val recojoEstado = recojo?.get("estado") as? String
        val entregaEstado = entrega?.get("estado") as? String
        val tieneMotorizadoEntrega = entrega?.get("motorizadoNombre") != null

        val status = when {
            // Si ya fue entregado
            fechas?.get("entrega") != null -> OrderStatus.DELIVERED

            // Si fue cancelado
            fechas?.get("anulacion") != null -> OrderStatus.CANCELED

            // Si está en proceso de entrega (motorizado en camino)
            entregaEstado == "en_camino" -> OrderStatus.IN_ROUTE

            // Si el recojo fue completado pero NO tiene motorizado de entrega → PENDING
            recojoEstado == "completada" && !tieneMotorizadoEntrega -> OrderStatus.PENDING

            // Si tiene motorizado de entrega asignado → IN_ROUTE
            tieneMotorizadoEntrega -> OrderStatus.IN_ROUTE

            // Si el recojo está en proceso (asignado o en camino) → IN_ROUTE
            recojoEstado == "en_camino" || recojoEstado == "asignada" -> OrderStatus.IN_ROUTE

            // Por defecto: PENDING (esperando asignación de recojo)
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

        // Verificar si el pedido ya fue recogido
        val pickedUpTimestamp = fechas?.get("recojo")
        val isPickedUp = pickedUpTimestamp != null

        // Verificar si tiene motorizado de entrega asignado
        val deliveryDriverName = entrega?.get("motorizadoNombre") as? String
        val hasDeliveryDriver = deliveryDriverName != null

        // Obtener nombre del motorizado (preferir entrega si existe, sino recojo)
        val pickupDriverName = recojo?.get("motorizadoNombre") as? String
        val motorizadoNombre = deliveryDriverName ?: pickupDriverName

        val driverInfo = when {
            status == OrderStatus.PENDING -> null
            status == OrderStatus.CANCELED -> null
            status == OrderStatus.IN_ROUTE && isPickedUp && !hasDeliveryDriver -> null
            motorizadoNombre != null -> "Motorizado: $motorizadoNombre"
            else -> null
        }

        return Order(
            id = (data["id"] as? String) ?: docId,
            status = status,
            client = cliente,
            recipient = recipiente,
            route = route,
            deliveryInfo = deliveryInfo,
            driverInfo = driverInfo,
            isPickedUp = isPickedUp,
            hasDeliveryDriver = hasDeliveryDriver
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