// app/src/main/java/ch/onepass/onepass/ui/scan/ScanCameraFragment.kt
package ch.onepass.onepass.ui.scan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Size
import android.view.*
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ch.onepass.onepass.R
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Fragment responsible for camera preview and QR code scanning using ML Kit.
 *
 * Requirements:
 * - CameraX dependencies: camera-core, camera-camera2, camera-lifecycle, camera-view
 * - ML Kit dependency: com.google.mlkit:barcode-scanning
 *
 * This component connects directly to ScannerViewModel to validate scanned passes.
 */
class ScanCameraFragment : Fragment() {

    companion object {
        private const val ARG_EVENT_ID = "event_id"

        fun newInstance(eventId: String) = ScanCameraFragment().apply {
            arguments = Bundle().apply { putString(ARG_EVENT_ID, eventId) }
        }
    }

    private lateinit var previewView: PreviewView
    private lateinit var messageText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var torchButton: ImageButton

    private var camera: Camera? = null
    private var analysis: ImageAnalysis? = null
    private var cameraExecutor: ExecutorService? = null
    private var analyzerJob: Job? = null

    private val viewModel: ScannerViewModel by viewModels {
        val eventId = requireArguments().getString(ARG_EVENT_ID).orEmpty()
        ScannerViewModelFactory(eventId)
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera() else showPermissionDenied()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_scan_camera, container, false)
        previewView = view.findViewById(R.id.previewView)
        messageText = view.findViewById(R.id.scanMessage)
        progressBar = view.findViewById(R.id.scanProgress)
        torchButton = view.findViewById(R.id.torchButton)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        torchButton.setOnClickListener { toggleTorch() }

        // Observe ViewModel state and effects
        observeViewModel()

        // Check for camera permission
        if (hasCameraPermission()) startCamera()
        else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    private fun showPermissionDenied() {
        messageText.text = "Camera permission is required to scan tickets."
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            analysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(cameraExecutor!!, QrAnalyzer { qr ->
                        viewModel.onQrScanned(qr)
                    })
                }

            val selector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                this, selector, preview, analysis
            )
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun toggleTorch() {
        camera?.let {
            val newState = !it.cameraInfo.torchState.value!!.equals(TorchState.ON)
            it.cameraControl.enableTorch(newState)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collectLatest { state ->
                        messageText.text = state.message
                        progressBar.isVisible = state.isProcessing
                    }
                }
                launch {
                    viewModel.effects.collectLatest { effect ->
                        when (effect) {
                            is ScannerEffect.Accepted -> vibrate()
                            is ScannerEffect.Rejected -> vibrate()
                            is ScannerEffect.Error -> vibrate()
                        }
                    }
                }
            }
        }
    }

    private fun vibrate() {
        val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        analysis?.clearAnalyzer()
        cameraExecutor?.shutdown()
        analyzerJob?.cancel()
    }
}

/**
 * Analyzer that decodes QR codes from camera frames using ML Kit BarcodeScanning.
 */
private class QrAnalyzer(
    private val onQrDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()

    private val scanner = BarcodeScanning.getClient(options)
    private var lastDetected: String? = null

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return imageProxy.close()
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val raw = barcode.rawValue ?: continue
                    if (raw != lastDetected) {
                        lastDetected = raw
                        onQrDetected(raw)
                    }
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}
