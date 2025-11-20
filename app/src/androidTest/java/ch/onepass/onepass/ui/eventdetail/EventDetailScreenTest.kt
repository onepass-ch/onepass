package ch.onepass.onepass.ui.eventdetail

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.event.PricingTier
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationStatus
import ch.onepass.onepass.ui.theme.OnePassTheme
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.util.Calendar
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EventDetailScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val testEventId = "test-event-id"
  private val testOrganizerId = "test-organizer-id"

  @Test
  fun eventDetailScreen_displaysLoadingState() {
    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState = EventDetailUiState(isLoading = true),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule
        .onNodeWithTag(EventDetailTestTags.LOADING, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun eventDetailScreen_displaysErrorState_and_goBackWorks() {
    var backClicked = false

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState = EventDetailUiState(errorMessage = "Network error"),
            onBack = { backClicked = true },
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule
        .onNodeWithTag(EventDetailTestTags.ERROR, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Network error", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Go Back", useUnmergedTree = true).performClick()
    assertTrue(backClicked)
  }

  @Test
  fun eventDetailScreen_displaysMainEventElements_and_interactions() {
    val testEvent = createTestEvent(title = "Amazing Event")

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState = EventDetailUiState(event = testEvent, isLiked = false),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    // Main elements
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.EVENT_TITLE, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Amazing Event", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.EVENT_IMAGE, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.LIKE_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()

    // Date & location elements
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.EVENT_DATE, useUnmergedTree = true)
        .assertExists()
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.EVENT_LOCATION, useUnmergedTree = true)
        .assertExists()
    composeTestRule.onNodeWithText("Test Location", useUnmergedTree = true).assertExists()

    // Map button clickable
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.MAP_BUTTON)
        .assertExists()
        .assertHasClickAction()
        .performClick()
  }

  @Test
  fun eventDetailScreen_buyTicketButtonIsClickable_and_displaysPrices() {
    // Buy button clickable
    val testEvent = createTestEvent()
    var buyClicked = false

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState = EventDetailUiState(event = testEvent),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = { buyClicked = true })
      }
    }

    composeTestRule
        .onNodeWithTag(EventDetailTestTags.BUY_TICKET_BUTTON, useUnmergedTree = true)
        .performClick()
    assertTrue(buyClicked)
  }

  @Test
  fun eventDetailScreen_displaysPaidPriceFormat() {
    val paidEvent = createTestEvent(lowestPrice = 35u)

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState = EventDetailUiState(event = paidEvent),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule
        .onNodeWithText("Buy ticket for 35chf", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun eventDetailScreen_displaysFreePriceFormat() {
    val freeEvent = createTestEvent(lowestPrice = 0u)

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState = EventDetailUiState(event = freeEvent),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule.onNodeWithText("FREE", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun eventDetailScreen_hidesRatingWhenZero_and_descriptionFallback() {
    val testEvent = createTestEvent(description = "")
    val testOrganization = createTestOrganization(averageRating = 0f)

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState = EventDetailUiState(event = testEvent, organization = testOrganization),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule
        .onNodeWithTag(EventDetailTestTags.ORGANIZER_RATING, useUnmergedTree = true)
        .assertDoesNotExist()
    composeTestRule
        .onNodeWithText("No description available.", useUnmergedTree = true)
        .assertExists()
  }

  // Helper function to create a test event
  private fun createTestEvent(
      title: String = "Test Event",
      description: String = "Test Description",
      lowestPrice: UInt = 35u
  ): Event {
    val calendar = Calendar.getInstance()
    calendar.set(2024, Calendar.DECEMBER, 15, 21, 0, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    return Event(
        eventId = testEventId,
        title = title,
        description = description,
        organizerId = testOrganizerId,
        organizerName = "Test Organizer",
        status = EventStatus.PUBLISHED,
        location =
            Location(
                coordinates = GeoPoint(46.5197, 6.6323),
                name = "Test Location",
                region = "Test Region"),
        startTime = Timestamp(calendar.time),
        endTime = Timestamp(calendar.time),
        capacity = 100,
        ticketsRemaining = 50,
        ticketsIssued = 50,
        ticketsRedeemed = 0,
        currency = "CHF",
        pricingTiers = listOf(PricingTier("General", lowestPrice.toDouble(), 100, 50)),
        images = listOf(),
        tags = listOf("test"))
  }

  // Helper function to create a test organization
  private fun createTestOrganization(
      name: String = "Test Organization",
      followerCount: Int = 1000,
      averageRating: Float = 4.5f
  ): Organization {
    return Organization(
        id = testOrganizerId,
        name = name,
        description = "Test Description",
        ownerId = "owner-id",
        status = OrganizationStatus.ACTIVE,
        verified = true,
        profileImageUrl = null,
        followerCount = followerCount,
        averageRating = averageRating,
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now())
  }
}
