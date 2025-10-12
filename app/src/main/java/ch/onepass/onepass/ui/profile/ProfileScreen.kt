package ch.onepass.onepass.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest

private val Background = Color(0xFF111111)
private val Surface = Color(0xFF1B1B1B)
private val Accent = Color(0xFF9C6BFF)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFB0B0B0)
private val Divider = Color(0xFF2A2A2A)


@Composable
fun ProfileScreen(
  viewModel: ProfileViewModel,
  onEffect: (ProfileEffect) -> Unit
) {
  val state by viewModel.state.collectAsState()

  LaunchedEffect(Unit) { viewModel.effects.collectLatest(onEffect) }

  DarkProfileContent(
    state = state,
    onCreateEvent = viewModel::onCreateEventClicked,
    onAccountSettings = viewModel::onAccountSettings,
    onPaymentMethods = viewModel::onPaymentMethods,
    onHelp = viewModel::onHelp,
    onSignOut = viewModel::onSignOut
  )
}


@Composable
private fun DarkProfileContent(
  state: ProfileUiState,
  onCreateEvent: () -> Unit,
  onAccountSettings: () -> Unit,
  onPaymentMethods: () -> Unit,
  onHelp: () -> Unit,
  onSignOut: () -> Unit
) {
  // Display loading spinner before data is available
  if (state.loading) {
    Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
      CircularProgressIndicator(color = Accent)
    }
    return
  }

  Scaffold(containerColor = Background) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
      // --- Header: avatar, name, and email ---
      HeaderBlockDark(
        initials = state.initials,
        name = state.displayName,
        email = state.email
      )

      Spacer(Modifier.height(12.dp))

      // --- User statistics row (events, upcoming, saved) ---
      StatsRow(stats = state.stats)

      Spacer(Modifier.height(12.dp))

      // --- Organizer section header ---
      Text(
        text = "ORGANIZER SETTINGS",
        color = TextSecondary,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
      )

      Spacer(Modifier.height(12.dp))

      // --- Organizer section card ---
      OrganizerCardDark(
        isOrganizer = state.isOrganizer,
        onCreateEvent = onCreateEvent
      )

      Spacer(Modifier.height(12.dp))

      // --- Divider before settings ---
      HorizontalDivider(Modifier.alpha(0.2f), thickness = 1.dp, color = Divider)

      Spacer(Modifier.height(8.dp))

      // --- Settings list items ---
      SettingsItemDark(Icons.Outlined.AccountCircle, "Account Settings", TextPrimary, onAccountSettings)
      SettingsItemDark(Icons.Outlined.Settings, "Payment Methods", TextPrimary, onPaymentMethods)
      SettingsItemDark(Icons.Outlined.Info, "Help & Support", TextPrimary, onHelp)
      SettingsItemDark(Icons.AutoMirrored.Outlined.ExitToApp, "Sign Out", Color(0xFFD33A2C), onSignOut)
    }
  }
}

/**
 * Displays three stat cards horizontally.
 */
@Composable
private fun StatsRow(stats: ProfileStats) {
  Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    StatCardDark(value = stats.events, label = "Events", modifier = Modifier.weight(1f))
    StatCardDark(value = stats.upcoming, label = "Upcoming", modifier = Modifier.weight(1f))
    StatCardDark(value = stats.saved, label = "Saved", modifier = Modifier.weight(1f))
  }
}

/**
 * Header with avatar placeholder, name, and email.
 */
@Composable
private fun HeaderBlockDark(
  initials: String,
  name: String,
  email: String
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    // Circular avatar placeholder (with initials)
    Box(
      modifier = Modifier
        .size(72.dp)
        .clip(CircleShape)
        .background(Color(0xFF2B2B2B)),
      contentAlignment = Alignment.Center
    ) {
      Text(initials, color = Color.White, fontWeight = FontWeight.Bold)
    }

    Spacer(Modifier.width(16.dp))

    // User info (name + email)
    Column(Modifier.weight(1f)) {
      Text(
        text = name,
        color = TextPrimary,
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(text = email, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
  }
}

/**
 * Single stat card: shows a number and a label.
 */
@Composable
private fun StatCardDark(
  value: Int,
  label: String,
  modifier: Modifier = Modifier
) {
  Surface(
    color = Surface,
    shape = RoundedCornerShape(16.dp),
    tonalElevation = 0.dp,
    modifier = modifier.height(96.dp)
  ) {
    Column(
      modifier = Modifier
        .padding(vertical = 18.dp)
        .fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(value.toString(), color = Accent, style = MaterialTheme.typography.headlineSmall)
      Text(label, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
  }
}

/**
 * Card for the organizer section.
 * If already an organizer → shows "Dashboard".
 * Otherwise → shows "Become an Organizer" with a CTA button.
 */
@Composable
private fun OrganizerCardDark(
  isOrganizer: Boolean,
  onCreateEvent: () -> Unit
) {
  Surface(color = Surface, shape = RoundedCornerShape(16.dp)) {
    Column(Modifier.padding(16.dp)) {
      Text(
        text = if (isOrganizer) "Organizer Dashboard" else "Become an Organizer",
        color = TextPrimary,
        style = MaterialTheme.typography.titleMedium
      )

      Spacer(Modifier.height(8.dp))

      Text(
        text = if (isOrganizer)
          "Create and manage your events."
        else
          "Create epic events, build your community, and grow your audience.",
        color = TextSecondary,
        style = MaterialTheme.typography.bodyMedium
      )

      Spacer(Modifier.height(16.dp))

      Button(
        onClick = onCreateEvent,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Accent)
      ) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = Color.White)
        Spacer(Modifier.width(8.dp))
        Text("Start Creating Events", color = Color.White)
      }
    }
  }
}

/**
 * List item used for settings actions (account, payments, help, sign-out).
 */
@Composable
private fun SettingsItemDark(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  titleColor: Color,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 52.dp)
      .clickable(onClick = onClick)
      .padding(horizontal = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(icon, contentDescription = null, tint = Color(0xFF9B9B9B))
    Spacer(Modifier.width(16.dp))
    Text(title, color = titleColor, style = MaterialTheme.typography.bodyLarge)
  }
}

