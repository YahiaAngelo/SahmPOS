package io.github.yahiaangelo.sahmpos.presentation.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.yahiaangelo.sahmpos.domain.hardware.PaymentEvent
import io.github.yahiaangelo.sahmpos.domain.model.PaymentMethod
import io.github.yahiaangelo.sahmpos.domain.model.Product
import io.github.yahiaangelo.sahmpos.domain.model.Receipt
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(vm: PosViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    var showScanner by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Catalog",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = { showScanner = true }) { Text("Scan barcode") }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            CatalogGrid(
                items = state.catalog,
                onAdd = vm::addProduct,
                modifier = Modifier.weight(1.4f),
            )
            Spacer(Modifier.width(12.dp))
            CartPanel(
                state = state,
                onIncrement = { id -> state.cart.lines.firstOrNull { it.product.id == id }?.let { vm.setQuantity(id, it.quantity + 1) } },
                onDecrement = { id ->
                    state.cart.lines.firstOrNull { it.product.id == id }?.let {
                        if (it.quantity <= 1) vm.removeProduct(id) else vm.setQuantity(id, it.quantity - 1)
                    }
                },
                onRemove = vm::removeProduct,
                onClear = vm::clearCart,
                onCheckoutCash = { vm.startCheckout(PaymentMethod.CASH) },
                onCheckoutCard = { vm.startCheckout(PaymentMethod.CARD) },
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (showScanner) {
        ScanDialog(
            onDismiss = { showScanner = false },
            onScan = { code ->
                vm.simulateScan(code)
                showScanner = false
            },
        )
    }

    when (val phase = state.checkout) {
        is CheckoutPhase.Processing -> PaymentDialog(phase.event)
        is CheckoutPhase.Done -> ReceiptDialog(
            receipt = phase.receipt,
            onDismiss = vm::dismissCheckout,
        )
        is CheckoutPhase.Failed -> AlertDialog(
            onDismissRequest = vm::dismissCheckout,
            confirmButton = { TextButton(onClick = vm::dismissCheckout) { Text("OK") } },
            title = { Text("Payment failed") },
            text = { Text(phase.reason) },
        )
        CheckoutPhase.Idle -> Unit
    }

    state.toast?.let { msg ->
        AlertDialog(
            onDismissRequest = vm::dismissToast,
            confirmButton = { TextButton(onClick = vm::dismissToast) { Text("OK") } },
            text = { Text(msg) },
        )
    }
}

@Composable
private fun CatalogGrid(
    items: List<Product>,
    onAdd: (Product) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        gridItems(items, key = { it.id }) { product ->
            Card(
                onClick = { onAdd(product) },
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                    Spacer(Modifier.height(4.dp))
                    Text(product.price.format(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Stock: ${product.stock}", style = MaterialTheme.typography.labelSmall)
                    Text(product.barcode, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun CartPanel(
    state: PosUiState,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onCheckoutCash: () -> Unit,
    onCheckoutCard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(12.dp).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Cart", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (state.cart.lines.isNotEmpty()) {
                    TextButton(onClick = onClear) { Text("Clear") }
                }
            }
            Divider()
            Box(modifier = Modifier.weight(1f)) {
                if (state.cart.lines.isEmpty()) {
                    Text(
                        "Empty cart — tap items or scan a barcode",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    LazyColumn {
                        items(state.cart.lines, key = { it.product.id }) { line ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(line.product.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${line.product.price.format()} ea  →  ${line.lineTotal.format()}",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                                IconButton(onClick = { onDecrement(line.product.id) }) { Text("-") }
                                Text("${line.quantity}", modifier = Modifier.padding(horizontal = 6.dp))
                                IconButton(onClick = { onIncrement(line.product.id) }) { Text("+") }
                                TextButton(onClick = { onRemove(line.product.id) }) { Text("×") }
                            }
                            Divider()
                        }
                    }
                }
            }
            Divider()
            Spacer(Modifier.height(8.dp))
            TotalsBlock(state)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onCheckoutCash,
                    enabled = state.cart.lines.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) { Text("Cash") }
                Button(
                    onClick = onCheckoutCard,
                    enabled = state.cart.lines.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(),
                ) { Text("Card") }
            }
        }
    }
}

@Composable
private fun TotalsBlock(state: PosUiState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TotalRow("Items", state.cart.itemCount.toString())
        TotalRow("Subtotal", state.cart.subtotal.format())
        TotalRow("Tax", state.cart.tax.format())
        if (state.cart.discount.cents > 0) TotalRow("Discount", "-" + state.cart.discount.format())
        TotalRow("Total", state.cart.total.format(), bold = true)
    }
}

@Composable
private fun TotalRow(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun PaymentDialog(event: PaymentEvent) {
    AlertDialog(
        onDismissRequest = { /* not dismissible */ },
        confirmButton = {},
        title = { Text("Payment") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.width(28.dp).height(28.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    when (event) {
                        PaymentEvent.Idle -> "Preparing..."
                        PaymentEvent.AwaitingCard -> "Please tap or insert card..."
                        PaymentEvent.Reading -> "Reading card..."
                        PaymentEvent.Authorizing -> "Authorizing..."
                        is PaymentEvent.Approved -> "Approved (${event.transactionRef})"
                        is PaymentEvent.Declined -> "Declined: ${event.reason}"
                        PaymentEvent.Timeout -> "Timed out"
                    }
                )
            }
        },
    )
}

@Composable
private fun ReceiptDialog(receipt: Receipt, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Receipt — ${receipt.order.total.format()}") },
        text = {
            Surface(tonalElevation = 1.dp) {
                Text(
                    receipt.rendered,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp),
                )
            }
        },
    )
}

@Composable
private fun ScanDialog(onDismiss: () -> Unit, onScan: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simulate barcode scan") },
        text = {
            Column {
                Text("Enter or paste a barcode (try 1000000001 through 1000000008)")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { ch -> ch.isDigit() } },
                    singleLine = true,
                    label = { Text("Barcode") },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = { onScan(text) },
            ) { Text("Scan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}