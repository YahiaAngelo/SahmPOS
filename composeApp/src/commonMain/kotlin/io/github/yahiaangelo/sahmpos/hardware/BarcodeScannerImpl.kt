package io.github.yahiaangelo.sahmpos.hardware

import io.github.yahiaangelo.sahmpos.domain.hardware.BarcodeScanner
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class BarcodeScannerImpl : BarcodeScanner {
    private val _scans = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val scans: SharedFlow<String> = _scans.asSharedFlow()

    override fun emit(barcode: String) {
        _scans.tryEmit(barcode.trim())
    }
}