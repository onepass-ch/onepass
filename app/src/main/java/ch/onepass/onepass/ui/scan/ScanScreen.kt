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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

private object ScanColors {
  val Background = Color(0xFF111111)
  val Card = Color(0xFF1B1B1B)
  val Accent = Color(0xFF9C6BFF)
  val Success = Color(0xFF4CAF50)
  val Error = Color(0xFFD33A2C)
  val Warning = Color(0xFFFF9800)
  val TextPrimary = Color.White
  val TextSecondary = Color(0xFFB0B0B0)
  val Scrim = Color(0xAA000000)
  val ScanFrame = Color(0xFF9C6BFF)
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
    } catch (e: Exception) {
      Log.w("ScanContent", "Failed to create success player", e)
      null
    }
  }

  // Custom error sound - negative buzz
  val errorPlayer = remember {
    try {
      MediaPlayer.create(context, R.raw.error_beep1)
    } catch (e: Exception) {
      Log.w("ScanContent", "Failed to create error player", e)
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
            flashColor = ScanColors.Success
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
          } catch (e: Exception) {
            Log.w("ScanContent", "Failed to play accept sound", e)
          }

          vibrator?.let { vib ->
            try {
              vibrateForEffect(vib, effect)
            } catch (e: Exception) {
              Log.w("ScanContent", "Vibration failed", e)
            }
          }
        }
        is ScannerEffect.Rejected -> {
          // Don't flash if dialogs are showing
          if (!showNetworkErrorDialog && !showSessionExpiredDialog) {
            flashColor = ScanColors.Error
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
          } catch (e: Exception) {
            Log.w("ScanContent", "Failed to play reject sound", e)
          }

          vibrator?.let { vib ->
            try {
              vibrateForEffect(vib, effect)
            } catch (e: Exception) {
              Log.w("ScanContent", "Vibration failed", e)
            }
          }
        }
        is ScannerEffect.Error -> {
          // Trigger red flash for errors too
          flashColor = ScanColors.Error
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
            else -> {}
          }

          // Play same error sound as rejected
          try {
            errorPlayer?.let { mp ->
              mp.seekTo(0)
              mp.start()
            }
          } catch (e: Exception) {
            Log.w("ScanContent", "Failed to play error sound", e)
          }

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
              tint = ScanColors.Error)
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
              color = ScanColors.TextSecondary)
        },
        confirmButton = {
          Button(
              onClick = {
                showSessionExpiredDialog = false
                onNavigateBack()
              },
              colors = ButtonDefaults.buttonColors(containerColor = ScanColors.Error)) {
                Text("OK")
              }
        },
        containerColor = ScanColors.Card,
        titleContentColor = ScanColors.TextPrimary,
        textContentColor = ScanColors.TextSecondary,
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
          Icon(
              imageVector = Icons.Outlined.Error,
              contentDescription = null,
              tint = ScanColors.Warning)
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
              color = ScanColors.TextSecondary)
        },
        confirmButton = {
          Button(
              onClick = {
                showNetworkErrorDialog = false
                viewModel.resetToIdle()
                lastScannedQr?.let { qr -> viewModel.onQrScanned(qr) }
              },
              colors = ButtonDefaults.buttonColors(containerColor = ScanColors.Accent)) {
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
        containerColor = ScanColors.Card,
        titleContentColor = ScanColors.TextPrimary,
        textContentColor = ScanColors.TextSecondary,
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

  Scaffold(containerColor = ScanColors.Background) { padding ->
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
                          listOf(Color.Transparent, Color(0x33000000), ScanColors.Scrim))))

      IconButton(
          onClick = onNavigateBack,
          modifier =
              Modifier.align(Alignment.TopStart)
                  .padding(16.dp)
                  .size(48.dp)
                  .background(ScanColors.Card.copy(alpha = 0.7f), CircleShape)
                  .testTag(ScanTestTags.BACK_BUTTON)) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(24.dp))
          }

      ScanningFrame(uiState = uiState)

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

      if (!showNetworkErrorDialog && !showSessionExpiredDialog) {
        ScanHud(uiState = uiState)
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
          // Stronger and longer vibration for rejected
          vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 100), -1))
      is ScannerEffect.Error ->
          // Strong and long vibration for errors
          vibrator.vibrate(VibrationEffect.createOneShot(400, 255))
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
          targetValue = if (showFlash) 0.4f else 0f,
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
    Box(
        modifier = Modifier.fillMaxSize().background(color.copy(alpha = alpha))
        // Removed pointerInput - don't block touches
        )
  }
}

