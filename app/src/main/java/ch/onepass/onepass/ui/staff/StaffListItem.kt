import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.model.staff.StaffSearchResult
import ch.onepass.onepass.ui.theme.OnePassTheme
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

@Composable
fun StaffListItem(
    user: StaffSearchResult,
    modifier: Modifier = Modifier,
    onClick: (StaffSearchResult) -> Unit
) {
  val primaryText = user.displayName.ifBlank { user.email }.ifBlank { "Unknown user" }

  Surface(
      shape = RoundedCornerShape(12.dp),
      tonalElevation = 2.dp,
      modifier = modifier.fillMaxWidth().clickable { onClick(user) }) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Avatar(
              displayName = primaryText,
              avatarUrl = user.avatarUrl,
              modifier = Modifier.size(40.dp))

          Spacer(Modifier.width(width = 12.dp))

          Column(Modifier.weight(1f)) {
            Text(
                text = primaryText,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            if (user.email.isNotBlank()) {
              Text(
                  text = user.email,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis)
            }
          }
        }
      }
}

@Composable
private fun InitialsAvatar(displayName: String, modifier: Modifier = Modifier) {
  val initials =
      remember(displayName) {
            displayName
                .split(" ")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(2)
                .joinToString("") { it.first().uppercaseChar().toString() }
          }
          .ifEmpty { "?" }

  Box(
      modifier = modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onPrimaryContainer)
      }
}

@Composable
private fun Avatar(displayName: String, avatarUrl: String?, modifier: Modifier = Modifier) {
  if (avatarUrl.isNullOrBlank()) {
    InitialsAvatar(displayName, modifier)
  } else {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(avatarUrl).crossfade(true).build(),
        contentDescription = "$displayName avatar",
        contentScale = ContentScale.Crop,
        modifier =
            modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        loading = { InitialsAvatar(displayName, modifier = Modifier.matchParentSize()) },
        error = { InitialsAvatar(displayName, modifier = Modifier.matchParentSize()) })
  }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun StaffListItemPreview() {
  OnePassTheme {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
      Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        StaffListItem(
            user =
                StaffSearchResult(
                    id = "1",
                    email = "alice@onepass.ch",
                    displayName = "Alice Keller",
                    avatarUrl = null),
            onClick = {})
        Spacer(Modifier.size(12.dp))
        StaffListItem(
            user = StaffSearchResult(id = "2", email = "", displayName = "", avatarUrl = null),
            onClick = {})
        Spacer(Modifier.size(12.dp))
        StaffListItem(
            user =
                StaffSearchResult(
                    id = "3",
                    email = "bob@onepass.ch",
                    displayName = "Bob Graf",
                    avatarUrl = "https://picsum.photos/200"),
            onClick = {})
      }
    }
  }
}
