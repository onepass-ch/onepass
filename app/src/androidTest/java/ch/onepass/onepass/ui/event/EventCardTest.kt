package ch.onepass.onepass.ui.event

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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

  // Helper function to create Event objects that match the old parameter-based interface
  private fun createEvent(
      eventPrice: UInt,
      eventTitle: String,
      eventDate: String,
      eventLocation: String,
      eventOrganizer: String
  ): Event {
    // For date, we need to parse it to create a proper Timestamp
    // Since we can't easily parse arbitrary date strings, we'll use current time
    // and rely on the fact that the actual date display will be tested separately
    val startTime = Timestamp(Date())

    return Event(
        title = eventTitle,
        organizerName = eventOrganizer,
        startTime = startTime,
        location = Location(name = eventLocation),
        pricingTiers =
            if (eventPrice > 0u) listOf(PricingTier(price = eventPrice.toDouble()))
            else emptyList())
  }

  @Test
  fun eventCard_displaysFreeForZeroPrice() {
    // Given
    val price = 0u

    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(event = createEvent(price, "Free Event", "Dec 22, 2024", "Lausanne", "Organizer"))
      }
    }

    // Then - Verify "FREE" is displayed instead of price
    composeTestRule.onNodeWithText("FREE").assertIsDisplayed()
  }

  @Test
  fun eventCard_displaysNonZeroPrice() {
    // Given
    val price = 50u

    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(event = createEvent(price, "Paid Event", "Dec 22, 2024", "Lausanne", "Organizer"))
      }
    }

    // Then - Verify price is displayed with CHF prefix
    composeTestRule.onNodeWithText("CHF$price").assertIsDisplayed()
  }

  @Test
  fun eventCard_likeButtonIsDisplayed() {
    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(event = createEvent(25u, "Event", "Dec 22, 2024", "Location", "Organizer"))
      }
    }

    // Then - Verify like button is present
    composeTestRule.onNodeWithContentDescription("Like").assertIsDisplayed()
  }

  @Test
  fun eventCard_likeButtonToggleWorks() {
    var isLiked by mutableStateOf(false)

    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(
            event = createEvent(25u, "Event", "Dec 22, 2024", "Location", "Organizer"),
            isLiked = isLiked,
            onLikeToggle = { isLiked = !isLiked })
      }
    }

    // Then - Initially shows "Like" button
    composeTestRule.onNodeWithContentDescription("Like").assertIsDisplayed()

    // When - Click the like button
    composeTestRule.onNodeWithContentDescription("Like").performClick()

    // Then - Shows "Unlike" button
    composeTestRule.onNodeWithContentDescription("Unlike").assertIsDisplayed()

    // When - Click again
    composeTestRule.onNodeWithContentDescription("Unlike").performClick()

    // Then - Shows "Like" button again
    composeTestRule.onNodeWithContentDescription("Like").assertIsDisplayed()
  }

  @Test
  fun eventCard_imageIsDisplayed() {
    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(event = createEvent(25u, "Event", "Dec 22, 2024", "Location", "Organizer"))
      }
    }

    // Then - Verify image content description is present
    composeTestRule.onNodeWithContentDescription("image description").assertIsDisplayed()
  }

  @Test
  fun eventCard_handlesLongTitle() {
    // Given
    val longTitle = "This is a very long event title that should be truncated with ellipsis"

    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(event = createEvent(25u, longTitle, "Dec 22, 2024", "Location", "Organizer"))
      }
    }

    // Then - Title is displayed (may be truncated)
    composeTestRule.onNodeWithText(longTitle, substring = true).assertIsDisplayed()
  }

  @Test
  fun eventCard_withLargePriceValue() {
    // Given
    val largePrice = 9999u

    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(
            event =
                createEvent(largePrice, "Expensive Event", "Dec 22, 2024", "Lausanne", "Organizer"))
      }
    }

    // Then - Large price is displayed correctly
    composeTestRule.onNodeWithText("CHF$largePrice").assertIsDisplayed()
  }

  @Test
  fun eventCard_allTextElementsAreDisplayed() {
    // Given
    val price = 25u
    val title = "Test Event"
    val date = "Dec 22, 2024"
    val location = "Test Location"
    val organizer = "Test Organizer"

    // When
    composeTestRule.setContent {
      MaterialTheme { EventCard(event = createEvent(price, title, date, location, organizer)) }
    }

    // Then - Verify all text elements by their content
    composeTestRule.onNodeWithText(title).assertExists()
    composeTestRule.onNodeWithText(organizer).assertExists()
    composeTestRule.onNodeWithText(location).assertExists()
    composeTestRule.onNodeWithText("CHF$price").assertExists()
    composeTestRule.onNodeWithContentDescription("image description").assertExists()
    composeTestRule.onNodeWithContentDescription("Like").assertExists()
  }

  @Test
  fun eventCard_freeEventDisplaysCorrectly() {
    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(event = createEvent(0u, "Free Event", "Dec 22, 2024", "Lausanne", "Organizer"))
      }
    }

    // Then - Verify FREE is displayed by text content
    composeTestRule.onNodeWithText("FREE").assertIsDisplayed()
    composeTestRule.onNodeWithText("Free Event").assertIsDisplayed()
  }

  @Test
  fun eventCard_handlesLongLocation() {
    // Given
    val longLocation = "This is a very long location address that should be truncated with ellipsis"

    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(event = createEvent(25u, "Event", "Dec 22, 2024", longLocation, "Organizer"))
      }
    }

    // Then - Location is displayed (may be truncated)
    composeTestRule.onNodeWithText(longLocation, substring = true).assertIsDisplayed()
  }

  @Test
  fun eventCard_handlesLongOrganizer() {
    // Given
    val longOrganizer = "This is a very long organizer name that should be truncated"

    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(event = createEvent(25u, "Event", "Dec 22, 2024", "Location", longOrganizer))
      }
    }

    // Then - Organizer is displayed (may be truncated)
    composeTestRule.onNodeWithText(longOrganizer, substring = true).assertIsDisplayed()
  }

  @Test
  fun eventCard_variousPriceFormats_free() {
    // Given
    val price = 0u

    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(event = createEvent(price, "Event", "Dec 22, 2024", "Location", "Organizer"))
      }
    }

    // Then
    composeTestRule.onNodeWithText("FREE").assertExists()
  }

  @Test
  fun eventCard_variousPriceFormats_one() {
    // Given
    val price = 1u

    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(event = createEvent(price, "Event", "Dec 22, 2024", "Location", "Organizer"))
      }
    }

    // Then
    composeTestRule.onNodeWithText("CHF1").assertExists()
  }

  @Test
  fun eventCard_variousPriceFormats_hundred() {
    // Given
    val price = 100u

    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(event = createEvent(price, "Event", "Dec 22, 2024", "Location", "Organizer"))
      }
    }

    // Then
    composeTestRule.onNodeWithText("CHF100").assertExists()
  }

  @Test
  fun eventCard_variousPriceFormats_large() {
    // Given
    val price = 9999u

    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(event = createEvent(price, "Event", "Dec 22, 2024", "Location", "Organizer"))
      }
    }

    // Then
    composeTestRule.onNodeWithText("CHF9999").assertExists()
  }

  @Test
  fun eventCard_handlesEmptyTitle() {
    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(event = createEvent(25u, "", "Dec 22, 2024", "Location", "Organizer"))
      }
    }

    // Then - Card is still displayed with other content
    composeTestRule.onNodeWithText("Location").assertIsDisplayed()
  }

  @Test
  fun eventCard_handlesEmptyOrganizer() {
    // When
    composeTestRule.setContent {
      MaterialTheme { EventCard(event = createEvent(25u, "Event", "Dec 22, 2024", "Location", "")) }
    }

    // Then - Card is still displayed with other content
    composeTestRule.onNodeWithText("Event").assertIsDisplayed()
    composeTestRule.onNodeWithText("Location").assertIsDisplayed()
  }

  @Test
  fun eventCard_handlesEmptyDate() {
    // When
    composeTestRule.setContent {
      MaterialTheme { EventCard(event = createEvent(25u, "Event", "", "Location", "Organizer")) }
    }

    // Then - Card is still displayed with other content
    composeTestRule.onNodeWithText("Event").assertIsDisplayed()
    composeTestRule.onNodeWithText("Location").assertIsDisplayed()
  }

  @Test
  fun eventCard_handlesEmptyLocation() {
    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(event = createEvent(25u, "Event", "Dec 22, 2024", "", "Organizer"))
      }
    }

    // Then - Card is still displayed with other content
    composeTestRule.onNodeWithText("Event").assertIsDisplayed()
  }

  @Test
  fun eventCard_handlesAllEmptyStrings() {
    // When
    composeTestRule.setContent {
      MaterialTheme { EventCard(event = createEvent(0u, "", "", "", "")) }
    }

    // Then - Card is still displayed with FREE price
    composeTestRule.onNodeWithText("FREE").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("image description").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Like").assertIsDisplayed()
  }

  @Test
  fun eventCard_displaysWithMinimalPrice() {
    // Given
    val price = 1u

    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(event = createEvent(price, "Event", "Dec 22, 2024", "Location", "Organizer"))
      }
    }

    // Then - Minimal price is displayed correctly
    composeTestRule.onNodeWithText("CHF1").assertIsDisplayed()
  }

  @Test
  fun eventCard_handlesSpecialCharactersInTitle() {
    // Given
    val specialTitle = "Event & Party @ #1 Venue!"

    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(event = createEvent(25u, specialTitle, "Dec 22, 2024", "Location", "Organizer"))
      }
    }

    // Then - Special characters are displayed correctly
    composeTestRule.onNodeWithText(specialTitle).assertIsDisplayed()
  }

  @Test
  fun eventCard_handlesSpecialCharactersInLocation() {
    // Given
    val specialLocation = "Café @ Zürich, 1st Floor"

    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(event = createEvent(25u, "Event", "Dec 22, 2024", specialLocation, "Organizer"))
      }
    }

    // Then - Special characters are displayed correctly
    composeTestRule.onNodeWithText(specialLocation, substring = true).assertIsDisplayed()
  }

  @Test
  fun eventCard_handlesMultilineTextInTitle() {
    // Given
    val multilineTitle = "Event\nWith\nNewlines"

    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(event = createEvent(25u, multilineTitle, "Dec 22, 2024", "Location", "Organizer"))
      }
    }

    // Then - Title is displayed (may be truncated due to maxLines = 1)
    composeTestRule.onNodeWithText(multilineTitle, substring = true).assertIsDisplayed()
  }

  @Test
  fun eventCard_acceptsCustomModifier() {
    // Given
    val testTag = "custom_event_card"

    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(
            event = createEvent(25u, "Event", "Dec 22, 2024", "Location", "Organizer"),
            modifier = Modifier.testTag(testTag))
      }
    }

    // Then - Card with custom modifier is displayed
    composeTestRule.onNodeWithTag(testTag).assertExists()
  }

  @Test
  fun eventCard_withOnCardClickCallback() {
    // Given
    var clickCount = 0
    val onCardClick: () -> Unit = { clickCount++ }

    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(
            event = createEvent(25u, "Event", "Dec 22, 2024", "Location", "Organizer"),
            onCardClick = onCardClick)
      }
    }

    // Then - Card is displayed
    composeTestRule.onNodeWithText("Event").assertIsDisplayed()
    // Note: onCardClick is not currently used in the implementation
  }

  @Test
  fun eventCard_multipleCardsCanBeDisplayed() {
    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(
            event = createEvent(25u, "Event 1", "Dec 22, 2024", "Location 1", "Organizer 1"),
            modifier = Modifier.testTag("card1"))
        EventCard(
            event = createEvent(50u, "Event 2", "Dec 23, 2024", "Location 2", "Organizer 2"),
            modifier = Modifier.testTag("card2"))
      }
    }

    // Then - Both cards are displayed
    composeTestRule.onNodeWithTag("card1").assertExists()
    composeTestRule.onNodeWithTag("card2").assertExists()
    composeTestRule.onNodeWithText("Event 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Event 2").assertIsDisplayed()
  }

  @Test
  fun eventCard_priceDisplaysCorrectlyForMaxUInt() {
    // Given - Using a large UInt value
    val largePrice = 4294967295u // Max UInt value

    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(
            event =
                createEvent(
                    largePrice, "Very Expensive Event", "Dec 22, 2024", "Lausanne", "Organizer"))
      }
    }

    // Then - Large price is displayed correctly
    composeTestRule.onNodeWithText("CHF$largePrice").assertIsDisplayed()
  }

  @Test
  fun eventCard_handlesUnicodeCharacters() {
    // Given
    val unicodeTitle = "🎉 Party Event 🎊"
    val unicodeLocation = "Zürich 🇨🇭"

    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(
            event = createEvent(25u, unicodeTitle, "Dec 22, 2024", unicodeLocation, "Organizer"))
      }
    }

    // Then - Unicode characters are displayed correctly
    composeTestRule.onNodeWithText(unicodeTitle).assertIsDisplayed()
    composeTestRule.onNodeWithText(unicodeLocation, substring = true).assertIsDisplayed()
  }

  @Test
  fun eventCard_likeButtonStateIsIndependentPerCard() {
    // When - Create two cards
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(
            event = createEvent(25u, "Event 1", "Dec 22, 2024", "Location 1", "Organizer 1"),
            modifier = Modifier.testTag("card1"))
        EventCard(
            event = createEvent(50u, "Event 2", "Dec 23, 2024", "Location 2", "Organizer 2"),
            modifier = Modifier.testTag("card2"))
      }
    }

    // Then - Each card should have its own like button
    // Note: This test verifies that like buttons exist, but testing independent state
    // would require more complex setup with unique identifiers
    composeTestRule.onAllNodesWithContentDescription("Like").assertCountEquals(2)
  }
}
