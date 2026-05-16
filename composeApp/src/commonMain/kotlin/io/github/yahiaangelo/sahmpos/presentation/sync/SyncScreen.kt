package io.github.yahiaangelo.sahmpos.presentation.sync

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SyncScreen(vm: SyncViewModel = koinViewModel()) {
    val status by vm.status.collectAsState()
    val log by vm.serverLog.collectAsState()

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Sync", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Surface(tonalElevation = 2.dp, shape = MaterialTheme.shapes.medium) {
            Column(Modifier.padding(12.dp).fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatusCell("Pending", status.pending)
                    StatusCell("In-flight", status.inFlight)
                    StatusCell("Done", status.done)
                    StatusCell("Failed", status.failed)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = vm::forceProcess) { Text("Process now") }
            OutlinedButton(onClick = vm::retryAll) { Text("Retry all") }
            OutlinedButton(onClick = vm::clearCompleted) { Text("Clear completed") }
        }
        Spacer(Modifier.height(12.dp))
        Text("Mock server events", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxSize()) {
            if (log.isEmpty()) {
                Column(Modifier.padding(12.dp)) { Text("No traffic yet.") }
            } else {
                LazyColumn(modifier = Modifier.padding(8.dp)) {
                    items(log) { line ->
                        Text(line, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCell(label: String, value: Int) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(value.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}