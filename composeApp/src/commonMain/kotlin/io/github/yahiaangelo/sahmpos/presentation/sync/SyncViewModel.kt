package io.github.yahiaangelo.sahmpos.presentation.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yahiaangelo.sahmpos.data.remote.MockSyncServer
import io.github.yahiaangelo.sahmpos.data.sync.SyncManager
import io.github.yahiaangelo.sahmpos.domain.model.SyncStatusSummary
import io.github.yahiaangelo.sahmpos.domain.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SyncViewModel(
    private val sync: SyncRepository,
    private val manager: SyncManager,
    private val mockServer: MockSyncServer,
) : ViewModel() {

    val status: StateFlow<SyncStatusSummary> = sync.observeStatus().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SyncStatusSummary(0, 0, 0, 0),
    )

    private val _serverLog = MutableStateFlow<List<String>>(emptyList())
    val serverLog: StateFlow<List<String>> = _serverLog.asStateFlow()

    init {
        manager.start()
        // poll mock server's event log periodically for the UI
        viewModelScope.launch {
            while (true) {
                _serverLog.update { mockServer.events.reversed() }
                kotlinx.coroutines.delay(1_000)
            }
        }
    }

    fun forceProcess() {
        viewModelScope.launch { sync.processOnce() }
    }

    fun retryAll() {
        viewModelScope.launch {
            sync.retryAll()
            sync.processOnce()
        }
    }

    fun clearCompleted() {
        viewModelScope.launch { sync.clearCompleted() }
    }

    override fun onCleared() {
        super.onCleared()
        // keep manager running app-wide; we manage stop at app exit instead
    }
}