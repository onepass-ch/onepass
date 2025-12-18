package ch.onepass.onepass.ui.profile.accountsettings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    viewModel: AccountSettingsViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onAccountDeleted: () -> Unit
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val state by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  var showDeleteConfirmation by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) { viewModel.checkPermissions(context) }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.checkPermissions(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  val notificationLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission(),
          onResult = { /* ViewModel updates on resume */})
  val locationLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission(),
          onResult = { /* ViewModel updates on resume */})
  val cameraLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission(),
          onResult = { /* ViewModel updates on resume */})

  fun handlePermissionToggle(
      permission: String,
      isCurrentlyEnabled: Boolean,
      launcher: ActivityResultLauncher<String>
  ) {
    if (isCurrentlyEnabled) {
      viewModel.openAppSettings(context)
    } else {
      launcher.launch(permission)
    }
  }

  LaunchedEffect(state.isAccountDeleted) {
    if (state.isAccountDeleted) {
      onAccountDeleted()
    }
  }

  LaunchedEffect(state.error) {
    state.error?.let { error ->
      snackbarHostState.showSnackbar(error)
      viewModel.clearError()
    }
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Text("Account Settings", color = OnBackground, fontWeight = FontWeight.SemiBold)
            },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OnBackground)
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background))
      },
      containerColor = Background,
      snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
          Column(
              modifier =
                  Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {

                // --- SECTION: PERMISSIONS ---
                SettingsSectionTitle("PERMISSIONS")

                // Notifications (Only Tiramisu+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  SwitchSettingItem(
                      title = "Push Notifications",
                      subtitle = "Receive updates about your events",
                      icon = Icons.Outlined.Notifications,
                      checked = state.notificationsEnabled,
                      onCheckedChange = {
                        handlePermissionToggle(
                            Manifest.permission.POST_NOTIFICATIONS,
                            state.notificationsEnabled,
                            notificationLauncher)
                      })
                }

                // Location
                SwitchSettingItem(
                    title = "Location Access",
                    subtitle = "See events near you on the map",
                    icon = Icons.Outlined.LocationOn,
                    checked = state.locationEnabled,
                    onCheckedChange = {
                      handlePermissionToggle(
                          Manifest.permission.ACCESS_FINE_LOCATION,
                          state.locationEnabled,
                          locationLauncher)
                    })

                // Camera
                SwitchSettingItem(
                    title = "Camera Access",
                    subtitle = "Required for scanning tickets",
                    icon = Icons.Outlined.CameraAlt,
                    checked = state.cameraEnabled,
                    onCheckedChange = {
                      handlePermissionToggle(
                          Manifest.permission.CAMERA, state.cameraEnabled, cameraLauncher)
                    })

                Spacer(modifier = Modifier.height(32.dp))

                // --- SECTION: PRIVACY ---
                SettingsSectionTitle("PRIVACY")

                SwitchSettingItem(
                    title = "Show Email on Profile",
                    subtitle = "Make your email visible to other users",
                    icon = Icons.Outlined.Visibility,
                    checked = state.showEmail,
                    onCheckedChange = { viewModel.toggleShowEmail(it) })

                SwitchSettingItem(
                    title = "Data Usage",
                    subtitle = "Allow usage analysis to improve the app",
                    icon = Icons.Outlined.Security,
                    checked = state.analyticsEnabled,
                    onCheckedChange = { isChecked ->
                      viewModel.toggleAnalytics(context, isChecked)
                    })

                Spacer(modifier = Modifier.height(40.dp))

                // --- SECTION: DANGER ZONE ---
                SettingsSectionTitle("DANGER ZONE")

                Surface(
                    color = Error.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()) {
                      Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "DELETE ACCOUNT",
                            color = Error,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text =
                                "Permanently remove your account and all associated data. This action cannot be undone.",
                            color = OnSurface.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showDeleteConfirmation = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Error),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()) {
                              Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(18.dp))
                              Spacer(Modifier.width(8.dp))
                              Text("Delete Account")
                            }
                      }
                    }
              }

          // Loading Overlay
          if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Background.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center) {
                  CircularProgressIndicator(color = Error)
                }
          }
        }
      }

  // Confirmation Dialog
  if (showDeleteConfirmation) {
    AlertDialog(
        onDismissRequest = { showDeleteConfirmation = false },
        title = { Text("Are you sure?", fontWeight = FontWeight.Bold) },
        text = {
          Text(
              "You are about to permanently delete your account. You will lose all your tickets, events, and history.")
        },
        confirmButton = {
          TextButton(
              onClick = {
                showDeleteConfirmation = false
                viewModel.deleteAccount()
              },
              colors = ButtonDefaults.textButtonColors(contentColor = Error)) {
                Text("Delete Forever", fontWeight = FontWeight.Bold)
              }
        },
        dismissButton = {
          TextButton(
              onClick = { showDeleteConfirmation = false },
              colors = ButtonDefaults.textButtonColors(contentColor = OnBackground)) {
                Text("Cancel")
              }
        },
        containerColor = Surface,
        titleContentColor = OnBackground,
        textContentColor = OnSurface)
  }
}

// --- REUSABLE COMPONENTS ---

@Composable
fun SwitchSettingItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OnSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(text = title, color = OnBackground, style = MaterialTheme.typography.bodyLarge)
          if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = OnSurface.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall)
          }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = OnBackground,
                    checkedTrackColor = Primary,
                    uncheckedThumbColor = OnSurface.copy(alpha = 0.4f),
                    uncheckedTrackColor = Surface))
      }
}

@Composable
fun SettingsSectionTitle(text: String) {
  Text(
      text = text.uppercase(),
      style = MaterialTheme.typography.labelMedium,
      color = OnSurface.copy(alpha = 0.6f),
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 8.dp, top = 8.dp))
}
