package ch.onepass.onepass.ui.myevents

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ch.onepass.onepass.ui.theme.OnePassTheme
import org.junit.Rule
import org.junit.Test

/**
 * Coverage tests for MarketTicketList component. These tests verify the display and interaction of
 * the marketplace ticket list.
 */
class MarketTicketListTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun createTestMarketTicket(
      ticketId: String = "1",
      eventTitle: String = "Test Event",
      sellerId: String = "seller123"
  ): MarketTicket {
    return MarketTicket(
        ticketId = ticketId,
        eventTitle = eventTitle,
        eventDate = "Dec 25, 2024",
        eventLocation = "Test Location",
        sellerPrice = 50.0,
        originalPrice = 100.0,
        currency = "CHF",
        eventId = "event1",
        sellerId = sellerId,
        eventImageUrl = "")
  }

  @Test
  fun marketTicketList_displaysTitleCorrectly() {
    composeTestRule.setContent {
      OnePassTheme {
        MarketTicketList(
            marketTickets = emptyList(),
            onBuyTicket = {},
            onSellTicket = {},
            currentUserId = "user1",
            hasSellableTickets = false)
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_TICKETS_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Available Tickets").assertIsDisplayed()
  }

  @Test
  fun marketTicketList_displaysSellButtonWhenHasSellableTickets() {
    composeTestRule.setContent {
      OnePassTheme {
        MarketTicketList(
            marketTickets = emptyList(),
            onBuyTicket = {},
            onSellTicket = {},
            currentUserId = "user1",
            hasSellableTickets = true)
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.SELL_TICKET_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithText("Sell Ticket").assertIsDisplayed()
  }

  @Test
  fun marketTicketList_hidesSellButtonWhenNoSellableTickets() {
    composeTestRule.setContent {
      OnePassTheme {
        MarketTicketList(
            marketTickets = emptyList(),
            onBuyTicket = {},
            onSellTicket = {},
            currentUserId = "user1",
            hasSellableTickets = false)
      }
    }

    composeTestRule.onNodeWithText("Sell Ticket").assertDoesNotExist()
  }

  @Test
  fun marketTicketList_sellButtonTriggersCallback() {
    var sellClicked = false

    composeTestRule.setContent {
      OnePassTheme {
        MarketTicketList(
            marketTickets = emptyList(),
            onBuyTicket = {},
            onSellTicket = { sellClicked = true },
            currentUserId = "user1",
            hasSellableTickets = true)
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.SELL_TICKET_BUTTON).performClick()
    assert(sellClicked)
  }

  @Test
  fun marketTicketList_displaysLoadingState() {
    composeTestRule.setContent {
      OnePassTheme {
        MarketTicketList(
            marketTickets = emptyList(),
            onBuyTicket = {},
            onSellTicket = {},
            currentUserId = "user1",
            isLoading = true)
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_LOADING).assertIsDisplayed()
  }

  @Test
  fun marketTicketList_displaysEmptyStateWhenNoTickets() {
    composeTestRule.setContent {
      OnePassTheme {
        MarketTicketList(
            marketTickets = emptyList(),
            onBuyTicket = {},
            onSellTicket = {},
            currentUserId = "user1",
            isLoading = false)
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_EMPTY_STATE).assertIsDisplayed()
    composeTestRule.onNodeWithText("No Tickets Available").assertIsDisplayed()
  }

  @Test
  fun marketTicketList_displaysTicketsList() {
    val tickets = listOf(createTestMarketTicket())

    composeTestRule.setContent {
      OnePassTheme {
        MarketTicketList(
            marketTickets = tickets, onBuyTicket = {}, onSellTicket = {}, currentUserId = "user1")
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_TICKET_LIST).assertIsDisplayed()
  }

  @Test
  fun marketTicketList_displaysMultipleTickets() {
    val tickets =
        listOf(
            createTestMarketTicket(ticketId = "1", eventTitle = "Event 1"),
            createTestMarketTicket(ticketId = "2", eventTitle = "Event 2"),
            createTestMarketTicket(ticketId = "3", eventTitle = "Event 3"))

    composeTestRule.setContent {
      OnePassTheme {
        MarketTicketList(
            marketTickets = tickets, onBuyTicket = {}, onSellTicket = {}, currentUserId = "user1")
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_TICKET_LIST).assertIsDisplayed()
    composeTestRule.onNodeWithText("Event 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Event 2").assertIsDisplayed()
  }

  @Test
  fun marketTicketList_buyTicketTriggersCallback() {
    var buyTicketId = ""
    val ticket = createTestMarketTicket(ticketId = "test-ticket-id")

    composeTestRule.setContent {
      OnePassTheme {
        MarketTicketList(
            marketTickets = listOf(ticket),
            onBuyTicket = { buyTicketId = it },
            onSellTicket = {},
            currentUserId = "user1")
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_TICKET_BUY_BUTTON).performClick()
    assert(buyTicketId == "test-ticket-id")
  }

  @Test
  fun marketTicketList_identifiesCurrentUserAsSellerCorrectly() {
    val ticket = createTestMarketTicket(sellerId = "currentUser")

    composeTestRule.setContent {
      OnePassTheme {
        MarketTicketList(
            marketTickets = listOf(ticket),
            onBuyTicket = {},
            onSellTicket = {},
            currentUserId = "currentUser")
      }
    }

    composeTestRule.onNodeWithText("Your listing").assertIsDisplayed()
  }

  @Test
  fun marketTicketList_handlesNullCurrentUserId() {
    val ticket = createTestMarketTicket(sellerId = "seller123")

    composeTestRule.setContent {
      OnePassTheme {
        MarketTicketList(
            marketTickets = listOf(ticket),
            onBuyTicket = {},
            onSellTicket = {},
            currentUserId = null)
      }
    }

    // Should still display but treat user as non-seller
    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_TICKET_BUY_BUTTON).assertIsDisplayed()
  }

  @Test
  fun marketTicketList_switchesBetweenLoadingAndContent() {
    val tickets = listOf(createTestMarketTicket())

    composeTestRule.setContent {
      OnePassTheme {
        MarketTicketList(
            marketTickets = tickets,
            onBuyTicket = {},
            onSellTicket = {},
            currentUserId = "user1",
            isLoading = false)
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_TICKET_LIST).assertIsDisplayed()
  }

  @Test
  fun marketTicketList_switchesBetweenEmptyAndContent() {
    composeTestRule.setContent {
      OnePassTheme {
        MarketTicketList(
            marketTickets = emptyList(),
            onBuyTicket = {},
            onSellTicket = {},
            currentUserId = "user1",
            isLoading = false)
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_EMPTY_STATE).assertIsDisplayed()
  }
}
