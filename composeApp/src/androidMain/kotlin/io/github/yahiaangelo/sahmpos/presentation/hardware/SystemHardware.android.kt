package io.github.yahiaangelo.sahmpos.presentation.hardware

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.FileOutputStream
import java.util.concurrent.Executors

@Composable
actual fun CameraQrScanner(
    modifier: Modifier,
    onScanned: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    if (!hasPermission) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                "Camera permission required to scan",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(24.dp),
            )
        }
        return
    }

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val mlScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
        )
    }
    val emittedRef = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    val providerRef = remember { java.util.concurrent.atomic.AtomicReference<ProcessCameraProvider?>(null) }
    val onScannedLatest by rememberUpdatedState(onScanned)

    DisposableEffect(Unit) {
        onDispose {
            providerRef.getAndSet(null)?.unbindAll()
            mlScanner.close()
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val mainExecutor = ContextCompat.getMainExecutor(ctx)
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get() ?: return@addListener
                providerRef.set(provider)
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(analysisExecutor) { proxy ->
                    val media = proxy.image
                    if (media == null || emittedRef.get()) {
                        proxy.close()
                        return@setAnalyzer
                    }
                    val input = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                    mlScanner.process(input)
                        .addOnSuccessListener { codes ->
                            codes.firstNotNullOfOrNull { it.rawValue }?.let { value ->
                                if (emittedRef.compareAndSet(false, true)) {
                                    mainExecutor.execute {
                                        providerRef.getAndSet(null)?.unbindAll()
                                        onScannedLatest(value)
                                    }
                                }
                            }
                        }
                        .addOnCompleteListener { proxy.close() }
                }
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                } catch (_: Exception) {
                    // ignore — camera unavailable
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}

@Composable
actual fun rememberReceiptPrintAction(): (text: String, jobName: String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { text: String, jobName: String ->
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            printManager.print(
                jobName,
                ReceiptPrintAdapter(context.applicationContext, text, jobName),
                PrintAttributes.Builder().build(),
            )
        }
    }
}

private class ReceiptPrintAdapter(
    private val context: Context,
    private val text: String,
    private val jobName: String,
) : PrintDocumentAdapter() {

    private var attributes: PrintAttributes? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        attributes = newAttributes
        val info = PrintDocumentInfo.Builder(jobName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
        callback.onLayoutFinished(info, oldAttributes != newAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback,
    ) {
        val attrs = attributes ?: PrintAttributes.Builder().build()
        val pdf = PrintedPdfDocument(context, attrs)
        try {
            val paint = Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 12f
                typeface = Typeface.MONOSPACE
                isAntiAlias = true
            }
            val lineHeight = paint.fontSpacing
            val marginX = 24f
            val marginY = 32f

            val lines = text.split("\n")
            var lineIndex = 0
            var pageNumber = 0

            while (lineIndex < lines.size) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                    return
                }
                val page = pdf.startPage(pageNumber)
                val canvas = page.canvas
                val usableHeight = page.info.contentRect.height() - marginY
                var y = marginY + lineHeight
                while (lineIndex < lines.size && y < usableHeight) {
                    canvas.drawText(lines[lineIndex], marginX, y, paint)
                    y += lineHeight
                    lineIndex++
                }
                pdf.finishPage(page)
                pageNumber++
            }

            FileOutputStream(destination.fileDescriptor).use { out ->
                pdf.writeTo(out)
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.message)
        } finally {
            pdf.close()
        }
    }
}