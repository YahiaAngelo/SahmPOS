package io.github.yahiaangelo.sahmpos.hardware

import io.github.yahiaangelo.sahmpos.domain.hardware.PaymentEvent
import io.github.yahiaangelo.sahmpos.domain.hardware.PaymentTerminal
import io.github.yahiaangelo.sahmpos.domain.model.Money
import io.github.yahiaangelo.sahmpos.domain.model.PaymentMethod
import io.github.yahiaangelo.sahmpos.domain.util.Ids
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

class PaymentTerminalImpl(
    private val approvalRate: Double = 0.85,
    private val timeoutRate: Double = 0.05,
    private val random: Random = Random.Default,
) : PaymentTerminal {

    override fun charge(amount: Money, method: PaymentMethod): Flow<PaymentEvent> = flow {
        emit(PaymentEvent.AwaitingCard)
        delay(800)

        if (method == PaymentMethod.CASH) {
            emit(PaymentEvent.Authorizing)
            delay(300)
            emit(PaymentEvent.Approved(transactionRef = Ids.newId("cash")))
            return@flow
        }

        emit(PaymentEvent.Reading)
        delay(1_200)
        emit(PaymentEvent.Authorizing)
        delay(1_000)

        val roll = random.nextDouble()
        when {
            roll < timeoutRate -> emit(PaymentEvent.Timeout)
            roll < timeoutRate + (1 - approvalRate - timeoutRate) -> emit(
                PaymentEvent.Declined("Insufficient funds")
            )
            else -> emit(PaymentEvent.Approved(transactionRef = Ids.newId("auth")))
        }
    }
}