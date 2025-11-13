package com.example.android_app.Orders

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class `OrderRepository.kt` {

    private val firestore = FirebaseFirestore.getInstance()
    private val ordersCollection = firestore.collection("pedidos")

    /**
     * Transforma documento de Firestore a Order UI
     */
    private fun transformFirestoreDoc(document: com.google.firebase.firestore.DocumentSnapshot): Order? {
        if (!document.exists()) return null

        try {
            val data = document.toObject(OrderFirestore::class.java) ?: return null
            val id = document.id

            // Determinar estado
            val status = when {
                data.fechas?.entrega != null -> OrderStatus.DELIVERED
                data.fechas?.anulacion != null -> OrderStatus.CANCELED
                data.asignacion?.recojo?.estado == "completada" ||
                        data.asignacion?.entrega?.estado == "en_camino" -> OrderStatus.IN_ROUTE
                else -> OrderStatus.PENDING
            }

            // Formatear fecha
            val formatDate: (Timestamp?) -> String = { timestamp ->
                timestamp?.toDate()?.let {
                    SimpleDateFormat("dd MMM, hh:mm a", Locale("es", "PE")).format(it)
                } ?: "-"
            }

            // Construir ruta
            val route = "${data.proveedor?.direccion?.distrito ?: "-"} → ${data.destinatario?.direccion?.distrito ?: "-"}"

            // Información de entrega
            val deliveryInfo = when (status) {
                OrderStatus.DELIVERED -> "Entregado: ${formatDate(data.fechas?.entrega)}"
                OrderStatus.CANCELED -> data.fechas?.anulacion?.let { "Cancelado: ${formatDate(it)}" } ?: "Cancelado"
                else -> "Entrega: ${formatDate(data.fechas?.entregaProgramada)}"
            }

            // Información del motorizado
            val driverInfo = when {
                status == OrderStatus.CANCELED -> null
                data.asignacion?.entrega?.motorizadoNombre?.isNotEmpty() == true ->
                    "Motorizado: ${data.asignacion.entrega.motorizadoNombre}"
                data.asignacion?.recojo?.motorizadoNombre?.isNotEmpty() == true ->
                    "Motorizado: ${data.asignacion.recojo.motorizadoNombre}"
                status == OrderStatus.PENDING -> "Sin motorizado asignado"
                else -> null
            }

            return Order(
                id = data.id.ifEmpty { id },
                status = status,
                client = data.proveedor?.nombre ?: "-",
                recipient = data.destinatario?.nombre ?: "-",
                route = route,
                deliveryInfo = deliveryInfo,
                driverInfo = driverInfo,
                rawData = data
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Obtiene todos los pedidos
     */
    suspend fun getAllOrders(): Result<List<Order>> = try {
        val querySnapshot = ordersCollection
            .orderBy("fechas.creacion", Query.Direction.DESCENDING)
            .get()
            .await()

        val orders = querySnapshot.documents.mapNotNull { doc ->
            transformFirestoreDoc(doc)
        }

        Result.success(orders)
    } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
    }

    /**
     * Obtiene un pedido por ID
     */
    suspend fun getOrderById(orderId: String): Result<Order> = try {
        val document = ordersCollection.document(orderId).get().await()
        val order = transformFirestoreDoc(document)

        if (order != null) {
            Result.success(order)
        } else {
            Result.failure(Exception("Pedido no encontrado"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Busca pedidos por término
     */
    fun searchOrders(orders: List<Order>, searchTerm: String): List<Order> {
        if (searchTerm.isBlank()) return orders

        val term = searchTerm.lowercase()
        return orders.filter { order ->
            order.id.lowercase().contains(term) ||
                    order.client.lowercase().contains(term) ||
                    order.recipient.lowercase().contains(term) ||
                    order.route.lowercase().contains(term)
        }
    }

    /**
     * Filtra pedidos por estado
     */
    fun filterByStatus(orders: List<Order>, status: OrderStatus?): List<Order> {
        return if (status == null) orders else orders.filter { it.status == status }
    }
}