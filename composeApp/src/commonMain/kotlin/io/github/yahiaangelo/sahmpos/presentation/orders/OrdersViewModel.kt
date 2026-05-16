package io.github.yahiaangelo.sahmpos.presentation.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yahiaangelo.sahmpos.domain.model.Order
import io.github.yahiaangelo.sahmpos.domain.usecase.ObserveOrders
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class OrdersViewModel(
    observeOrders: ObserveOrders,
) : ViewModel() {
    val orders: StateFlow<List<Order>> = observeOrders().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )
}