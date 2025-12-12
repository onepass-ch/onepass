package ch.onepass.onepass.ui.myevents

import android.content.Context
import androidx.annotation.ColorRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.onepass.onepass.R
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRepositoryFirebase
import ch.onepass.onepass.model.pass.PassRepository
import ch.onepass.onepass.model.payment.PaymentRepository
import ch.onepass.onepass.model.payment.PaymentRepositoryFirebase
import ch.onepass.onepass.model.ticket.TicketRepository
import ch.onepass.onepass.model.ticket.TicketRepositoryFirebase
import ch.onepass.onepass.model.ticket.toUiTicket
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Extension property for DataStore (can be used elsewhere in the app).
 *
 * SECURITY NOTE: Cache stores QR in plain text. If a device is compromised, cached QRs could be
 * extracted and replayed until server-side revocation.
 */
val Context.passDataStore: DataStore<Preferences> by preferencesDataStore(name = "onepass_cache")

/**
 * Data class representing a ticket in the UI.
 *
 * @param ticketId Unique identifier for the ticket.
 * @param title The title of the event.
 * @param status The status of the ticket (e.g., CURRENTLY, UPCOMING, EXPIRED).
 * @param dateTime The display date and time of the event.
 * @param location The display location of the event.
 */
data class Ticket(
    val ticketId: String,
    val title: String,
    val status: TicketStatus,
    val dateTime: String,
    val location: String,
)

/**
 * Enum representing the status of a ticket for UI display purposes.
 *
 * @param colorRes The color resource associated with the status.
 */
enum class TicketStatus(@ColorRes val colorRes: Int) {
  CURRENTLY(R.color.status_currently),
  UPCOMING(R.color.status_upcoming),
  EXPIRED(R.color.status_expired)
}

/** Enum representing the main tabs in the My Events screen. */
enum class MyEventsMainTab {
  YOUR_TICKETS,
  MARKET
}

/** Enum representing the sub-tabs within Your Tickets section. */
enum class TicketTab {
  CURRENT,
  EXPIRED,
  LISTED
}

/**
 * Data class representing a ticket available in the marketplace.
 *
 * @property ticketId Unique identifier for the ticket.
 * @property eventTitle Title of the event.
 * @property eventDate Display date and time of the event.
 * @property eventLocation Display location of the event.
 * @property sellerPrice Price at which the seller is listing the ticket.
 * @property originalPrice Original purchase price of the ticket.
 * @property currency Currency for the price.
 * @property eventId ID of the associated event.
 * @property sellerId ID of the seller (current ticket owner).
 * @property eventImageUrl URL of the event image.
 */
data class MarketTicket(
    val ticketId: String,
    val eventTitle: String,
    val eventDate: String,
    val eventLocation: String,
    val sellerPrice: Double,
    val originalPrice: Double,
    val currency: String,
    val eventId: String,
    val sellerId: String,
    val eventImageUrl: String = ""
)

/** Sealed class representing search results in the marketplace. */
sealed class SearchResult {
  /** Search result representing an event. */
  data class EventResult(val event: Event) : SearchResult()

  /** Search result representing an organization. */
  data class OrganizerResult(val organizer: Organization) : SearchResult()
}

/**
 * Data class representing a ticket that can be sold, enriched with event info.
 *
 * @property ticketId Unique identifier for the ticket.
 * @property eventId ID of the associated event.
 * @property eventTitle Title of the event.
 * @property eventDate Display date of the event.
 * @property eventLocation Location of the event.
 * @property originalPrice Original purchase price of the ticket.
 * @property currency Currency for the price.
 * @property rawTicket The underlying raw ticket object.
 */
data class SellableTicket(
    val ticketId: String,
    val eventId: String,
    val eventTitle: String,
    val eventDate: String,
    val eventLocation: String,
    val originalPrice: Double,
    val currency: String,
    val rawTicket: ch.onepass.onepass.model.ticket.Ticket
)

