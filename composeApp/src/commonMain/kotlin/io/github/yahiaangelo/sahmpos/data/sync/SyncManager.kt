package io.github.yahiaangelo.sahmpos.data.sync

import io.github.yahiaangelo.sahmpos.domain.repository.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Periodically drains the sync outbox.
 * Run pattern: tick every [intervalMs], call [SyncRepository.processOnce] until it returns 0.
 */
class SyncManager(
    private val sync: SyncRepository,
    private val intervalMs: Long = 5_000L,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                try {
                    var n: Int
                    do {
                        n = sync.processOnce()
                    } while (isActive && n > 0)
                } catch (_: Throwable) {
                    // swallow; will retry next tick
                }
                delay(intervalMs)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}