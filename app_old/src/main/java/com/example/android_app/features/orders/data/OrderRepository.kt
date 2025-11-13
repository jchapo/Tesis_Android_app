package com.example.android_app.features.orders.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class OrderRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val ordersCollection = firestore.collection("pedidos")

    private fun transformFirestoreDoc(document: com.google.firebase.firestore.DocumentSnapshot): Order? {
        if (!document.exists()) return null

        try {
            val data = document.toObject(OrderFirestore::class.java) ?: return null
            val id = document.id

            val status = when {
                data.fechas?.entrega != null -> OrderStatus.DELIVERED
                data.fechas?.anulacion != null -> OrderStatus.CANCELED
                data.asignacion?.recojo?.estado == "completada" ||
                data.asignacion?.entrega?.estado == "en_camino" -> OrderStatus.IN_ROUTE
                else -> OrderStatus.PENDING
            }

            val formatDate: (com.google.firebase.Timestamp?) -> String = { timestamp ->
                timestamp?.toDate()?.let {
                    SimpleDateFormat("dd MMM, hh:mm a", Locale("es", "PE")).format(it)
                } ?: "-"
            }

            val route = "${data.proveedor?.direccion?.distrito ?: "-"} → ${data.destinatario?.direccion?.distrito ?: "-"}"

            val deliveryInfo = when (status) {
                OrderStatus.DELIVERED -> "Entregado: ${formatDate(data.fechas?.entrega)}"
                OrderStatus.CANCELED -> "Cancelado"
                else -> "Entrega: ${formatDate(data.fechas?.entregaProgramada)}"
            }

            val driverInfo = when {
                status == OrderStatus.CANCELED -> null
                data.asignacion?.entrega?.motorizadoNombre?.isNotEmpty() == true ->
                    "Motorizado: ${data.asignacion.entrega.motorizadoNombre}"
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

    suspend fun getAllOrders(): Result<List<Order>> = try {
        val querySnapshot = ordersCollection
            .orderBy("fechas.creacion", Query.Direction.DESCENDING)
            .get()
            .await()

        val orders = querySnapshot.documents.mapNotNull { transformFirestoreDoc(it) }
        Result.success(orders)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun searchOrders(orders: List<Order>, searchTerm: String): List<Order> {
        if (searchTerm.isBlank()) return orders
        val term = searchTerm.lowercase()
        return orders.filter { order ->
            order.id.lowercase().contains(term) ||
            order.client.lowercase().contains(term) ||
            order.recipient.lowercase().contains(term)
        }
    }

    fun filterByStatus(orders: List<Order>, status: OrderStatus?): List<Order> {
        return if (status == null) orders else orders.filter { it.status == status }
    }
}
