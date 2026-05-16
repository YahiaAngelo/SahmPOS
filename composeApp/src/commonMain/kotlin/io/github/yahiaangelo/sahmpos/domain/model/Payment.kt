package io.github.yahiaangelo.sahmpos.domain.model

import kotlinx.datetime.Instant

enum class PaymentStatus { PENDING, APPROVED, DECLINED, TIMEOUT, ERROR }

data class Payment(
    val id: String,
    val orderId: String,
    val amount: Money,
    val method: PaymentMethod,
    val status: PaymentStatus,
    val transactionRef: String?,
    val processedAt: Instant,
)