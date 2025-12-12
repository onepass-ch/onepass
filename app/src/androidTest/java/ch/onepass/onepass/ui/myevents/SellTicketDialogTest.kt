package ch.onepass.onepass.ui.myevents

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import ch.onepass.onepass.model.ticket.Ticket
import ch.onepass.onepass.ui.theme.OnePassTheme
import org.junit.Rule
import org.junit.Test

/**
 * Coverage tests for SellTicketDialog component. These tests verify the display and interaction of
 * the sell ticket dialog.
 */
class SellTicketDialogTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun createTestSellableTicket(
      ticketId: String = "1",
      eventTitle: String = "Test Event",
      originalPrice: Double = 100.0
  ): SellableTicket {
    val rawTicket = Ticket(ticketId = ticketId, eventId = "event1", ownerId = "user1")
    return SellableTicket(
        ticketId = ticketId,
        eventId = "event1",
        eventTitle = eventTitle,
        eventDate = "Dec 25, 2024",
        eventLocation = "Test Location",
        originalPrice = originalPrice,
        currency = "CHF",
        rawTicket = rawTicket)
  }

  @Test
  fun sellTicketDialog_hidesWhenShowDialogIsFalse() {
    composeTestRule.setContent {
      OnePassTheme {
        SellTicketDialog(
            showDialog = false,
            onDismiss = {},
            sellableTickets = emptyList(),
            selectedTicket = null,
            onTicketSelected = {},
            sellingPrice = "",
            onPriceChange = {},
            onListForSale = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.SELL_DIALOG).assertDoesNotExist()
  }

  @Test
  fun sellTicketDialog_displaysWhenShowDialogIsTrue() {
    composeTestRule.setContent {
      OnePassTheme {
        SellTicketDialog(
            showDialog = true,
            onDismiss = {},
            sellableTickets = emptyList(),
            selectedTicket = null,
            onTicketSelected = {},
            sellingPrice = "",
            onPriceChange = {},
            onListForSale = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.SELL_DIALOG).assertIsDisplayed()
  }

  @Test
  fun sellTicketDialog_displaysTitleCorrectly() {
    composeTestRule.setContent {
      OnePassTheme {
        SellTicketDialog(
            showDialog = true,
            onDismiss = {},
            sellableTickets = emptyList(),
            selectedTicket = null,
            onTicketSelected = {},
            sellingPrice = "",
            onPriceChange = {},
            onListForSale = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.SELL_DIALOG_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Sell Your Ticket").assertIsDisplayed()
  }

  @Test
  fun sellTicketDialog_displaysErrorMessage() {
    composeTestRule.setContent {
      OnePassTheme {
        SellTicketDialog(
            showDialog = true,
            onDismiss = {},
            sellableTickets = emptyList(),
            selectedTicket = null,
            onTicketSelected = {},
            sellingPrice = "",
            onPriceChange = {},
            onListForSale = {},
            errorMessage = "Test error message")
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.SELL_DIALOG_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithText("Test error message").assertIsDisplayed()
  }

  @Test
  fun sellTicketDialog_displaysTicketList() {
    val ticket = createTestSellableTicket()

    composeTestRule.setContent {
      OnePassTheme {
        SellTicketDialog(
            showDialog = true,
            onDismiss = {},
            sellableTickets = listOf(ticket),
            selectedTicket = null,
            onTicketSelected = {},
            sellingPrice = "",
            onPriceChange = {},
            onListForSale = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.SELL_DIALOG_TICKET_LIST).assertIsDisplayed()
  }

  @Test
  fun sellTicketDialog_displaysEmptyStateWhenNoTickets() {
    composeTestRule.setContent {
      OnePassTheme {
        SellTicketDialog(
            showDialog = true,
            onDismiss = {},
            sellableTickets = emptyList(),
            selectedTicket = null,
            onTicketSelected = {},
            sellingPrice = "",
            onPriceChange = {},
            onListForSale = {})
      }
    }

    composeTestRule.onNodeWithText("No tickets available to sell").assertIsDisplayed()
  }

  @Test
  fun sellTicketDialog_displaysPriceInputWhenTicketSelected() {
    val ticket = createTestSellableTicket()

    composeTestRule.setContent {
      OnePassTheme {
        SellTicketDialog(
            showDialog = true,
            onDismiss = {},
            sellableTickets = listOf(ticket),
            selectedTicket = ticket,
            onTicketSelected = {},
            sellingPrice = "",
            onPriceChange = {},
            onListForSale = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.SELL_DIALOG_PRICE_INPUT).assertIsDisplayed()
  }

  @Test
  fun sellTicketDialog_priceInputTriggersCallback() {
    var newPrice = ""
    val ticket = createTestSellableTicket()

    composeTestRule.setContent {
      OnePassTheme {
        SellTicketDialog(
            showDialog = true,
            onDismiss = {},
            sellableTickets = listOf(ticket),
            selectedTicket = ticket,
            onTicketSelected = {},
            sellingPrice = "",
            onPriceChange = { newPrice = it },
            onListForSale = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.SELL_DIALOG_PRICE_INPUT).performTextInput("50")
    assert(newPrice.contains("50"))
  }

  @Test
  fun sellTicketDialog_confirmButtonDisabledWhenNoTicketSelected() {
    composeTestRule.setContent {
      OnePassTheme {
        SellTicketDialog(
            showDialog = true,
            onDismiss = {},
            sellableTickets = listOf(createTestSellableTicket()),
            selectedTicket = null,
            onTicketSelected = {},
            sellingPrice = "",
            onPriceChange = {},
            onListForSale = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.SELL_DIALOG_CONFIRM_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun sellTicketDialog_confirmButtonDisabledWhenNoPriceEntered() {
    val ticket = createTestSellableTicket()

    composeTestRule.setContent {
      OnePassTheme {
        SellTicketDialog(
            showDialog = true,
            onDismiss = {},
            sellableTickets = listOf(ticket),
            selectedTicket = ticket,
            onTicketSelected = {},
            sellingPrice = "",
            onPriceChange = {},
            onListForSale = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.SELL_DIALOG_CONFIRM_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun sellTicketDialog_confirmButtonEnabledWhenValidInputs() {
    val ticket = createTestSellableTicket()

    composeTestRule.setContent {
      OnePassTheme {
        SellTicketDialog(
            showDialog = true,
            onDismiss = {},
            sellableTickets = listOf(ticket),
            selectedTicket = ticket,
            onTicketSelected = {},
            sellingPrice = "50.00",
            onPriceChange = {},
            onListForSale = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.SELL_DIALOG_CONFIRM_BUTTON).assertIsEnabled()
  }

  @Test
  fun sellTicketDialog_confirmButtonDisabledWhenLoading() {
    val ticket = createTestSellableTicket()

    composeTestRule.setContent {
      OnePassTheme {
        SellTicketDialog(
            showDialog = true,
            onDismiss = {},
            sellableTickets = listOf(ticket),
            selectedTicket = ticket,
            onTicketSelected = {},
            sellingPrice = "50.00",
            onPriceChange = {},
            onListForSale = {},
            isLoading = true)
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.SELL_DIALOG_CONFIRM_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun sellTicketDialog_confirmButtonTriggersCallback() {
    var confirmCalled = false
    val ticket = createTestSellableTicket()

    composeTestRule.setContent {
      OnePassTheme {
        SellTicketDialog(
            showDialog = true,
            onDismiss = {},
            sellableTickets = listOf(ticket),
            selectedTicket = ticket,
            onTicketSelected = {},
            sellingPrice = "50.00",
            onPriceChange = {},
            onListForSale = { confirmCalled = true })
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.SELL_DIALOG_CONFIRM_BUTTON).performClick()
    assert(confirmCalled)
  }

  @Test
  fun sellTicketDialog_cancelButtonTriggersCallback() {
    var dismissCalled = false

    composeTestRule.setContent {
      OnePassTheme {
        SellTicketDialog(
            showDialog = true,
            onDismiss = { dismissCalled = true },
            sellableTickets = emptyList(),
            selectedTicket = null,
            onTicketSelected = {},
            sellingPrice = "",
            onPriceChange = {},
            onListForSale = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.SELL_DIALOG_CANCEL_BUTTON).performClick()
    assert(dismissCalled)
  }

  @Test
  fun sellTicketDialog_handlesMultipleSellableTickets() {
    val tickets =
        listOf(
            createTestSellableTicket(ticketId = "1", eventTitle = "Event 1"),
            createTestSellableTicket(ticketId = "2", eventTitle = "Event 2"),
            createTestSellableTicket(ticketId = "3", eventTitle = "Event 3"))

    composeTestRule.setContent {
      OnePassTheme {
        SellTicketDialog(
            showDialog = true,
            onDismiss = {},
            sellableTickets = tickets,
            selectedTicket = null,
            onTicketSelected = {},
            sellingPrice = "",
            onPriceChange = {},
            onListForSale = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.SELL_DIALOG_TICKET_LIST).assertIsDisplayed()
  }

  @Test
  fun sellTicketDialog_displaysSelectedTicketDetails() {
    val ticket = createTestSellableTicket(eventTitle = "Selected Event")

    composeTestRule.setContent {
      OnePassTheme {
        SellTicketDialog(
            showDialog = true,
            onDismiss = {},
            sellableTickets = listOf(ticket),
            selectedTicket = ticket,
            onTicketSelected = {},
            sellingPrice = "",
            onPriceChange = {},
            onListForSale = {})
      }
    }

    composeTestRule.onAllNodesWithText("Selected Event", useUnmergedTree = true)[0].assertExists()
  }

  @Test
  fun sellTicketDialog_confirmButtonDisabledForInvalidPrice() {
    val ticket = createTestSellableTicket()

    composeTestRule.setContent {
      OnePassTheme {
        SellTicketDialog(
            showDialog = true,
            onDismiss = {},
            sellableTickets = listOf(ticket),
            selectedTicket = ticket,
            onTicketSelected = {},
            sellingPrice = "0",
            onPriceChange = {},
            onListForSale = {})
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.SELL_DIALOG_CONFIRM_BUTTON).assertIsNotEnabled()
  }
}