@Composable
private fun BoxScope.ScanningFrame(uiState: ScannerUiState) {
  val frameColor by
      animateColorAsState(
          targetValue =
              when (uiState.status) {
                ScannerUiState.Status.ACCEPTED -> ScanColors.Success
                ScannerUiState.Status.REJECTED -> ScanColors.Error
                ScannerUiState.Status.ERROR -> ScanColors.Warning
                else -> ScanColors.ScanFrame
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
internal fun TopStatsCard(validated: Int, eventTitle: String?) {
  Surface(
      color = ScanColors.Card.copy(alpha = 0.95f),
      shape = RoundedCornerShape(16.dp),
      tonalElevation = 4.dp,
      modifier = Modifier.padding(top = 24.dp).testTag(ScanTestTags.STATS_CARD)) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
              if (eventTitle != null) {
                Text(
                    text = eventTitle,
                    color = ScanColors.TextPrimary,
                    style =
                        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
              }

              Text(
                  text = validated.toString(),
                  color = ScanColors.Accent,
                  style =
                      MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
              Text(
                  text = "Validated",
                  color = ScanColors.TextSecondary,
                  style = MaterialTheme.typography.bodySmall)
            }
      }
}

@Composable
private fun BoxScope.ScanHud(uiState: ScannerUiState) {
  val backgroundColor by
      animateColorAsState(
          targetValue =
              when (uiState.status) {
                ScannerUiState.Status.ACCEPTED -> ScanColors.Success.copy(alpha = 0.15f)
                ScannerUiState.Status.REJECTED -> ScanColors.Error.copy(alpha = 0.15f)
                ScannerUiState.Status.ERROR -> ScanColors.Warning.copy(alpha = 0.15f)
                else -> Color.Transparent
              },
          animationSpec = tween(300),
          label = "bg_color")

  Surface(
      color = ScanColors.Card.copy(alpha = 0.95f),
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
                            color = ScanColors.TextPrimary,
                            style =
                                MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold),
                            modifier = Modifier.testTag(ScanTestTags.MESSAGE))

                        if (uiState.lastScannedUserName != null &&
                            uiState.status == ScannerUiState.Status.ACCEPTED) {
                          Spacer(Modifier.height(4.dp))
                          Text(
                              text = uiState.lastScannedUserName,
                              color = ScanColors.TextPrimary,
                              style =
                                  MaterialTheme.typography.bodyLarge.copy(
                                      fontWeight = FontWeight.Medium))
                        }

                        if (uiState.lastTicketId != null) {
                          Spacer(Modifier.height(6.dp))
                          Text(
                              text = "Ticket ${uiState.lastTicketId}",
                              color = ScanColors.Accent,
                              style =
                                  MaterialTheme.typography.bodyMedium.copy(
                                      fontWeight = FontWeight.SemiBold))
                        }

                        if (uiState.status == ScannerUiState.Status.IDLE) {
                          Spacer(Modifier.height(6.dp))
                          Text(
                              text = "Position the QR code within the frame",
                              color = ScanColors.TextSecondary,
                              style = MaterialTheme.typography.bodySmall)
                        }
                      }

                      if (uiState.isProcessing) {
                        Spacer(Modifier.width(16.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp).testTag(ScanTestTags.PROGRESS),
                            color = ScanColors.Accent,
                            strokeWidth = 3.dp)
                      }
                    }
              }
        }
      }
}

@Composable
internal fun PermissionDeniedScreen() {
  Box(
      modifier =
          Modifier.fillMaxSize().background(ScanColors.Background).testTag(ScanTestTags.PERMISSION),
      contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)) {
              Icon(
                  imageVector = Icons.Filled.QrCodeScanner,
                  contentDescription = "QR code scanner required",
                  tint = ScanColors.TextSecondary,
                  modifier = Modifier.size(80.dp))
              Spacer(Modifier.height(24.dp))
              Text(
                  text = "Camera Access Required",
                  color = ScanColors.TextPrimary,
                  style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                  textAlign = TextAlign.Center)
              Spacer(Modifier.height(12.dp))
              Text(
                  text =
                      "To scan tickets, we need access to your camera. Please grant permission to continue.",
                  color = ScanColors.TextSecondary,
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

@Composable
internal fun PreviewHudContainer(state: ScannerUiState) {
  Box(modifier = Modifier.fillMaxSize().background(ScanColors.Background)) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(
                    Brush.verticalGradient(colors = listOf(Color(0xFF0F0F10), Color(0xFF171718)))),
        contentAlignment = Alignment.Center) {
          Text(
              text = "Camera Preview",
              color = ScanColors.TextSecondary,
              modifier = Modifier.alpha(0.5f))
        }

    Box(
        modifier =
            Modifier.align(Alignment.Center)
                .size(280.dp)
                .border(
                    width = 3.dp,
                    color =
                        when (state.status) {
                          ScannerUiState.Status.ACCEPTED -> ScanColors.Success
                          ScannerUiState.Status.REJECTED -> ScanColors.Error
                          ScannerUiState.Status.ERROR -> ScanColors.Warning
                          else -> ScanColors.ScanFrame
                        },
                    shape = RoundedCornerShape(24.dp)))

    if (state.validated > 0 || state.eventTitle != null) {
      TopStatsCard(validated = state.validated, eventTitle = state.eventTitle)
    }

    ScanHud(uiState = state)
  }
}
