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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Fragment responsible for displaying the camera preview and scanning QR codes using ML Kit.
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

    // Keep an analyzer instance so we can reset debounce from the ViewModel
    private lateinit var analyzer: QrAnalyzer

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
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                analyzer = QrAnalyzer(
                    onQrDetected = { qr -> viewModel.onQrScanned(qr) }
                )

                analysis = ImageAnalysis.Builder()
                    // Prefer aspect ratio for better device compatibility
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply { setAnalyzer(cameraExecutor!!, analyzer) }

                val selector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, selector, preview, analysis
                )

                // Robust torch handling: hide if no flash, and observe state
                camera?.cameraInfo?.let { info ->
                    if (!info.hasFlashUnit()) {
                        torchButton.isVisible = false
                    } else {
                        info.torchState.observe(viewLifecycleOwner) { state ->
                            torchButton.setImageResource(
                                if (state == TorchState.ON) R.drawable.ic_flash_on
                                else R.drawable.ic_flash_off
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                messageText.text = getString(R.string.camera_error_generic)
                // Log.e("ScanCameraFragment", "Camera error", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun toggleTorch() {
        camera?.let {
            val info = it.cameraInfo
            if (!info.hasFlashUnit()) return
            val isOn = info.torchState.value == TorchState.ON
            it.cameraControl.enableTorch(!isOn)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collectLatest { state ->
                        messageText.text = state.message
                        progressBar.isVisible = state.isProcessing

                        // When returning to IDLE, allow rescanning the same code
                        if (state.status == ScannerUiState.Status.IDLE && ::analyzer.isInitialized) {
                            analyzer.resetDebounce()
                        }
                    }
                }
                launch {
                    viewModel.effects.collectLatest { effect ->
                        // Light haptic feedback for all effects
                        vibrate()
                    }
                }
            }
        }
    }

    private fun vibrate() {
        val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            // Haptic fallback if device has no vibrator
            view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        analysis?.clearAnalyzer()
        cameraExecutor?.shutdown()
    }
}

/**
 * Analyzer that decodes QR codes from camera frames using ML Kit BarcodeScanning.
 * Includes anti-spam (busy) and cooldown logic to prevent repeated triggers.
 */
private class QrAnalyzer(
    private val onQrDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    @Volatile private var busy = false
    private var lastDetected: String? = null
    private var lastTime: Long = 0
    private val coolDownMs = 1200L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (busy) { imageProxy.close(); return }
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        busy = true
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                // Process only the first QR code from the frame
                val raw = barcodes.firstOrNull()?.rawValue
                if (raw != null) {
                    val now = System.currentTimeMillis()
                    val notTooSoon = now - lastTime > coolDownMs
                    if (notTooSoon || raw != lastDetected) {
                        lastDetected = raw
                        lastTime = now
                        onQrDetected(raw)
                    }
                }
            }
            .addOnCompleteListener {
                busy = false
                imageProxy.close()
            }
    }

    fun resetDebounce() {
        lastDetected = null
        lastTime = 0
    }
}