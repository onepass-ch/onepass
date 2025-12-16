package ch.onepass.onepass.ui.scan

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import ch.onepass.onepass.ui.theme.Success
import ch.onepass.onepass.ui.theme.Warning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.collectLatest

/** Test tags for instrumentation and automated UI checks. */
object ScanTestTags {
  const val SCREEN = "scan_screen"
  const val CAMERA = "scan_camera_preview"
  const val HUD = "scan_overlay_hud"
  const val MESSAGE = "scan_message"
  const val PROGRESS = "scan_progress"
  const val PERMISSION = "scan_permission_prompt"
  const val SCAN_FRAME = "scan_frame"
  const val STATUS_ICON = "scan_status_icon"
  const val STATS_CARD = "scan_stats_card"
  const val BACK_BUTTON = "scan_back_button"
  const val NETWORK_ERROR_DIALOG = "scan_network_error_dialog"
}

/** Top-level scanner screen: wires ViewModel, permission gate, camera preview, and HUD. */
@Composable
fun ScanScreen(
    viewModel: ScannerViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onEffect: ((ScannerEffect) -> Unit)? = null
) {
  LaunchedEffect(viewModel, onEffect) {
    viewModel.effects.collectLatest { effect -> onEffect?.invoke(effect) }
  }

  CameraPermissionGate(
      modifier = modifier.fillMaxSize(),
      grantedContent = { ScanContent(viewModel, onNavigateBack) },
      deniedContent = { PermissionDeniedScreen() })
}

