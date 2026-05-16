package io.github.yahiaangelo.sahmpos.domain.usecase

import io.github.yahiaangelo.sahmpos.domain.hardware.PaymentEvent
import io.github.yahiaangelo.sahmpos.domain.hardware.PaymentTerminal
import io.github.yahiaangelo.sahmpos.domain.hardware.ReceiptPrinter
import io.github.yahiaangelo.sahmpos.domain.hardware.ReceiptRenderer
import io.github.yahiaangelo.sahmpos.domain.model.Cart
import io.github.yahiaangelo.sahmpos.domain.model.Order
import io.github.yahiaangelo.sahmpos.domain.model.OrderItem
import io.github.yahiaangelo.sahmpos.domain.model.OrderStatus
import io.github.yahiaangelo.sahmpos.domain.model.Payment
import io.github.yahiaangelo.sahmpos.domain.model.PaymentMethod
import io.github.yahiaangelo.sahmpos.domain.model.PaymentStatus
import io.github.yahiaangelo.sahmpos.domain.model.Product
import io.github.yahiaangelo.sahmpos.domain.model.Receipt
import io.github.yahiaangelo.sahmpos.domain.model.SyncState
import io.github.yahiaangelo.sahmpos.domain.repository.OrderRepository
import io.github.yahiaangelo.sahmpos.domain.repository.ProductRepository
import io.github.yahiaangelo.sahmpos.domain.repository.SyncRepository
import io.github.yahiaangelo.sahmpos.domain.util.Ids
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ScanBarcodeUseCase(private val products: ProductRepository) {
    suspend operator fun invoke(barcode: String): Product? = products.findByBarcode(barcode)
}

class ObserveCatalog(private val products: ProductRepository) {
    operator fun invoke(): Flow<List<Product>> = products.observeAll()
}

class ObserveOrders(private val orders: OrderRepository) {
    operator fun invoke(): Flow<List<Order>> = orders.observeAll()
}

sealed interface CheckoutResult {
    data class Success(val receipt: Receipt) : CheckoutResult
    data class PaymentFailed(val reason: String) : CheckoutResult
    data object EmptyCart : CheckoutResult
}

@OptIn(ExperimentalTime::class)
class CheckoutOrderUseCase(
    private val orders: OrderRepository,
    private val sync: SyncRepository,
    private val terminal: PaymentTerminal,
    private val printer: ReceiptPrinter,
    private val renderer: ReceiptRenderer,
) {
    suspend operator fun invoke(
        cart: Cart,
        method: PaymentMethod,
        paymentEvents: ((PaymentEvent) -> Unit)? = null,
    ): CheckoutResult {
        if (cart.isEmpty) return CheckoutResult.EmptyCart

        val terminalFlow = terminal.charge(cart.total, method)
            .onEach { paymentEvents?.invoke(it) }

        val terminalResult = terminalFlow.first { event ->
            event is PaymentEvent.Approved ||
                event is PaymentEvent.Declined ||
                event is PaymentEvent.Timeout
        }
        val transactionRef = (terminalResult as? PaymentEvent.Approved)?.transactionRef
        val failureReason = when (terminalResult) {
            is PaymentEvent.Declined -> terminalResult.reason
            is PaymentEvent.Timeout -> "Timed out"
            else -> null
        }

        val now = Clock.System.now()
        val orderId = Ids.newId("ord")
        val payment = Payment(
            id = Ids.newId("pay"),
            orderId = orderId,
            amount = cart.total,
            method = method,
            status = if (transactionRef != null) PaymentStatus.APPROVED
            else if (failureReason == "Timed out") PaymentStatus.TIMEOUT
            else PaymentStatus.DECLINED,
            transactionRef = transactionRef,
            processedAt = now,
        )

        if (payment.status != PaymentStatus.APPROVED) {
            return CheckoutResult.PaymentFailed(failureReason ?: "Declined")
        }

        val order = Order(
            id = orderId,
            createdAt = now,
            items = cart.lines.map { line ->
                OrderItem(
                    id = Ids.newId("oi"),
                    productId = line.product.id,
                    name = line.product.name,
                    unitPrice = line.product.price,
                    quantity = line.quantity,
                )
            },
            subtotal = cart.subtotal,
            tax = cart.tax,
            discount = cart.discount,
            total = cart.total,
            paymentMethod = method,
            status = OrderStatus.COMPLETED,
            syncState = SyncState.PENDING,
            version = 1,
        )

        orders.save(order, payment)
        sync.enqueueOrder(order)

        val rendered = renderer.render(order, payment)
        val receipt = Receipt(order, payment, now, rendered)
        printer.print(receipt)

        return CheckoutResult.Success(receipt)
    }
}