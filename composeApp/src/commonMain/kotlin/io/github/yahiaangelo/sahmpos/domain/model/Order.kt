package io.github.yahiaangelo.sahmpos.domain.model

import kotlinx.datetime.Instant

enum class OrderStatus { COMPLETED, REFUNDED, VOIDED }
enum class SyncState { PENDING, IN_FLIGHT, SYNCED, FAILED }
enum class PaymentMethod { CASH, CARD }

data class Order(
    val id: String,
    val createdAt: Instant,
    val items: List<OrderItem>,
    val subtotal: Money,
    val tax: Money,
    val discount: Money,
    val total: Money,
    val paymentMethod: PaymentMethod,
    val status: OrderStatus,
    val syncState: SyncState,
    val version: Long,
)

data class OrderItem(
    val id: String,
    val productId: String,
    val name: String,
    val unitPrice: Money,
    val quantity: Int,
) {
    val lineTotal: Money get() = unitPrice * quantity
}