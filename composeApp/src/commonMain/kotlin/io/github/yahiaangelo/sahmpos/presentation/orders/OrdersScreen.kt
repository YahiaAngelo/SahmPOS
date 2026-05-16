package io.github.yahiaangelo.sahmpos.presentation.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.yahiaangelo.sahmpos.domain.model.Order
import io.github.yahiaangelo.sahmpos.domain.model.PaymentMethod
import io.github.yahiaangelo.sahmpos.domain.model.SyncState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OrdersScreen(vm: OrdersViewModel = koinViewModel()) {
    val orders by vm.orders.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Orders",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${orders.size} total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (orders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("No orders yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Check out a sale on the POS tab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(orders, key = { it.id }) { order -> OrderCard(order) }
            }
        }
    }
}

@Composable
private fun OrderCard(order: Order) {
    val ts = order.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(14.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PaymentBadge(order.paymentMethod)
                Spacer(Modifier.width(8.dp))
                Text(
                    order.id,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SyncBadge(order.syncState)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                ts.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            order.items.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(
                        "${item.name} × ${item.quantity}",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(item.lineTotal.format(), style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "Total",
                    Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    order.total.format(),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun PaymentBadge(method: PaymentMethod) {
    val icon = if (method == PaymentMethod.CASH) Icons.Filled.AttachMoney else Icons.Filled.CreditCard
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (method == PaymentMethod.CASH) "Cash" else "Card",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun SyncBadge(state: SyncState) {
    val (bg, fg, label) = when (state) {
        SyncState.PENDING -> Triple(Color(0xFFFFF3CD), Color(0xFF664D03), "Pending")
        SyncState.IN_FLIGHT -> Triple(Color(0xFFCFE2FF), Color(0xFF052C65), "Syncing")
        SyncState.SYNCED -> Triple(Color(0xFFD1E7DD), Color(0xFF0A3622), "Synced")
        SyncState.FAILED -> Triple(Color(0xFFF8D7DA), Color(0xFF58151C), "Failed")
    }
    Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
        Text(
            label,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}