/** Main content: edge-to-edge camera preview with scanning frame and bottom HUD. */
@Composable
fun ScanContent(viewModel: ScannerViewModel, onNavigateBack: () -> Unit = {}) {
  val context = LocalContext.current
  val lifecycle = LocalLifecycleOwner.current
  val uiState by viewModel.state.collectAsState()

  // Network error dialog state
  var showNetworkErrorDialog by remember { mutableStateOf(false) }
  var showSessionExpiredDialog by remember { mutableStateOf(false) }
  var lastScannedQr by remember { mutableStateOf<String?>(null) }

  // Audio feedback with ToneGenerator
  val toneGenerator = remember {
    try {
      android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 80)
    } catch (e: Exception) {
      Log.w("ScanContent", "Failed to create ToneGenerator", e)
      null
    }
  }

  DisposableEffect(toneGenerator) { onDispose { toneGenerator?.release() } }

  // Haptic feedback (safe vibration with permission check)
  val vibrator = remember {
    @Suppress("DEPRECATION")
    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
  }

  // Detect errors and show appropriate dialog + play sounds
  LaunchedEffect(viewModel) {
    viewModel.effects.collectLatest { effect ->
      when (effect) {
        is ScannerEffect.Accepted -> {
          // Play success beep (short, pleasant tone)
          try {
            toneGenerator?.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150)
          } catch (e: Exception) {
            Log.w("ScanContent", "Failed to play accept sound", e)
          }

          // Vibration
          if (ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE) ==
              PackageManager.PERMISSION_GRANTED) {
            vibrator?.let { vib ->
              try {
                vibrateForEffect(vib, effect)
              } catch (e: Exception) {
                Log.w("ScanContent", "Vibration failed", e)
              }
            }
          }
        }
        is ScannerEffect.Rejected -> {
          // Play error buzz (harsh, double beep)
          try {
            toneGenerator?.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
          } catch (e: Exception) {
            Log.w("ScanContent", "Failed to play reject sound", e)
          }

          // Vibration
          if (ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE) ==
              PackageManager.PERMISSION_GRANTED) {
            vibrator?.let { vib ->
              try {
                vibrateForEffect(vib, effect)
              } catch (e: Exception) {
                Log.w("ScanContent", "Vibration failed", e)
              }
            }
          }
        }
        is ScannerEffect.Error -> {
          val message = effect.message.lowercase()
          when {
            // Session expired - should navigate to login
            message.contains("session expired") || message.contains("please login") -> {
              showSessionExpiredDialog = true
            }
            // Network errors - can retry
            message.contains("network") ||
                message.contains("connection") ||
                message.contains("internet") ||
                message.contains("timeout") -> {
              showNetworkErrorDialog = true
            }
            // Other errors - just show in HUD (dialog not needed)
            else -> {
              // Error is already shown in the HUD message, no dialog needed
            }
          }

          // Play warning sound (medium tone)
          try {
            toneGenerator?.startTone(android.media.ToneGenerator.TONE_PROP_NACK, 300)
          } catch (e: Exception) {
            Log.w("ScanContent", "Failed to play error sound", e)
          }

          // Vibration
          if (ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE) ==
              PackageManager.PERMISSION_GRANTED) {
            vibrator?.let { vib ->
              try {
                vibrateForEffect(vib, effect)
              } catch (e: Exception) {
                Log.w("ScanContent", "Vibration failed", e)
              }
            }
          }
        }
      }
    }
  }

  // Session expired dialog
  if (showSessionExpiredDialog) {
    AlertDialog(
        onDismissRequest = {
          showSessionExpiredDialog = false
          onNavigateBack()
        },
        icon = {
          Icon(
              imageVector = Icons.Outlined.Error,
              contentDescription = null,
              tint = ch.onepass.onepass.ui.theme.Error)
        },
        title = {
          Text(
              text = "Session Expired",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        },
        text = {
          Text(
              text = "Your session has expired. Please login again to continue scanning tickets.",
              style = MaterialTheme.typography.bodyMedium,
              color = colorScheme.primary)
        },
        confirmButton = {
          Button(
              onClick = {
                showSessionExpiredDialog = false
                onNavigateBack() // Go back, auth system will handle redirect to login
              },
              colors =
                  ButtonDefaults.buttonColors(containerColor = ch.onepass.onepass.ui.theme.Error)) {
                Text("OK")
              }
        },
        containerColor = colorScheme.secondaryContainer,
        titleContentColor = colorScheme.primary,
        textContentColor = colorScheme.primary,
        modifier = Modifier.testTag("scan_session_expired_dialog"))
  }

  // Network error dialog
  if (showNetworkErrorDialog) {
    AlertDialog(
        onDismissRequest = {
          showNetworkErrorDialog = false
          // Reset state to IDLE when dismissing to prevent error showing in HUD
          viewModel.resetToIdle()
          onNavigateBack() // Back on dismiss
        },
        icon = {
          Icon(imageVector = Icons.Outlined.Error, contentDescription = null, tint = Warning)
        },
        title = {
          Text(
              text = "No Internet Connection",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        },
        text = {
          Text(
              text =
                  "Please check your internet connection and try again. The scanner requires an active internet connection to validate tickets.",
              style = MaterialTheme.typography.bodyMedium,
              color = colorScheme.primary)
        },
        confirmButton = {
          Button(
              onClick = {
                showNetworkErrorDialog = false
                // Reset state before retry
                viewModel.resetToIdle()
                lastScannedQr?.let { qr -> viewModel.onQrScanned(qr) }
              },
              colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)) {
                Text("Retry")
              }
        },
        dismissButton = {
          TextButton(
              onClick = {
                showNetworkErrorDialog = false
                viewModel.resetToIdle()
                onNavigateBack()
              }) {
                Text("Back")
              }
        },
        containerColor = colorScheme.secondaryContainer,
        titleContentColor = colorScheme.primary,
        textContentColor = colorScheme.primary,
        modifier = Modifier.testTag(ScanTestTags.NETWORK_ERROR_DIALOG))
  }

  val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

  // Scanner with throttling and lifecycle management
  val analyzer = remember {
    val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    val scanner = BarcodeScanning.getClient(options)
    var isActive = true
    var lastAnalysisTime = 0L
    val minAnalysisIntervalMs = 300L

    fun analyzeImage(imageProxy: androidx.camera.core.ImageProxy) {
      if (!isActive) {
        imageProxy.close()
        return
      }

      val currentTime = System.currentTimeMillis()
      if (currentTime - lastAnalysisTime < minAnalysisIntervalMs) {
        imageProxy.close()
        return
      }
      lastAnalysisTime = currentTime

      @Suppress("UnsafeOptInUsageError") val media = imageProxy.image
      if (media == null) {
        imageProxy.close()
        return
      }

      val image = InputImage.fromMediaImage(media, imageProxy.imageInfo.rotationDegrees)
      scanner
          .process(image)
          .addOnSuccessListener { barcodes ->
            if (isActive) {
              barcodes.firstOrNull()?.rawValue?.let { qr ->
                lastScannedQr = qr
                viewModel.onQrScanned(qr)
              }
            }
          }
          .addOnCompleteListener { imageProxy.close() }
    }

    val imageAnalyzer = ImageAnalysis.Analyzer(::analyzeImage)

    Triple(scanner, imageAnalyzer) { isActive = false }
  }

  DisposableEffect(Unit) {
    onDispose {
      analyzer.third.invoke() // Deactivate analyzer first
      try {
        analyzer.first.close()
      } catch (e: Exception) {
        Log.w("ScanContent", "Failed to close scanner", e)
      }
      analysisExecutor.shutdown()
      try {
        if (!analysisExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
          analysisExecutor.shutdownNow()
          if (!analysisExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
            Log.e("ScanContent", "Executor did not terminate")
          }
        }
      } catch (e: InterruptedException) {
        analysisExecutor.shutdownNow()
        Thread.currentThread().interrupt()
      }
    }
  }

  val controller =
      remember(context) {
        LifecycleCameraController(context).apply {
          cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
          setImageAnalysisAnalyzer(analysisExecutor, analyzer.second)
        }
      }

  LaunchedEffect(lifecycle) {
    try {
      controller.unbind()
      controller.bindToLifecycle(lifecycle)
    } catch (e: Exception) {
      Log.e("ScanContent", "Failed to bind camera", e)
    }
  }

  DisposableEffect(controller) { onDispose { controller.unbind() } }

  Scaffold(containerColor = colorScheme.background) { padding ->
    Box(modifier = Modifier.fillMaxSize().padding(padding).testTag(ScanTestTags.SCREEN)) {
      // Camera Preview
      AndroidView(
          factory = { ctx ->
            PreviewView(ctx).apply {
              this.controller = controller
              this.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
              this.scaleType = PreviewView.ScaleType.FILL_CENTER
            }
          },
          modifier = Modifier.fillMaxSize().testTag(ScanTestTags.CAMERA))

      // Dark gradient overlay
      Box(
          modifier =
              Modifier.fillMaxSize()
                  .background(
                      Brush.verticalGradient(
                          listOf(
                              Color.Transparent,
                              Color(0x33000000),
                              colorScheme.secondaryContainer))))

      // Back button (top-left)
      IconButton(
          onClick = onNavigateBack,
          modifier =
              Modifier.align(Alignment.TopStart)
                  .padding(16.dp)
                  .size(48.dp)
                  .background(colorScheme.secondaryContainer.copy(alpha = 0.7f), CircleShape)
                  .testTag(ScanTestTags.BACK_BUTTON)) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = colorScheme.onBackground,
                modifier = Modifier.size(24.dp))
          }

      // Animated scanning frame
      ScanningFrame(uiState = uiState)

      // Top stats colorScheme.secondaryContainer (when not idle)
      AnimatedVisibility(
          visible =
              uiState.validated > 0 ||
                  uiState.status != ScannerUiState.Status.IDLE ||
                  uiState.eventTitle != null,
          enter = fadeIn() + slideInVertically(),
          exit = fadeOut() + slideOutVertically(),
          modifier = Modifier.align(Alignment.TopCenter)) {
            TopStatsCard(validated = uiState.validated, eventTitle = uiState.eventTitle)
          }

      // Bottom HUD (hide when dialogs are shown to avoid redundancy)
      if (!showNetworkErrorDialog && !showSessionExpiredDialog) {
        ScanHud(uiState = uiState)
      }
    }
  }
}

