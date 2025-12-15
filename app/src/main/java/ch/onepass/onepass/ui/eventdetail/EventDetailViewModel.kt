package ch.onepass.onepass.ui.eventdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import ch.onepass.onepass.model.payment.PaymentRepository
import ch.onepass.onepass.model.payment.PaymentRepositoryFirebase
import ch.onepass.onepass.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Represents the different states of a payment process. */
sealed class PaymentState {
  /** No payment in progress */
  data object Idle : PaymentState()

  /** Payment intent is being created */
  data object CreatingPaymentIntent : PaymentState()

  /** Payment sheet is ready to be presented */
  data class ReadyToPay(val clientSecret: String, val paymentIntentId: String) : PaymentState()

  /** Payment sheet is being presented */
  data object ProcessingPayment : PaymentState()

  /** Payment succeeded */
  data object PaymentSucceeded : PaymentState()

  /** Payment was cancelled by the user */
  data object PaymentCancelled : PaymentState()

  /** Payment failed with an error */
  data class PaymentFailed(val errorMessage: String) : PaymentState()
}

/** ViewModel for EventDetailScreen that manages event and organizer data. */
class EventDetailViewModel(
    private val eventId: String,
    private val eventRepository: EventRepository = RepositoryProvider.eventRepository,
    private val organizationRepository: OrganizationRepository = OrganizationRepositoryFirebase(),
    private val paymentRepository: PaymentRepository = PaymentRepositoryFirebase()
) : ViewModel() {

  private val _event = MutableStateFlow<Event?>(null)
  val event: StateFlow<Event?> = _event.asStateFlow()

  private val _organization = MutableStateFlow<Organization?>(null)
  val organization: StateFlow<Organization?> = _organization.asStateFlow()

  private val _isLoading = MutableStateFlow(true)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  // Payment state
  private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
  val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

  init {
    loadEvent()
  }

  private fun loadEvent() {
    viewModelScope.launch {
      eventRepository
          .getEventById(eventId)
          .catch { e ->
            _error.value = e.message ?: "Failed to load event"
            _isLoading.value = false
          }
          .collect { event ->
            _event.value = event
            if (event != null) {
              loadOrganization(event.organizerId)
            } else {
              _isLoading.value = false
            }
          }
    }
  }

  private fun loadOrganization(organizerId: String) {
    if (organizerId.isBlank()) {
      _isLoading.value = false
      return
    }

    viewModelScope.launch {
      organizationRepository
          .getOrganizationById(organizerId)
          .catch { _ ->
            // Don't set error for organization, just log it
            _isLoading.value = false
          }
          .collect { organization ->
            _organization.value = organization
            _isLoading.value = false
          }
    }
  }

  /**
   * Initiates the payment process by creating a payment intent. Call this when user clicks the "Buy
   * Ticket" button.
   */
  fun initiatePayment() {
    val event =
        _event.value
            ?: run {
              android.util.Log.e("EventDetailViewModel", "❌ Cannot initiate payment: event is null")
              return
            }

    android.util.Log.d("EventDetailViewModel", "🎫 Initiating payment for event: ${event.title}")

    // For free events, skip payment
    if (event.lowestPrice == 0u) {
      android.util.Log.d("EventDetailViewModel", "✓ Free event, skipping payment")
      _paymentState.value = PaymentState.PaymentSucceeded
      return
    }

    val amount = (event.lowestPrice * 100u).toLong()
    android.util.Log.d("EventDetailViewModel", "Calculated amount: $amount cents")

    if (amount <= 0) {
      android.util.Log.e("EventDetailViewModel", "❌ Invalid amount: $amount")
      _paymentState.value = PaymentState.PaymentFailed("Invalid amount")
      return
    }

    _paymentState.value = PaymentState.CreatingPaymentIntent
    android.util.Log.d("EventDetailViewModel", "🔨 Creating payment intent...")

    viewModelScope.launch {
      val result =
          paymentRepository.createPaymentIntent(
              amount = amount,
              eventId = eventId,
              description = "Ticket purchase for ${event.title}")

      result.fold(
          onSuccess = { response ->
            android.util.Log.d(
                "EventDetailViewModel",
                "✅ Payment intent created successfully: ${response.paymentIntentId}")
            _paymentState.value =
                PaymentState.ReadyToPay(
                    clientSecret = response.clientSecret,
                    paymentIntentId = response.paymentIntentId)
          },
          onFailure = { exception ->
            android.util.Log.e(
                "EventDetailViewModel", "❌ Payment intent creation failed", exception)
            _paymentState.value =
                PaymentState.PaymentFailed(exception.message ?: "Failed to create payment")
          })
    }
  }

  /** Call this when the payment sheet is being presented. */
  fun onPaymentSheetPresented() {
    _paymentState.value = PaymentState.ProcessingPayment
  }

  /** Call this when the payment succeeds. */
  fun onPaymentSuccess() {
    _paymentState.value = PaymentState.PaymentSucceeded
  }

  /** Call this when the user cancels the payment. */
  fun onPaymentCancelled() {
    _paymentState.value = PaymentState.PaymentCancelled
  }

  /** Call this when the payment fails. */
  fun onPaymentFailed(errorMessage: String) {
    _paymentState.value = PaymentState.PaymentFailed(errorMessage)
  }

  /**
   * Resets the payment state to Idle. Call this after handling a payment result (success,
   * cancelled, or failed).
   */
  fun resetPaymentState() {
    _paymentState.value = PaymentState.Idle
  }

  /** Returns true if the event is free. */
  fun isEventFree(): Boolean {
    return _event.value?.lowestPrice == 0u
  }
}
