package ch.onepass.onepass.ui.eventdetail

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.event.PricingTier
import ch.onepass.onepass.model.map.Location
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationStatus
import ch.onepass.onepass.ui.theme.OnePassTheme
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.util.Calendar
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EventDetailScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val testEventId = "test-event-id"
  private val testOrganizerId = "test-organizer-id"

  @Test
  fun eventDetailScreen_displaysLoadingState() {
    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState = EventDetailUiState(isLoading = true),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule
        .onNodeWithTag(EventDetailTestTags.LOADING, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun eventDetailScreen_displaysErrorState_and_goBackWorks() {
    var backClicked = false

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState = EventDetailUiState(errorMessage = "Network error"),
            onBack = { backClicked = true },
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule
        .onNodeWithTag(EventDetailTestTags.ERROR, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Network error", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Go Back", useUnmergedTree = true).performClick()
    assertTrue(backClicked)
  }

  @Test
  fun eventDetailScreen_displaysMainEventElements_and_interactions() {
    val testEvent = createTestEvent(title = "Amazing Event")

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState = EventDetailUiState(event = testEvent, isLiked = false),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    // Main elements
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.EVENT_TITLE, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Amazing Event", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.EVENT_IMAGE, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.LIKE_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()

    // Date & location elements
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.EVENT_DATE, useUnmergedTree = true)
        .assertExists()
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.EVENT_LOCATION, useUnmergedTree = true)
        .assertExists()
    composeTestRule.onNodeWithText("Test Location", useUnmergedTree = true).assertExists()

    // Map button clickable
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.MAP_BUTTON)
        .assertExists()
        .assertHasClickAction()
        .performClick()
  }

  @Test
  fun eventDetailScreen_buyTicketButtonIsClickable_and_displaysPrices() {
    // Buy button clickable
    val testEvent = createTestEvent()
    var buyClicked = false

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState = EventDetailUiState(event = testEvent),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = { buyClicked = true })
      }
    }

    composeTestRule
        .onNodeWithTag(EventDetailTestTags.BUY_TICKET_BUTTON, useUnmergedTree = true)
        .performClick()
    assertTrue(buyClicked)
  }

  @Test
  fun eventDetailScreen_displaysPaidPriceFormat() {
    val paidEvent = createTestEvent(lowestPrice = 35u)

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState = EventDetailUiState(event = paidEvent),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule
        .onNodeWithText("Buy ticket for 35chf", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun eventDetailScreen_displaysFreePriceFormat() {
    val freeEvent = createTestEvent(lowestPrice = 0u)

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState = EventDetailUiState(event = freeEvent),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule.onNodeWithText("FREE", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun eventDetailScreen_hidesRatingWhenZero_and_descriptionFallback() {
    val testEvent = createTestEvent(description = "")
    val testOrganization = createTestOrganization(averageRating = 0f)

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState = EventDetailUiState(event = testEvent, organization = testOrganization),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule
        .onNodeWithTag(EventDetailTestTags.ORGANIZER_RATING, useUnmergedTree = true)
        .assertDoesNotExist()
    composeTestRule
        .onNodeWithText("No description available.", useUnmergedTree = true)
        .assertExists()
  }

  // ==================== Payment State UI Tests ====================

  @Test
  fun eventDetailScreen_displaysLoadingOverlay_whenCreatingPaymentIntent() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState =
                EventDetailUiState(
                    event = testEvent, paymentState = PaymentState.CreatingPaymentIntent),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    // Loading overlay should be displayed
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.PAYMENT_LOADING, useUnmergedTree = true)
        .assertIsDisplayed()

    // Loading message should be displayed
    composeTestRule
        .onNodeWithText("Preparing payment...", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun eventDetailScreen_displaysLoadingOverlay_whenProcessingPayment() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState =
                EventDetailUiState(
                    event = testEvent, paymentState = PaymentState.ProcessingPayment),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    // Loading overlay should be displayed
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.PAYMENT_LOADING, useUnmergedTree = true)
        .assertIsDisplayed()

    // Processing message should be displayed
    composeTestRule
        .onNodeWithText("Processing payment...", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun eventDetailScreen_buyButton_showsLoadingIndicator_whenPaymentInProgress() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState =
                EventDetailUiState(
                    event = testEvent, paymentState = PaymentState.CreatingPaymentIntent),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    // Buy button should exist and be disabled
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.BUY_TICKET_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertIsNotEnabled()
  }

  @Test
  fun eventDetailScreen_buyButton_isEnabled_whenPaymentIsIdle() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState = EventDetailUiState(event = testEvent, paymentState = PaymentState.Idle),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    // Buy button should be enabled
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.BUY_TICKET_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertIsEnabled()
  }

  @Test
  fun eventDetailScreen_buyButton_isDisabled_whenProcessingPayment() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState =
                EventDetailUiState(
                    event = testEvent, paymentState = PaymentState.ProcessingPayment),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    // Buy button should be disabled
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.BUY_TICKET_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertIsNotEnabled()
  }

  @Test
  fun eventDetailScreen_noLoadingOverlay_whenPaymentIsIdle() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState = EventDetailUiState(event = testEvent, paymentState = PaymentState.Idle),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    // Loading overlay should not be displayed
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.PAYMENT_LOADING, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun eventDetailScreen_noLoadingOverlay_whenPaymentReadyToPay() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState =
                EventDetailUiState(
                    event = testEvent,
                    paymentState =
                        PaymentState.ReadyToPay(
                            clientSecret = "test_secret", paymentIntentId = "test_id")),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    // Loading overlay should not be displayed (payment sheet is shown by Stripe)
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.PAYMENT_LOADING, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun eventDetailScreen_buyButton_triggersPayment_whenClicked() {
    val testEvent = createTestEvent()
    var buyTicketCalled = false

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState = EventDetailUiState(event = testEvent, paymentState = PaymentState.Idle),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = { buyTicketCalled = true })
      }
    }

    // Click buy button
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.BUY_TICKET_BUTTON, useUnmergedTree = true)
        .performClick()

    // Verify callback was invoked
    assertTrue(buyTicketCalled)
  }

  @Test
  fun eventDetailScreen_buyButton_doesNotTrigger_whenPaymentInProgress() {
    val testEvent = createTestEvent()
    var buyTicketCalled = false

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState =
                EventDetailUiState(
                    event = testEvent, paymentState = PaymentState.CreatingPaymentIntent),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = { buyTicketCalled = true })
      }
    }

    // Try to click buy button (should be disabled)
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.BUY_TICKET_BUTTON, useUnmergedTree = true)
        .assertIsNotEnabled()

    // Verify callback was not invoked
    assertTrue(!buyTicketCalled)
  }

  @Test
  fun eventDetailScreen_paymentStateIdle_noLoadingOverlay() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState = EventDetailUiState(event = testEvent, paymentState = PaymentState.Idle),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule
        .onNodeWithTag(EventDetailTestTags.PAYMENT_LOADING, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun eventDetailScreen_paymentStateCreating_showsLoadingOverlay() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState =
                EventDetailUiState(
                    event = testEvent, paymentState = PaymentState.CreatingPaymentIntent),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule
        .onNodeWithTag(EventDetailTestTags.PAYMENT_LOADING, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun eventDetailScreen_paymentSucceeded_doesNotShowLoadingOverlay() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState =
                EventDetailUiState(event = testEvent, paymentState = PaymentState.PaymentSucceeded),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    // Loading overlay should not be displayed after success
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.PAYMENT_LOADING, useUnmergedTree = true)
        .assertDoesNotExist()

    // Buy button should be enabled again
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.BUY_TICKET_BUTTON, useUnmergedTree = true)
        .assertIsEnabled()
  }

  @Test
  fun eventDetailScreen_paymentCancelled_doesNotShowLoadingOverlay() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState =
                EventDetailUiState(event = testEvent, paymentState = PaymentState.PaymentCancelled),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    // Loading overlay should not be displayed after cancellation
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.PAYMENT_LOADING, useUnmergedTree = true)
        .assertDoesNotExist()

    // Buy button should be enabled again
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.BUY_TICKET_BUTTON, useUnmergedTree = true)
        .assertIsEnabled()
  }

  @Test
  fun eventDetailScreen_paymentFailed_doesNotShowLoadingOverlay() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState =
                EventDetailUiState(
                    event = testEvent, paymentState = PaymentState.PaymentFailed("Network error")),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    // Loading overlay should not be displayed after failure
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.PAYMENT_LOADING, useUnmergedTree = true)
        .assertDoesNotExist()

    // Buy button should be enabled again
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.BUY_TICKET_BUTTON, useUnmergedTree = true)
        .assertIsEnabled()
  }

  @Test
  fun eventDetailScreen_creatingPaymentIntentState_buyButtonDisabled() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState =
                EventDetailUiState(
                    event = testEvent, paymentState = PaymentState.CreatingPaymentIntent),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule
        .onNodeWithTag(EventDetailTestTags.BUY_TICKET_BUTTON, useUnmergedTree = true)
        .assertIsNotEnabled()
  }

  @Test
  fun eventDetailScreen_processingPaymentState_buyButtonDisabled() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState =
                EventDetailUiState(
                    event = testEvent, paymentState = PaymentState.ProcessingPayment),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule
        .onNodeWithTag(EventDetailTestTags.BUY_TICKET_BUTTON, useUnmergedTree = true)
        .assertIsNotEnabled()
  }

  @Test
  fun eventDetailScreen_idleState_buyButtonEnabled() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState = EventDetailUiState(event = testEvent, paymentState = PaymentState.Idle),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule
        .onNodeWithTag(EventDetailTestTags.BUY_TICKET_BUTTON, useUnmergedTree = true)
        .assertIsEnabled()
  }

  @Test
  fun eventDetailScreen_succeededState_buyButtonEnabled() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState =
                EventDetailUiState(event = testEvent, paymentState = PaymentState.PaymentSucceeded),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule
        .onNodeWithTag(EventDetailTestTags.BUY_TICKET_BUTTON, useUnmergedTree = true)
        .assertIsEnabled()
  }

  @Test
  fun eventDetailScreen_cancelledState_buyButtonEnabled() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState =
                EventDetailUiState(event = testEvent, paymentState = PaymentState.PaymentCancelled),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule
        .onNodeWithTag(EventDetailTestTags.BUY_TICKET_BUTTON, useUnmergedTree = true)
        .assertIsEnabled()
  }

  @Test
  fun eventDetailScreen_failedState_buyButtonEnabled() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState =
                EventDetailUiState(
                    event = testEvent, paymentState = PaymentState.PaymentFailed("error")),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule
        .onNodeWithTag(EventDetailTestTags.BUY_TICKET_BUTTON, useUnmergedTree = true)
        .assertIsEnabled()
  }

  @Test
  fun eventDetailScreen_readyToPayState_buyButtonEnabled() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState =
                EventDetailUiState(
                    event = testEvent, paymentState = PaymentState.ReadyToPay("secret", "id")),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule
        .onNodeWithTag(EventDetailTestTags.BUY_TICKET_BUTTON, useUnmergedTree = true)
        .assertIsEnabled()
  }

  @Test
  fun eventDetailScreen_creatingPaymentState_showsLoadingOverlay() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState =
                EventDetailUiState(
                    event = testEvent, paymentState = PaymentState.CreatingPaymentIntent),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule
        .onNodeWithTag(EventDetailTestTags.PAYMENT_LOADING, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun eventDetailScreen_processingPaymentState_showsLoadingOverlay() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState =
                EventDetailUiState(
                    event = testEvent, paymentState = PaymentState.ProcessingPayment),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule
        .onNodeWithTag(EventDetailTestTags.PAYMENT_LOADING, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun eventDetailScreen_readyToPayState_noLoadingOverlay() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState =
                EventDetailUiState(
                    event = testEvent, paymentState = PaymentState.ReadyToPay("secret", "id")),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule
        .onNodeWithTag(EventDetailTestTags.PAYMENT_LOADING, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun eventDetailScreen_succeededState_noLoadingOverlay() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState =
                EventDetailUiState(event = testEvent, paymentState = PaymentState.PaymentSucceeded),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule
        .onNodeWithTag(EventDetailTestTags.PAYMENT_LOADING, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun eventDetailScreen_cancelledState_noLoadingOverlay() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState =
                EventDetailUiState(event = testEvent, paymentState = PaymentState.PaymentCancelled),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule
        .onNodeWithTag(EventDetailTestTags.PAYMENT_LOADING, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun eventDetailScreen_failedState_noLoadingOverlay() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState =
                EventDetailUiState(
                    event = testEvent, paymentState = PaymentState.PaymentFailed("error")),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    composeTestRule
        .onNodeWithTag(EventDetailTestTags.PAYMENT_LOADING, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun eventDetailScreen_buyButton_showsCorrectTextWhenNotLoading() {
    val testEvent = createTestEvent(lowestPrice = 50u)

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState = EventDetailUiState(event = testEvent, paymentState = PaymentState.Idle),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    // Button should show price text when not loading
    composeTestRule
        .onNodeWithText("Buy ticket for 50chf", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun eventDetailScreen_freeEvent_showsCorrectButtonText() {
    val freeEvent = createTestEvent(lowestPrice = 0u)

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState = EventDetailUiState(event = freeEvent, paymentState = PaymentState.Idle),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    // Free event should show "FREE" text
    composeTestRule.onNodeWithText("FREE", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun eventDetailScreen_paymentInProgress_buyButtonStillDisplayed() {
    val testEvent = createTestEvent()

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState =
                EventDetailUiState(
                    event = testEvent, paymentState = PaymentState.CreatingPaymentIntent),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    // Buy button should still be visible (just disabled and showing loading indicator)
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.BUY_TICKET_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun eventDetailScreen_displaysTags_whenTagsArePresent() {
    val testEvent = createTestEvent(tags = listOf("Technology", "Workshop", "Free", "In-Person"))

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState = EventDetailUiState(event = testEvent),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    // Tags section should be displayed
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.TAGS_SECTION, useUnmergedTree = true)
        .assertIsDisplayed()

    // Each tag should be visible
    composeTestRule
        .onNodeWithText("Technology", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Workshop", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Free", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText("In-Person", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()

    // Verify tag chips are present
    composeTestRule
        .onNodeWithTag("${EventDetailTestTags.TAG_CHIP}_Technology", useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun eventDetailScreen_doesNotDisplayTagsSection_whenNoTags() {
    val testEvent = createTestEvent(tags = emptyList())

    composeTestRule.setContent {
      OnePassTheme {
        EventDetailScreenContent(
            uiState = EventDetailUiState(event = testEvent),
            onBack = {},
            onLikeToggle = {},
            onNavigateToMap = {},
            onBuyTicket = {})
      }
    }

    // Tags section should not be displayed when there are no tags
    composeTestRule
        .onNodeWithTag(EventDetailTestTags.TAGS_SECTION, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  // Helper function to create a test event
  private fun createTestEvent(
      title: String = "Test Event",
      description: String = "Test Description",
      lowestPrice: UInt = 35u,
      tags: List<String> = listOf("test")
  ): Event {
    val calendar = Calendar.getInstance()
    calendar.set(2024, Calendar.DECEMBER, 15, 21, 0, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    return Event(
        eventId = testEventId,
        title = title,
        description = description,
        organizerId = testOrganizerId,
        organizerName = "Test Organizer",
        status = EventStatus.PUBLISHED,
        location =
            Location(
                coordinates = GeoPoint(46.5197, 6.6323),
                name = "Test Location",
                region = "Test Region"),
        startTime = Timestamp(calendar.time),
        endTime = Timestamp(calendar.time),
        capacity = 100,
        ticketsRemaining = 50,
        ticketsIssued = 50,
        ticketsRedeemed = 0,
        currency = "CHF",
        pricingTiers = listOf(PricingTier("General", lowestPrice.toDouble(), 100, 50)),
        images = listOf(),
        tags = tags)
  }

  // Helper function to create a test organization
  private fun createTestOrganization(
      name: String = "Test Organization",
      followerCount: Int = 1000,
      averageRating: Float = 4.5f
  ): Organization {
    return Organization(
        id = testOrganizerId,
        name = name,
        description = "Test Description",
        ownerId = "owner-id",
        status = OrganizationStatus.ACTIVE,
        verified = true,
        profileImageUrl = null,
        followerCount = followerCount,
        averageRating = averageRating,
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now())
  }
}