/**
 * Helper function to trigger vibration based on effect type. Permission is checked before calling
 * this function.
 */
@SuppressLint("MissingPermission")
private fun vibrateForEffect(vibrator: Vibrator, effect: ScannerEffect) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    when (effect) {
      is ScannerEffect.Accepted -> vibrator.vibrate(VibrationEffect.createOneShot(100, 128))
      is ScannerEffect.Rejected ->
          vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 50), -1))
      is ScannerEffect.Error -> vibrator.vibrate(VibrationEffect.createOneShot(200, 255))
    }
  } else {
    @Suppress("DEPRECATION")
    vibrator.vibrate(
        when (effect) {
          is ScannerEffect.Accepted -> 100L
          is ScannerEffect.Rejected -> 200L
          is ScannerEffect.Error -> 200L
        })
  }
}

/** Animated scanning frame in the center */
@Composable
private fun BoxScope.ScanningFrame(uiState: ScannerUiState) {
  val frameColor by
      animateColorAsState(
          targetValue =
              when (uiState.status) {
                ScannerUiState.Status.ACCEPTED -> Success
                ScannerUiState.Status.REJECTED -> ch.onepass.onepass.ui.theme.Error
                ScannerUiState.Status.ERROR -> Warning
                else -> colorScheme.primary
              },
          animationSpec = tween(300),
          label = "frame_color")

  val scale by
      animateFloatAsState(
          targetValue = if (uiState.isProcessing) 1.05f else 1f,
          animationSpec =
              spring(
                  dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
          label = "frame_scale")

  Box(
      modifier =
          Modifier.align(Alignment.Center)
              .size(280.dp)
              .scale(scale)
              .border(width = 3.dp, color = frameColor, shape = RoundedCornerShape(24.dp))
              .testTag(ScanTestTags.SCAN_FRAME),
      contentAlignment = Alignment.Center) {
        FrameCorners(color = frameColor)

        AnimatedVisibility(
            visible = uiState.status != ScannerUiState.Status.IDLE,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()) {
              Box(
                  modifier =
                      Modifier.size(80.dp)
                          .clip(CircleShape)
                          .background(frameColor.copy(alpha = 0.2f)),
                  contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector =
                            when (uiState.status) {
                              ScannerUiState.Status.ACCEPTED -> Icons.Filled.CheckCircle
                              ScannerUiState.Status.REJECTED -> Icons.Filled.Close
                              ScannerUiState.Status.ERROR -> Icons.Outlined.Error
                              else -> Icons.Filled.QrCodeScanner
                            },
                        contentDescription =
                            when (uiState.status) {
                              ScannerUiState.Status.ACCEPTED -> "Access granted"
                              ScannerUiState.Status.REJECTED -> "Access denied"
                              ScannerUiState.Status.ERROR -> "Validation error"
                              else -> "Waiting for scan"
                            },
                        tint = frameColor,
                        modifier = Modifier.size(48.dp).testTag(ScanTestTags.STATUS_ICON))
                  }
            }
      }
}

