package ch.onepass.onepass.ui.scan

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import ch.onepass.onepass.R
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

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

private object ScanAlpha {
  const val GLOW_BASE = 0.3f
  const val GLOW_PULSE = 0.2f
  const val RADIAL_START = 0.4f
  const val RADIAL_MID = 0.1f
  const val SWEEP_1 = 0.2f
  const val SWEEP_2 = 0.5f
  const val SWEEP_3 = 0.8f
  const val SWEEP_4 = 0.3f
  const val SWEEP_5 = 0.6f
  const val DOT_MAIN = 0.8f
  const val DOT_SECONDARY = 0.4f
  const val CENTER_BASE = 0.8f
  const val CENTER_PULSE = 0.2f
  const val CARD = 0.7f
  const val CARD_SURFACE = 0.95f
  const val STATUS_BG = 0.2f
  const val STATUS_SUCCESS = 0.15f
  const val STATUS_ERROR = 0.15f
  const val STATUS_WARNING = 0.15f
  const val FLASH = 0.4f
  const val PREVIEW = 0.5f
}

/** Stylish rotating gradient spinner with multiple animated layers */
@Composable
private fun RotatingSpinner(modifier: Modifier = Modifier, color: Color) {
  var rotation by remember { mutableFloatStateOf(0f) }
  var glowPulse by remember { mutableFloatStateOf(0f) }

  // Main rotation animation
  LaunchedEffect(Unit) {
    while (true) {
      rotation = (rotation + 10f) % 360f
      delay(16)
    }
  }

  // Glow pulse animation
  LaunchedEffect(Unit) {
    while (true) {
      for (i in 0..100) {
        glowPulse = i / 100f
        delay(10)
      }
      for (i in 100 downTo 0) {
        glowPulse = i / 100f
        delay(10)
      }
    }
  }

  Box(modifier = modifier.size(40.dp), contentAlignment = Alignment.Center) {
    // Outer glow ring (static, pulsing)
    Canvas(
        modifier =
            Modifier.fillMaxSize().alpha(ScanAlpha.GLOW_BASE + glowPulse * ScanAlpha.GLOW_PULSE)) {
          drawCircle(
              brush =
                  Brush.radialGradient(
                      colors =
                          listOf(
                              color.copy(alpha = ScanAlpha.RADIAL_START),
                              color.copy(alpha = ScanAlpha.RADIAL_MID),
                              Color.Transparent)),
              radius = size.minDimension / 2)
        }

    // Primary spinning arc with gradient
    Canvas(modifier = Modifier.fillMaxSize().rotate(rotation)) {
      val strokeWidth = 4.dp.toPx()

      // Main gradient arc
      drawArc(
          brush =
              Brush.sweepGradient(
                  colors =
                      listOf(
                          Color.Transparent,
                          color.copy(alpha = ScanAlpha.SWEEP_1),
                          color.copy(alpha = ScanAlpha.SWEEP_2),
                          color.copy(alpha = ScanAlpha.SWEEP_3),
                          color,
                          color,
                          color.copy(alpha = ScanAlpha.SWEEP_3),
                          color.copy(alpha = ScanAlpha.SWEEP_2),
                          Color.Transparent)),
          startAngle = 0f,
          sweepAngle = 360f,
          useCenter = false,
          style =
              androidx.compose.ui.graphics.drawscope.Stroke(
                  width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }

    // Secondary counter-rotating arc (faster, thinner)
    Canvas(modifier = Modifier.size(32.dp).rotate(-rotation * 1.8f)) {
      val strokeWidth = 2.5.dp.toPx()

      drawArc(
          brush =
              Brush.sweepGradient(
                  colors =
                      listOf(
                          Color.Transparent,
                          color.copy(alpha = ScanAlpha.SWEEP_4),
                          color.copy(alpha = ScanAlpha.SWEEP_5),
                          color.copy(alpha = ScanAlpha.SWEEP_4),
                          Color.Transparent)),
          startAngle = 0f,
          sweepAngle = 180f,
          useCenter = false,
          style =
              androidx.compose.ui.graphics.drawscope.Stroke(
                  width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }

    // Orbiting dots with trails
    Canvas(modifier = Modifier.size(28.dp).rotate(rotation * 0.7f)) {
      val orbitRadius = size.minDimension / 2.5f

      for (i in 0..5) {
        val angle = (i * 60f) * (Math.PI / 180f).toFloat()
        val x = center.x + orbitRadius * kotlin.math.cos(angle)
        val y = center.y + orbitRadius * kotlin.math.sin(angle)

        val dotSize = if (i % 2 == 0) 2.5.dp.toPx() else 1.5.dp.toPx()
        val alpha = if (i % 2 == 0) ScanAlpha.DOT_MAIN else ScanAlpha.DOT_SECONDARY

        drawCircle(
            color = color.copy(alpha = alpha),
            radius = dotSize,
            center = androidx.compose.ui.geometry.Offset(x, y))
      }
    }

    // Center dot with pulse
    Canvas(modifier = Modifier.size(6.dp)) {
      drawCircle(
          color = color.copy(alpha = ScanAlpha.CENTER_BASE + glowPulse * ScanAlpha.CENTER_PULSE),
          radius = (2.dp.toPx() + glowPulse * 1.dp.toPx()))
    }
  }
}

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

@Composable
fun ScanContent(viewModel: ScannerViewModel, onNavigateBack: () -> Unit = {}) {
  val context = LocalContext.current
  val lifecycle = LocalLifecycleOwner.current
  val uiState by viewModel.state.collectAsState()

  // Load colors from resources
  val colorBackground = colorResource(R.color.scan_background)
  val colorCard = colorResource(R.color.scan_card)
  val colorAccent = colorResource(R.color.scan_accent)
  val colorSuccess = colorResource(R.color.scan_success)
  val colorError = colorResource(R.color.scan_error)
  val colorWarning = colorResource(R.color.scan_warning)
  val colorTextPrimary = colorResource(R.color.scan_text_primary)
  val colorTextSecondary = colorResource(R.color.scan_text_secondary)
  val colorScrim = colorResource(R.color.scan_scrim)
  val colorFrame = colorResource(R.color.scan_frame)

  var showNetworkErrorDialog by remember { mutableStateOf(false) }
  var showSessionExpiredDialog by remember { mutableStateOf(false) }
  var lastScannedQr by remember { mutableStateOf<String?>(null) }

  // Flash screen animation state
  var flashColor by remember { mutableStateOf<Color?>(null) }
  var showFlash by remember { mutableStateOf(false) }

  // Custom success sound - ultra gratifying beep
  val successPlayer = remember {
    try {
      MediaPlayer.create(context, R.raw.success_beep1)
    } catch (_: Exception) {
      Log.w("ScanContent", "Failed to create success player")
      null
    }
  }

  // Custom error sound - negative buzz
  val errorPlayer = remember {
    try {
      MediaPlayer.create(context, R.raw.error_beep1)
    } catch (_: Exception) {
      Log.w("ScanContent", "Failed to create error player")
      null
    }
  }

  DisposableEffect(successPlayer, errorPlayer) {
    onDispose {
      successPlayer?.release()
      errorPlayer?.release()
    }
  }

  val vibrator = remember {
    @Suppress("DEPRECATION")
    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
  }

  LaunchedEffect(viewModel) {
    viewModel.effects.collectLatest { effect ->
      when (effect) {
        is ScannerEffect.Accepted -> {
          // Don't flash if dialogs are showing
          if (!showNetworkErrorDialog && !showSessionExpiredDialog) {
            flashColor = colorSuccess
            showFlash = true
          }

          // Play ultra gratifying success sound
          try {
            successPlayer?.let { mp ->
              mp.seekTo(0)
              mp.start()
            }
                ?: run {
                  // Fallback if sound not available
                  context.getSystemService(Context.AUDIO_SERVICE)?.let { audioManager ->
                    (audioManager as android.media.AudioManager).playSoundEffect(
                        android.media.AudioManager.FX_KEY_CLICK, 1.0f)
                  }
                }
          } catch (_: Exception) {
            Log.w("ScanContent", "Failed to play accept sound")
          }

          vibrator?.let { vib ->
            try {
              vibrateForEffect(vib, effect)
            } catch (_: Exception) {
              Log.w("ScanContent", "Vibration failed")
            }
          }
        }
        is ScannerEffect.Rejected -> {
          // Don't flash if dialogs are showing
          if (!showNetworkErrorDialog && !showSessionExpiredDialog) {
            flashColor = colorError
            showFlash = true
          }

          // Play negative error buzz
          try {
            errorPlayer?.let { mp ->
              mp.seekTo(0)
              mp.start()
            }
                ?: run {
                  // Fallback if sound not available
                  context.getSystemService(Context.AUDIO_SERVICE)?.let { audioManager ->
                    (audioManager as android.media.AudioManager).playSoundEffect(
                        android.media.AudioManager.FX_KEYPRESS_INVALID, 1.0f)
                  }
                }
          } catch (_: Exception) {
            Log.w("ScanContent", "Failed to play reject sound")
          }

          vibrator?.let { vib ->
            try {
              vibrateForEffect(vib, effect)
            } catch (_: Exception) {
              Log.w("ScanContent", "Vibration failed")
            }
          }
        }
        is ScannerEffect.Error -> {
          // Trigger red flash for errors too
          flashColor = colorError
          showFlash = true

          val message = effect.message.lowercase()
          when {
            message.contains("session expired") || message.contains("please login") -> {
              showSessionExpiredDialog = true
            }
            message.contains("network") ||
                message.contains("connection") ||
                message.contains("internet") ||
                message.contains("timeout") -> {
              showNetworkErrorDialog = true
            }
          }

          // Play same error sound as rejected
          try {
            errorPlayer?.let { mp ->
              mp.seekTo(0)
              mp.start()
            }
          } catch (_: Exception) {
            Log.w("ScanContent", "Failed to play error sound")
          }

          vibrator?.let { vib ->
            try {
              vibrateForEffect(vib, effect)
            } catch (_: Exception) {
              Log.w("ScanContent", "Vibration failed")
            }
          }
        }
      }
    }
  }

  if (showSessionExpiredDialog) {
    AlertDialog(
        onDismissRequest = {
          showSessionExpiredDialog = false
          onNavigateBack()
        },
        icon = {
          Icon(imageVector = Icons.Outlined.Error, contentDescription = null, tint = colorError)
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
              color = colorTextSecondary)
        },
        confirmButton = {
          Button(
              onClick = {
                showSessionExpiredDialog = false
                onNavigateBack()
              },
              colors = ButtonDefaults.buttonColors(containerColor = colorError)) {
                Text("OK")
              }
        },
        containerColor = colorCard,
        titleContentColor = colorTextPrimary,
        textContentColor = colorTextSecondary,
        modifier = Modifier.testTag("scan_session_expired_dialog"))
  }

  if (showNetworkErrorDialog) {
    AlertDialog(
        onDismissRequest = {
          showNetworkErrorDialog = false
          viewModel.resetToIdle()
          onNavigateBack()
        },
        icon = {
          Icon(imageVector = Icons.Outlined.Error, contentDescription = null, tint = colorWarning)
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
              color = colorTextSecondary)
        },
        confirmButton = {
          Button(
              onClick = {
                showNetworkErrorDialog = false
                viewModel.resetToIdle()
                lastScannedQr?.let { qr -> viewModel.onQrScanned(qr) }
              },
              colors = ButtonDefaults.buttonColors(containerColor = colorAccent)) {
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
        containerColor = colorCard,
        titleContentColor = colorTextPrimary,
        textContentColor = colorTextSecondary,
        modifier = Modifier.testTag(ScanTestTags.NETWORK_ERROR_DIALOG))
  }

  val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

  val analyzer = remember {
    val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    val scanner = BarcodeScanning.getClient(options)
    var isActive = true
    var lastAnalysisTime = 0L
    val minAnalysisIntervalMs = 300L

    fun analyzeImage(imageProxy: androidx.camera.core.ImageProxy) {
      // Block scanning if dialogs are showing
      if (!isActive || showNetworkErrorDialog || showSessionExpiredDialog) {
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
            if (isActive && !showNetworkErrorDialog && !showSessionExpiredDialog) {
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
      analyzer.third.invoke()
      try {
        analyzer.first.close()
      } catch (_: Exception) {
        Log.w("ScanContent", "Failed to close scanner")
      }
      analysisExecutor.shutdown()
      try {
        if (!analysisExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
          analysisExecutor.shutdownNow()
          if (!analysisExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
            Log.e("ScanContent", "Executor did not terminate")
          }
        }
      } catch (_: InterruptedException) {
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
    } catch (_: Exception) {
      Log.e("ScanContent", "Failed to bind camera")
    }
  }

  DisposableEffect(controller) { onDispose { controller.unbind() } }

  Scaffold(containerColor = colorBackground) { padding ->
    Box(modifier = Modifier.fillMaxSize().padding(padding).testTag(ScanTestTags.SCREEN)) {
      AndroidView(
          factory = { ctx ->
            PreviewView(ctx).apply {
              this.controller = controller
              this.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
              this.scaleType = PreviewView.ScaleType.FILL_CENTER
            }
          },
          modifier = Modifier.fillMaxSize().testTag(ScanTestTags.CAMERA))

      Box(
          modifier =
              Modifier.fillMaxSize()
                  .background(
                      Brush.verticalGradient(
                          listOf(Color.Transparent, Color(0x33000000), colorScrim))))

      IconButton(
          onClick = onNavigateBack,
          modifier =
              Modifier.align(Alignment.TopStart)
                  .padding(16.dp)
                  .size(48.dp)
                  .background(colorCard.copy(alpha = ScanAlpha.CARD), CircleShape)
                  .testTag(ScanTestTags.BACK_BUTTON)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(24.dp))
          }

      ScanningFrame(
          uiState = uiState,
          colorSuccess = colorSuccess,
          colorError = colorError,
          colorWarning = colorWarning,
          colorFrame = colorFrame)

      AnimatedVisibility(
          visible =
              uiState.validated > 0 ||
                  uiState.status != ScannerUiState.Status.IDLE ||
                  uiState.eventTitle != null,
          enter = fadeIn() + slideInVertically(),
          exit = fadeOut() + slideOutVertically(),
          modifier = Modifier.align(Alignment.TopCenter)) {
            TopStatsCard(
                validated = uiState.validated,
                eventTitle = uiState.eventTitle,
                colorCard = colorCard,
                colorAccent = colorAccent,
                colorTextPrimary = colorTextPrimary,
                colorTextSecondary = colorTextSecondary)
          }

      if (!showNetworkErrorDialog && !showSessionExpiredDialog) {
        ScanHud(
            uiState = uiState,
            colorCard = colorCard,
            colorAccent = colorAccent,
            colorSuccess = colorSuccess,
            colorError = colorError,
            colorWarning = colorWarning,
            colorTextPrimary = colorTextPrimary,
            colorTextSecondary = colorTextSecondary)
      }

      // Flash screen overlay
      FlashOverlay(
          showFlash = showFlash, color = flashColor, onFlashComplete = { showFlash = false })
    }
  }
}

@SuppressLint("MissingPermission")
private fun vibrateForEffect(vibrator: Vibrator, effect: ScannerEffect) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    when (effect) {
      is ScannerEffect.Accepted -> vibrator.vibrate(VibrationEffect.createOneShot(100, 128))
      is ScannerEffect.Rejected ->
          vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 100), -1))
      is ScannerEffect.Error -> vibrator.vibrate(VibrationEffect.createOneShot(400, 255))
    }
  } else {
    @Suppress("DEPRECATION")
    vibrator.vibrate(
        when (effect) {
          is ScannerEffect.Accepted -> 100L
          is ScannerEffect.Rejected -> 300L
          is ScannerEffect.Error -> 400L
        })
  }
}

@Composable
private fun FlashOverlay(showFlash: Boolean, color: Color?, onFlashComplete: () -> Unit) {
  val alpha by
      animateFloatAsState(
          targetValue = if (showFlash) ScanAlpha.FLASH else 0f,
          animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
          finishedListener = { if (!showFlash) onFlashComplete() },
          label = "flash_alpha")

  LaunchedEffect(showFlash) {
    if (showFlash) {
      delay(200)
      onFlashComplete()
    }
  }

  if (color != null) {
    Box(modifier = Modifier.fillMaxSize().background(color.copy(alpha = alpha)))
  }
}

@Composable
private fun BoxScope.ScanningFrame(
    uiState: ScannerUiState,
    colorSuccess: Color,
    colorError: Color,
    colorWarning: Color,
    colorFrame: Color
) {
  val frameColor by
      animateColorAsState(
          targetValue =
              when (uiState.status) {
                ScannerUiState.Status.ACCEPTED -> colorSuccess
                ScannerUiState.Status.REJECTED -> colorError
                ScannerUiState.Status.ERROR -> colorWarning
                else -> colorFrame
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
                          .background(frameColor.copy(alpha = ScanAlpha.STATUS_BG)),
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

@Composable
private fun BoxScope.FrameCorners(color: Color) {
  val cornerSize = 40.dp
  val cornerWidth = 4.dp

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

@Composable
internal fun TopStatsCard(
    validated: Int,
    eventTitle: String?,
    colorCard: Color,
    colorAccent: Color,
    colorTextPrimary: Color,
    colorTextSecondary: Color
) {
  Surface(
      color = colorCard.copy(alpha = ScanAlpha.CARD_SURFACE),
      shape = RoundedCornerShape(16.dp),
      tonalElevation = 4.dp,
      modifier = Modifier.padding(top = 24.dp).testTag(ScanTestTags.STATS_CARD)) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
              if (eventTitle != null) {
                Text(
                    text = eventTitle,
                    color = colorTextPrimary,
                    style =
                        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
              }

              Text(
                  text = validated.toString(),
                  color = colorAccent,
                  style =
                      MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
              Text(
                  text = "Validated",
                  color = colorTextSecondary,
                  style = MaterialTheme.typography.bodySmall)
            }
      }
}

@Composable
private fun BoxScope.ScanHud(
    uiState: ScannerUiState,
    colorCard: Color,
    colorAccent: Color,
    colorSuccess: Color,
    colorError: Color,
    colorWarning: Color,
    colorTextPrimary: Color,
    colorTextSecondary: Color
) {
  val backgroundColor by
      animateColorAsState(
          targetValue =
              when (uiState.status) {
                ScannerUiState.Status.ACCEPTED ->
                    colorSuccess.copy(alpha = ScanAlpha.STATUS_SUCCESS)
                ScannerUiState.Status.REJECTED -> colorError.copy(alpha = ScanAlpha.STATUS_ERROR)
                ScannerUiState.Status.ERROR -> colorWarning.copy(alpha = ScanAlpha.STATUS_WARNING)
                else -> Color.Transparent
              },
          animationSpec = tween(300),
          label = "bg_color")

  Surface(
      color = colorCard.copy(alpha = ScanAlpha.CARD_SURFACE),
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
                            color = colorTextPrimary,
                            style =
                                MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold),
                            modifier = Modifier.testTag(ScanTestTags.MESSAGE))

                        if (uiState.lastScannedUserName != null &&
                            uiState.status == ScannerUiState.Status.ACCEPTED) {
                          Spacer(Modifier.height(4.dp))
                          Text(
                              text = uiState.lastScannedUserName,
                              color = colorTextPrimary,
                              style =
                                  MaterialTheme.typography.bodyLarge.copy(
                                      fontWeight = FontWeight.Medium))
                        }

                        if (uiState.status == ScannerUiState.Status.IDLE) {
                          Spacer(Modifier.height(6.dp))
                          Text(
                              text = "Position the QR code within the frame",
                              color = colorTextSecondary,
                              style = MaterialTheme.typography.bodySmall)
                        }
                      }

                      AnimatedVisibility(
                          visible = uiState.isProcessing,
                          enter =
                              fadeIn(animationSpec = tween(150)) +
                                  scaleIn(animationSpec = tween(150)),
                          exit =
                              fadeOut(animationSpec = tween(150)) +
                                  scaleOut(animationSpec = tween(150))) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                              Spacer(Modifier.width(16.dp))
                              RotatingSpinner(
                                  modifier = Modifier.testTag(ScanTestTags.PROGRESS),
                                  color = colorAccent)
                            }
                          }
                    }
              }
        }
      }
}

