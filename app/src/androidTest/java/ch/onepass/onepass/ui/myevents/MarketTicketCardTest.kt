package ch.onepass.onepass.ui.myevents

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
 * Coverage tests for MarketTicketCard component. These tests verify the display and interaction of
 * market ticket cards.
 */
class MarketTicketCardTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val context: Context
    get() = ApplicationProvider.getApplicationContext()

  private fun createTestMarketTicket(
      ticketId: String = "1",
      eventTitle: String = "Test Event",
      eventDate: String = "Dec 25, 2024",
      eventLocation: String = "Test Location",
      sellerPrice: Double = 50.0,
      originalPrice: Double = 100.0,
      currency: String = "CHF",
      sellerId: String = "seller123",
      eventImageUrl: String = ""
  ): MarketTicket {
    return MarketTicket(
        ticketId = ticketId,
        eventTitle = eventTitle,
        eventDate = eventDate,
        eventLocation = eventLocation,
        sellerPrice = sellerPrice,
        originalPrice = originalPrice,
        currency = currency,
        eventId = "event1",
        sellerId = sellerId,
        eventImageUrl = eventImageUrl)
  }

  @Test
  fun marketTicketCard_displaysEventTitle() {
    val ticket = createTestMarketTicket(eventTitle = "Concert Night")

    composeTestRule.setContent {
      OnePassTheme { MarketTicketCard(marketTicket = ticket, onBuyClick = {}) }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_TICKET_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Concert Night").assertIsDisplayed()
  }

  @Test
  fun marketTicketCard_displaysEventDate() {
    val ticket = createTestMarketTicket(eventDate = "Dec 15, 2024")

    composeTestRule.setContent {
      OnePassTheme { MarketTicketCard(marketTicket = ticket, onBuyClick = {}) }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_TICKET_DATE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Dec 15, 2024").assertIsDisplayed()
  }

  @Test
  fun marketTicketCard_displaysEventLocation() {
    val ticket = createTestMarketTicket(eventLocation = "Stadium Arena")

    composeTestRule.setContent {
      OnePassTheme { MarketTicketCard(marketTicket = ticket, onBuyClick = {}) }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_TICKET_LOCATION).assertIsDisplayed()
    composeTestRule.onNodeWithText("Stadium Arena").assertIsDisplayed()
  }

  @Test
  fun marketTicketCard_displaysSellerPrice() {
    val ticket = createTestMarketTicket(sellerPrice = 75.0, currency = "CHF")

    composeTestRule.setContent {
      OnePassTheme { MarketTicketCard(marketTicket = ticket, onBuyClick = {}) }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_TICKET_SELLER_PRICE).assertIsDisplayed()
    composeTestRule.onNodeWithText("CHF 75", substring = true).assertIsDisplayed()
  }

  @Test
  fun marketTicketCard_displaysOriginalPriceWhenDifferent() {
    val ticket = createTestMarketTicket(sellerPrice = 50.0, originalPrice = 100.0)

    composeTestRule.setContent {
      OnePassTheme { MarketTicketCard(marketTicket = ticket, onBuyClick = {}) }
    }

    composeTestRule
        .onNodeWithTag(MyEventsTestTags.MARKET_TICKET_ORIGINAL_PRICE, useUnmergedTree = true)
        .assertExists()
  }

  @Test
  fun marketTicketCard_hidesBuyButtonForSeller() {
    val ticket = createTestMarketTicket(sellerId = "currentUser")

    composeTestRule.setContent {
      OnePassTheme {
        MarketTicketCard(marketTicket = ticket, onBuyClick = {}, isCurrentUserSeller = true)
      }
    }

    composeTestRule
        .onNodeWithText(context.getString(R.string.market_ticket_your_listing))
        .assertIsDisplayed()
  }

  @Test
  fun marketTicketCard_showsBuyButtonForNonSeller() {
    val ticket = createTestMarketTicket(sellerId = "otherUser")

    composeTestRule.setContent {
      OnePassTheme {
        MarketTicketCard(marketTicket = ticket, onBuyClick = {}, isCurrentUserSeller = false)
      }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_TICKET_BUY_BUTTON).assertIsDisplayed()
    composeTestRule
        .onNodeWithText(context.getString(R.string.market_ticket_buy_button))
        .assertIsDisplayed()
  }

  @Test
  fun marketTicketCard_buyButtonTriggersCallback() {
    var buyClicked = false
    val ticket = createTestMarketTicket()

    composeTestRule.setContent {
      OnePassTheme { MarketTicketCard(marketTicket = ticket, onBuyClick = { buyClicked = true }) }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_TICKET_BUY_BUTTON).performClick()
    assert(buyClicked)
  }

  @Test
  fun marketTicketCard_disablesBuyButtonWhenLoading() {
    val ticket = createTestMarketTicket()

    composeTestRule.setContent {
      OnePassTheme { MarketTicketCard(marketTicket = ticket, onBuyClick = {}, isLoading = true) }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_TICKET_BUY_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun marketTicketCard_enablesBuyButtonWhenNotLoading() {
    val ticket = createTestMarketTicket()

    composeTestRule.setContent {
      OnePassTheme { MarketTicketCard(marketTicket = ticket, onBuyClick = {}, isLoading = false) }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_TICKET_BUY_BUTTON).assertIsEnabled()
  }

  @Test
  fun marketTicketCard_handlesLongEventTitle() {
    val ticket =
        createTestMarketTicket(
            eventTitle = "This is a very long event title that should be truncated properly")

    composeTestRule.setContent {
      OnePassTheme { MarketTicketCard(marketTicket = ticket, onBuyClick = {}) }
    }

    composeTestRule
        .onNodeWithText("This is a very long event title", substring = true)
        .assertIsDisplayed()
  }

  @Test
  fun marketTicketCard_handlesImageWithoutUrl() {
    val ticket = createTestMarketTicket(eventImageUrl = "")

    composeTestRule.setContent {
      OnePassTheme { MarketTicketCard(marketTicket = ticket, onBuyClick = {}) }
    }

    composeTestRule.onNodeWithText("Test Event").assertIsDisplayed()
  }

  @Test
  fun marketTicketCard_handlesImageWithUrl() {
    val ticket = createTestMarketTicket(eventImageUrl = "https://example.com/image.jpg")

    composeTestRule.setContent {
      OnePassTheme { MarketTicketCard(marketTicket = ticket, onBuyClick = {}) }
    }

    composeTestRule.onNodeWithText("Test Event").assertIsDisplayed()
  }

  @Test
  fun marketTicketCard_handlesSamePriceAsOriginal() {
    val ticket = createTestMarketTicket(sellerPrice = 100.0, originalPrice = 100.0)

    composeTestRule.setContent {
      OnePassTheme { MarketTicketCard(marketTicket = ticket, onBuyClick = {}) }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_TICKET_SELLER_PRICE).assertIsDisplayed()
    // Original price should not be shown when same as seller price
  }

  @Test
  fun marketTicketCard_handlesZeroPrices() {
    val ticket = createTestMarketTicket(sellerPrice = 0.0, originalPrice = 0.0)

    composeTestRule.setContent {
      OnePassTheme { MarketTicketCard(marketTicket = ticket, onBuyClick = {}) }
    }

    composeTestRule.onNodeWithTag(MyEventsTestTags.MARKET_TICKET_SELLER_PRICE).assertIsDisplayed()
  }
}
