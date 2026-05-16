package io.github.yahiaangelo.sahmpos.presentation.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SyncScreen(vm: SyncViewModel = koinViewModel()) {
    val status by vm.status.collectAsState()
    val log by vm.serverLog.collectAsState()

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
                        Icons.Filled.Sync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Sync",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Order replication status",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Column(Modifier.padding(16.dp).fillMaxSize()) {
            Surface(
                tonalElevation = 0.dp,
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StatusCell("Pending", status.pending, MaterialTheme.colorScheme.tertiaryContainer)
                    StatusCell("In-flight", status.inFlight, MaterialTheme.colorScheme.primaryContainer)
                    StatusCell("Done", status.done, MaterialTheme.colorScheme.secondaryContainer)
                    StatusCell("Failed", status.failed, MaterialTheme.colorScheme.errorContainer)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = vm::forceProcess,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Process")
                }
                OutlinedButton(
                    onClick = vm::retryAll,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Retry")
                }
                OutlinedButton(
                    onClick = vm::clearCompleted,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.CleaningServices, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Clear")
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Mock server events",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                tonalElevation = 0.dp,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (log.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No traffic yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(12.dp)) {
                        items(log) { line ->
                            Text(
                                line,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCell(label: String, value: Int, container: androidx.compose.ui.graphics.Color) {
    Surface(
        color = container,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}