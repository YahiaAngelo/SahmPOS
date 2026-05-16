package io.github.yahiaangelo.sahmpos.hardware

import io.github.yahiaangelo.sahmpos.domain.hardware.ReceiptPrinter
import io.github.yahiaangelo.sahmpos.domain.hardware.ReceiptRenderer
import io.github.yahiaangelo.sahmpos.domain.model.Order
import io.github.yahiaangelo.sahmpos.domain.model.Payment
import io.github.yahiaangelo.sahmpos.domain.model.Receipt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class ReceiptPrinterImpl : ReceiptPrinter {
    private val _history = MutableStateFlow<List<Receipt>>(emptyList())
    override val history: StateFlow<List<Receipt>> = _history.asStateFlow()

    override suspend fun print(receipt: Receipt): String {
        // Simulate thermal printer latency
        delay(400)
        _history.update { (listOf(receipt) + it).take(50) }
        return receipt.rendered
    }
}

@OptIn(ExperimentalTime::class)
class ReceiptRendererImpl(
    private val storeName: String = "SahmPOS Demo",
    private val storeFooter: String = "Thank you for your visit!",
) : ReceiptRenderer {
    private val width = 32

    override fun render(order: Order, payment: Payment): String {
        val ts = order.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
        val sb = StringBuilder()
        sb.appendCenter(storeName.uppercase())
        sb.appendCenter("==============================")
        sb.appendLine()
        sb.appendLine("Order: ${order.id}")
        sb.appendLine("Date:  $ts")
        sb.appendLine("Paid:  ${payment.method.name}  ref=${payment.transactionRef ?: "-"}")
        sb.appendLine("-".repeat(width))
        sb.appendLine(rightPad("Item", width - 12) + rightPad("Qty", 4) + leftPad("Amount", 8))
        sb.appendLine("-".repeat(width))
        order.items.forEach { item ->
            val name = item.name.take(width - 12).padEnd(width - 12)
            val qty = "x${item.quantity}".padEnd(4)
            val amt = leftPad(item.lineTotal.format(""), 8)
            sb.appendLine(name + qty + amt)
        }
        sb.appendLine("-".repeat(width))
        sb.appendLine(rightPad("Subtotal", width - 8) + leftPad(order.subtotal.format(""), 8))
        sb.appendLine(rightPad("Tax", width - 8) + leftPad(order.tax.format(""), 8))
        if (order.discount.cents > 0) {
            sb.appendLine(rightPad("Discount", width - 8) + leftPad("-${order.discount.format("")}", 8))
        }
        sb.appendLine(rightPad("TOTAL", width - 8) + leftPad(order.total.format(""), 8))
        sb.appendLine("=".repeat(width))
        sb.appendCenter(storeFooter)
        sb.appendCenter("printed ${Clock.System.now()}")
        return sb.toString()
    }

    private fun StringBuilder.appendCenter(text: String) {
        val pad = ((width - text.length).coerceAtLeast(0)) / 2
        appendLine(" ".repeat(pad) + text)
    }

    private fun rightPad(s: String, width: Int) = s.take(width).padEnd(width)
    private fun leftPad(s: String, width: Int) = s.take(width).padStart(width)
}