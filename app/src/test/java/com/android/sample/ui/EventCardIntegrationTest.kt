package com.android.sample.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.resources.C
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Integration tests for EventCard component */
@RunWith(AndroidJUnit4::class)
class EventCardIntegrationTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun eventCard_allElementsDisplayCorrectly() {
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

    // Then - Verify all elements using test tags
    composeTestRule.onNodeWithTag(C.Tag.event_card).assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.Tag.event_card_image).assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.Tag.event_card_title).assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.Tag.event_card_title).assertTextEquals(title)
    composeTestRule.onNodeWithTag(C.Tag.event_card_organizer).assertTextEquals(organizer)
    composeTestRule.onNodeWithTag(C.Tag.event_card_date).assertTextEquals(date)
    composeTestRule.onNodeWithTag(C.Tag.event_card_location).assertTextEquals(location)
    composeTestRule.onNodeWithTag(C.Tag.event_card_price).assertTextEquals("CHF$price")
    composeTestRule.onNodeWithTag(C.Tag.event_card_like_button).assertIsDisplayed()
  }

  @Test
  fun eventCard_freeEvent_displaysFree() {
    // When
    composeTestRule.setContent {
      MaterialTheme {
        EventCard(
            eventPrice = 0u,
            eventTitle = "Free Event",
            eventDate = "Dec 22, 2024",
            eventLocation = "Lausanne",
            eventOrganizer = "Organizer")
      }
    }

    // Then - Verify FREE is displayed
    composeTestRule.onNodeWithTag(C.Tag.event_card_price).assertTextEquals("FREE")
  }

  @Test
  fun eventCard_likeButtonInteraction() {
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

    // Then - Click like button
    composeTestRule.onNodeWithTag(C.Tag.event_card_like_button).assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.Tag.event_card_like_button).performClick()
  }
}
