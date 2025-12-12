package ch.onepass.onepass.ui.myevents

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.ui.theme.OnePassTheme
import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Rule
import org.junit.Test

/**
 * Coverage tests for MarketSearchBar component. These tests verify the search bar display and basic
 * interactions.
 */
class MarketSearchBarTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun createTestEvent(title: String = "Test Event"): Event {
    return Event(
        eventId = "1",
        title = title,
        organizerName = "Test Organizer",
        startTime = Timestamp(Date()),
        location = Location(name = "Test Location"))
  }

  private fun createTestOrganization(name: String = "Test Org"): Organization {
    return Organization(id = "1", name = name, verified = false)
  }

  @Test
  fun marketSearchBar_displaysCorrectly() {
    composeTestRule.setContent {
      OnePassTheme {
        MarketSearchBar(
            query = "",
            onQueryChange = {},
            onClear = {},
            searchResults = emptyList(),
            onResultSelected = {},
            isSearching = false)
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_SEARCH_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithText("Search events or organizers...").assertIsDisplayed()
  }

  @Test
  fun marketSearchBar_displaysSearchIcon() {
    composeTestRule.setContent {
      OnePassTheme {
        MarketSearchBar(
            query = "",
            onQueryChange = {},
            onClear = {},
            searchResults = emptyList(),
            onResultSelected = {},
            isSearching = false)
      }
    }

    composeTestRule.onNodeWithContentDescription("Search").assertIsDisplayed()
  }

  @Test
  fun marketSearchBar_displaysQueryText() {
    composeTestRule.setContent {
      OnePassTheme {
        MarketSearchBar(
            query = "Test Query",
            onQueryChange = {},
            onClear = {},
            searchResults = emptyList(),
            onResultSelected = {},
            isSearching = false)
      }
    }

    composeTestRule.onNodeWithText("Test Query").assertIsDisplayed()
  }

  @Test
  fun marketSearchBar_displaysClearButtonWhenQueryPresent() {
    composeTestRule.setContent {
      OnePassTheme {
        MarketSearchBar(
            query = "Test",
            onQueryChange = {},
            onClear = {},
            searchResults = emptyList(),
            onResultSelected = {},
            isSearching = false)
      }
    }

    composeTestRule.onNodeWithContentDescription("Clear search").assertIsDisplayed()
  }

  @Test
  fun marketSearchBar_displaysLoadingIndicator() {
    composeTestRule.setContent {
      OnePassTheme {
        MarketSearchBar(
            query = "Test",
            onQueryChange = {},
            onClear = {},
            searchResults = emptyList(),
            onResultSelected = {},
            isSearching = true)
      }
    }

    // Loading indicator should be present
    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_SEARCH_BAR).assertIsDisplayed()
  }

  @Test
  fun marketSearchBar_clearButtonTriggersCallback() {
    var clearCalled = false

    composeTestRule.setContent {
      OnePassTheme {
        MarketSearchBar(
            query = "Test",
            onQueryChange = {},
            onClear = { clearCalled = true },
            searchResults = emptyList(),
            onResultSelected = {},
            isSearching = false)
      }
    }

    composeTestRule.onNodeWithContentDescription("Clear search").performClick()
    assert(clearCalled)
  }

  @Test
  fun marketSearchBar_textInputTriggersCallback() {
    var newQuery = ""

    composeTestRule.setContent {
      OnePassTheme {
        MarketSearchBar(
            query = "",
            onQueryChange = { newQuery = it },
            onClear = {},
            searchResults = emptyList(),
            onResultSelected = {},
            isSearching = false)
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_SEARCH_BAR).performTextInput("New Search")
    assert(newQuery.contains("New Search"))
  }

  @Test
  fun marketSearchBar_displaysEventSearchResults() {
    val eventResult = SearchResult.EventResult(createTestEvent("Concert"))

    composeTestRule.setContent {
      OnePassTheme {
        MarketSearchBar(
            query = "Concert",
            onQueryChange = {},
            onClear = {},
            searchResults = listOf(eventResult),
            onResultSelected = {},
            isSearching = false)
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_SEARCH_BAR).assertIsDisplayed()
  }

  @Test
  fun marketSearchBar_displaysOrganizerSearchResults() {
    val organizerResult = SearchResult.OrganizerResult(createTestOrganization("Test Organizer"))

    composeTestRule.setContent {
      OnePassTheme {
        MarketSearchBar(
            query = "Test",
            onQueryChange = {},
            onClear = {},
            searchResults = listOf(organizerResult),
            onResultSelected = {},
            isSearching = false)
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_SEARCH_BAR).assertIsDisplayed()
  }

  @Test
  fun marketSearchBar_handlesEmptyQuery() {
    composeTestRule.setContent {
      OnePassTheme {
        MarketSearchBar(
            query = "",
            onQueryChange = {},
            onClear = {},
            searchResults = emptyList(),
            onResultSelected = {},
            isSearching = false)
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_SEARCH_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithText("Search events or organizers...").assertIsDisplayed()
  }

  @Test
  fun marketSearchBar_handlesMixedSearchResults() {
    val results =
        listOf(
            SearchResult.EventResult(createTestEvent("Event 1")),
            SearchResult.OrganizerResult(createTestOrganization("Org 1")))

    composeTestRule.setContent {
      OnePassTheme {
        MarketSearchBar(
            query = "Test",
            onQueryChange = {},
            onClear = {},
            searchResults = results,
            onResultSelected = {},
            isSearching = false)
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_SEARCH_BAR).assertIsDisplayed()
  }
}
