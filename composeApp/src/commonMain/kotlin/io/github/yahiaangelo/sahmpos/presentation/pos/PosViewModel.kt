package io.github.yahiaangelo.sahmpos.presentation.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yahiaangelo.sahmpos.domain.hardware.BarcodeScanner
import io.github.yahiaangelo.sahmpos.domain.hardware.PaymentEvent
import io.github.yahiaangelo.sahmpos.domain.model.Cart
import io.github.yahiaangelo.sahmpos.domain.model.Money
import io.github.yahiaangelo.sahmpos.domain.model.PaymentMethod
import io.github.yahiaangelo.sahmpos.domain.model.Product
import io.github.yahiaangelo.sahmpos.domain.model.Receipt
import io.github.yahiaangelo.sahmpos.domain.repository.ProductRepository
import io.github.yahiaangelo.sahmpos.domain.usecase.CheckoutOrderUseCase
import io.github.yahiaangelo.sahmpos.domain.usecase.CheckoutResult
import io.github.yahiaangelo.sahmpos.domain.usecase.ObserveCatalog
import io.github.yahiaangelo.sahmpos.domain.usecase.ScanBarcodeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PosUiState(
    val catalog: List<Product> = emptyList(),
    val cart: Cart = Cart(),
    val checkout: CheckoutPhase = CheckoutPhase.Idle,
    val lastReceipt: Receipt? = null,
    val toast: String? = null,
)

sealed interface CheckoutPhase {
    data object Idle : CheckoutPhase
    data class Processing(val event: PaymentEvent) : CheckoutPhase
    data class Done(val receipt: Receipt) : CheckoutPhase
    data class Failed(val reason: String) : CheckoutPhase
}

class PosViewModel(
    private val products: ProductRepository,
    private val scanner: BarcodeScanner,
    observeCatalog: ObserveCatalog,
    private val scanBarcode: ScanBarcodeUseCase,
    private val checkout: CheckoutOrderUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(PosUiState())
    val state: StateFlow<PosUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { products.seedIfEmpty() }

        observeCatalog().stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        ).also { flow ->
            viewModelScope.launch {
                flow.collect { catalog -> _state.update { it.copy(catalog = catalog) } }
            }
        }

        viewModelScope.launch {
            scanner.scans.collect { barcode ->
                onBarcode(barcode)
            }
        }
    }

    fun addProduct(product: Product) {
        _state.update { it.copy(cart = it.cart.addProduct(product)) }
    }

    fun setQuantity(productId: String, qty: Int) {
        _state.update { it.copy(cart = it.cart.setQuantity(productId, qty)) }
    }

    fun removeProduct(productId: String) {
        _state.update { it.copy(cart = it.cart.removeProduct(productId)) }
    }

    fun applyDiscount(amount: Money) {
        _state.update { it.copy(cart = it.cart.applyDiscount(amount)) }
    }

    fun clearCart() {
        _state.update { it.copy(cart = it.cart.clear(), checkout = CheckoutPhase.Idle) }
    }

    fun simulateScan(barcode: String) {
        scanner.emit(barcode)
    }

    private fun onBarcode(barcode: String) {
        viewModelScope.launch {
            val product = scanBarcode(barcode)
            if (product != null) {
                _state.update { it.copy(cart = it.cart.addProduct(product), toast = "Scanned: ${product.name}") }
            } else {
                _state.update { it.copy(toast = "Unknown barcode: $barcode") }
            }
        }
    }

    fun dismissToast() {
        _state.update { it.copy(toast = null) }
    }

    fun startCheckout(method: PaymentMethod) {
        if (state.value.cart.isEmpty) return
        viewModelScope.launch {
            _state.update { it.copy(checkout = CheckoutPhase.Processing(PaymentEvent.Idle)) }
            val result = checkout(
                cart = state.value.cart,
                method = method,
                paymentEvents = { ev ->
                    _state.update { it.copy(checkout = CheckoutPhase.Processing(ev)) }
                },
            )
            when (result) {
                is CheckoutResult.Success -> {
                    _state.update {
                        it.copy(
                            checkout = CheckoutPhase.Done(result.receipt),
                            cart = Cart(),
                            lastReceipt = result.receipt,
                        )
                    }
                }
                is CheckoutResult.PaymentFailed -> {
                    _state.update { it.copy(checkout = CheckoutPhase.Failed(result.reason)) }
                }
                CheckoutResult.EmptyCart -> {
                    _state.update { it.copy(checkout = CheckoutPhase.Idle) }
                }
            }
        }
    }

    fun dismissCheckout() {
        _state.update { it.copy(checkout = CheckoutPhase.Idle) }
    }
}