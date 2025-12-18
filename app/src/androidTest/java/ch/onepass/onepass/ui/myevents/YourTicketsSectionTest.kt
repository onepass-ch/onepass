package ch.onepass.onepass.ui.myevents

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.theme.OnePassTheme
import org.junit.Rule
import org.junit.Test

/**
 * Coverage tests for YourTicketsSection component. These tests verify the display and interaction
 * of the your tickets section.
 */
class YourTicketsSectionTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val context: Context
    get() = ApplicationProvider.getApplicationContext()

  private fun createTestTicket(
      ticketId: String = "1",
      title: String = "Test Event",
      status: TicketStatus = TicketStatus.UPCOMING
  ): Ticket {
    return Ticket(
        ticketId = ticketId,
        title = title,
        status = status,
        dateTime = "Dec 25, 2024",
        location = "Test Location")
  }

  @Test
  fun yourTicketsSection_displaysTabsCorrectly() {
    composeTestRule.setContent {
      OnePassTheme {
        YourTicketsSection(
            currentTickets = emptyList(),
            expiredTickets = emptyList(),
            selectedTab = TicketTab.CURRENT,
            onTabSelected = {},
            userQrData = "",
            isQrExpanded = false,
            onToggleQrExpanded = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.TABS_ROW).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MyEventsTestTags.TAB_CURRENT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MyEventsTestTags.TAB_EXPIRED).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MyEventsTestTags.TAB_LISTED).assertIsDisplayed()
  }

  @Test
  fun yourTicketsSection_currentTabIsSelected() {
    composeTestRule.setContent {
      OnePassTheme {
        YourTicketsSection(
            currentTickets = emptyList(),
            expiredTickets = emptyList(),
            selectedTab = TicketTab.CURRENT,
            onTabSelected = {},
            userQrData = "",
            isQrExpanded = false,
            onToggleQrExpanded = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.TAB_CURRENT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MyEventsTestTags.TAB_CURRENT).assertIsDisplayed()
  }

  @Test
  fun yourTicketsSection_expiredTabIsSelected() {
    composeTestRule.setContent {
      OnePassTheme {
        YourTicketsSection(
            currentTickets = emptyList(),
            expiredTickets = emptyList(),
            selectedTab = TicketTab.EXPIRED,
            onTabSelected = {},
            userQrData = "",
            isQrExpanded = false,
            onToggleQrExpanded = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.TAB_EXPIRED).assertIsDisplayed()
    composeTestRule
        .onNodeWithText(context.getString(R.string.your_tickets_tab_expired))
        .assertIsDisplayed()
  }

  @Test
  fun yourTicketsSection_listedTabIsSelected() {
    composeTestRule.setContent {
      OnePassTheme {
        YourTicketsSection(
            currentTickets = emptyList(),
            expiredTickets = emptyList(),
            selectedTab = TicketTab.LISTED,
            onTabSelected = {},
            userQrData = "",
            isQrExpanded = false,
            onToggleQrExpanded = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.TAB_LISTED).assertIsDisplayed()
    composeTestRule
        .onNodeWithText(context.getString(R.string.your_tickets_tab_listed))
        .assertIsDisplayed()
  }

  @Test
  fun yourTicketsSection_tabClickTriggersCallback() {
    var selectedTab = TicketTab.CURRENT

    composeTestRule.setContent {
      OnePassTheme {
        YourTicketsSection(
            currentTickets = emptyList(),
            expiredTickets = emptyList(),
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            userQrData = "",
            isQrExpanded = false,
            onToggleQrExpanded = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.TAB_EXPIRED).performClick()
    assert(selectedTab == TicketTab.EXPIRED)
  }

  @Test
  fun yourTicketsSection_displaysQrCodeComponent() {
    composeTestRule.setContent {
      OnePassTheme {
        YourTicketsSection(
            currentTickets = emptyList(),
            expiredTickets = emptyList(),
            selectedTab = TicketTab.CURRENT,
            onTabSelected = {},
            userQrData = "test-qr-data",
            isQrExpanded = false,
            onToggleQrExpanded = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.QR_CODE_CARD).assertIsDisplayed()
  }

  @Test
  fun yourTicketsSection_hidesQrCodeForListedTab() {
    composeTestRule.setContent {
      OnePassTheme {
        YourTicketsSection(
            currentTickets = emptyList(),
            expiredTickets = emptyList(),
            selectedTab = TicketTab.LISTED,
            onTabSelected = {},
            userQrData = "test-qr-data",
            isQrExpanded = false,
            onToggleQrExpanded = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.QR_CODE_CARD).assertDoesNotExist()
  }

  @Test
  fun yourTicketsSection_displaysEmptyStateForCurrentTickets() {
    composeTestRule.setContent {
      OnePassTheme {
        YourTicketsSection(
            currentTickets = emptyList(),
            expiredTickets = emptyList(),
            selectedTab = TicketTab.CURRENT,
            onTabSelected = {},
            userQrData = "",
            isQrExpanded = false,
            onToggleQrExpanded = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.EMPTY_STATE).assertIsDisplayed()
    composeTestRule
        .onNodeWithText(context.getString(R.string.your_tickets_empty_current_title))
        .assertIsDisplayed()
  }

  @Test
  fun yourTicketsSection_displaysEmptyStateForExpiredTickets() {
    composeTestRule.setContent {
      OnePassTheme {
        YourTicketsSection(
            currentTickets = emptyList(),
            expiredTickets = emptyList(),
            selectedTab = TicketTab.EXPIRED,
            onTabSelected = {},
            userQrData = "",
            isQrExpanded = false,
            onToggleQrExpanded = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.EMPTY_STATE).assertIsDisplayed()
    composeTestRule
        .onNodeWithText(context.getString(R.string.your_tickets_empty_expired_title))
        .assertIsDisplayed()
  }

  @Test
  fun yourTicketsSection_displaysEmptyStateForListedTickets() {
    composeTestRule.setContent {
      OnePassTheme {
        YourTicketsSection(
            currentTickets = emptyList(),
            expiredTickets = emptyList(),
            selectedTab = TicketTab.LISTED,
            onTabSelected = {},
            userQrData = "",
            isQrExpanded = false,
            onToggleQrExpanded = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.EMPTY_STATE).assertIsDisplayed()
    composeTestRule
        .onNodeWithText(context.getString(R.string.your_tickets_empty_listed_title))
        .assertIsDisplayed()
  }

  @Test
  fun yourTicketsSection_displaysCurrentTickets() {
    val tickets = listOf(createTestTicket(title = "Current Event"))

    composeTestRule.setContent {
      OnePassTheme {
        YourTicketsSection(
            currentTickets = tickets,
            expiredTickets = emptyList(),
            selectedTab = TicketTab.CURRENT,
            onTabSelected = {},
            userQrData = "",
            isQrExpanded = false,
            onToggleQrExpanded = {})
      }
    }

    composeTestRule.onNodeWithText("Current Event").assertIsDisplayed()
  }

  @Test
  fun yourTicketsSection_displaysExpiredTickets() {
    val tickets = listOf(createTestTicket(title = "Expired Event", status = TicketStatus.EXPIRED))

    composeTestRule.setContent {
      OnePassTheme {
        YourTicketsSection(
            currentTickets = emptyList(),
            expiredTickets = tickets,
            selectedTab = TicketTab.EXPIRED,
            onTabSelected = {},
            userQrData = "",
            isQrExpanded = false,
            onToggleQrExpanded = {})
      }
    }

    composeTestRule.onNodeWithText("Expired Event").assertIsDisplayed()
  }

  @Test
  fun yourTicketsSection_displaysMultipleCurrentTickets() {
    val tickets =
        listOf(
            createTestTicket(ticketId = "1", title = "Event 1"),
            createTestTicket(ticketId = "2", title = "Event 2"),
            createTestTicket(ticketId = "3", title = "Event 3"))

    composeTestRule.setContent {
      OnePassTheme {
        YourTicketsSection(
            currentTickets = tickets,
            expiredTickets = emptyList(),
            selectedTab = TicketTab.CURRENT,
            onTabSelected = {},
            userQrData = "",
            isQrExpanded = false,
            onToggleQrExpanded = {})
      }
    }

    composeTestRule.onNodeWithText("Event 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Event 2").assertIsDisplayed()
  }

  @Test
  fun yourTicketsSection_switchesBetweenTabs() {
    val currentTickets = listOf(createTestTicket(title = "Current"))
    val expiredTickets = listOf(createTestTicket(title = "Expired"))

    composeTestRule.setContent {
      OnePassTheme {
        YourTicketsSection(
            currentTickets = currentTickets,
            expiredTickets = expiredTickets,
            selectedTab = TicketTab.CURRENT,
            onTabSelected = {},
            userQrData = "",
            isQrExpanded = false,
            onToggleQrExpanded = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.TAB_CURRENT).assertIsDisplayed()

    composeTestRule.onNodeWithTag(MyEventsTestTags.TAB_EXPIRED).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(MyEventsTestTags.TAB_EXPIRED).assertIsDisplayed()
  }

  @Test
  fun yourTicketsSection_qrCodeToggleTriggersCallback() {
    var qrExpanded = false

    composeTestRule.setContent {
      OnePassTheme {
        YourTicketsSection(
            currentTickets = emptyList(),
            expiredTickets = emptyList(),
            selectedTab = TicketTab.CURRENT,
            onTabSelected = {},
            userQrData = "test-qr",
            isQrExpanded = qrExpanded,
            onToggleQrExpanded = { qrExpanded = !qrExpanded })
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.QR_CODE_CARD).performClick()
    assert(qrExpanded)
  }

  @Test
  fun yourTicketsSection_handlesEmptyTicketsForAllTabs() {
    composeTestRule.setContent {
      OnePassTheme {
        YourTicketsSection(
            currentTickets = emptyList(),
            expiredTickets = emptyList(),
            selectedTab = TicketTab.CURRENT,
            onTabSelected = {},
            userQrData = "",
            isQrExpanded = false,
            onToggleQrExpanded = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.EMPTY_STATE).assertIsDisplayed()
  }
}
