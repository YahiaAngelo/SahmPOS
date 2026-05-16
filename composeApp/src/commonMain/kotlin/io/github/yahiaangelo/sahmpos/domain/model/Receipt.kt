package io.github.yahiaangelo.sahmpos.domain.model

import kotlinx.datetime.Instant

data class Receipt(
    val order: Order,
    val payment: Payment,
    val printedAt: Instant,
    val rendered: String,
)