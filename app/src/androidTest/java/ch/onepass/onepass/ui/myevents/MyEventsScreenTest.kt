package ch.onepass.onepass.ui.myevents

import androidx.compose.foundation.layout.height
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.onepass.onepass.ui.theme.OnePassTheme
import org.junit.Rule
import org.junit.Test

class MyEventsScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val currentTickets =
      listOf(
          Ticket(
              "Lausanne Party", TicketStatus.CURRENTLY, "Dec 15, 2024 • 9:00 PM", "Lausanne, Flon"))

  private val expiredTickets =
      listOf(Ticket("Morges Party", TicketStatus.EXPIRED, "Nov 10, 2024 • 8:00 PM", "Morges"))

  private fun setContent() {
    composeTestRule.setContent {
      OnePassTheme {
        MyEventsContent(
            userQrData = "USER-QR-DATA",
            currentTickets = currentTickets,
            expiredTickets = expiredTickets)
      }
    }
  }

  @Test
  fun tabs_exist_and_switchingTabs_showsCorrectTickets() {
    setContent()

    // --- Verify tabs exist (replaces the old test 'tabsAreDisplayed_correctly')
    composeTestRule.onNodeWithTag(MyEventsTestTags.TABS_ROW).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MyEventsTestTags.TAB_CURRENT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MyEventsTestTags.TAB_EXPIRED).assertIsDisplayed()

    // --- Default state: current tab selected
    composeTestRule.onNodeWithText("Lausanne Party").assertIsDisplayed()
    composeTestRule.onNodeWithText("Morges Party").assertDoesNotExist()

    // --- Switch to Expired tab
    composeTestRule.onNodeWithTag(MyEventsTestTags.TAB_EXPIRED).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Morges Party").assertIsDisplayed()
    composeTestRule.onNodeWithText("Lausanne Party").assertDoesNotExist()
  }

  @Test
  fun clickingTicket_showsDetailsDialog() {
    setContent()
    // Click ticket card
    composeTestRule.onNodeWithTag(MyEventsTestTags.TICKET_CARD).performClick()
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(MyEventsTestTags.TICKET_DIALOG_TITLE)
        .assertTextEquals("Lausanne Party")
    composeTestRule.onNodeWithTag(MyEventsTestTags.TICKET_DIALOG_STATUS).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(MyEventsTestTags.TICKET_DIALOG_DATE)
        .assertTextEquals("Dec 15, 2024 • 9:00 PM")
    composeTestRule
        .onNodeWithTag(MyEventsTestTags.TICKET_DIALOG_LOCATION)
        .assertTextEquals("Lausanne, Flon")
  }

  @Test
  fun qrCodeCard_expandsOnClick() {
    setContent()

    val qrCard = composeTestRule.onNodeWithTag(MyEventsTestTags.QR_CODE_CARD)

    val initialHeight = qrCard.fetchSemanticsNode().boundsInRoot.height

    qrCard.performClick()
    composeTestRule.waitForIdle()

    val expandedHeight = qrCard.fetchSemanticsNode().boundsInRoot.height

    assert(expandedHeight > initialHeight) {
      "QR card did not expand (initial=$initialHeight, expanded=$expandedHeight)"
    }
  }
}