/** Decorative corner brackets for scanning frame */
@Composable
private fun BoxScope.FrameCorners(color: Color) {
  val cornerSize = 40.dp
  val cornerWidth = 4.dp

  // Top-left
  Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
    Box(
        modifier =
            Modifier.width(cornerSize)
                .height(cornerWidth)
                .background(color, RoundedCornerShape(2.dp)))
    Box(
        modifier =
            Modifier.width(cornerWidth)
                .height(cornerSize)
                .background(color, RoundedCornerShape(2.dp)))
  }

  // Top-right
  Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
    Box(
        modifier =
            Modifier.width(cornerSize)
                .height(cornerWidth)
                .align(Alignment.TopEnd)
                .background(color, RoundedCornerShape(2.dp)))
    Box(
        modifier =
            Modifier.width(cornerWidth)
                .height(cornerSize)
                .align(Alignment.TopEnd)
                .background(color, RoundedCornerShape(2.dp)))
  }

  // Bottom-left
  Box(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)) {
    Box(
        modifier =
            Modifier.width(cornerSize)
                .height(cornerWidth)
                .align(Alignment.BottomStart)
                .background(color, RoundedCornerShape(2.dp)))
    Box(
        modifier =
            Modifier.width(cornerWidth)
                .height(cornerSize)
                .align(Alignment.BottomStart)
                .background(color, RoundedCornerShape(2.dp)))
  }

  // Bottom-right
  Box(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
    Box(
        modifier =
            Modifier.width(cornerSize)
                .height(cornerWidth)
                .align(Alignment.BottomEnd)
                .background(color, RoundedCornerShape(2.dp)))
    Box(
        modifier =
            Modifier.width(cornerWidth)
                .height(cornerSize)
                .align(Alignment.BottomEnd)
                .background(color, RoundedCornerShape(2.dp)))
  }
}

