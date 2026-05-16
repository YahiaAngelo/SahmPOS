package io.github.yahiaangelo.sahmpos.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.yahiaangelo.sahmpos.data.local.db.SahmDatabase
import io.github.yahiaangelo.sahmpos.data.local.db.Orders as OrderRow
import io.github.yahiaangelo.sahmpos.data.local.db.Order_item as OrderItemRow
import io.github.yahiaangelo.sahmpos.domain.model.Money
import io.github.yahiaangelo.sahmpos.domain.model.Order
import io.github.yahiaangelo.sahmpos.domain.model.OrderItem
import io.github.yahiaangelo.sahmpos.domain.model.OrderStatus
import io.github.yahiaangelo.sahmpos.domain.model.Payment
import io.github.yahiaangelo.sahmpos.domain.model.PaymentMethod
import io.github.yahiaangelo.sahmpos.domain.model.SyncState
import io.github.yahiaangelo.sahmpos.domain.repository.OrderRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

class OrderRepositoryImpl(
    private val db: SahmDatabase,
    private val io: CoroutineDispatcher = Dispatchers.Default,
) : OrderRepository {

    override fun observeAll(): Flow<List<Order>> =
        db.ordersQueries.selectAllOrders().asFlow().mapToList(io)
            .map { rows ->
                rows.map { row ->
                    val items = db.ordersQueries.selectItemsByOrder(row.id).executeAsList()
                    row.toDomain(items)
                }
            }

    override suspend fun getById(id: String): Order? = withContext(io) {
        val row = db.ordersQueries.selectOrder(id).executeAsOneOrNull() ?: return@withContext null
        val items = db.ordersQueries.selectItemsByOrder(id).executeAsList()
        row.toDomain(items)
    }

    override suspend fun save(order: Order, payment: Payment) = withContext(io) {
        db.transaction {
            db.ordersQueries.insertOrder(
                id = order.id,
                created_at = order.createdAt.toEpochMilliseconds(),
                subtotal_cents = order.subtotal.cents,
                tax_cents = order.tax.cents,
                discount_cents = order.discount.cents,
                total_cents = order.total.cents,
                payment_method = order.paymentMethod.name,
                status = order.status.name,
                sync_state = order.syncState.name,
                version = order.version,
            )
            order.items.forEach { item ->
                db.ordersQueries.insertOrderItem(
                    id = item.id,
                    order_id = order.id,
                    product_id = item.productId,
                    name = item.name,
                    unit_price_cents = item.unitPrice.cents,
                    quantity = item.quantity.toLong(),
                    line_total_cents = item.lineTotal.cents,
                )
            }
            db.paymentQueries.insert(
                id = payment.id,
                order_id = payment.orderId,
                amount_cents = payment.amount.cents,
                method = payment.method.name,
                status = payment.status.name,
                transaction_ref = payment.transactionRef,
                processed_at = payment.processedAt.toEpochMilliseconds(),
            )
        }
    }

    override suspend fun updateSyncState(orderId: String, state: SyncState, version: Long) {
        withContext(io) {
            db.ordersQueries.updateSyncState(state.name, version, orderId)
        }
    }

    override suspend fun ordersBySyncState(state: SyncState): List<Order> = withContext(io) {
        val rows = db.ordersQueries.selectOrdersBySyncState(state.name).executeAsList()
        rows.map { row ->
            val items = db.ordersQueries.selectItemsByOrder(row.id).executeAsList()
            row.toDomain(items)
        }
    }

    private fun OrderRow.toDomain(itemRows: List<OrderItemRow>): Order = Order(
        id = id,
        createdAt = Instant.fromEpochMilliseconds(created_at),
        items = itemRows.map { it.toDomain() },
        subtotal = Money(subtotal_cents),
        tax = Money(tax_cents),
        discount = Money(discount_cents),
        total = Money(total_cents),
        paymentMethod = PaymentMethod.valueOf(payment_method),
        status = OrderStatus.valueOf(status),
        syncState = SyncState.valueOf(sync_state),
        version = version,
    )

    private fun OrderItemRow.toDomain(): OrderItem = OrderItem(
        id = id,
        productId = product_id,
        name = name,
        unitPrice = Money(unit_price_cents),
        quantity = quantity.toInt(),
    )
}