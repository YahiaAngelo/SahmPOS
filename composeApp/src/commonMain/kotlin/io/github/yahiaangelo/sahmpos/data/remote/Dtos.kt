package io.github.yahiaangelo.sahmpos.data.remote

import io.github.yahiaangelo.sahmpos.domain.model.Order
import io.github.yahiaangelo.sahmpos.domain.model.OrderItem
import kotlinx.serialization.Serializable

@Serializable
data class OrderDto(
    val id: String,
    val createdAt: Long,
    val items: List<OrderItemDto>,
    val subtotalCents: Long,
    val taxCents: Long,
    val discountCents: Long,
    val totalCents: Long,
    val paymentMethod: String,
    val status: String,
    val version: Long,
)

@Serializable
data class OrderItemDto(
    val id: String,
    val productId: String,
    val name: String,
    val unitPriceCents: Long,
    val quantity: Int,
)

@Serializable
data class OrderAck(
    val id: String,
    val acceptedVersion: Long,
)

@Serializable
data class ConflictResponse(
    val id: String,
    val serverVersion: Long,
    val message: String,
)

fun Order.toDto(): OrderDto = OrderDto(
    id = id,
    createdAt = createdAt.toEpochMilliseconds(),
    items = items.map { it.toDto() },
    subtotalCents = subtotal.cents,
    taxCents = tax.cents,
    discountCents = discount.cents,
    totalCents = total.cents,
    paymentMethod = paymentMethod.name,
    status = status.name,
    version = version,
)

fun OrderItem.toDto(): OrderItemDto = OrderItemDto(
    id = id,
    productId = productId,
    name = name,
    unitPriceCents = unitPrice.cents,
    quantity = quantity,
)