/**
 * Top stats colorScheme.secondaryContainer showing event title and validated tickets count -
 * Internal for testing
 */
@Composable
internal fun TopStatsCard(validated: Int, eventTitle: String?) {
  Surface(
      color = colorScheme.secondaryContainer.copy(alpha = 0.95f),
      shape = RoundedCornerShape(16.dp),
      tonalElevation = 4.dp,
      modifier = Modifier.padding(top = 24.dp).testTag(ScanTestTags.STATS_CARD)) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
              // Event title
              if (eventTitle != null) {
                Text(
                    text = eventTitle,
                    color = colorScheme.primary,
                    style =
                        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
              }

              // Validated count
              Text(
                  text = validated.toString(),
                  color = colorScheme.primary,
                  style =
                      MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
              Text(
                  text = "Validated",
                  color = colorScheme.primary,
                  style = MaterialTheme.typography.bodySmall)
            }
      }
}

/** Bottom overlay showing the current status and details */
@Composable
private fun BoxScope.ScanHud(uiState: ScannerUiState) {
  val backgroundColor by
      animateColorAsState(
          targetValue =
              when (uiState.status) {
                ScannerUiState.Status.ACCEPTED -> Success.copy(alpha = 0.15f)
                ScannerUiState.Status.REJECTED ->
                    ch.onepass.onepass.ui.theme.Error.copy(alpha = 0.15f)
                ScannerUiState.Status.ERROR -> Warning.copy(alpha = 0.15f)
                else -> Color.Transparent
              },
          animationSpec = tween(300),
          label = "bg_color")

  Surface(
      color = colorScheme.secondaryContainer.copy(alpha = 0.95f),
      tonalElevation = 0.dp,
      shadowElevation = 8.dp,
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
      modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().testTag(ScanTestTags.HUD)) {
        Box {
          Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(backgroundColor))

          Column(
              modifier =
                  Modifier.padding(horizontal = 24.dp, vertical = 20.dp).navigationBarsPadding()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.message,
                            color = colorScheme.primary,
                            style =
                                MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold),
                            modifier = Modifier.testTag(ScanTestTags.MESSAGE))

                        if (uiState.lastScannedUserName != null &&
                            uiState.status == ScannerUiState.Status.ACCEPTED) {
                          Spacer(Modifier.height(4.dp))
                          Text(
                              text = uiState.lastScannedUserName,
                              color = colorScheme.primary,
                              style =
                                  MaterialTheme.typography.bodyLarge.copy(
                                      fontWeight = FontWeight.Medium))
                        }

                        if (uiState.lastTicketId != null) {
                          Spacer(Modifier.height(6.dp))
                          Text(
                              text = "Ticket ${uiState.lastTicketId}",
                              color = colorScheme.primary,
                              style =
                                  MaterialTheme.typography.bodyMedium.copy(
                                      fontWeight = FontWeight.SemiBold))
                        }

                        if (uiState.status == ScannerUiState.Status.IDLE) {
                          Spacer(Modifier.height(6.dp))
                          Text(
                              text = "Position the QR code within the frame",
                              color = colorScheme.primary,
                              style = MaterialTheme.typography.bodySmall)
                        }
                      }

                      if (uiState.isProcessing) {
                        Spacer(Modifier.width(16.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp).testTag(ScanTestTags.PROGRESS),
                            color = colorScheme.primary,
                            strokeWidth = 3.dp)
                      }
                    }
              }
        }
      }
}

