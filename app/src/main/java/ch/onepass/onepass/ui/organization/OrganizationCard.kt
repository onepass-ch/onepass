package ch.onepass.onepass.ui.organization

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.R
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.ui.organization.OrganizationCardTestTags.getTestTagForOrganizationCard
import ch.onepass.onepass.utils.DateTimeUtils
import ch.onepass.onepass.utils.FormatUtils
import coil.compose.AsyncImage
import com.google.firebase.Timestamp

object OrganizationCardTestTags {
  const val ORGANIZER_IMAGE = "organizerImage"
  const val ORGANIZER_NAME = "organizerName"
  const val ORGANIZER_VERIFIED_BADGE = "organizerVerifiedBadge"
  const val ORGANIZER_DESCRIPTION = "organizerDescription"
  const val ORGANIZER_STATS_ROW = "organizerStatsRow"
  const val ORGANIZER_FOLLOWER_COUNT = "organizerFollowerCount"
  const val ORGANIZER_RATING = "organizerRating"
  const val ORGANIZER_CREATED_DATE = "organizerCreatedDate"

  fun getTestTagForOrganizationCard(organizationId: String) = "organizerCard_$organizationId"
}

@Composable
private fun OrganizationProfileImage(imageUrl: String?, modifier: Modifier = Modifier) {
  AsyncImage(
      model = imageUrl,
      contentDescription = stringResource(R.string.organization_card_profile_description),
      placeholder = rememberVectorPainter(Icons.Default.Business),
      modifier =
          modifier
              .size(80.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(colorScheme.onSurface)
              .testTag(OrganizationCardTestTags.ORGANIZER_IMAGE),
      contentScale = ContentScale.Crop,
      error = rememberVectorPainter(Icons.Default.Business))
}

@Composable
private fun OrganizationNameRow(name: String, verified: Boolean, modifier: Modifier = Modifier) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = modifier) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            color = colorScheme.onBackground,
            modifier =
                Modifier.weight(1f, fill = false)
                    .semantics(mergeDescendants = true) {}
                    .testTag(OrganizationCardTestTags.ORGANIZER_NAME),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis)
        if (verified) {
          Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = stringResource(R.string.organization_card_verified_badge),
              tint = colorScheme.onBackground,
              modifier =
                  Modifier.size(18.dp)
                      .semantics(mergeDescendants = true) {}
                      .testTag(OrganizationCardTestTags.ORGANIZER_VERIFIED_BADGE))
        }
      }
}

@Composable
private fun OrganizationDescription(description: String, modifier: Modifier = Modifier) {
  Text(
      text = description,
      style = MaterialTheme.typography.bodyMedium,
      color = colorScheme.onBackground,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      modifier =
          modifier
              .semantics(mergeDescendants = true) {}
              .testTag(OrganizationCardTestTags.ORGANIZER_DESCRIPTION))
}

@SuppressLint("DefaultLocale")
@Composable
private fun OrganizationStatsRow(
    followerCount: Int,
    averageRating: Float,
    createdAt: Timestamp?,
    modifier: Modifier = Modifier
) {
  Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = modifier.fillMaxWidth().testTag(OrganizationCardTestTags.ORGANIZER_STATS_ROW)) {
        // followers
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.semantics(mergeDescendants = true) {}
                    .testTag(OrganizationCardTestTags.ORGANIZER_FOLLOWER_COUNT)) {
              Icon(
                  imageVector = Icons.Default.Group,
                  contentDescription =
                      stringResource(R.string.organization_card_followers_description),
                  modifier = Modifier.size(16.dp))
              Text(
                  text = FormatUtils.formatCompactNumber(followerCount),
                  style = MaterialTheme.typography.bodyMedium,
                  color = colorScheme.onBackground)
            }

        // avg rating
        if (averageRating > 0f) {
          Row(
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically,
              modifier =
                  Modifier.semantics(mergeDescendants = true) {}
                      .testTag(OrganizationCardTestTags.ORGANIZER_RATING)) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription =
                        stringResource(R.string.organization_card_rating_description),
                    modifier = Modifier.size(16.dp),
                    tint = colorScheme.onBackground)
                Text(
                    text = String.format("%.1f", averageRating),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onBackground)
              }
        }

        // creation date
        createdAt?.let { timestamp ->
          Row(
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.testTag(OrganizationCardTestTags.ORGANIZER_CREATED_DATE)) {
                Text(
                    text =
                        stringResource(
                            R.string.organization_card_since_label,
                            DateTimeUtils.formatMemberSince(timestamp)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onBackground)
              }
        }
      }
}

@Composable
fun OrganizationCard(
    organization: Organization,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  Card(
      modifier =
          modifier
              .fillMaxWidth()
              .clickable { onClick() }
              .testTag(getTestTagForOrganizationCard(organization.id)),
      shape = RoundedCornerShape(10.dp),
      colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(15.dp)) {
              OrganizationProfileImage(imageUrl = organization.profileImageUrl)
              Column(
                  modifier = Modifier.fillMaxWidth(),
                  verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OrganizationNameRow(name = organization.name, verified = organization.verified)
                    OrganizationDescription(description = organization.description)
                    OrganizationStatsRow(
                        followerCount = organization.followerCount,
                        averageRating = organization.averageRating,
                        createdAt = organization.createdAt,
                        modifier = Modifier.padding(top = 5.dp))
                  }
            }
      }
}
