package ch.onepass.onepass.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Diversity3
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.ui.theme.Error
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.flow.collectLatest

object ProfileTestTags {
  const val SCREEN = "profile_screen"
  const val LOADING = "profile_loading"
  const val HEADER = "profile_header"
  const val HEADER_INITIALS = "profile_header_initials"
  const val HEADER_AVATAR = "profile_header_avatar"
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
  const val SETTINGS_INVITATIONS = "profile_settings_invitations"
  const val SETTINGS_INVITATIONS_BADGE = "profile_settings_invitations_badge"
  const val SETTINGS_PAYMENTS = "profile_settings_payments"
  const val SETTINGS_HELP = "profile_settings_help"
  const val SETTINGS_SIGN_OUT = "profile_settings_sign_out"
}

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onEffect: (ProfileEffect) -> Unit) {
  val state by viewModel.state.collectAsState()
  LaunchedEffect(viewModel) { viewModel.effects.collectLatest(onEffect) }

  // Reload profile when screen is shown to reflect any changes (e.g., new organization created)
  LaunchedEffect(Unit) { viewModel.loadProfile() }

  ProfileContent(
      state = state,
      onOrganizationButton = viewModel::onOrganizationButton,
      onInvitations = viewModel::onInvitations,
      onAccountSettings = viewModel::onAccountSettings,
      onPaymentMethods = viewModel::onPaymentMethods,
      onHelp = viewModel::onHelp,
      onSignOut = viewModel::onSignOut)
}

@Composable
private fun ProfileContent(
    state: ProfileUiState,
    onOrganizationButton: () -> Unit,
    onAccountSettings: () -> Unit,
    onInvitations: () -> Unit = {},
    onPaymentMethods: () -> Unit,
    onHelp: () -> Unit,
    onSignOut: () -> Unit
) {
  if (state.loading) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(colorScheme.background)
                .testTag(ProfileTestTags.LOADING),
        contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = colorScheme.surface)
        }
    return
  }

  Scaffold(containerColor = colorScheme.background) { padding ->
    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag(ProfileTestTags.SCREEN)) {
          HeaderBlock(
              initials = state.initials,
              name = state.displayName,
              email = state.email,
              avatarUrl = state.avatarUrl)

          Spacer(Modifier.height(12.dp))

          StatsRow(stats = state.stats)

          Spacer(Modifier.height(12.dp))

          Text(
              text = "ORGANIZER SETTINGS",
              color = colorScheme.onBackground,
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
              modifier = Modifier.testTag(ProfileTestTags.ORG_SECTION_TITLE))

          Spacer(Modifier.height(12.dp))

          OrganizerCard(
              isOrganizer = state.isOrganizer, onOrganizationButton = onOrganizationButton)

          Spacer(Modifier.height(12.dp))

          HorizontalDivider(
              modifier = Modifier.alpha(0.2f), thickness = 1.dp, color = colorScheme.surface)

          Spacer(Modifier.height(8.dp))

          SettingsItem(
              icon = Icons.Outlined.Diversity3,
              title = "My Invitations",
              titleColor = colorScheme.onBackground,
              onClick = onInvitations,
              testTag = ProfileTestTags.SETTINGS_INVITATIONS,
              badgeCount = state.pendingInvitations)
          SettingsItem(
              icon = Icons.Outlined.AccountCircle,
              title = "Account Settings",
              titleColor = colorScheme.onBackground,
              onClick = onAccountSettings,
              testTag = ProfileTestTags.SETTINGS_ACCOUNT)
          SettingsItem(
              icon = Icons.Outlined.Settings,
              title = "Payment Methods",
              titleColor = colorScheme.onBackground,
              onClick = onPaymentMethods,
              testTag = ProfileTestTags.SETTINGS_PAYMENTS)
          SettingsItem(
              icon = Icons.Outlined.Info,
              title = "Help & Support",
              titleColor = colorScheme.onBackground,
              onClick = onHelp,
              testTag = ProfileTestTags.SETTINGS_HELP)
          SettingsItem(
              icon = Icons.AutoMirrored.Outlined.ExitToApp,
              title = "Sign Out",
              titleColor = Error,
              onClick = onSignOut,
              testTag = ProfileTestTags.SETTINGS_SIGN_OUT)
        }
  }
}

/** Displays three stat cards horizontally. */
@Composable
private fun StatsRow(stats: ProfileStats) {
  Row(
      Modifier.fillMaxWidth().testTag(ProfileTestTags.STATS_ROW),
      horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            value = stats.events,
            label = "Events",
            modifier = Modifier.weight(1f).testTag(ProfileTestTags.STAT_EVENTS))
        StatCard(
            value = stats.upcoming,
            label = "Upcoming",
            modifier = Modifier.weight(1f).testTag(ProfileTestTags.STAT_UPCOMING))
        StatCard(
            value = stats.saved,
            label = "Saved",
            modifier = Modifier.weight(1f).testTag(ProfileTestTags.STAT_SAVED))
      }
}

