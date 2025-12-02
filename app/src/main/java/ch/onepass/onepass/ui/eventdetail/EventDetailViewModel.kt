package ch.onepass.onepass.ui.eventdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.PricingTier
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import ch.onepass.onepass.model.payment.PaymentIntentResponse
import ch.onepass.onepass.model.payment.PaymentRepository
import ch.onepass.onepass.model.payment.PaymentRepositoryFirebase
import ch.onepass.onepass.repository.RepositoryProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Represents the different states of a payment process.
 */
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

    // Selected pricing tier for purchase
    private val _selectedTier = MutableStateFlow<PricingTier?>(null)
    val selectedTier: StateFlow<PricingTier?> = _selectedTier.asStateFlow()

    // Quantity of tickets to purchase
    private val _quantity = MutableStateFlow(1)
    val quantity: StateFlow<Int> = _quantity.asStateFlow()

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
                        // Auto-select the first pricing tier if available
                        if (event.pricingTiers.isNotEmpty() && _selectedTier.value == null) {
                            _selectedTier.value = event.pricingTiers.first()
                        }
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
                .catch { e ->
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
     * Selects a pricing tier for purchase.
     */
    fun selectPricingTier(tier: PricingTier) {
        _selectedTier.value = tier
    }

    /**
     * Updates the quantity of tickets to purchase.
     */
    fun updateQuantity(newQuantity: Int) {
        if (newQuantity in 1..10) { // Limit to 10 tickets per purchase
            _quantity.value = newQuantity
        }
    }

    /**
     * Calculates the total price based on selected tier and quantity.
     * Returns price in cents.
     */
    fun calculateTotalPrice(): Long {
        val tier = _selectedTier.value ?: return 0L
        return (tier.price * 100 * _quantity.value).toLong()
    }

    /**
     * Initiates the payment process by creating a payment intent.
     * Call this when user clicks the "Buy Ticket" button.
     */
    fun initiatePayment() {
        val event = _event.value ?: return
        val tier = _selectedTier.value

        // For free events, skip payment
        if (event.lowestPrice == 0u || (tier != null && tier.price == 0.0)) {
            _paymentState.value = PaymentState.PaymentSucceeded
            return
        }

        val amount = calculateTotalPrice()
        if (amount <= 0) {
            _paymentState.value = PaymentState.PaymentFailed("Invalid amount")
            return
        }

        _paymentState.value = PaymentState.CreatingPaymentIntent

        viewModelScope.launch {
            val result = paymentRepository.createPaymentIntent(
                amount = amount,
                eventId = eventId,
                ticketTypeId = tier?.name,
                quantity = _quantity.value,
                description = "Ticket purchase for ${event.title}"
            )

            result.fold(
                onSuccess = { response ->
                    _paymentState.value = PaymentState.ReadyToPay(
                        clientSecret = response.clientSecret,
                        paymentIntentId = response.paymentIntentId
                    )
                },
                onFailure = { exception ->
                    _paymentState.value = PaymentState.PaymentFailed(
                        exception.message ?: "Failed to create payment"
                    )
                }
            )
        }
    }

    /**
     * Call this when the payment sheet is being presented.
     */
    fun onPaymentSheetPresented() {
        _paymentState.value = PaymentState.ProcessingPayment
    }

    /**
     * Call this when the payment succeeds.
     */
    fun onPaymentSuccess() {
        _paymentState.value = PaymentState.PaymentSucceeded
    }

    /**
     * Call this when the user cancels the payment.
     */
    fun onPaymentCancelled() {
        _paymentState.value = PaymentState.PaymentCancelled
    }

    /**
     * Call this when the payment fails.
     */
    fun onPaymentFailed(errorMessage: String) {
        _paymentState.value = PaymentState.PaymentFailed(errorMessage)
    }

    /**
     * Resets the payment state to Idle.
     * Call this after handling a payment result (success, cancelled, or failed).
     */
    fun resetPaymentState() {
        _paymentState.value = PaymentState.Idle
    }

    /**
     * Returns true if the event is free.
     */
    fun isEventFree(): Boolean {
        return _event.value?.lowestPrice == 0u
    }
}