/** Permission denied screen - Internal for testing */
@Composable
internal fun PermissionDeniedScreen() {
  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(colorScheme.background)
              .testTag(ScanTestTags.PERMISSION),
      contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)) {
              Icon(
                  imageVector = Icons.Filled.QrCodeScanner,
                  contentDescription = "QR code scanner required",
                  tint = colorScheme.primary,
                  modifier = Modifier.size(80.dp))
              Spacer(Modifier.height(24.dp))
              Text(
                  text = "Camera Access Required",
                  color = colorScheme.primary,
                  style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                  textAlign = TextAlign.Center)
              Spacer(Modifier.height(12.dp))
              Text(
                  text =
                      "To scan tickets, we need access to your camera. Please grant permission to continue.",
                  color = colorScheme.primary,
                  style = MaterialTheme.typography.bodyMedium,
                  textAlign = TextAlign.Center)
            }
      }
}

/** Permission gate for Camera in Compose. */
@Composable
private fun CameraPermissionGate(
    modifier: Modifier = Modifier,
    grantedContent: @Composable () -> Unit,
    deniedContent: @Composable () -> Unit
) {
  val context = LocalContext.current
  var granted by remember {
    mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED)
  }
  val launcher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
          wasGranted ->
        granted = wasGranted
      }

  LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

  Box(modifier = modifier) { if (granted) grantedContent() else deniedContent() }
}

/* ---------------------------------- PREVIEWS ---------------------------------- */

@Preview(name = "Scan HUD - Idle", showBackground = true, backgroundColor = 0xFF111111)
@Composable
internal fun PreviewScanHudIdle() {
  PreviewHudContainer(
      state =
          ScannerUiState(
              isProcessing = false, message = "Scan a pass", status = ScannerUiState.Status.IDLE))
}

@Preview(name = "Scan HUD - Validating", showBackground = true, backgroundColor = 0xFF111111)
@Composable
internal fun PreviewScanHudValidating() {
  PreviewHudContainer(
      state =
          ScannerUiState(
              isProcessing = true, message = "Validating…", status = ScannerUiState.Status.IDLE))
}

@Preview(name = "Scan HUD - Accepted", showBackground = true, backgroundColor = 0xFF111111)
@Composable
internal fun PreviewScanHudAccepted() {
  PreviewHudContainer(
      state =
          ScannerUiState(
              isProcessing = false,
              message = "Access Granted",
              lastTicketId = "T-4821",
              lastScannedUserName = "John Doe",
              validated = 41,
              eventTitle = "Summer Music Festival",
              status = ScannerUiState.Status.ACCEPTED))
}

@Preview(name = "Scan HUD - Rejected", showBackground = true, backgroundColor = 0xFF111111)
@Composable
internal fun PreviewScanHudRejected() {
  PreviewHudContainer(
      state =
          ScannerUiState(
              isProcessing = false,
              message = "Already scanned",
              status = ScannerUiState.Status.REJECTED))
}

/** Shared preview container - Internal for testing */
@Composable
internal fun PreviewHudContainer(state: ScannerUiState) {
  Box(modifier = Modifier.fillMaxSize().background(colorScheme.background)) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(
                    Brush.verticalGradient(colors = listOf(Color(0xFF0F0F10), Color(0xFF171718)))),
        contentAlignment = Alignment.Center) {
          Text(
              text = "Camera Preview", color = colorScheme.primary, modifier = Modifier.alpha(0.5f))
        }

    Box(
        modifier =
            Modifier.align(Alignment.Center)
                .size(280.dp)
                .border(
                    width = 3.dp,
                    color =
                        when (state.status) {
                          ScannerUiState.Status.ACCEPTED -> Success
                          ScannerUiState.Status.REJECTED -> ch.onepass.onepass.ui.theme.Error
                          ScannerUiState.Status.ERROR -> Warning
                          else -> colorScheme.primary
                        },
                    shape = RoundedCornerShape(24.dp)))

    if (state.validated > 0 || state.eventTitle != null) {
      TopStatsCard(validated = state.validated, eventTitle = state.eventTitle)
    }

    ScanHud(uiState = state)
  }
}
