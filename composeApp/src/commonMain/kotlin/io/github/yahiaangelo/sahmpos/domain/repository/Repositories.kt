package io.github.yahiaangelo.sahmpos.domain.repository

import io.github.yahiaangelo.sahmpos.domain.model.Order
import io.github.yahiaangelo.sahmpos.domain.model.Payment
import io.github.yahiaangelo.sahmpos.domain.model.Product
import io.github.yahiaangelo.sahmpos.domain.model.SyncState
import io.github.yahiaangelo.sahmpos.domain.model.SyncStatusSummary
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun observeAll(): Flow<List<Product>>
    suspend fun findByBarcode(barcode: String): Product?
    suspend fun findById(id: String): Product?
    suspend fun seedIfEmpty()
}

interface OrderRepository {
    fun observeAll(): Flow<List<Order>>
    suspend fun getById(id: String): Order?
    suspend fun save(order: Order, payment: Payment)
    suspend fun updateSyncState(orderId: String, state: SyncState, version: Long)
    suspend fun ordersBySyncState(state: SyncState): List<Order>
}

interface SyncRepository {
    fun observeStatus(): Flow<SyncStatusSummary>
    suspend fun enqueueOrder(order: Order)
    suspend fun processOnce(): Int
    suspend fun retryAll()
    suspend fun clearCompleted()
}