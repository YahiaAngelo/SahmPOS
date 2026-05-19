package io.github.yahiaangelo.sahmpos.presentation.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.yahiaangelo.sahmpos.domain.hardware.PaymentEvent
import io.github.yahiaangelo.sahmpos.domain.model.PaymentMethod
import io.github.yahiaangelo.sahmpos.domain.model.Product
import io.github.yahiaangelo.sahmpos.domain.model.Receipt
import io.github.yahiaangelo.sahmpos.presentation.hardware.CameraQrScanner
import io.github.yahiaangelo.sahmpos.presentation.hardware.rememberReceiptPrintAction
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

private const val TABLET_BREAKPOINT_DP = 700

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(vm: PosViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    var showScanner by remember { mutableStateOf(false) }
    var showCartSheet by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val filtered = remember(state.catalog, query) {
        if (query.isBlank()) state.catalog
        else state.catalog.filter { p ->
            p.name.contains(query, ignoreCase = true) || p.barcode.contains(query)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isTablet = maxWidth.value >= TABLET_BREAKPOINT_DP

        if (isTablet) {
            TabletPosLayout(
                state = state,
                catalog = filtered,
                query = query,
                onQuery = { query = it },
                onScan = { showScanner = true },
                vm = vm,
            )
        } else {
            PhonePosLayout(
                state = state,
                catalog = filtered,
                query = query,
                onQuery = { query = it },
                onScan = { showScanner = true },
                onOpenCart = { showCartSheet = true },
                onAdd = vm::addProduct,
            )
        }

        if (!isTablet && showCartSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCartSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                CartSheetContent(
                    state = state,
                    onIncrement = { id ->
                        state.cart.lines.firstOrNull { it.product.id == id }
                            ?.let { vm.setQuantity(id, it.quantity + 1) }
                    },
                    onDecrement = { id ->
                        state.cart.lines.firstOrNull { it.product.id == id }?.let {
                            if (it.quantity <= 1) vm.removeProduct(id)
                            else vm.setQuantity(id, it.quantity - 1)
                        }
                    },
                    onRemove = vm::removeProduct,
                    onClear = vm::clearCart,
                    onCheckoutCash = {
                        vm.startCheckout(PaymentMethod.CASH)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showCartSheet = false }
                    },
                    onCheckoutCard = {
                        vm.startCheckout(PaymentMethod.CARD)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showCartSheet = false }
                    },
                )
            }
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
private fun PhonePosLayout(
    state: PosUiState,
    catalog: List<Product>,
    query: String,
    onQuery: (String) -> Unit,
    onScan: () -> Unit,
    onOpenCart: () -> Unit,
    onAdd: (Product) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            PosHeader(
                query = query,
                onQuery = onQuery,
                onScan = onScan,
                itemCount = catalog.size,
            )
            CatalogGrid(
                items = catalog,
                onAdd = onAdd,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 4.dp,
                    bottom = if (state.cart.isEmpty) 12.dp else 96.dp,
                ),
                columns = GridCells.Adaptive(minSize = 150.dp),
            )
        }
        if (!state.cart.isEmpty) {
            CartFab(
                itemCount = state.cart.itemCount,
                total = state.cart.total.format(),
                onClick = onOpenCart,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun TabletPosLayout(
    state: PosUiState,
    catalog: List<Product>,
    query: String,
    onQuery: (String) -> Unit,
    onScan: () -> Unit,
    vm: PosViewModel,
) {
    Column(Modifier.fillMaxSize()) {
        PosHeader(query = query, onQuery = onQuery, onScan = onScan, itemCount = catalog.size)
        Row(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp)) {
            CatalogGrid(
                items = catalog,
                onAdd = vm::addProduct,
                modifier = Modifier.weight(1.5f).fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 0.dp),
                columns = GridCells.Adaptive(minSize = 160.dp),
            )
            Spacer(Modifier.width(12.dp))
            Surface(
                modifier = Modifier.weight(1f).fillMaxSize().padding(vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                CartSheetContent(
                    state = state,
                    onIncrement = { id ->
                        state.cart.lines.firstOrNull { it.product.id == id }
                            ?.let { vm.setQuantity(id, it.quantity + 1) }
                    },
                    onDecrement = { id ->
                        state.cart.lines.firstOrNull { it.product.id == id }?.let {
                            if (it.quantity <= 1) vm.removeProduct(id)
                            else vm.setQuantity(id, it.quantity - 1)
                        }
                    },
                    onRemove = vm::removeProduct,
                    onClear = vm::clearCart,
                    onCheckoutCash = { vm.startCheckout(PaymentMethod.CASH) },
                    onCheckoutCard = { vm.startCheckout(PaymentMethod.CARD) },
                    embedded = true,
                )
            }
        }
    }
}

@Composable
private fun PosHeader(
    query: String,
    onQuery: (String) -> Unit,
    onScan: () -> Unit,
    itemCount: Int,
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Sahm POS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "$itemCount items available",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQuery,
                    placeholder = { Text("Search items or barcode") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { onQuery("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                FilledTonalButton(
                    onClick = onScan,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                ) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan")
                    Spacer(Modifier.width(6.dp))
                    Text("Scan")
                }
            }
        }
    }
}

