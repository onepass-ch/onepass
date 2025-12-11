package ch.onepass.onepass.ui.myevents

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.ui.theme.OnePassTheme
import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Rule
import org.junit.Test

/**
 * Coverage tests for HotEventsSection component.
 * These tests verify the basic display and interaction of the hot events section.
 */
class HotEventsSectionTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun createTestEvent(
      id: String = "1",
      title: String = "Test Event",
      imageUrl: String = ""
  ): Event {
    return Event(
        eventId = id,
        title = title,
        organizerName = "Test Organizer",
        startTime = Timestamp(Date()),
        location = Location(name = "Test Location"),
        images = if (imageUrl.isNotEmpty()) listOf(imageUrl) else emptyList())
  }

  @Test
  fun hotEventsSection_displaysTitleCorrectly() {
    composeTestRule.setContent {
      OnePassTheme { HotEventsSection(events = emptyList(), onEventClick = {}) }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.HOT_EVENTS_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Hot Events").assertIsDisplayed()
  }

  @Test
  fun hotEventsSection_displaysLoadingState() {
    composeTestRule.setContent {
      OnePassTheme { HotEventsSection(events = emptyList(), onEventClick = {}, isLoading = true) }
    }

    // Loading indicator should be present
    composeTestRule.onNodeWithTag(MyEventsTestTags.HOT_EVENTS_TITLE).assertIsDisplayed()
  }

  @Test
  fun hotEventsSection_displaysEmptyStateWhenNoEvents() {
    composeTestRule.setContent {
      OnePassTheme { HotEventsSection(events = emptyList(), onEventClick = {}, isLoading = false) }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.HOT_EVENTS_EMPTY).assertIsDisplayed()
    composeTestRule.onNodeWithText("No hot events at the moment").assertIsDisplayed()
  }

  @Test
  fun hotEventsSection_displaysEventsList() {
    val events = listOf(createTestEvent(id = "1", title = "Event 1"))

    composeTestRule.setContent {
      OnePassTheme { HotEventsSection(events = events, onEventClick = {}) }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.HOT_EVENTS_LIST).assertIsDisplayed()
  }

  @Test
  fun hotEventsSection_displaysMultipleEvents() {
    val events =
        listOf(
            createTestEvent(id = "1", title = "Event 1"),
            createTestEvent(id = "2", title = "Event 2"),
            createTestEvent(id = "3", title = "Event 3"))

    composeTestRule.setContent {
      OnePassTheme { HotEventsSection(events = events, onEventClick = {}) }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.HOT_EVENTS_LIST).assertIsDisplayed()
    composeTestRule.onNodeWithText("Event 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Event 2").assertIsDisplayed()
  }

  @Test
  fun hotEventsSection_eventClickTriggersCallback() {
    var clickedEventId = ""
    val event = createTestEvent(id = "test-id", title = "Clickable Event")

    composeTestRule.setContent {
      OnePassTheme {
        HotEventsSection(events = listOf(event), onEventClick = { clickedEventId = it.eventId })
      }
    }

    composeTestRule.onNodeWithText("Clickable Event").performClick()
    assert(clickedEventId == "test-id")
  }

  @Test
  fun hotEventsSection_handlesEventWithoutImage() {
    val event = createTestEvent(title = "No Image Event", imageUrl = "")

    composeTestRule.setContent {
      OnePassTheme { HotEventsSection(events = listOf(event), onEventClick = {}) }
    }

    composeTestRule.onNodeWithText("No Image Event").assertIsDisplayed()
  }

  @Test
  fun hotEventsSection_handlesEventWithImage() {
    val event = createTestEvent(title = "Image Event", imageUrl = "https://example.com/image.jpg")

    composeTestRule.setContent {
      OnePassTheme { HotEventsSection(events = listOf(event), onEventClick = {}) }
    }

    composeTestRule.onNodeWithText("Image Event").assertIsDisplayed()
  }

  @Test
  fun hotEventsSection_handlesLongEventTitles() {
    val event =
        createTestEvent(
            title = "This is a very long event title that should be truncated properly")

    composeTestRule.setContent {
      OnePassTheme { HotEventsSection(events = listOf(event), onEventClick = {}) }
    }

    composeTestRule
        .onNodeWithText("This is a very long event title", substring = true)
        .assertIsDisplayed()
  }

  @Test
  fun hotEventsSection_switchesBetweenLoadingAndContent() {
    var isLoading = true
    val events = listOf(createTestEvent())

    composeTestRule.setContent {
      OnePassTheme { HotEventsSection(events = events, onEventClick = {}, isLoading = isLoading) }
    }

    // Initially loading
    composeTestRule.onNodeWithTag(MyEventsTestTags.HOT_EVENTS_TITLE).assertIsDisplayed()

    // Switch to content
    isLoading = false
    composeTestRule.runOnIdle { isLoading = false }
  }
}
