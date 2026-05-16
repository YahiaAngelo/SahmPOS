package io.github.yahiaangelo.sahmpos.domain.model

data class SyncStatusSummary(
    val pending: Int,
    val inFlight: Int,
    val failed: Int,
    val done: Int,
) {
    val total: Int get() = pending + inFlight + failed + done
}