@Composable
private fun CatalogGrid(
    items: List<Product>,
    onAdd: (Product) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    columns: GridCells,
) {
    if (items.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "No items match",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }
    LazyVerticalGrid(
        columns = columns,
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        gridItems(items, key = { it.id }) { product ->
            ProductCard(product = product, onClick = { onAdd(product) })
        }
    }
}

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    val outOfStock = product.stock <= 0
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 2.dp),
        enabled = !outOfStock,
    ) {
        Column(Modifier.padding(12.dp).fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                product.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                product.price.format(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StockChip(stock = product.stock)
                Spacer(Modifier.weight(1f))
                if (!outOfStock) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add to cart",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StockChip(stock: Int) {
    val (bg, fg, text) = when {
        stock <= 0 -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Out of stock",
        )
        stock < 5 -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "$stock left",
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "$stock in stock",
        )
    }
    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
        Text(
            text,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun CartFab(itemCount: Int, total: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
                Surface(
                    color = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (itemCount > 99) "99+" else "$itemCount",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "View cart",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    total,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun CartSheetContent(
    state: PosUiState,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onCheckoutCash: () -> Unit,
    onCheckoutCard: () -> Unit,
    embedded: Boolean = false,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
        if (embedded) {
            Spacer(Modifier.height(12.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.ShoppingCart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Your cart",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (state.cart.lines.isNotEmpty()) {
                AssistChip(
                    onClick = onClear,
                    label = { Text("Clear") },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize),
                        )
                    },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        if (state.cart.lines.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Cart is empty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Tap items or scan a barcode to add them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = if (embedded) 9999.dp else 360.dp).weight(1f, fill = embedded),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(state.cart.lines, key = { it.product.id }) { line ->
                    CartLineRow(
                        name = line.product.name,
                        unitPrice = line.product.price.format(),
                        lineTotal = line.lineTotal.format(),
                        quantity = line.quantity,
                        onIncrement = { onIncrement(line.product.id) },
                        onDecrement = { onDecrement(line.product.id) },
                        onRemove = { onRemove(line.product.id) },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        TotalsBlock(state)
        Spacer(Modifier.height(12.dp))
        CheckoutButtons(
            enabled = state.cart.lines.isNotEmpty(),
            onCash = onCheckoutCash,
            onCard = onCheckoutCard,
        )
    }
}

@Composable
private fun CartLineRow(
    name: String,
    unitPrice: String,
    lineTotal: String,
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "$unitPrice each",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    lineTotal,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                QtyButton(icon = Icons.Filled.Remove, contentDescription = "Decrease", onClick = onDecrement)
                Text(
                    "$quantity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
                QtyButton(icon = Icons.Filled.Add, contentDescription = "Increase", onClick = onIncrement)
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = onRemove,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove")
                }
            }
        }
    }
}

@Composable
private fun QtyButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.size(36.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun TotalsBlock(state: PosUiState) {
    Column(Modifier.fillMaxWidth()) {
        TotalRow("Items", state.cart.itemCount.toString())
        TotalRow("Subtotal", state.cart.subtotal.format())
        TotalRow("Tax", state.cart.tax.format())
        if (state.cart.discount.cents > 0) TotalRow("Discount", "-" + state.cart.discount.format())
        Spacer(Modifier.height(4.dp))
        TotalRow("Total", state.cart.total.format(), bold = true, large = true)
    }
}

@Composable
private fun TotalRow(label: String, value: String, bold: Boolean = false, large: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (bold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CheckoutButtons(enabled: Boolean, onCash: () -> Unit, onCard: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilledTonalButton(
            onClick = onCash,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).height(52.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            Icon(Icons.Filled.AttachMoney, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Cash", fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = onCard,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(Icons.Filled.CreditCard, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Card", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PaymentDialog(event: PaymentEvent) {
    AlertDialog(
        onDismissRequest = { /* not dismissible */ },
        confirmButton = {},
        icon = {
            Icon(
                Icons.Filled.CreditCard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text("Processing payment") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    when (event) {
                        PaymentEvent.Idle -> "Preparing…"
                        PaymentEvent.AwaitingCard -> "Please tap or insert card…"
                        PaymentEvent.Reading -> "Reading card…"
                        PaymentEvent.Authorizing -> "Authorizing…"
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
    val print = rememberReceiptPrintAction()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) { Text("Done") }
        },
        dismissButton = {
            TextButton(
                onClick = { print(receipt.rendered, "Receipt ${receipt.order.id}") },
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Filled.Print, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Print")
            }
        },
        icon = {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text("Receipt — ${receipt.order.total.format()}") },
        text = {
            Surface(
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    receipt.rendered,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                )
            }
        },
    )
}

@Composable
private fun ScanDialog(onDismiss: () -> Unit, onScan: (String) -> Unit) {
    var handled by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
        ) {
            CameraQrScanner(
                modifier = Modifier.fillMaxSize(),
                onScanned = { code ->
                    if (!handled) {
                        handled = true
                        onScan(code)
                    }
                },
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Icon(
                        Icons.Filled.QrCodeScanner,
                        contentDescription = null,
                        tint = Color.White,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Point the camera at a barcode or QR code",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}