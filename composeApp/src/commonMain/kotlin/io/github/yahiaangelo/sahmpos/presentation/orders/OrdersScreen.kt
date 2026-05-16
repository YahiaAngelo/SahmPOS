package io.github.yahiaangelo.sahmpos.presentation.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.yahiaangelo.sahmpos.domain.model.Order
import io.github.yahiaangelo.sahmpos.domain.model.SyncState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OrdersScreen(vm: OrdersViewModel = koinViewModel()) {
    val orders by vm.orders.collectAsState()
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Orders (${orders.size})", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        if (orders.isEmpty()) {
            Text("No orders yet — check out a sale on the POS tab.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(orders, key = { it.id }) { order -> OrderCard(order) }
            }
        }
    }
}

@Composable
private fun OrderCard(order: Order) {
    val ts = order.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(12.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(order.id, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                SyncBadge(order.syncState)
            }
            Spacer(Modifier.height(4.dp))
            Text("$ts — ${order.paymentMethod.name}", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            order.items.forEach { item ->
                Row(Modifier.fillMaxWidth()) {
                    Text("${item.name} x${item.quantity}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Text(item.lineTotal.format(), style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth()) {
                Text("Total", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text(order.total.format(), fontWeight = FontWeight.SemiBold)
            }
            Text("v${order.version}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SyncBadge(state: SyncState) {
    val (bg, label) = when (state) {
        SyncState.PENDING -> Color(0xFFFFE082) to "Pending"
        SyncState.IN_FLIGHT -> Color(0xFFB3E5FC) to "Syncing"
        SyncState.SYNCED -> Color(0xFFC8E6C9) to "Synced"
        SyncState.FAILED -> Color(0xFFFFCDD2) to "Failed"
    }
    Surface(color = bg, shape = MaterialTheme.shapes.small) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
    }
}