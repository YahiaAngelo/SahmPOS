package io.github.yahiaangelo.sahmpos.domain.model

data class Product(
    val id: String,
    val barcode: String,
    val name: String,
    val price: Money,
    val taxRate: Double,
    val stock: Int,
)