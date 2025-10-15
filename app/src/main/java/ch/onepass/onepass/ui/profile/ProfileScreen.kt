package ch.onepass.onepass.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest

object ProfileTestTags {
  const val SCREEN = "profile_screen"
  const val LOADING = "profile_loading"
  const val HEADER = "profile_header"
  const val HEADER_INITIALS = "profile_header_initials"
  const val HEADER_NAME = "profile_header_name"
  const val HEADER_EMAIL = "profile_header_email"
  const val STATS_ROW = "profile_stats_row"
  const val STAT_EVENTS = "profile_stat_events"
  const val STAT_UPCOMING = "profile_stat_upcoming"
  const val STAT_SAVED = "profile_stat_saved"
  const val ORG_SECTION_TITLE = "profile_org_section_title"
  const val ORG_CARD = "profile_org_card"
  const val ORG_CTA = "profile_org_cta"
  const val SETTINGS_ACCOUNT = "profile_settings_account"
  const val SETTINGS_PAYMENTS = "profile_settings_payments"
  const val SETTINGS_HELP = "profile_settings_help"
  const val SETTINGS_SIGN_OUT = "profile_settings_sign_out"
}

// Centralized colors to avoid name clash with Material3.Surface
private object ProfileColors {
  val Background = Color(0xFF111111)
  val Card = Color(0xFF1B1B1B)
  val Accent = Color(0xFF9C6BFF)
  val TextPrimary = Color.White
  val TextSecondary = Color(0xFFB0B0B0)
  val Divider = Color(0xFF2A2A2A)
}

@Composable
fun ProfileScreen(
  viewModel: ProfileViewModel,
  onEffect: (ProfileEffect) -> Unit
) {
  val state by viewModel.state.collectAsState()
  LaunchedEffect(viewModel) { viewModel.effects.collectLatest(onEffect) }

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
  if (state.loading) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(ProfileColors.Background)
        .testTag(ProfileTestTags.LOADING),
      contentAlignment = Alignment.Center
    ) {
      CircularProgressIndicator(color = ProfileColors.Accent)
    }
    return
  }

  Scaffold(containerColor = ProfileColors.Background) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 12.dp)
        .testTag(ProfileTestTags.SCREEN)
    ) {
      HeaderBlockDark(
        initials = state.initials,
        name = state.displayName,
        email = state.email
      )

      Spacer(Modifier.height(12.dp))

      StatsRow(stats = state.stats)

      Spacer(Modifier.height(12.dp))

      Text(
        text =  "ORGANIZER SETTINGS",
        color = ProfileColors.TextSecondary,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.testTag(ProfileTestTags.ORG_SECTION_TITLE)
      )

      Spacer(Modifier.height(12.dp))

      OrganizerCardDark(
        isOrganizer = state.isOrganizer,
        onCreateEvent = onCreateEvent
      )

      Spacer(Modifier.height(12.dp))

      HorizontalDivider(
        modifier = Modifier.alpha(0.2f),
        thickness = 1.dp,
        color = ProfileColors.Divider
      )

      Spacer(Modifier.height(8.dp))

      SettingsItemDark(
        icon = Icons.Outlined.AccountCircle,
        title = "Account Settings",
        titleColor = ProfileColors.TextPrimary,
        onClick = onAccountSettings,
        testTag = ProfileTestTags.SETTINGS_ACCOUNT
      )
      SettingsItemDark(
        icon = Icons.Outlined.Settings,
        title = "Payment Methods",
        titleColor = ProfileColors.TextPrimary,
        onClick = onPaymentMethods,
        testTag = ProfileTestTags.SETTINGS_PAYMENTS
      )
      SettingsItemDark(
        icon = Icons.Outlined.Info,
        title = "Help & Support",
        titleColor = ProfileColors.TextPrimary,
        onClick = onHelp,
        testTag = ProfileTestTags.SETTINGS_HELP
      )
      SettingsItemDark(
        icon = Icons.AutoMirrored.Outlined.ExitToApp,
        title = "Sign Out",
        titleColor = Color(0xFFD33A2C),
        onClick = onSignOut,
        testTag = ProfileTestTags.SETTINGS_SIGN_OUT
      )
    }
  }
}

