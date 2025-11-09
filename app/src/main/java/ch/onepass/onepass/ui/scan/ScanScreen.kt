package ch.onepass.onepass.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import ch.onepass.onepass.model.scan.ScanDecision
import ch.onepass.onepass.model.scan.TicketScanRepository
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.Executors

/** Test tags for instrumentation and automated UI checks. */
object ScanTestTags {
    const val SCREEN = "scan_screen"
    const val CAMERA = "scan_camera_preview"
    const val HUD = "scan_overlay_hud"
    const val MESSAGE = "scan_message"
    const val PROGRESS = "scan_progress"
    const val TORCH = "scan_torch"
    const val PERMISSION = "scan_permission_prompt"
}

/** Dark palette aligned with the Profile screen aesthetics. */
private object ScanColors {
    val Background = Color(0xFF111111)
    val Card = Color(0xFF1B1B1B)
    val Accent = Color(0xFF9C6BFF)
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFB0B0B0)
    val Scrim = Color(0xAA000000)
}

/**
 * Top-level scanner screen: wires ViewModel, permission gate, camera preview, and HUD.
 * The same ScannerViewModel used in your Fragment integrates here without changes.
 */
@Composable
fun ScanScreen(
    viewModel: ScannerViewModel,
    modifier: Modifier = Modifier,
    onEffect: ((ScannerEffect) -> Unit)? = null
) {
    // Side-effect collection (vibration/snackbar/sound can be handled by the caller).
    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect -> onEffect?.invoke(effect) }
    }

    CameraPermissionGate(
        modifier = modifier.fillMaxSize(),
        grantedContent = { ScanContent(viewModel) },
        deniedContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ScanColors.Background)
                    .testTag(ScanTestTags.PERMISSION),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Camera permission is required to scan tickets.",
                        color = ScanColors.TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Grant access and try again.",
                        color = ScanColors.TextSecondary
                    )
                }
            }
        }
    )
}

/**
 * Main content: edge-to-edge camera preview with a bottom HUD showing status and controls.
 */
@Composable
private fun ScanContent(viewModel: ScannerViewModel) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val uiState by viewModel.state.collectAsState()

    // Dedicated executor for analysis work.
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { analysisExecutor.shutdown() } }

    // ML Kit QR-only analyzer.
    val analyzer = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val scanner = BarcodeScanning.getClient(options)

        ImageAnalysis.Analyzer { imageProxy ->
            val media = imageProxy.image ?: run { imageProxy.close(); return@Analyzer }
            val image = InputImage.fromMediaImage(media, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull()?.rawValue?.let(viewModel::onQrScanned)
                }
                .addOnCompleteListener { imageProxy.close() }
        }
    }

    // Lifecycle-aware CameraX controller (preview + analysis).
    val controller = remember(context) {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setImageAnalysisAnalyzer(analysisExecutor, analyzer)
        }
    }

    // Bind/unbind the controller when lifecycle changes.
    LaunchedEffect(lifecycle) {
        controller.unbind()
        controller.bindToLifecycle(lifecycle)
    }

    Scaffold(containerColor = ScanColors.Background) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(ScanTestTags.SCREEN)
        ) {
            // Camera preview.
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        this.controller = controller
                        this.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                        this.scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(ScanTestTags.CAMERA)
            )

            // Subtle scrim for readability against bright scenes.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, ScanColors.Scrim)))
            )

            // Bottom HUD with message, progress and torch control.
            ScanHud(
                uiState = uiState,
                showTorch = true,
                torchController = controller::enableTorch,
                hasFlash = runCatching { controller.hasFlashUnit() }.getOrDefault(false)
            )
        }
    }
}

/** Bottom overlay showing the current status, details, progress and torch control. */
@Composable
private fun BoxScope.ScanHud(
    uiState: ScannerUiState,
    showTorch: Boolean,
    torchController: (Boolean) -> Unit,
    hasFlash: Boolean
) {
    Surface(
        color = ScanColors.Card.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .testTag(ScanTestTags.HUD)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiState.message,
                    color = ScanColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.testTag(ScanTestTags.MESSAGE)
                )
                Spacer(Modifier.height(4.dp))
                val detail = when {
                    uiState.lastTicketId != null && uiState.remaining != null ->
                        "Ticket ${uiState.lastTicketId} • Remaining ${uiState.remaining}"
                    uiState.lastTicketId != null ->
                        "Ticket ${uiState.lastTicketId}"
                    else -> null
                }
                if (detail != null) {
                    Text(
                        text = detail,
                        color = ScanColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            if (uiState.isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(22.dp)
                        .testTag(ScanTestTags.PROGRESS),
                    color = ScanColors.Accent,
                    strokeWidth = 2.5.dp
                )
                Spacer(Modifier.width(12.dp))
            }

            if (showTorch && hasFlash) {
                TorchButton(onToggle = torchController)
            }
        }
    }
}

