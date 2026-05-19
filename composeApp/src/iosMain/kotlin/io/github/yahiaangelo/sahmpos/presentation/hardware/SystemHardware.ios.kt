package io.github.yahiaangelo.sahmpos.presentation.hardware

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeAztecCode
import platform.AVFoundation.AVMetadataObjectTypeCode128Code
import platform.AVFoundation.AVMetadataObjectTypeCode39Code
import platform.AVFoundation.AVMetadataObjectTypeCode93Code
import platform.AVFoundation.AVMetadataObjectTypeDataMatrixCode
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeEAN8Code
import platform.AVFoundation.AVMetadataObjectTypePDF417Code
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.AVMetadataObjectTypeUPCECode
import platform.AVFoundation.requestAccessForMediaType
import platform.UIKit.UIFont
import platform.UIKit.UIPrintInfo
import platform.UIKit.UIPrintInfoOutputType
import platform.UIKit.UIPrintInteractionController
import platform.UIKit.UISimpleTextPrintFormatter
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraQrScanner(
    modifier: Modifier,
    onScanned: (String) -> Unit,
) {
    val current = rememberUpdatedState(onScanned)
    UIKitViewController(
        factory = {
            QrScannerViewController { value -> current.value(value) }
        },
        modifier = modifier,
    )
}

@Composable
actual fun rememberReceiptPrintAction(): (text: String, jobName: String) -> Unit {
    return remember {
        { text: String, jobName: String ->
            val info = UIPrintInfo.printInfo().apply {
                setOutputType(UIPrintInfoOutputType.UIPrintInfoOutputGeneral)
                setJobName(jobName)
            }
            val formatter = UISimpleTextPrintFormatter(text = text).apply {
                font = UIFont.fontWithName("Menlo", 10.0)
                    ?: UIFont.systemFontOfSize(10.0)
            }
            val controller = UIPrintInteractionController.sharedPrintController().apply {
                printInfo = info
                printFormatter = formatter
            }
            controller.presentAnimated(true, completionHandler = null)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class QrScannerViewController(
    private val onScanned: (String) -> Unit,
) : UIViewController(nibName = null, bundle = null) {

    private val session = AVCaptureSession()
    private val previewLayer = AVCaptureVideoPreviewLayer(session = session)
    private var emitted = false

    private val delegate = object : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
        override fun captureOutput(
            output: AVCaptureOutput,
            didOutputMetadataObjects: List<*>,
            fromConnection: AVCaptureConnection,
        ) {
            if (emitted) return
            val first = didOutputMetadataObjects.firstOrNull() as? AVMetadataMachineReadableCodeObject
            val value = first?.stringValue ?: return
            emitted = true
            onScanned(value)
        }
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
        previewLayer.setFrame(view.bounds)
        view.layer.addSublayer(previewLayer)

        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
            if (!granted) return@requestAccessForMediaType
            dispatch_async(dispatch_get_main_queue()) {
                configureSession()
                dispatch_async(
                    dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)
                ) {
                    if (!session.isRunning()) session.startRunning()
                }
            }
        }
    }

    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        previewLayer.setFrame(view.bounds)
    }

    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)
        if (session.isRunning()) {
            dispatch_async(
                dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)
            ) {
                session.stopRunning()
            }
        }
    }

    private fun configureSession() {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null) ?: return
        if (session.canAddInput(input)) session.addInput(input)

        val output = AVCaptureMetadataOutput()
        if (session.canAddOutput(output)) {
            session.addOutput(output)
            output.setMetadataObjectsDelegate(delegate, dispatch_get_main_queue())
            output.metadataObjectTypes = listOf(
                AVMetadataObjectTypeQRCode,
                AVMetadataObjectTypeEAN13Code,
                AVMetadataObjectTypeEAN8Code,
                AVMetadataObjectTypeCode128Code,
                AVMetadataObjectTypeCode39Code,
                AVMetadataObjectTypeCode93Code,
                AVMetadataObjectTypeUPCECode,
                AVMetadataObjectTypePDF417Code,
                AVMetadataObjectTypeDataMatrixCode,
                AVMetadataObjectTypeAztecCode,
            )
        }
    }
}