/** Header with avatar placeholder, name, and email. */
@Composable
private fun HeaderBlock(initials: String, name: String, email: String, avatarUrl: String?) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.testTag(ProfileTestTags.HEADER)) {
        Box(
            modifier =
                Modifier.size(72.dp)
                    .clip(CircleShape)
                    .background(colorScheme.surface)
                    .testTag(ProfileTestTags.HEADER_INITIALS),
            contentAlignment = Alignment.Center) {
              if (avatarUrl.isNullOrBlank()) {
                Text(initials, color = colorScheme.onBackground, fontWeight = FontWeight.Bold)
              } else {
                SubcomposeAsyncImage(
                    model =
                        ImageRequest.Builder(LocalContext.current)
                            .data(avatarUrl)
                            .crossfade(true)
                            .build(),
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier.fillMaxSize()
                            .clip(CircleShape)
                            .testTag(ProfileTestTags.HEADER_AVATAR),
                    loading = {
                      Box(modifier = Modifier.fillMaxSize().background(colorScheme.surface))
                    })
              }
            }

        Spacer(Modifier.width(16.dp))

        Column(Modifier.weight(1f)) {
          Text(
              text = name,
              color = colorScheme.onBackground,
              style =
                  MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.testTag(ProfileTestTags.HEADER_NAME))
          Text(
              text = email,
              color = colorScheme.onBackground,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.testTag(ProfileTestTags.HEADER_EMAIL))
        }
      }
}

/** Single stat card: shows a number and a label. */
@Composable
private fun StatCard(value: Int, label: String, modifier: Modifier = Modifier) {
  Surface(
      color = colorScheme.surface,
      shape = RoundedCornerShape(16.dp),
      tonalElevation = 0.dp,
      modifier = modifier.height(96.dp)) {
        Column(
            modifier = Modifier.padding(vertical = 18.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                  value.toString(),
                  color = colorScheme.onBackground,
                  style = MaterialTheme.typography.headlineSmall)
              Text(
                  label,
                  color = colorScheme.onBackground,
                  style = MaterialTheme.typography.bodyMedium)
            }
      }
}

/** Card for the organizer section. */
@Composable
private fun OrganizerCard(isOrganizer: Boolean, onOrganizationButton: () -> Unit) {
  Surface(color = colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
    Column(Modifier.padding(16.dp).testTag(ProfileTestTags.ORG_CARD)) {
      Text(
          text = if (isOrganizer) "Organization Management" else "Start Your Journey",
          color = colorScheme.onBackground,
          style = MaterialTheme.typography.titleMedium)

      Spacer(Modifier.height(8.dp))

      Text(
          text =
              if (isOrganizer) "Create and manage your organizations."
              else "Create epic events, build your community, and grow your audience.",
          color = colorScheme.onBackground,
          style = MaterialTheme.typography.bodyMedium)

      Spacer(Modifier.height(16.dp))

      Button(
          onClick = onOrganizationButton,
          modifier = Modifier.fillMaxWidth().testTag(ProfileTestTags.ORG_CTA),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)) {
            if (!isOrganizer)
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = colorScheme.onBackground)
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isOrganizer) "My Organizations" else "Become an organizer",
                color = colorScheme.onBackground)
          }
    }
  }
}
/** Displays a circular badge with a count, styled in purple. Only displays when count > 0. */
@Composable
private fun BadgeCount(count: Int, modifier: Modifier = Modifier) {
  if (count > 0) {
    Box(
        modifier =
            modifier
                .background(color = colorScheme.primary, shape = CircleShape)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag(ProfileTestTags.SETTINGS_INVITATIONS_BADGE),
        contentAlignment = Alignment.Center) {
          Text(
              text = count.toString(),
              color = colorScheme.onBackground,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              maxLines = 1)
        }
  }
}
/** List item used for settings actions. */
@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    titleColor: Color,
    onClick: () -> Unit,
    testTag: String,
    badgeCount: Int = 0
) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .heightIn(min = 52.dp)
              .clickable(onClick = onClick)
              .semantics(mergeDescendants = false) {
                role = Role.Button
                this.contentDescription = title
              }
              .padding(horizontal = 6.dp)
              .testTag(testTag),
      verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = colorScheme.onBackground)
        Spacer(Modifier.width(16.dp))
        Text(
            title,
            color = titleColor,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f))
        if (badgeCount > 0) {
          BadgeCount(count = badgeCount)
        }
      }
}
