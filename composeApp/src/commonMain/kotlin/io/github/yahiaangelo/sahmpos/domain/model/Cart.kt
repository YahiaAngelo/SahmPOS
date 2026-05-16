package io.github.yahiaangelo.sahmpos.domain.model

data class CartLine(
    val product: Product,
    val quantity: Int,
) {
    val lineTotal: Money get() = product.price * quantity
    val lineTax: Money get() = Money((lineTotal.cents * product.taxRate).toLong())
}

data class Cart(
    val lines: List<CartLine> = emptyList(),
    val discount: Money = Money.ZERO,
) {
    val subtotal: Money get() = lines.fold(Money.ZERO) { acc, l -> acc + l.lineTotal }
    val tax: Money get() = lines.fold(Money.ZERO) { acc, l -> acc + l.lineTax }
    val total: Money get() {
        val raw = subtotal + tax - discount
        return if (raw.cents < 0) Money.ZERO else raw
    }
    val itemCount: Int get() = lines.sumOf { it.quantity }
    val isEmpty: Boolean get() = lines.isEmpty()

    fun addProduct(product: Product, qty: Int = 1): Cart {
        val existing = lines.indexOfFirst { it.product.id == product.id }
        val updated = if (existing >= 0) {
            lines.toMutableList().also {
                val l = it[existing]
                it[existing] = l.copy(quantity = l.quantity + qty)
            }
        } else lines + CartLine(product, qty)
        return copy(lines = updated)
    }

    fun setQuantity(productId: String, qty: Int): Cart {
        val updated = lines.mapNotNull {
            if (it.product.id != productId) it
            else if (qty <= 0) null
            else it.copy(quantity = qty)
        }
        return copy(lines = updated)
    }

    fun removeProduct(productId: String): Cart =
        copy(lines = lines.filterNot { it.product.id == productId })

    fun applyDiscount(amount: Money): Cart = copy(discount = amount)

    fun clear(): Cart = Cart()
}