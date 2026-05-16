package io.github.yahiaangelo.sahmpos.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.yahiaangelo.sahmpos.data.local.db.Product as ProductRow
import io.github.yahiaangelo.sahmpos.data.local.db.SahmDatabase
import io.github.yahiaangelo.sahmpos.domain.model.Money
import io.github.yahiaangelo.sahmpos.domain.model.Product
import io.github.yahiaangelo.sahmpos.domain.repository.ProductRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProductRepositoryImpl(
    private val db: SahmDatabase,
    private val io: CoroutineDispatcher = Dispatchers.Default,
) : ProductRepository {

    override fun observeAll(): Flow<List<Product>> =
        db.productQueries.selectAll().asFlow().mapToList(io).map { rows -> rows.map { it.toDomain() } }

    override suspend fun findByBarcode(barcode: String): Product? = withContext(io) {
        db.productQueries.findByBarcode(barcode).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun findById(id: String): Product? = withContext(io) {
        db.productQueries.findById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun seedIfEmpty() = withContext(io) {
        val existing = db.productQueries.selectAll().executeAsList()
        if (existing.isNotEmpty()) return@withContext
        db.transaction {
            SEED_CATALOG.forEach { p ->
                db.productQueries.upsert(
                    id = p.id,
                    barcode = p.barcode,
                    name = p.name,
                    price_cents = p.price.cents,
                    tax_rate = p.taxRate,
                    stock = p.stock.toLong(),
                )
            }
        }
    }

    private fun ProductRow.toDomain() = Product(
        id = id,
        barcode = barcode,
        name = name,
        price = Money(price_cents),
        taxRate = tax_rate,
        stock = stock.toInt(),
    )

    companion object {
        val SEED_CATALOG = listOf(
            Product("p_espresso", "1000000001", "Espresso", Money(2500), 0.10, 99),
            Product("p_latte", "1000000002", "Latte", Money(4000), 0.10, 99),
            Product("p_cappuccino", "1000000003", "Cappuccino", Money(3800), 0.10, 99),
            Product("p_croissant", "1000000004", "Croissant", Money(2200), 0.05, 50),
            Product("p_muffin", "1000000005", "Blueberry Muffin", Money(2600), 0.05, 50),
            Product("p_sandwich", "1000000006", "Club Sandwich", Money(6500), 0.05, 25),
            Product("p_water", "1000000007", "Bottled Water", Money(1500), 0.0, 200),
            Product("p_juice", "1000000008", "Orange Juice", Money(3500), 0.0, 80),
        )
    }
}