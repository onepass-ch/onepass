package ch.onepass.onepass.ui.event

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.PricingTier
import ch.onepass.onepass.model.map.Location
import com.google.firebase.Timestamp
import java.util.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for EventCard component */
@RunWith(AndroidJUnit4::class)
class EventCardTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun createEvent(
      price: UInt,
      title: String,
      location: String,
      organizer: String,
      id: String = "1"
  ): Event {
    val startTime = Timestamp(Date())
    return Event(
        eventId = id,
        title = title,
        organizerName = organizer,
        startTime = startTime,
        location = Location(name = location),
        pricingTiers =
            if (price > 0u) listOf(PricingTier(price = price.toDouble())) else emptyList())
  }

  @Test
  fun eventCard_displaysAllElementsCorrectly() {
    val event = createEvent(25u, "Test Event", "Test Location", "Test Organizer")

    composeTestRule.setContent { MaterialTheme { EventCard(event = event) } }
    composeTestRule.onNodeWithText("Test Event").assertExists()
    composeTestRule.onNodeWithText("Test Organizer").assertExists()
    composeTestRule.onNodeWithText("Test Location").assertExists()
    composeTestRule.onNodeWithText("CHF25").assertExists()
    composeTestRule.onNodeWithTag("event_card_image", useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithContentDescription("Like").assertExists()
  }

  @Test
  fun eventCard_likeButtonTogglesState() {
    var isLiked by mutableStateOf(false)

    composeTestRule.setContent {
      MaterialTheme {
        EventCard(
            event = createEvent(25u, "Event", "Location", "Organizer"),
            isLiked = isLiked,
            onLikeToggle = { isLiked = !isLiked })
      }
    }

    composeTestRule.onNodeWithContentDescription("Like").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Like").performClick()
    composeTestRule.onNodeWithContentDescription("Unlike").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Unlike").performClick()
    composeTestRule.onNodeWithContentDescription("Like").assertIsDisplayed()
  }

  data class TextTestCase(val title: String, val location: String, val organizer: String)

  @Test
  fun eventCard_handlesVariousTextInputs() {
    val testCases =
        listOf(
            // Long text
            TextTestCase(
                "This is a very long event title that should be truncated",
                "Location",
                "Organizer"),
            TextTestCase(
                "Event", "This is a very long location that should be truncated", "Organizer"),
            TextTestCase(
                "Event", "Location", "This is a very long organizer name that should be truncated"),
            // Empty text
            TextTestCase("", "Location", "Organizer"),
            TextTestCase("Event", "", "Organizer"),
            TextTestCase("Event", "Location", ""),
            // Special characters
            TextTestCase("Event & Party @ #1 Venue!", "Location", "Organizer"),
            TextTestCase("Event", "Café @ Zürich, 1st Floor", "Organizer"),
            // Multiline
            TextTestCase("Event\nWith\nNewlines", "Location", "Organizer"))

    lateinit var currentEvent: MutableState<Event>

    composeTestRule.setContent {
      currentEvent = remember {
        mutableStateOf(
            createEvent(25u, testCases[0].title, testCases[0].location, testCases[0].organizer))
      }
      MaterialTheme { EventCard(event = currentEvent.value) }
    }

    testCases.forEach { case ->
      composeTestRule.runOnIdle {
        currentEvent.value = createEvent(25u, case.title, case.location, case.organizer)
      }

      // Perform assertions for the current case
      if (case.title.isNotEmpty())
          composeTestRule.onNodeWithText(case.title, substring = true).assertExists()
      if (case.location.isNotEmpty())
          composeTestRule.onNodeWithText(case.location, substring = true).assertExists()
      if (case.organizer.isNotEmpty())
          composeTestRule.onNodeWithText(case.organizer, substring = true).assertExists()
    }
  }

  @Test
  fun eventCard_handlesAllEmpty() {
    composeTestRule.setContent { MaterialTheme { EventCard(event = createEvent(0u, "", "", "")) } }
    composeTestRule.onNodeWithText("FREE").assertIsDisplayed()
    composeTestRule.onNodeWithTag("event_card_image", useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithContentDescription("Like").assertIsDisplayed()
  }

  @Test
  fun eventCard_handlesVariousPriceFormats() {
    val prices = mapOf(0u to "FREE", 1u to "CHF1", 100u to "CHF100", 9999u to "CHF9999")
    lateinit var currentEvent: MutableState<Event>

    composeTestRule.setContent {
      // Initialize the state with the first price
      currentEvent = remember {
        mutableStateOf(createEvent(prices.keys.first(), "Event", "Location", "Organizer"))
      }
      MaterialTheme {
        // The EventCard will recompose when the currentEvent state changes
        EventCard(event = currentEvent.value)
      }
    }

    prices.forEach { (price, expectedText) ->
      // Update the state with a new event for the current price
      // This triggers recomposition, not a new setContent call
      composeTestRule.runOnIdle {
        currentEvent.value = createEvent(price, "Event", "Location", "Organizer")
      }

      // Assert that the text for the current price exists
      composeTestRule.onNodeWithText(expectedText).assertExists()
    }
  }

  @Test
  fun eventCard_multipleCardsAreIndependent() {
    composeTestRule.setContent {
      Column {
        // Card 1
        var isLiked1 by remember { mutableStateOf(false) }
        EventCard(
            event = createEvent(25u, "Event 1", "Location 1", "Organizer 1", id = "1"),
            modifier = Modifier.testTag("card1"),
            isLiked = isLiked1,
            onLikeToggle = { isLiked1 = !isLiked1 })
        // Card 2
        var isLiked2 by remember { mutableStateOf(false) }
        EventCard(
            event = createEvent(50u, "Event 2", "Location 2", "Organizer 2", id = "2"),
            modifier = Modifier.testTag("card2"),
            isLiked = isLiked2,
            onLikeToggle = { isLiked2 = !isLiked2 })
      }
    }

    // Assert both cards are displayed with "Like"
    composeTestRule
        .onNodeWithTag("card1")
        .onChildren()
        .filterToOne(hasContentDescription("Like"))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("card2")
        .onChildren()
        .filterToOne(hasContentDescription("Like"))
        .assertIsDisplayed()

    // Click like on the first card
    composeTestRule
        .onNodeWithTag("card1")
        .onChildren()
        .filterToOne(hasContentDescription("Like"))
        .performClick()

    // Assert first card's icon changes to "Unlike", and second remains "Like"
    composeTestRule
        .onNodeWithTag("card1")
        .onChildren()
        .filterToOne(hasContentDescription("Unlike"))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("card2")
        .onChildren()
        .filterToOne(hasContentDescription("Like"))
        .assertIsDisplayed()
  }
}