/** Displays three stat cards horizontally. */
@Composable
private fun StatsRow(stats: ProfileStats) {
  Row(
    Modifier
      .fillMaxWidth()
      .testTag(ProfileTestTags.STATS_ROW),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    StatCardDark(
      value = stats.events,
      label = "Events",
      modifier = Modifier.weight(1f).testTag(ProfileTestTags.STAT_EVENTS)
    )
    StatCardDark(
      value = stats.upcoming,
      label = "Upcoming",
      modifier = Modifier.weight(1f).testTag(ProfileTestTags.STAT_UPCOMING)
    )
    StatCardDark(
      value = stats.saved,
      label = "Saved",
      modifier = Modifier.weight(1f).testTag(ProfileTestTags.STAT_SAVED)
    )
  }
}

/** Header with avatar placeholder, name, and email. */
@Composable
private fun HeaderBlockDark(
  initials: String,
  name: String,
  email: String
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.testTag(ProfileTestTags.HEADER)
  ) {
    Box(
      modifier = Modifier
        .size(72.dp)
        .clip(CircleShape)
        .background(Color(0xFF2B2B2B))
        .testTag(ProfileTestTags.HEADER_INITIALS),
      contentAlignment = Alignment.Center
    ) {
      Text(initials, color = Color.White, fontWeight = FontWeight.Bold)
    }

    Spacer(Modifier.width(16.dp))

    Column(Modifier.weight(1f)) {
      Text(
        text = name,
        color = ProfileColors.TextPrimary,
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.testTag(ProfileTestTags.HEADER_NAME)
      )
      Text(
        text = email,
        color = ProfileColors.TextSecondary,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.testTag(ProfileTestTags.HEADER_EMAIL)
      )
    }
  }
}

/** Single stat card: shows a number and a label. */
@Composable
private fun StatCardDark(
  value: Int,
  label: String,
  modifier: Modifier = Modifier
) {
  Surface(
    color = ProfileColors.Card,
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
      Text(value.toString(), color = ProfileColors.Accent, style = MaterialTheme.typography.headlineSmall)
      Text(label, color = ProfileColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
  }
}

/** Card for the organizer section. */
@Composable
private fun OrganizerCardDark(
  isOrganizer: Boolean,
  onCreateEvent: () -> Unit
) {
  Surface(color = ProfileColors.Card, shape = RoundedCornerShape(16.dp)) {
    Column(Modifier.padding(16.dp).testTag(ProfileTestTags.ORG_CARD)) {
      Text(
        text = if (isOrganizer) "Organizer Dashboard" else "Become an Organizer",
        color = ProfileColors.TextPrimary,
        style = MaterialTheme.typography.titleMedium
      )

      Spacer(Modifier.height(8.dp))

      Text(
        text = if (isOrganizer)
          "Create and manage your events."
        else
          "Create epic events, build your community, and grow your audience.",
        color = ProfileColors.TextSecondary,
        style = MaterialTheme.typography.bodyMedium
      )

      Spacer(Modifier.height(16.dp))

      Button(
        onClick = onCreateEvent,
        modifier = Modifier.fillMaxWidth().testTag(ProfileTestTags.ORG_CTA),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ProfileColors.Accent)
      ) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = Color.White)
        Spacer(Modifier.width(8.dp))
        Text(text = "Start Creating Events", color = Color.White)
      }
    }
  }
}

/** List item used for settings actions. */
@Composable
private fun SettingsItemDark(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  titleColor: Color,
  onClick: () -> Unit,
  testTag: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 52.dp)
      .clickable(onClick = onClick)
      .semantics {
        role = Role.Button
        this.contentDescription = title
      }
      .padding(horizontal = 6.dp)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(icon, contentDescription = null, tint = Color(0xFF9B9B9B))
    Spacer(Modifier.width(16.dp))
    Text(title, color = titleColor, style = MaterialTheme.typography.bodyLarge)
  }
}
