package ch.onepass.onepass.ui.organization

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.event.PricingTier
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.model.membership.MembershipRepository
import ch.onepass.onepass.model.organization.*
import ch.onepass.onepass.model.user.User
import ch.onepass.onepass.utils.UI_WAIT_TIMEOUT
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import java.util.*
import kotlinx.coroutines.tasks.await

/** Helper class for Firestore integration tests. Provides common setup and utility functions. */
object FirestoreTestHelper {

  fun createFirestoreOrganization(
      ownerId: String,
      name: String = "Test Organization"
  ): Organization =
      Organization(
          id = "",
          name = name,
          description = "Test Description",
          ownerId = ownerId,
          status = OrganizationStatus.ACTIVE,
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

  /**
   * Populates memberships in Firestore. This method is decoupled from the Organization.members
   * field and accepts a map of user IDs to Roles directly.
   */
  suspend fun populateMemberships(
      organizationId: String,
      members: Map<String, OrganizationRole>,
      membershipRepository: MembershipRepository
  ) {
    members.forEach { (userId, role) ->
      membershipRepository.addMembership(userId, organizationId, role)
    }
  }

  /** Creates a user in Firestore to ensure profile fetching works. */
  suspend fun createFirestoreUser(
      userId: String,
      displayName: String = "User $userId",
      email: String = "$userId@example.com",
      db: FirebaseFirestore = FirebaseFirestore.getInstance()
  ) {
    val user =
        User(
            uid = userId, // Note: @Exclude means this won't be in the doc body, which matches
            // Firestore behavior
            displayName = displayName,
            email = email)
    db.collection("users").document(userId).set(user).await()
  }

  fun ComposeTestRule.waitForTag(tag: String) {
    waitUntil(UI_WAIT_TIMEOUT) { onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
  }

  fun ComposeTestRule.waitUntilNodeDisappears(text: String) {
    waitUntil(UI_WAIT_TIMEOUT) { onAllNodesWithText(text).fetchSemanticsNodes().isEmpty() }
  }
}
