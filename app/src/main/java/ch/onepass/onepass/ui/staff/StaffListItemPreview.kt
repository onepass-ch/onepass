import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.R
import ch.onepass.onepass.model.staff.StaffSearchResult
import ch.onepass.onepass.ui.staff.StaffListItem
import ch.onepass.onepass.ui.theme.OnePassTheme

@Preview(showBackground = true)
@Composable
private fun StaffListItemPreview() {
  OnePassTheme {
    var lastClick by remember { mutableStateOf<StaffSearchResult?>(null) }

    val user1 =
        StaffSearchResult(
            id = "1",
            email = stringResource(R.string.preview_staff_email_alice),
            displayName = stringResource(R.string.preview_staff_name_alice),
            avatarUrl = null)
    val user2 =
        StaffSearchResult(
            id = "2",
            email = stringResource(R.string.preview_staff_email_unknown),
            displayName = stringResource(R.string.preview_staff_name_unknown),
            avatarUrl = "https://www.gravatar.com/avatar/?d=mp")
    val user3 =
        StaffSearchResult(
            id = "3",
            email = stringResource(R.string.preview_staff_email_bruce),
            displayName = stringResource(R.string.preview_staff_name_bruce),
            avatarUrl = "https://picsum.photos/200")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
      StaffListItem(user = user1, onClick = { lastClick = user1 })
      Spacer(Modifier.size(8.dp))
      StaffListItem(user = user2, onClick = { lastClick = user2 })
      Spacer(Modifier.size(8.dp))
      StaffListItem(user = user3, onClick = { lastClick = user3 })
      Spacer(Modifier.size(12.dp))

      Text(
          text =
              lastClick?.let {
                stringResource(R.string.preview_staff_last_click, it.displayName, it.email)
              } ?: stringResource(R.string.preview_staff_last_click_none),
          style = MaterialTheme.typography.bodyMedium)
    }
  }
}
