package io.github.yahiaangelo.sahmpos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.yahiaangelo.sahmpos.presentation.orders.OrdersScreen
import io.github.yahiaangelo.sahmpos.presentation.pos.PosScreen
import io.github.yahiaangelo.sahmpos.presentation.sync.SyncScreen
import io.github.yahiaangelo.sahmpos.presentation.theme.SahmTheme

private enum class Tab(val label: String, val icon: ImageVector) {
    Pos("POS", Icons.Filled.ShoppingCart),
    Orders("Orders", Icons.AutoMirrored.Filled.ReceiptLong),
    Sync("Sync", Icons.Filled.Sync),
}

@Composable
@Preview
fun App() {
    SahmTheme {
        var selected by remember { mutableStateOf(Tab.Pos) }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                ) {
                    Tab.entries.forEach { tab ->
                        val isSelected = selected == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selected = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        ) { padding ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(Modifier.fillMaxSize().padding(padding)) {
                    when (selected) {
                        Tab.Pos -> PosScreen()
                        Tab.Orders -> OrdersScreen()
                        Tab.Sync -> SyncScreen()
                    }
                }
            }
        }
    }
}