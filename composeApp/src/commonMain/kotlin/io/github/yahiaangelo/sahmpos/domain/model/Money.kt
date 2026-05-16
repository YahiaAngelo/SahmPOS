package io.github.yahiaangelo.sahmpos.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class Money(val cents: Long) : Comparable<Money> {
    operator fun plus(other: Money): Money = Money(cents + other.cents)
    operator fun minus(other: Money): Money = Money(cents - other.cents)
    operator fun times(qty: Int): Money = Money(cents * qty)
    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    fun format(currency: String = "$"): String {
        val whole = cents / 100
        val frac = (kotlin.math.abs(cents) % 100).toString().padStart(2, '0')
        val sign = if (cents < 0) "-" else ""
        return "$sign$currency${kotlin.math.abs(whole)}.$frac"
    }

    companion object {
        val ZERO = Money(0)
        fun fromMajor(major: Double): Money = Money((major * 100).toLong())
    }
}