/** Minimal torch toggle styled to match the HUD. */
@Composable
private fun TorchButton(onToggle: (Boolean) -> Unit) {
    var torchOn by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(ScanColors.Card)
            .clickable {
                torchOn = !torchOn
                onToggle(torchOn)
            }
            .testTag(ScanTestTags.TORCH),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Bolt,
            contentDescription = if (torchOn) "Turn torch off" else "Turn torch on",
            tint = if (torchOn) ScanColors.Accent else ScanColors.TextSecondary
        )
    }
}

/**
 * Permission gate for Camera in Compose. Displays either grantedContent or deniedContent.
 */
@Composable
private fun CameraPermissionGate(
    modifier: Modifier = Modifier,
    grantedContent: @Composable () -> Unit,
    deniedContent: @Composable () -> Unit
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { wasGranted -> granted = wasGranted }

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = modifier) {
        if (granted) grantedContent() else deniedContent()
    }
}

/* ---------------------------------- PREVIEWS ---------------------------------- */

/**
 * Preview-safe HUD-only rendering (no CameraX). This allows design validation in Android Studio.
 * It simulates a few common states of ScannerUiState.
 */
@Preview(name = "Scan HUD - Idle", showBackground = true, backgroundColor = 0xFF111111)
@Composable
private fun PreviewScanHudIdle() {
    PreviewHudContainer(
        state = ScannerUiState(
            isProcessing = false,
            message = "Scan a pass…",
            status = ScannerUiState.Status.IDLE
        )
    )
}

@Preview(name = "Scan HUD - Validating", showBackground = true, backgroundColor = 0xFF111111)
@Composable
private fun PreviewScanHudValidating() {
    PreviewHudContainer(
        state = ScannerUiState(
            isProcessing = true,
            message = "Validating…",
            status = ScannerUiState.Status.IDLE
        )
    )
}

@Preview(name = "Scan HUD - Accepted", showBackground = true, backgroundColor = 0xFF111111)
@Composable
private fun PreviewScanHudAccepted() {
    PreviewHudContainer(
        state = ScannerUiState(
            isProcessing = false,
            message = "Access Granted",
            lastTicketId = "T-4821",
            remaining = 41,
            status = ScannerUiState.Status.ACCEPTED
        )
    )
}

@Preview(name = "Scan HUD - Rejected", showBackground = true, backgroundColor = 0xFF111111)
@Composable
private fun PreviewScanHudRejected() {
    PreviewHudContainer(
        state = ScannerUiState(
            isProcessing = false,
            message = "Already scanned",
            status = ScannerUiState.Status.REJECTED
        )
    )
}

/** Shared preview container that paints a camera placeholder and draws the HUD. */
@Composable
private fun PreviewHudContainer(state: ScannerUiState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScanColors.Background)
    ) {
        // Camera placeholder for contrast in preview.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F0F10), Color(0xFF171718))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Camera Preview Placeholder", color = ScanColors.TextSecondary)
        }

        // HUD only (no torch in preview to avoid controller dependency).
        Surface(
            color = ScanColors.Card.copy(alpha = 0.92f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .testTag(ScanTestTags.HUD)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.message,
                        color = ScanColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(Modifier.height(4.dp))
                    val detail = when {
                        state.lastTicketId != null && state.remaining != null ->
                            "Ticket ${state.lastTicketId} • Remaining ${state.remaining}"
                        state.lastTicketId != null ->
                            "Ticket ${state.lastTicketId}"
                        else -> null
                    }
                    if (detail != null) {
                        Text(
                            text = detail,
                            color = ScanColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                if (state.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(22.dp)
                            .testTag(ScanTestTags.PROGRESS),
                        color = ScanColors.Accent,
                        strokeWidth = 2.5.dp
                    )
                }
            }
        }
    }
}

/* ---------------- Optional dummy repo for previews or local UI demos ---------------- */

/** No-op repository example for local UI demos (not used at runtime). */
private class DummyScanRepo : ch.onepass.onepass.model.scan.TicketScanRepository {
    override suspend fun validateByPass(passQr: String, eventId: String)
            : Result<ScanDecision> = Result.success(ScanDecision.Rejected(ScanDecision.Reason.UNKNOWN))
}

/** Preview-friendly ViewModel facade exposing a mutable StateFlow. */
private class PreviewScannerViewModel(
    initial: ScannerUiState
) : ScannerViewModel(
    eventId = "preview",
    repo = DummyScanRepo(),
    clock = { 0L },
    enableAutoCleanup = false,
    coroutineScope = null
) {
    private val _previewState = MutableStateFlow(initial)
    override val state: StateFlow<ScannerUiState> get() = _previewState
}