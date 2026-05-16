package io.github.yahiaangelo.sahmpos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.yahiaangelo.sahmpos.presentation.orders.OrdersScreen
import io.github.yahiaangelo.sahmpos.presentation.pos.PosScreen
import io.github.yahiaangelo.sahmpos.presentation.sync.SyncScreen

private enum class Tab(val label: String, val icon: String) {
    Pos("POS", "🛒"),
    Orders("Orders", "🧾"),
    Sync("Sync", "🔄"),
}

@Composable
@Preview
fun App() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        var selected by remember { mutableStateOf(Tab.Pos) }
        Scaffold(
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selected == tab,
                            onClick = { selected = tab },
                            icon = { Text(tab.icon) },
                            label = { Text(tab.label) },
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