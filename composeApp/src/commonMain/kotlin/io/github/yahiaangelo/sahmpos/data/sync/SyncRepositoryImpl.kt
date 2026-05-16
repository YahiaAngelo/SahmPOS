package io.github.yahiaangelo.sahmpos.data.sync

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.yahiaangelo.sahmpos.data.local.db.SahmDatabase
import io.github.yahiaangelo.sahmpos.data.remote.PushResult
import io.github.yahiaangelo.sahmpos.data.remote.SyncApi
import io.github.yahiaangelo.sahmpos.data.remote.toDto
import io.github.yahiaangelo.sahmpos.domain.model.Order
import io.github.yahiaangelo.sahmpos.domain.model.SyncState
import io.github.yahiaangelo.sahmpos.domain.model.SyncStatusSummary
import io.github.yahiaangelo.sahmpos.domain.repository.OrderRepository
import io.github.yahiaangelo.sahmpos.domain.repository.SyncRepository
import io.github.yahiaangelo.sahmpos.domain.util.Ids
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.github.yahiaangelo.sahmpos.data.remote.OrderDto

private const val MAX_ATTEMPTS = 6
private const val BASE_BACKOFF_MS = 1_000L
private const val MAX_BACKOFF_MS = 60_000L

@OptIn(ExperimentalTime::class)
class SyncRepositoryImpl(
    private val db: SahmDatabase,
    private val api: SyncApi,
    private val orders: OrderRepository,
    private val io: CoroutineDispatcher = Dispatchers.Default,
) : SyncRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun observeStatus(): Flow<SyncStatusSummary> =
        db.syncOutboxQueries.countByStatus().asFlow().mapToList(io).map { rows ->
            val byStatus = rows.associate { it.status to (it.cnt.toInt()) }
            SyncStatusSummary(
                pending = byStatus["PENDING"] ?: 0,
                inFlight = byStatus["IN_FLIGHT"] ?: 0,
                failed = byStatus["FAILED"] ?: 0,
                done = byStatus["DONE"] ?: 0,
            )
        }

    override suspend fun enqueueOrder(order: Order) {
        withContext(io) {
            val dto = order.toDto()
            val payload = json.encodeToString(OrderDto.serializer(), dto)
            val now = Clock.System.now().toEpochMilliseconds()
            db.syncOutboxQueries.enqueue(
                id = Ids.newId("sync"),
                entity_type = "ORDER",
                entity_id = order.id,
                operation = "UPSERT",
                payload = payload,
                created_at = now,
                attempts = 0L,
                last_error = null,
                last_attempt_at = null,
                next_attempt_at = now,
                status = "PENDING",
            )
        }
    }

    override suspend fun processOnce(): Int = withContext(io) {
        val now = Clock.System.now().toEpochMilliseconds()
        val due = db.syncOutboxQueries.selectDue(now = now, limit = 25L).executeAsList()
        var processed = 0
        for (item in due) {
            db.syncOutboxQueries.markInFlight(now = now, id = item.id)
            val dto = try {
                json.decodeFromString(OrderDto.serializer(), item.payload)
            } catch (e: Throwable) {
                db.syncOutboxQueries.markDead(
                    error = "decode failure: ${e.message}",
                    now = now,
                    id = item.id,
                )
                continue
            }
            when (val res = api.pushOrder(dto)) {
                is PushResult.Accepted -> {
                    db.syncOutboxQueries.markDone(now = now, id = item.id)
                    orders.updateSyncState(item.entity_id, SyncState.SYNCED, res.version)
                }
                is PushResult.Conflict -> {
                    // Server-wins: accept server version, mark synced, keep local data, bump version.
                    db.syncOutboxQueries.markDone(now = now, id = item.id)
                    orders.updateSyncState(item.entity_id, SyncState.SYNCED, res.serverVersion)
                }
                is PushResult.RetryableError -> {
                    val attempts = item.attempts.toInt() + 1
                    if (attempts >= MAX_ATTEMPTS) {
                        db.syncOutboxQueries.markDead(
                            error = "HTTP ${res.code}: ${res.message} after $attempts attempts",
                            now = now, id = item.id,
                        )
                        orders.updateSyncState(item.entity_id, SyncState.FAILED, dto.version)
                    } else {
                        val backoff = backoffMs(attempts)
                        db.syncOutboxQueries.markFailed(
                            error = "HTTP ${res.code}: ${res.message}",
                            now = now,
                            nextAttempt = now + backoff,
                            id = item.id,
                        )
                    }
                }
                is PushResult.FatalError -> {
                    db.syncOutboxQueries.markDead(
                        error = "HTTP ${res.code}: ${res.message}",
                        now = now, id = item.id,
                    )
                    orders.updateSyncState(item.entity_id, SyncState.FAILED, dto.version)
                }
            }
            processed++
        }
        processed
    }

    override suspend fun retryAll() = withContext(io) {
        val now = Clock.System.now().toEpochMilliseconds()
        // Reset all FAILED rows back to PENDING with next_attempt_at = now
        val all = db.syncOutboxQueries.selectAll().executeAsList()
        db.transaction {
            for (row in all) {
                if (row.status == "FAILED" || row.status == "PENDING") {
                    db.syncOutboxQueries.markFailed(
                        error = row.last_error ?: "retry requested",
                        now = now,
                        nextAttempt = now,
                        id = row.id,
                    )
                }
            }
        }
    }

    override suspend fun clearCompleted() {
        withContext(io) {
            db.syncOutboxQueries.deleteDone()
        }
    }

    private fun backoffMs(attempt: Int): Long {
        // Exponential with cap: base * 2^(attempt-1)
        val expo = BASE_BACKOFF_MS shl (attempt - 1).coerceAtMost(20)
        return expo.coerceAtMost(MAX_BACKOFF_MS)
    }
}