package io.github.yahiaangelo.sahmpos.domain.hardware

import io.github.yahiaangelo.sahmpos.domain.model.Money
import io.github.yahiaangelo.sahmpos.domain.model.Payment
import io.github.yahiaangelo.sahmpos.domain.model.PaymentMethod
import io.github.yahiaangelo.sahmpos.domain.model.Receipt
import kotlinx.coroutines.flow.Flow

interface BarcodeScanner {
    val scans: Flow<String>
    fun emit(barcode: String)
}

sealed interface PaymentEvent {
    data object Idle : PaymentEvent
    data object AwaitingCard : PaymentEvent
    data object Reading : PaymentEvent
    data object Authorizing : PaymentEvent
    data class Approved(val transactionRef: String) : PaymentEvent
    data class Declined(val reason: String) : PaymentEvent
    data object Timeout : PaymentEvent
}

interface PaymentTerminal {
    fun charge(amount: Money, method: PaymentMethod): Flow<PaymentEvent>
}

interface ReceiptPrinter {
    suspend fun print(receipt: Receipt): String
    val history: Flow<List<Receipt>>
}

interface ReceiptRenderer {
    fun render(order: io.github.yahiaangelo.sahmpos.domain.model.Order, payment: Payment): String
}