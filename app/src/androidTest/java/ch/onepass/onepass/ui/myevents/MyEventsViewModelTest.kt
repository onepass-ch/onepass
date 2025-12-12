package ch.onepass.onepass.ui.myevents

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.pass.Pass
import ch.onepass.onepass.model.pass.PassRepository
import ch.onepass.onepass.model.payment.MarketplacePaymentIntentResponse
import ch.onepass.onepass.model.payment.PaymentRepository
import ch.onepass.onepass.model.ticket.Ticket
import ch.onepass.onepass.model.ticket.TicketRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive tests for MyEventsViewModel.
 *
 * Tests cover:
 * - QR code loading (success, failure, caching)
 * - Cache management (load, clear, per-user isolation)
 * - Tickets display (active, expired, enrichment)
 * - Error handling (network failures, exceptions, edge cases)
 * - Loading states
 * - UI state management
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MyEventsViewModelTest {

  private val dispatcher = StandardTestDispatcher()
  private lateinit var context: Context
  private lateinit var dataStore: DataStore<Preferences>
  private lateinit var passRepo: PassRepository
  private lateinit var ticketRepo: TicketRepository
  private lateinit var eventRepo: EventRepository
  private lateinit var orgRepo: OrganizationRepository
  private lateinit var paymentRepo: PaymentRepository

  /** Generates unique UID per test to avoid cache conflicts */
  private fun uniqueUid(prefix: String = "test") = "$prefix-${UUID.randomUUID()}"

  private fun createViewModel(
      uid: String?,
      ticketRepository: TicketRepository = ticketRepo,
      eventRepository: EventRepository = eventRepo,
      organizationRepository: OrganizationRepository = orgRepo,
      paymentRepository: PaymentRepository = paymentRepo
  ): MyEventsViewModel {
    return MyEventsViewModel(
        dataStore = dataStore,
        passRepository = passRepo,
        ticketRepo = ticketRepository,
        eventRepo = eventRepository,
        orgRepo = organizationRepository,
        paymentRepo = paymentRepository,
        userId = uid)
  }

  private class FakePaymentRepository(private val response: MarketplacePaymentIntentResponse) :
      PaymentRepository {
    val cancelledIds = mutableListOf<String>()

    override suspend fun createPaymentIntent(
        amount: Long,
        eventId: String,
        ticketTypeId: String?,
        quantity: Int,
        description: String?
    ): Result<ch.onepass.onepass.model.payment.PaymentIntentResponse> {
      return Result.failure(Exception("Not used"))
    }

    override suspend fun createMarketplacePaymentIntent(
        ticketId: String,
        description: String?
    ): Result<MarketplacePaymentIntentResponse> {
      return Result.success(response.copy(ticketId = ticketId))
    }

    override suspend fun cancelMarketplaceReservation(ticketId: String): Result<Unit> {
      cancelledIds.add(ticketId)
      return Result.success(Unit)
    }
  }

  @Before
  fun setup() {
    Dispatchers.setMain(dispatcher)

    context = ApplicationProvider.getApplicationContext()
    dataStore = context.passDataStore

    passRepo = mockk()
    ticketRepo = mockk()
    eventRepo = mockk()
    orgRepo = mockk(relaxed = true)
    paymentRepo = mockk(relaxed = true)

    // Default mock behaviors
    coEvery { ticketRepo.getActiveTickets(any()) } returns emptyFlow()
    coEvery { ticketRepo.getExpiredTickets(any()) } returns emptyFlow()
    coEvery { ticketRepo.getListedTickets() } returns emptyFlow()
    coEvery { ticketRepo.getListedTicketsByUser(any()) } returns emptyFlow()
    coEvery { ticketRepo.getListedTicketsByEvent(any()) } returns emptyFlow()
    coEvery { eventRepo.getFeaturedEvents() } returns emptyFlow()
    coEvery { paymentRepo.createMarketplacePaymentIntent(any(), any()) } returns
        Result.failure(Exception("not implemented"))
    coEvery { paymentRepo.cancelMarketplaceReservation(any()) } returns Result.success(Unit)
  }

  @After
  fun tearDown() {
    runBlocking { dataStore.edit { it.clear() } }
    Dispatchers.resetMain()
  }

  // ==================== QR CODE LOADING ====================

  @Test
  fun loadUserPass_failure_usesCachedQr_ifPreviouslyCached() = runTest {
    val uid = uniqueUid("cached")
    val pass = Pass(uid = uid, kid = "k", issuedAt = 1, version = 1, signature = "cached")

    // First VM: load and cache
    val writer = createViewModel(uid)
    advanceUntilIdle()
    coEvery { passRepo.getOrCreateSignedPass(uid) } returns Result.success(pass)
    writer.loadUserPass()
    advanceUntilIdle()
    val cachedQr = writer.userQrData.value

    // Second VM: fail to load but retrieve from cache
    val reader = createViewModel(uid)
    advanceUntilIdle()
    coEvery { passRepo.getOrCreateSignedPass(uid) } returns
        Result.failure(Exception("Network down"))
    reader.loadUserPass()
    advanceUntilIdle()

    assertEquals(cachedQr, reader.userQrData.value)
    assertNotNull(reader.error.value)
  }

  @Test
  fun loadUserPass_blankUserId_setsError() = runTest {
    val vm = createViewModel("   ")
    advanceUntilIdle()

    vm.loadUserPass()
    advanceUntilIdle()

    assertNotNull(vm.error.value)
    assertTrue(vm.error.value!!.contains("not authenticated"))
  }

  @Test
  fun failure_after_success_keepsPreviousQr() = runTest {
    val uid = uniqueUid("mix")
    val pass = Pass(uid = uid, kid = "k", issuedAt = 5, version = 1, signature = "mix")
    val vm = createViewModel(uid)
    advanceUntilIdle()

    // First success
    coEvery { passRepo.getOrCreateSignedPass(uid) } returns Result.success(pass)
    vm.loadUserPass()
    advanceUntilIdle()
    val okQr = vm.userQrData.value

    // Then failure - should keep cached QR
    coEvery { passRepo.getOrCreateSignedPass(uid) } returns Result.failure(Exception("Network"))
    vm.loadUserPass()
    advanceUntilIdle()

    assertEquals(okQr, vm.userQrData.value)
    assertNotNull(vm.error.value)
  }

  @Test
  fun repoThrowsException_setsError_andKeepsQr() = runTest {
    val uid = uniqueUid("ex")
    val pass = Pass(uid = uid, kid = "k", issuedAt = 11, version = 1, signature = "ex")
    val vm = createViewModel(uid)
    advanceUntilIdle()

    // First success
    coEvery { passRepo.getOrCreateSignedPass(uid) } returns Result.success(pass)
    vm.loadUserPass()
    advanceUntilIdle()
    val qr = vm.userQrData.value

    // Then exception
    coEvery { passRepo.getOrCreateSignedPass(uid) } throws RuntimeException("Crash!")
    vm.loadUserPass()
    advanceUntilIdle()

    assertEquals(qr, vm.userQrData.value)
    assertTrue(vm.error.value?.contains("Crash") == true)
  }

  // ==================== CACHE MANAGEMENT ====================

  @Test
  fun init_loadsCachedQr_onStartup() = runTest {
    val uid = uniqueUid("startup")
    val pass = Pass(uid = uid, kid = "k", issuedAt = 1, version = 1, signature = "startup")

    // First VM: cache the QR
    val writer = createViewModel(uid)
    advanceUntilIdle()
    coEvery { passRepo.getOrCreateSignedPass(uid) } returns Result.success(pass)
    writer.loadUserPass()
    advanceUntilIdle()

    // Second VM: should load from cache on init
    val reader = createViewModel(uid)
    advanceUntilIdle()

    assertEquals(writer.userQrData.value, reader.userQrData.value)
  }

  @Test
  fun init_loadsNothing_whenNoCacheForUser() = runTest {
    val uid = uniqueUid("fresh")
    val vm = createViewModel(uid)
    advanceUntilIdle()

    assertNull(vm.userQrData.value)
  }

  @Test
  fun init_withNullUser_doesNotLoadAnyCachedQr() = runTest {
    val vm = createViewModel(null)
    advanceUntilIdle()

    assertNull(vm.userQrData.value)
  }

  @Test
  fun refreshPass_withNullUser_setsError() = runTest {
    val vm = createViewModel(null)
    advanceUntilIdle()

    vm.refreshPass()
    advanceUntilIdle()

    assertNotNull(vm.error.value)
    assertNull(vm.userQrData.value)
  }

  // ==================== TICKETS ====================

  @Test
  fun tickets_areEmpty_whenReposReturnEmpty() = runTest {
    val uid = uniqueUid("tickets")
    val vm = createViewModel(uid)
    advanceUntilIdle()

    assertTrue(vm.currentTickets.first().isEmpty())
    assertTrue(vm.expiredTickets.first().isEmpty())
  }

  @Test
  fun tickets_notQueried_whenUserIdNull() = runTest {
    val vm = createViewModel(null)
    advanceUntilIdle()

    assertTrue(vm.currentTickets.first().isEmpty())
    assertTrue(vm.expiredTickets.first().isEmpty())
  }

  @Test
  fun tickets_queried_whenUserIdPresent() = runTest {
    val uid = uniqueUid("uZ")
    coEvery { ticketRepo.getActiveTickets(uid) } returns flowOf(emptyList())
    coEvery { ticketRepo.getExpiredTickets(uid) } returns flowOf(emptyList())

    val vm = createViewModel(uid)
    advanceUntilIdle()

    assertEquals(emptyList<ch.onepass.onepass.ui.myevents.Ticket>(), vm.currentTickets.first())
    assertEquals(emptyList<ch.onepass.onepass.ui.myevents.Ticket>(), vm.expiredTickets.first())
  }

  @Test
  fun enrichTickets_emptyList_returnsEmpty_andDoesNotQueryEvents() = runTest {
    val uid = uniqueUid("enrich")
    coEvery { ticketRepo.getActiveTickets(uid) } returns flowOf(emptyList())
    coEvery { ticketRepo.getExpiredTickets(uid) } returns flowOf(emptyList())

    val vm = createViewModel(uid)
    advanceUntilIdle()

    assertTrue(vm.currentTickets.first().isEmpty())
    verify(exactly = 0) { eventRepo.getEventById(any()) }
  }

  @Test
  fun enrichTickets_withTickets_queriesEvents() = runTest {
    val uid = uniqueUid("enrich_query")
    val mockTicket = mockk<ch.onepass.onepass.model.ticket.Ticket>(relaxed = true)
    val mockEvent = mockk<ch.onepass.onepass.model.event.Event>(relaxed = true)

    coEvery { mockTicket.eventId } returns "event123"
    coEvery { ticketRepo.getActiveTickets(uid) } returns flowOf(listOf(mockTicket))
    coEvery { ticketRepo.getExpiredTickets(uid) } returns flowOf(emptyList())
    coEvery { eventRepo.getEventById("event123") } returns flowOf(mockEvent)

    val vm = createViewModel(uid)
    advanceUntilIdle()

    // Just verify that eventRepo was called (enrichment happened)
    coVerify { eventRepo.getEventById("event123") }
  }

  @Test
  fun currentAndExpiredTickets_areIndependent() = runTest {
    val uid = uniqueUid("independent")
    coEvery { ticketRepo.getActiveTickets(uid) } returns flowOf(emptyList())
    coEvery { ticketRepo.getExpiredTickets(uid) } returns flowOf(emptyList())

    val vm = createViewModel(uid)
    advanceUntilIdle()

    assertNotSame(vm.currentTickets, vm.expiredTickets)
    assertEquals(emptyList<ch.onepass.onepass.ui.myevents.Ticket>(), vm.currentTickets.value)
    assertEquals(emptyList<ch.onepass.onepass.ui.myevents.Ticket>(), vm.expiredTickets.value)
  }

  // ==================== UI STATE ====================

  @Test
  fun uiState_initialState_isCorrect() = runTest {
    val uid = uniqueUid("ui_init")
    val vm = createViewModel(uid)
    advanceUntilIdle()

    val state = vm.uiState.first()
    assertEquals(TicketTab.CURRENT, state.selectedTab)
    assertFalse(state.isQrExpanded)
    assertTrue(state.currentTickets.isEmpty())
    assertTrue(state.expiredTickets.isEmpty())
  }

  @Test
  fun selectTab_updatesUiState() = runTest {
    val uid = uniqueUid("select_tab")
    val vm = createViewModel(uid)
    advanceUntilIdle()

    vm.selectTab(TicketTab.EXPIRED)
    advanceUntilIdle()

    assertEquals(TicketTab.EXPIRED, vm.uiState.first().selectedTab)
  }

  @Test
  fun toggleQrExpansion_togglesState() = runTest {
    val uid = uniqueUid("toggle")
    val vm = createViewModel(uid)
    advanceUntilIdle()

    assertFalse(vm.uiState.first().isQrExpanded)

    vm.toggleQrExpansion()
    advanceUntilIdle()
    assertTrue(vm.uiState.first().isQrExpanded)

    vm.toggleQrExpansion()
    advanceUntilIdle()
    assertFalse(vm.uiState.first().isQrExpanded)
  }

  @Test
  fun observeCurrentTickets_updatesUiState() = runTest {
    val uid = uniqueUid("observe_current")
    val mockTicket = mockk<ch.onepass.onepass.model.ticket.Ticket>(relaxed = true)
    val mockEvent = mockk<ch.onepass.onepass.model.event.Event>(relaxed = true)

    coEvery { mockTicket.eventId } returns "event456"
    coEvery { ticketRepo.getActiveTickets(uid) } returns flowOf(listOf(mockTicket))
    coEvery { ticketRepo.getExpiredTickets(uid) } returns flowOf(emptyList())
    coEvery { eventRepo.getEventById("event456") } returns flowOf(mockEvent)

    val vm = createViewModel(uid)
    advanceUntilIdle()

    val uiState = vm.uiState.first()
    // Verify that UI state has been updated with tickets (size check)
    assertTrue(uiState.currentTickets.isNotEmpty())
  }

  @Test
  fun observeExpiredTickets_updatesUiState() = runTest {
    val uid = uniqueUid("observe_expired")
    val mockTicket = mockk<ch.onepass.onepass.model.ticket.Ticket>(relaxed = true)
    val mockEvent = mockk<ch.onepass.onepass.model.event.Event>(relaxed = true)

    coEvery { mockTicket.eventId } returns "event789"
    coEvery { ticketRepo.getActiveTickets(uid) } returns flowOf(emptyList())
    coEvery { ticketRepo.getExpiredTickets(uid) } returns flowOf(listOf(mockTicket))
    coEvery { eventRepo.getEventById("event789") } returns flowOf(mockEvent)

    val vm = createViewModel(uid)
    advanceUntilIdle()

    val uiState = vm.uiState.first()
    // Verify that UI state has been updated with expired tickets
    assertTrue(uiState.expiredTickets.isNotEmpty())
  }

  // ==================== MARKET & PAYMENT ====================

  @Test
  fun selectMainTab_updatesUiState() = runTest {
    val uid = uniqueUid("main_tab")
    val vm = createViewModel(uid)
    advanceUntilIdle()

    vm.selectMainTab(MyEventsMainTab.MARKET)
    advanceUntilIdle()

    assertEquals(MyEventsMainTab.MARKET, vm.uiState.first().mainTab)
  }

  @Test
  fun listTicketForSale_withInvalidPrice_setsError() = runTest {
    val vm = createViewModel(uniqueUid("invalid_price"))
    advanceUntilIdle()

    vm.listTicketForSale(ticketId = "ticket-1", price = 0.0)
    advanceUntilIdle()

    assertEquals("Price must be greater than zero", vm.uiState.value.marketError)
  }

  @Test
  fun purchaseTicket_success_populatesPaymentFields() = runTest {
    val uid = uniqueUid("buyer")
    val customPaymentRepo =
        FakePaymentRepository(
            response =
                MarketplacePaymentIntentResponse(
                    clientSecret = "secret_123",
                    paymentIntentId = "pi_123",
                    ticketId = "ticket-123",
                    eventName = "Market Event",
                    amount = 42.0,
                    currency = "CHF"))

    val vm =
        createViewModel(
            uid = uid,
            paymentRepository = customPaymentRepo,
            ticketRepository = ticketRepo,
            eventRepository = eventRepo,
            organizationRepository = orgRepo)
    advanceUntilIdle()

    vm.purchaseTicket("ticket-123")
    val state = vm.uiState.first { it.purchaseClientSecret != null }

    assertEquals("secret_123", state.purchaseClientSecret)
    assertEquals("ticket-123", state.purchasingTicketId)
    assertTrue(state.showPaymentSheet)
    assertFalse(state.isPurchasing)
  }

  @Test
  fun onPaymentCancelled_releasesReservation_andClearsState() = runTest {
    val uid = uniqueUid("cancel_payment")
    val customPaymentRepo =
        FakePaymentRepository(
            response =
                MarketplacePaymentIntentResponse(
                    clientSecret = "secret_cancel",
                    paymentIntentId = "pi_cancel",
                    ticketId = "ticket-cancel",
                    eventName = "Cancel Event",
                    amount = 10.0,
                    currency = "CHF"))

    val vm =
        createViewModel(
            uid = uid,
            paymentRepository = customPaymentRepo,
            ticketRepository = ticketRepo,
            eventRepository = eventRepo,
            organizationRepository = orgRepo)
    advanceUntilIdle()

    vm.purchaseTicket("ticket-cancel")
    advanceUntilIdle()

    vm.onPaymentCancelled()
    advanceUntilIdle()

    assert(customPaymentRepo.cancelledIds.contains("ticket-cancel"))
    val state = vm.uiState.value
    assertNull(state.purchaseClientSecret)
    assertNull(state.purchasingTicketId)
    assertNull(state.marketError)
  }

  @Test
  fun onPaymentFailed_setsError_andReleasesReservation() = runTest {
    val uid = uniqueUid("payment_failed")
    val customPaymentRepo =
        FakePaymentRepository(
            response =
                MarketplacePaymentIntentResponse(
                    clientSecret = "secret_fail",
                    paymentIntentId = "pi_fail",
                    ticketId = "ticket-fail",
                    eventName = "Fail Event",
                    amount = 15.0,
                    currency = "CHF"))

    val vm =
        createViewModel(
            uid = uid,
            paymentRepository = customPaymentRepo,
            ticketRepository = ticketRepo,
            eventRepository = eventRepo,
            organizationRepository = orgRepo)
    advanceUntilIdle()

    vm.purchaseTicket("ticket-fail")
    advanceUntilIdle()

    vm.onPaymentFailed("network")
    advanceUntilIdle()

    assert(customPaymentRepo.cancelledIds.contains("ticket-fail"))
    val state = vm.uiState.value
    assertNull(state.purchaseClientSecret)
    assertNull(state.purchasingTicketId)
    assertTrue(state.marketError?.contains("network") == true)
  }

  // ==================== LOADING STATES ====================

  @Test
  fun userQrData_isNullInitially_beforeAnyLoad() = runTest {
    val uid = uniqueUid("init")
    val vm = createViewModel(uid)
    advanceUntilIdle()

    assertNull(vm.userQrData.value)
    assertNull(vm.error.value)
    assertFalse(vm.isLoading.value)
  }

  @Test
  fun initial_states_areSane_beforeAnyAction() = runTest {
    val uid = uniqueUid("sanity")
    val vm = createViewModel(uid)
    advanceUntilIdle()

    assertNull(vm.userQrData.value)
    assertNull(vm.error.value)
    assertFalse(vm.isLoading.value)
    assertTrue(vm.currentTickets.first().isEmpty())
    assertTrue(vm.expiredTickets.first().isEmpty())
  }
}