/**
 * Data class representing a ticket that is listed for sale by the user.
 *
 * @property ticketId Unique identifier for the ticket.
 * @property eventId ID of the associated event.
 * @property eventTitle Title of the event.
 * @property eventDate Display date of the event.
 * @property eventLocation Location of the event.
 * @property listingPrice Price at which the ticket is listed.
 * @property originalPrice Original purchase price of the ticket.
 * @property currency Currency for the price.
 * @property listedAt Display string for when the ticket was listed.
 * @property rawTicket The underlying raw ticket object.
 */
data class ListedTicket(
    val ticketId: String,
    val eventId: String,
    val eventTitle: String,
    val eventDate: String,
    val eventLocation: String,
    val listingPrice: Double,
    val originalPrice: Double,
    val currency: String,
    val listedAt: String,
    val rawTicket: ch.onepass.onepass.model.ticket.Ticket
)

/**
 * Immutable UI state for the My Events screen.
 *
 * @property currentTickets List of current (active) tickets.
 * @property expiredTickets List of expired tickets.
 * @property listedTickets List of tickets the user has listed for sale.
 * @property selectedTab The currently selected sub-tab (CURRENT, EXPIRED, or LISTED).
 * @property isQrExpanded Whether the QR code is expanded.
 * @property mainTab The currently selected main tab (YOUR_TICKETS or MARKET).
 * @property searchQuery The current search query for the marketplace.
 * @property searchResults List of search results (events and organizers).
 * @property marketTickets List of tickets available in the marketplace.
 * @property hotEvents List of featured events with available tickets.
 * @property isLoadingMarket Whether the marketplace is loading.
 * @property isSearching Whether a search is in progress.
 * @property selectedTicketForSale Ticket selected to list for sale (null if none).
 * @property showSellDialog Whether to show the sell ticket dialog.
 * @property sellingPrice The price entered for listing a ticket.
 * @property marketError Error message for market operations (null if no error).
 * @property sellableTickets List of user's tickets that can be listed for sale.
 */
data class MyEventsUiState(
    // Existing fields for Your Tickets
    val currentTickets: List<Ticket> = emptyList(),
    val expiredTickets: List<Ticket> = emptyList(),
    val listedTickets: List<ListedTicket> = emptyList(),
    val selectedTab: TicketTab = TicketTab.CURRENT,
    val isQrExpanded: Boolean = false,

    // New fields for Market
    val mainTab: MyEventsMainTab = MyEventsMainTab.YOUR_TICKETS,
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val marketTickets: List<MarketTicket> = emptyList(),
    val hotEvents: List<Event> = emptyList(),
    val isLoadingMarket: Boolean = false,
    val isSearching: Boolean = false,
    val selectedTicketForSale: SellableTicket? = null,
    val showSellDialog: Boolean = false,
    val sellingPrice: String = "",
    val marketError: String? = null,
    val sellableTickets: List<SellableTicket> = emptyList(),

    // Payment fields for marketplace purchases
    val purchaseClientSecret: String? = null,
    val purchasingTicketId: String? = null,
    val purchaseEventName: String? = null,
    val purchaseAmount: Double? = null,
    val purchaseCurrency: String? = null,
    val showPaymentSheet: Boolean = false,
    val isPurchasing: Boolean = false
)

/**
 * ViewModel for managing and displaying the user's tickets and the ticket marketplace.
 *
 * @param dataStore DataStore for caching QR codes (injected, not via Application)
 * @param passRepository Repository for signed passes
 * @param ticketRepo The repository for ticket data (default is Firebase implementation).
 * @param eventRepo The repository for event data (default is Firebase implementation).
 * @param orgRepo The repository for organization data (default is Firebase implementation).
 * @param paymentRepo The repository for payment data (default is Firebase implementation).
 * @param userId The ID of the current user whose tickets are being managed.
 */
