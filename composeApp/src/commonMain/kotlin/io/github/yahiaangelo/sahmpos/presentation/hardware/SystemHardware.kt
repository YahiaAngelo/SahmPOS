package io.github.yahiaangelo.sahmpos.presentation.hardware

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun CameraQrScanner(
    modifier: Modifier = Modifier,
    onScanned: (String) -> Unit,
)

@Composable
expect fun rememberReceiptPrintAction(): (text: String, jobName: String) -> Unit