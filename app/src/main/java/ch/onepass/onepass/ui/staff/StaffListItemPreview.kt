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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.model.staff.StaffSearchResult
import ch.onepass.onepass.ui.staff.StaffListItem
import ch.onepass.onepass.ui.theme.OnePassTheme

@Preview(showBackground = true)
@Composable
private fun StaffListItemPreview() {
  OnePassTheme {
    var lastClick by remember { mutableStateOf<StaffSearchResult?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
      StaffListItem(
          user =
              StaffSearchResult(
                  id = "1",
                  email = "alice@onepass.ch",
                  displayName = "Alice Keller",
                  avatarUrl = null),
          onClick = { lastClick = it })
      Spacer(Modifier.size(8.dp))
      StaffListItem(
          user =
              StaffSearchResult(
                  id = "2",
                  email = "unknown@email.address",
                  displayName = "Unknown user",
                  avatarUrl = "https://www.gravatar.com/avatar/?d=mp"),
          onClick = { lastClick = it })
      Spacer(Modifier.size(8.dp))
      StaffListItem(
          user =
              StaffSearchResult(
                  id = "3",
                  email = "bruce@onepass.ch",
                  displayName = "Bruce Dai",
                  avatarUrl = "https://picsum.photos/200"),
          onClick = { lastClick = it })
      Spacer(Modifier.size(12.dp))

      Text(
          text =
              lastClick?.let { "Last click: ${it.displayName} <${it.email}>" } ?: "Last click: —",
          style = MaterialTheme.typography.bodyMedium)
    }
  }
}
