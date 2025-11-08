package ch.onepass.onepass.ui.organization

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.event.PricingTier
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.model.organization.*
import ch.onepass.onepass.utils.UI_WAIT_TIMEOUT
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.util.*

/** Helper class for Firestore integration tests. Provides common setup and utility functions. */
object FirestoreTestHelper {

  fun createFirestoreOrganization(
      ownerId: String,
      name: String = "Test Organization",
      members: Map<String, OrganizationMember> =
          mapOf(ownerId to OrganizationMember(role = OrganizationRole.OWNER))
  ): Organization =
      Organization(
          id = "",
          name = name,
          description = "Test Description",
          ownerId = ownerId,
          status = OrganizationStatus.ACTIVE,
          members = members,
          verified = false,
          followerCount = 1000,
          averageRating = 4.0f)

  fun createFirestoreEvent(organizerId: String, title: String = "Test Event"): Event =
      Event(
          eventId = "",
          title = title,
          description = "Test Description",
          organizerId = organizerId,
          organizerName = "Test Organizer",
          status = EventStatus.PUBLISHED,
          location = Location(GeoPoint(46.5191, 6.5668), "EPFL"),
          startTime = Timestamp(Date()),
          capacity = 100,
          ticketsRemaining = 50,
          ticketsIssued = 50,
          pricingTiers = listOf(PricingTier("General", 25.0, 100, 50)))

  fun ComposeTestRule.waitForTag(tag: String) {
    waitUntil(UI_WAIT_TIMEOUT) { onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
  }
}