class MyEventsViewModel(
    private val dataStore: DataStore<Preferences>,
    private val passRepository: PassRepository,
    private val ticketRepo: TicketRepository = TicketRepositoryFirebase(),
    private val eventRepo: EventRepository = EventRepositoryFirebase(),
    private val orgRepo: OrganizationRepository = OrganizationRepositoryFirebase(),
    private val paymentRepo: PaymentRepository = PaymentRepositoryFirebase(),
    private val userId: String?
) : ViewModel() {

  // ---------- Companion Object ----------
  companion object {
    /** Per-user cache key to avoid leaks between users/tests */
    private fun qrKey(uid: String) = stringPreferencesKey("cached_qr_text_$uid")

    /** Debounce delay for search queries */
    private const val SEARCH_DEBOUNCE_MS = 300L
  }

  // ---------- Search Query State ----------
  private val _searchQuery = MutableStateFlow("")

  // ---------- QR / Pass State ----------

  private val _userQrData = MutableStateFlow<String?>(null)
  /** Current user's QR code data (null if not loaded or unavailable) */
  val userQrData: StateFlow<String?> = _userQrData

  private val _isLoading = MutableStateFlow(false)
  /** True when fetching QR code from server */
  val isLoading: StateFlow<Boolean> = _isLoading

  private val _error = MutableStateFlow<String?>(null)
  /** Error message if QR code loading failed (null if no error) */
  val error: StateFlow<String?> = _error

  // ---------- User ID Resolution ----------

  /** Resolved user ID: either from constructor or Firebase Auth */
  private val resolvedUserId: String?
    get() = userId ?: FirebaseAuth.getInstance().currentUser?.uid

  // ---------- UI State ----------

  /** Backing state for the UI state flow */
  private val _uiState = MutableStateFlow(MyEventsUiState())
  /** Publicly exposed UI state as a StateFlow */
  val uiState: StateFlow<MyEventsUiState> = _uiState

  // ---------- Tickets State ----------

  /** StateFlow of the user's current (active) tickets enriched with event details. */
  @OptIn(ExperimentalCoroutinesApi::class)
  val currentTickets: StateFlow<List<Ticket>> =
      (userId?.let { uid -> ticketRepo.getActiveTickets(uid).flatMapLatest { enrichTickets(it) } }
              ?: flowOf(emptyList()))
          .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  /** StateFlow of the user's expired or redeemed tickets enriched with event details. */
  @OptIn(ExperimentalCoroutinesApi::class)
  val expiredTickets: StateFlow<List<Ticket>> =
      (userId?.let { uid -> ticketRepo.getExpiredTickets(uid).flatMapLatest { enrichTickets(it) } }
              ?: flowOf(emptyList()))
          .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  // ---------- Initialization ----------

  init {
    // Load cached QR at startup for offline support
    viewModelScope.launch { loadCachedQr() }
    // Observe tickets and update UI state
    observeCurrentTickets()
    observeExpiredTickets()
    observeListedTickets()
    // Initialize market data observers
    observeMarketTickets()
    observeHotEvents()
    observeSellableTickets()
    setupSearchObserver()
  }

  // ---------- Tickets Observation ----------

  /** Observes current tickets and updates UI state */
  @OptIn(ExperimentalCoroutinesApi::class)
  private fun observeCurrentTickets() {
    val uid = userId ?: return
    ticketRepo
        .getActiveTickets(uid)
        .flatMapLatest { enrichTickets(it) }
        .catch { e ->
          // Handle error gracefully - show empty list and log
          android.util.Log.e(
              "MyEventsViewModel", "Error observing current tickets: ${e.message}", e)
          emit(emptyList())
        }
        .onEach { tickets -> _uiState.value = _uiState.value.copy(currentTickets = tickets) }
        .launchIn(viewModelScope)
  }

  /** Observes expired tickets and updates the UI state accordingly */
  @OptIn(ExperimentalCoroutinesApi::class)
  private fun observeExpiredTickets() {
    val uid = userId ?: return
    ticketRepo
        .getExpiredTickets(uid)
        .flatMapLatest { enrichTickets(it) }
        .catch { e ->
          // Handle error gracefully - show empty list and log (fix the crash issue earlier)
          android.util.Log.e(
              "MyEventsViewModel", "Error observing expired tickets: ${e.message}", e)
          emit(emptyList())
        }
        .onEach { tickets -> _uiState.value = _uiState.value.copy(expiredTickets = tickets) }
        .launchIn(viewModelScope)
  }

  /** Observes user's listed tickets and updates the UI state accordingly */
  @OptIn(ExperimentalCoroutinesApi::class)
  private fun observeListedTickets() {
    val uid = userId ?: return
    ticketRepo
        .getListedTicketsByUser(uid)
        .flatMapLatest { tickets -> enrichListedTickets(tickets) }
        .catch { e ->
          // Handle error gracefully - show empty list and log
          android.util.Log.e("MyEventsViewModel", "Error observing listed tickets: ${e.message}", e)
          emit(emptyList())
        }
        .onEach { tickets -> _uiState.value = _uiState.value.copy(listedTickets = tickets) }
        .launchIn(viewModelScope)
  }

  /**
   * Enriches a list of listed tickets with their associated event data.
   *
   * @param tickets List of raw tickets to enrich.
   * @return Flow emitting a list of enriched listed tickets.
   */
  private fun enrichListedTickets(
      tickets: List<ch.onepass.onepass.model.ticket.Ticket>
  ): Flow<List<ListedTicket>> {
    if (tickets.isEmpty()) return flowOf(emptyList())
    return combine(
        tickets.map { ticket ->
          eventRepo
              .getEventById(ticket.eventId)
              .catch { e ->
                android.util.Log.w(
                    "MyEventsViewModel", "Failed to get event ${ticket.eventId}: ${e.message}")
                emit(null)
              }
              .map { event ->
                ListedTicket(
                    ticketId = ticket.ticketId,
                    eventId = ticket.eventId,
                    eventTitle = event?.title ?: "Unknown Event",
                    eventDate = event?.displayDateTime ?: "Date not set",
                    eventLocation = event?.displayLocation ?: "Unknown Location",
                    listingPrice = ticket.listingPrice ?: 0.0,
                    originalPrice = ticket.purchasePrice,
                    currency = ticket.currency,
                    listedAt =
                        ticket.listedAt?.toDate()?.let {
                          java.text
                              .SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
                              .format(it)
                        } ?: "Recently",
                    rawTicket = ticket)
              }
        }) {
          it.toList()
        }
  }

  // ---------- UI Actions ----------

  /**
   * Selects a main tab in the UI (YOUR_TICKETS or MARKET).
   *
   * @param tab The main tab to select.
   */
  fun selectMainTab(tab: MyEventsMainTab) {
    _uiState.value = _uiState.value.copy(mainTab = tab)
  }

  /**
   * Selects a sub-tab in the UI (CURRENT or EXPIRED).
   *
   * @param tab The sub-tab to select.
   */
  fun selectTab(tab: TicketTab) {
    _uiState.value = _uiState.value.copy(selectedTab = tab)
  }

  /** Toggles the expansion state of the user's QR code. */
  fun toggleQrExpansion() {
    _uiState.value = _uiState.value.copy(isQrExpanded = !_uiState.value.isQrExpanded)
  }

  // ---------- Market Observation ----------

  /** Observes market tickets and updates UI state */
  @OptIn(ExperimentalCoroutinesApi::class)
  private fun observeMarketTickets() {
    ticketRepo
        .getListedTickets()
        .flatMapLatest { tickets -> enrichMarketTickets(tickets) }
        .catch { e ->
          // Handle error gracefully - log and show empty list
          android.util.Log.e("MyEventsViewModel", "Error observing market tickets: ${e.message}", e)
          _uiState.value = _uiState.value.copy(marketTickets = emptyList(), isLoadingMarket = false)
          emit(emptyList())
        }
        .onEach { marketTickets ->
          _uiState.value =
              _uiState.value.copy(marketTickets = marketTickets, isLoadingMarket = false)
        }
        .launchIn(viewModelScope)
  }

  /** Observes hot/featured events and updates UI state */
  private fun observeHotEvents() {
    eventRepo
        .getFeaturedEvents()
        .catch { e ->
          // Handle error gracefully - show empty list
          android.util.Log.e("MyEventsViewModel", "Error observing hot events: ${e.message}", e)
          emit(emptyList())
        }
        .onEach { events -> _uiState.value = _uiState.value.copy(hotEvents = events) }
        .launchIn(viewModelScope)
  }

  /** Observes user's sellable tickets (tickets that can be listed for sale) */
  @OptIn(ExperimentalCoroutinesApi::class)
  private fun observeSellableTickets() {
    val uid = userId ?: return
    ticketRepo
        .getActiveTickets(uid)
        .map { tickets -> tickets.filter { it.canBeListed } }
        .flatMapLatest { tickets -> enrichSellableTickets(tickets) }
        .catch { e ->
          // Handle error gracefully - show empty list
          android.util.Log.e(
              "MyEventsViewModel", "Error observing sellable tickets: ${e.message}", e)
          emit(emptyList())
        }
        .onEach { tickets -> _uiState.value = _uiState.value.copy(sellableTickets = tickets) }
        .launchIn(viewModelScope)
  }

  /**
   * Enriches a list of sellable tickets with their associated event data.
   *
   * @param tickets List of raw tickets to enrich.
   * @return Flow emitting a list of enriched sellable tickets.
   */
  private fun enrichSellableTickets(
      tickets: List<ch.onepass.onepass.model.ticket.Ticket>
  ): Flow<List<SellableTicket>> {
    if (tickets.isEmpty()) return flowOf(emptyList())
    return combine(
        tickets.map { ticket ->
          eventRepo
              .getEventById(ticket.eventId)
              .catch { e ->
                android.util.Log.w(
                    "MyEventsViewModel", "Failed to get event ${ticket.eventId}: ${e.message}")
                emit(null)
              }
              .map { event ->
                SellableTicket(
                    ticketId = ticket.ticketId,
                    eventId = ticket.eventId,
                    eventTitle = event?.title ?: "Unknown Event",
                    eventDate = event?.displayDateTime ?: "Date not set",
                    eventLocation = event?.displayLocation ?: "Unknown Location",
                    originalPrice = ticket.purchasePrice,
                    currency = ticket.currency,
                    rawTicket = ticket)
              }
        }) {
          it.toList()
        }
  }

  /** Sets up the debounced search observer */
  @OptIn(FlowPreview::class)
  private fun setupSearchObserver() {
    _searchQuery
        .debounce(SEARCH_DEBOUNCE_MS)
        .distinctUntilChanged()
        .onEach { query -> performSearch(query) }
        .launchIn(viewModelScope)
  }

  // ---------- Search Functionality ----------

  /**
   * Updates the search query and triggers a debounced search.
   *
   * @param query The new search query.
   */
  fun updateSearchQuery(query: String) {
    _searchQuery.value = query
    _uiState.value = _uiState.value.copy(searchQuery = query)
  }

  /** Clears the search query and results. */
  fun clearSearch() {
    _searchQuery.value = ""
    _uiState.value =
        _uiState.value.copy(searchQuery = "", searchResults = emptyList(), isSearching = false)
  }

  /**
   * Performs the actual search for events and organizers.
   *
   * @param query The search query.
   */
  private fun performSearch(query: String) {
    if (query.isBlank()) {
      _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false)
      return
    }

    _uiState.value = _uiState.value.copy(isSearching = true)

    viewModelScope.launch {
      try {
        // Combine event and organization search results
        // TODO here check for the query logic and why no permission sometimes
        combine(eventRepo.searchEvents(query), orgRepo.searchOrganizations(query)) {
                events,
                organizers ->
              val eventResults = events.map { SearchResult.EventResult(it) }
              val orgResults = organizers.map { SearchResult.OrganizerResult(it) }
              eventResults + orgResults
            }
            .first()
            .let { results ->
              _uiState.value =
                  _uiState.value.copy(
                      searchResults = results, isSearching = false, marketError = null)
            }
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(isSearching = false, marketError = "Search failed: ${e.message}")
      }
    }
  }

  /**
   * Filters market tickets based on a search result selection.
   *
   * @param searchResult The selected search result.
   */
  fun filterMarketBySearchResult(searchResult: SearchResult) {
    when (searchResult) {
      is SearchResult.EventResult -> {
        viewModelScope.launch {
          ticketRepo
              .getListedTicketsByEvent(searchResult.event.eventId)
              .flatMapLatest { enrichMarketTickets(it) }
              .first()
              .let { filteredTickets ->
                _uiState.value = _uiState.value.copy(marketTickets = filteredTickets)
              }
        }
      }
      is SearchResult.OrganizerResult -> {
        // Filter by organizer - get all events by this organizer, then filter market tickets
        viewModelScope.launch {
          eventRepo.getEventsByOrganization(searchResult.organizer.id).first().let { events ->
            val eventIds = events.map { it.eventId }.toSet()
            val currentMarket = _uiState.value.marketTickets
            val filteredTickets = currentMarket.filter { it.eventId in eventIds }
            _uiState.value = _uiState.value.copy(marketTickets = filteredTickets)
          }
        }
      }
    }
  }

  // ---------- Sell Ticket Functionality ----------

  /**
   * Opens the sell ticket dialog.
   *
   * @param ticket Optional ticket to preselect for sale.
   */
  fun openSellDialog(ticket: SellableTicket? = null) {
    _uiState.value =
        _uiState.value.copy(
            selectedTicketForSale = ticket, showSellDialog = true, sellingPrice = "")
  }

  /**
   * Selects a ticket for sale within the dialog.
   *
   * @param ticket The ticket to select.
   */
  fun selectTicketForSale(ticket: SellableTicket) {
    _uiState.value = _uiState.value.copy(selectedTicketForSale = ticket)
  }

  /** Closes the sell ticket dialog. */
  fun closeSellDialog() {
    _uiState.value =
        _uiState.value.copy(
            selectedTicketForSale = null,
            showSellDialog = false,
            sellingPrice = "",
            marketError = null)
  }

  /**
   * Updates the selling price input.
   *
   * @param price The new price as a string.
   */
  fun updateSellingPrice(price: String) {
    _uiState.value = _uiState.value.copy(sellingPrice = price)
  }

  /**
   * Lists a ticket for sale at the specified price.
   *
   * @param ticketId The ticket's unique ID.
   * @param price The asking price.
   */
  fun listTicketForSale(ticketId: String, price: Double) {
    if (price <= 0) {
      _uiState.value = _uiState.value.copy(marketError = "Price must be greater than zero")
      return
    }

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoadingMarket = true, marketError = null)

      ticketRepo
          .listTicketForSale(ticketId, price)
          .fold(
              onSuccess = {
                _uiState.value =
                    _uiState.value.copy(
                        isLoadingMarket = false,
                        showSellDialog = false,
                        selectedTicketForSale = null,
                        sellingPrice = "")
              },
              onFailure = { error ->
                _uiState.value =
                    _uiState.value.copy(
                        isLoadingMarket = false,
                        marketError = "Failed to list ticket: ${error.message}")
              })
    }
  }

  /**
   * Cancels a ticket listing.
   *
   * @param ticketId The ticket's unique ID.
   */
  fun cancelTicketListing(ticketId: String) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoadingMarket = true, marketError = null)

      ticketRepo
          .cancelTicketListing(ticketId)
          .fold(
              onSuccess = { _uiState.value = _uiState.value.copy(isLoadingMarket = false) },
              onFailure = { error ->
                _uiState.value =
                    _uiState.value.copy(
                        isLoadingMarket = false,
                        marketError = "Failed to cancel listing: ${error.message}")
              })
    }
  }

  // ---------- Buy Ticket Functionality ----------

  /**
   * Initiates the purchase of a listed ticket from the marketplace. Creates a PaymentIntent and
   * prepares the payment sheet.
   *
   * @param ticketId The ticket's unique ID.
   */
  fun purchaseTicket(ticketId: String) {
    val buyerId = resolvedUserId
    if (buyerId == null) {
      _uiState.value = _uiState.value.copy(marketError = "You must be logged in to purchase")
      return
    }

    viewModelScope.launch {
      _uiState.value =
          _uiState.value.copy(
              isPurchasing = true, purchasingTicketId = ticketId, marketError = null)

      // Find the ticket details for description
      val ticketInfo = _uiState.value.marketTickets.find { it.ticketId == ticketId }

      paymentRepo
          .createMarketplacePaymentIntent(
              ticketId = ticketId,
              description = ticketInfo?.let { "Marketplace: ${it.eventTitle}" })
          .fold(
              onSuccess = { response ->
                _uiState.value =
                    _uiState.value.copy(
                        isPurchasing = false,
                        purchaseClientSecret = response.clientSecret,
                        purchasingTicketId = response.ticketId,
                        purchaseEventName = response.eventName,
                        purchaseAmount = response.amount,
                        purchaseCurrency = response.currency,
                        showPaymentSheet = true)
              },
              onFailure = { error ->
                _uiState.value =
                    _uiState.value.copy(
                        isPurchasing = false,
                        purchasingTicketId = null,
                        marketError = error.message ?: "Failed to initiate purchase")
              })
    }
  }

  /**
   * Called when the payment sheet has been presented. Resets the showPaymentSheet flag to prevent
   * re-presentation.
   */
  fun onPaymentSheetPresented() {
    _uiState.value = _uiState.value.copy(showPaymentSheet = false)
  }

  /**
   * Called when the payment succeeds. The actual ticket transfer happens via webhook, so we just
   * update UI.
   */
  fun onPaymentSuccess() {
    _uiState.value =
        _uiState.value.copy(
            purchaseClientSecret = null,
            purchasingTicketId = null,
            purchaseEventName = null,
            purchaseAmount = null,
            purchaseCurrency = null,
            marketError = null)
    // Show success message (optional - ticket will appear in their list after webhook)
    // The ticket transfer happens server-side via Stripe webhook
  }

  /** Called when the user cancels the payment. Releases the ticket reservation. */
  fun onPaymentCancelled() {
    val ticketId = _uiState.value.purchasingTicketId

    _uiState.value =
        _uiState.value.copy(
            purchaseClientSecret = null,
            purchasingTicketId = null,
            purchaseEventName = null,
            purchaseAmount = null,
            purchaseCurrency = null)

    // Release the reservation
    if (ticketId != null) {
      viewModelScope.launch { paymentRepo.cancelMarketplaceReservation(ticketId) }
    }
  }

  /**
   * Called when the payment fails. Releases the ticket reservation and shows error.
   *
   * @param errorMessage The error message from the payment sheet.
   */
  fun onPaymentFailed(errorMessage: String) {
    val ticketId = _uiState.value.purchasingTicketId

    _uiState.value =
        _uiState.value.copy(
            purchaseClientSecret = null,
            purchasingTicketId = null,
            purchaseEventName = null,
            purchaseAmount = null,
            purchaseCurrency = null,
            marketError = "Payment failed: $errorMessage")

    // Release the reservation
    if (ticketId != null) {
      viewModelScope.launch { paymentRepo.cancelMarketplaceReservation(ticketId) }
    }
  }

  /** Clears any market-related error messages. */
  fun clearMarketError() {
    _uiState.value = _uiState.value.copy(marketError = null)
  }

  // ---------- Market Ticket Enrichment ----------

  /**
   * Enriches a list of market tickets with their associated event data.
   *
   * @param tickets List of raw tickets to enrich.
   * @return Flow emitting a list of enriched market tickets.
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  private fun enrichMarketTickets(
      tickets: List<ch.onepass.onepass.model.ticket.Ticket>
  ): Flow<List<MarketTicket>> {
    if (tickets.isEmpty()) return flowOf(emptyList())
    return combine(
        tickets.map { ticket ->
          eventRepo
              .getEventById(ticket.eventId)
              .catch { e ->
                android.util.Log.w(
                    "MyEventsViewModel", "Failed to get event ${ticket.eventId}: ${e.message}")
                emit(null)
              }
              .map { event ->
                MarketTicket(
                    ticketId = ticket.ticketId,
                    eventTitle = event?.title ?: "Unknown Event",
                    eventDate = event?.displayDateTime ?: "Date not set",
                    eventLocation = event?.displayLocation ?: "Unknown Location",
                    sellerPrice = ticket.listingPrice ?: 0.0,
                    originalPrice = ticket.purchasePrice,
                    currency = ticket.currency,
                    eventId = ticket.eventId,
                    sellerId = ticket.ownerId,
                    eventImageUrl = event?.imageUrl ?: "")
              }
        }) {
          it.toList()
        }
  }

  // ---------- Ticket Enrichment ----------

  /**
   * Enriches a list of tickets with their associated event data.
   *
   * @param tickets List of tickets to enrich.
   * @return Flow emitting a list of enriched tickets.
   */
  private fun enrichTickets(
      tickets: List<ch.onepass.onepass.model.ticket.Ticket>
  ): Flow<List<Ticket>> {
    if (tickets.isEmpty()) return flowOf(emptyList())
    return combine(
        tickets.map { ticket ->
          eventRepo
              .getEventById(ticket.eventId)
              .catch { e ->
                android.util.Log.w(
                    "MyEventsViewModel", "Failed to get event ${ticket.eventId}: ${e.message}")
                emit(null)
              }
              .map { event -> ticket.toUiTicket(event) }
        }) {
          it.toList()
        }
  }

  // ---------- Pass / QR API ----------

  /**
   * Loads the user's pass from the server (or creates it if missing). On success: updates state and
   * saves to cache. On failure: falls back to cached data for offline support.
   */
  fun loadUserPass() {
    viewModelScope.launch {
      _isLoading.value = true
      _error.value = null

      try {
        val authUid = resolvedUserId
        require(!authUid.isNullOrBlank()) { "User not authenticated" }

        val result = passRepository.getOrCreateSignedPass(authUid)

        when {
          result.isSuccess -> {
            val pass = result.getOrThrow()
            _userQrData.value = pass.qrText
            // Await cache save completion
            saveCachedQr(pass.qrText)
          }
          result.isFailure -> {
            _error.value = result.exceptionOrNull()?.message ?: "Failed to load pass"
            // Fallback to cache for offline mode
            loadCachedQr()
          }
        }
      } catch (t: Throwable) {
        _error.value = t.message ?: "Authentication error"
        loadCachedQr()
      } finally {
        _isLoading.value = false
      }
    }
  }

  /** Alias to reload the pass (e.g., for pull-to-refresh) */
  fun refreshPass() = loadUserPass()

  // ---------- DataStore Cache Helpers ----------

  /**
   * Saves QR text to DataStore cache for offline access. Note: Suspending function - must complete
   * before returning.
   */
  private suspend fun saveCachedQr(qrText: String) {
    val uid = resolvedUserId ?: return

    try {
      dataStore.edit { prefs -> prefs[qrKey(uid)] = qrText }
    } catch (e: Exception) {
      // Silently fail - cache is optional, don't disrupt user experience
      _error.value = "Failed to cache QR code: ${e.message}"
    }
  }

  /**
   * Loads QR text from DataStore cache (for offline mode). Sets state to null if no cache exists or
   * user is not authenticated.
   */
  private suspend fun loadCachedQr() {
    val uid =
        resolvedUserId
            ?: run {
              _userQrData.value = null
              return
            }

    try {
      val prefs = dataStore.data.first()
      _userQrData.value = prefs[qrKey(uid)]
    } catch (e: Exception) {
      // Cache read failed - not critical, just leave state null
      _userQrData.value = null
    }
  }

  /**
   * Clears the cached QR code for the current user. Useful for logout or security-sensitive
   * operations.
   */
  fun clearCache() {
    viewModelScope.launch {
      val uid = resolvedUserId ?: return@launch

      try {
        dataStore.edit { prefs -> prefs.remove(qrKey(uid)) }
        _userQrData.value = null
      } catch (e: Exception) {}
    }
  }
}
