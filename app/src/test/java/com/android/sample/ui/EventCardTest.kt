package com.android.sample.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for EventCard component using Robolectric */
@RunWith(AndroidJUnit4::class)
class EventCardTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun eventCard_displaysAllContent() {
    // Given
    val price = 25u
    val title = "LAUSANNE PARTY"
    val date = "Dec 22, 2024 • 7:00 PM"
    val location = "Lausanne, flon"
    val organizer = "modern organizer"

    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(
            eventPrice = price,
            eventTitle = title,
            eventDate = date,
            eventLocation = location,
            eventOrganizer = organizer)
      }
    }

    // Then - Verify all text content is displayed
    composeTestRule.onNodeWithText(title).assertIsDisplayed()
    composeTestRule.onNodeWithText(organizer).assertIsDisplayed()
    composeTestRule.onNodeWithText(date).assertIsDisplayed()
    composeTestRule.onNodeWithText(location).assertIsDisplayed()
    composeTestRule.onNodeWithText("CHF$price").assertIsDisplayed()
  }

  @Test
  fun eventCard_displaysFreeForZeroPrice() {
    // Given
    val price = 0u

    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(
            eventPrice = price,
            eventTitle = "Free Event",
            eventDate = "Dec 22, 2024",
            eventLocation = "Lausanne",
            eventOrganizer = "Organizer")
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
        EventCard(
            eventPrice = price,
            eventTitle = "Paid Event",
            eventDate = "Dec 22, 2024",
            eventLocation = "Lausanne",
            eventOrganizer = "Organizer")
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
        EventCard(
            eventPrice = 25u,
            eventTitle = "Event",
            eventDate = "Dec 22, 2024",
            eventLocation = "Location",
            eventOrganizer = "Organizer")
      }
    }

    // Then - Verify like button is present
    composeTestRule.onNodeWithContentDescription("Like").assertIsDisplayed()
  }

  @Test
  fun eventCard_likeButtonToggleWorks() {
    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(
            eventPrice = 25u,
            eventTitle = "Event",
            eventDate = "Dec 22, 2024",
            eventLocation = "Location",
            eventOrganizer = "Organizer")
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
        EventCard(
            eventPrice = 25u,
            eventTitle = "Event",
            eventDate = "Dec 22, 2024",
            eventLocation = "Location",
            eventOrganizer = "Organizer")
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
        EventCard(
            eventPrice = 25u,
            eventTitle = longTitle,
            eventDate = "Dec 22, 2024",
            eventLocation = "Location",
            eventOrganizer = "Organizer")
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
            eventPrice = largePrice,
            eventTitle = "Expensive Event",
            eventDate = "Dec 22, 2024",
            eventLocation = "Lausanne",
            eventOrganizer = "Organizer")
      }
    }

    // Then - Large price is displayed correctly
    composeTestRule.onNodeWithText("CHF$largePrice").assertIsDisplayed()
  }
}