@Composable
internal fun PermissionDeniedScreen() {
  val colorBackground = colorResource(R.color.scan_background)
  val colorTextPrimary = colorResource(R.color.scan_text_primary)
  val colorTextSecondary = colorResource(R.color.scan_text_secondary)

  Box(
      modifier =
          Modifier.fillMaxSize().background(colorBackground).testTag(ScanTestTags.PERMISSION),
      contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)) {
              Icon(
                  imageVector = Icons.Filled.QrCodeScanner,
                  contentDescription = "QR code scanner required",
                  tint = colorTextSecondary,
                  modifier = Modifier.size(80.dp))
              Spacer(Modifier.height(24.dp))
              Text(
                  text = "Camera Access Required",
                  color = colorTextPrimary,
                  style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                  textAlign = TextAlign.Center)
              Spacer(Modifier.height(12.dp))
              Text(
                  text =
                      "To scan tickets, we need access to your camera. Please grant permission to continue.",
                  color = colorTextSecondary,
                  style = MaterialTheme.typography.bodyMedium,
                  textAlign = TextAlign.Center)
            }
      }
}

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

@Composable
internal fun PreviewHudContainer(state: ScannerUiState) {
  val colorBackground = colorResource(R.color.scan_background)
  val colorCard = colorResource(R.color.scan_card)
  val colorAccent = colorResource(R.color.scan_accent)
  val colorSuccess = colorResource(R.color.scan_success)
  val colorError = colorResource(R.color.scan_error)
  val colorWarning = colorResource(R.color.scan_warning)
  val colorTextPrimary = colorResource(R.color.scan_text_primary)
  val colorTextSecondary = colorResource(R.color.scan_text_secondary)
  val colorFrame = colorResource(R.color.scan_frame)

  Box(modifier = Modifier.fillMaxSize().background(colorBackground)) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(
                    Brush.verticalGradient(colors = listOf(Color(0xFF0F0F10), Color(0xFF171718)))),
        contentAlignment = Alignment.Center) {
          Text(
              text = "Camera Preview",
              color = colorTextSecondary,
              modifier = Modifier.alpha(ScanAlpha.PREVIEW))
        }

    Box(
        modifier =
            Modifier.align(Alignment.Center)
                .size(280.dp)
                .border(
                    width = 3.dp,
                    color =
                        when (state.status) {
                          ScannerUiState.Status.ACCEPTED -> colorSuccess
                          ScannerUiState.Status.REJECTED -> colorError
                          ScannerUiState.Status.ERROR -> colorWarning
                          else -> colorFrame
                        },
                    shape = RoundedCornerShape(24.dp)))

    if (state.validated > 0 || state.eventTitle != null) {
      TopStatsCard(
          validated = state.validated,
          eventTitle = state.eventTitle,
          colorCard = colorCard,
          colorAccent = colorAccent,
          colorTextPrimary = colorTextPrimary,
          colorTextSecondary = colorTextSecondary)
    }

    ScanHud(
        uiState = state,
        colorCard = colorCard,
        colorAccent = colorAccent,
        colorSuccess = colorSuccess,
        colorError = colorError,
        colorWarning = colorWarning,
        colorTextPrimary = colorTextPrimary,
        colorTextSecondary = colorTextSecondary)
  }
}
