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

  @Before
  fun setup() {
    Dispatchers.setMain(dispatcher)

    context = ApplicationProvider.getApplicationContext()
    dataStore = context.passDataStore

    passRepo = mockk()
    ticketRepo = mockk()
    eventRepo = mockk()
    orgRepo = mockk()
    paymentRepo = mockk()

    // Default mock behaviors
    coEvery { ticketRepo.getActiveTickets(any()) } returns emptyFlow()
    coEvery { ticketRepo.getExpiredTickets(any()) } returns emptyFlow()
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
    val writer = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, uid)
    advanceUntilIdle()
    coEvery { passRepo.getOrCreateSignedPass(uid) } returns Result.success(pass)
    writer.loadUserPass()
    advanceUntilIdle()
    val cachedQr = writer.userQrData.value

    // Second VM: fail to load but retrieve from cache
    val reader = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, uid)
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
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, "   ")
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
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, uid)
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
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, uid)
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
    val writer = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, uid)
    advanceUntilIdle()
    coEvery { passRepo.getOrCreateSignedPass(uid) } returns Result.success(pass)
    writer.loadUserPass()
    advanceUntilIdle()

    // Second VM: should load from cache on init
    val reader = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, uid)
    advanceUntilIdle()

    assertEquals(writer.userQrData.value, reader.userQrData.value)
  }

  @Test
  fun init_loadsNothing_whenNoCacheForUser() = runTest {
    val uid = uniqueUid("fresh")
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, uid)
    advanceUntilIdle()

    assertNull(vm.userQrData.value)
  }

  @Test
  fun init_withNullUser_doesNotLoadAnyCachedQr() = runTest {
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, null)
    advanceUntilIdle()

    assertNull(vm.userQrData.value)
  }

  @Test
  fun clearCache_withNullUser_doesNothing() = runTest {
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, null)
    advanceUntilIdle()

    // Should not crash
    vm.clearCache()
    advanceUntilIdle()

    assertNull(vm.userQrData.value)
  }

  @Test
  fun refreshPass_withNullUser_setsError() = runTest {
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, null)
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
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, uid)
    advanceUntilIdle()

    assertTrue(vm.currentTickets.first().isEmpty())
    assertTrue(vm.expiredTickets.first().isEmpty())
  }

  @Test
  fun tickets_notQueried_whenUserIdNull() = runTest {
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, null)
    advanceUntilIdle()

    assertTrue(vm.currentTickets.first().isEmpty())
    assertTrue(vm.expiredTickets.first().isEmpty())
  }

  @Test
  fun tickets_queried_whenUserIdPresent() = runTest {
    val uid = uniqueUid("uZ")
    coEvery { ticketRepo.getActiveTickets(uid) } returns flowOf(emptyList())
    coEvery { ticketRepo.getExpiredTickets(uid) } returns flowOf(emptyList())

    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, uid)
    advanceUntilIdle()

    assertEquals(emptyList<ch.onepass.onepass.ui.myevents.Ticket>(), vm.currentTickets.first())
    assertEquals(emptyList<ch.onepass.onepass.ui.myevents.Ticket>(), vm.expiredTickets.first())
  }

  @Test
  fun enrichTickets_emptyList_returnsEmpty_andDoesNotQueryEvents() = runTest {
    val uid = uniqueUid("enrich")
    coEvery { ticketRepo.getActiveTickets(uid) } returns flowOf(emptyList())
    coEvery { ticketRepo.getExpiredTickets(uid) } returns flowOf(emptyList())

    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, uid)
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

    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, uid)
    advanceUntilIdle()

    // Just verify that eventRepo was called (enrichment happened)
    coVerify { eventRepo.getEventById("event123") }
  }

  @Test
  fun currentAndExpiredTickets_areIndependent() = runTest {
    val uid = uniqueUid("independent")
    coEvery { ticketRepo.getActiveTickets(uid) } returns flowOf(emptyList())
    coEvery { ticketRepo.getExpiredTickets(uid) } returns flowOf(emptyList())

    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, uid)
    advanceUntilIdle()

    assertNotSame(vm.currentTickets, vm.expiredTickets)
    assertEquals(emptyList<ch.onepass.onepass.ui.myevents.Ticket>(), vm.currentTickets.value)
    assertEquals(emptyList<ch.onepass.onepass.ui.myevents.Ticket>(), vm.expiredTickets.value)
  }

  // ==================== UI STATE ====================

  @Test
  fun uiState_initialState_isCorrect() = runTest {
    val uid = uniqueUid("ui_init")
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, uid)
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
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, uid)
    advanceUntilIdle()

    vm.selectTab(TicketTab.EXPIRED)
    advanceUntilIdle()

    assertEquals(TicketTab.EXPIRED, vm.uiState.first().selectedTab)
  }

  @Test
  fun toggleQrExpansion_togglesState() = runTest {
    val uid = uniqueUid("toggle")
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, uid)
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

    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, uid)
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

    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, uid)
    advanceUntilIdle()

    val uiState = vm.uiState.first()
    // Verify that UI state has been updated with expired tickets
    assertTrue(uiState.expiredTickets.isNotEmpty())
  }

  // ==================== LOADING STATES ====================

  @Test
  fun userQrData_isNullInitially_beforeAnyLoad() = runTest {
    val uid = uniqueUid("init")
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, uid)
    advanceUntilIdle()

    assertNull(vm.userQrData.value)
    assertNull(vm.error.value)
    assertFalse(vm.isLoading.value)
  }

  @Test
  fun initial_states_areSane_beforeAnyAction() = runTest {
    val uid = uniqueUid("sanity")
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, orgRepo, paymentRepo, uid)
    advanceUntilIdle()

    assertNull(vm.userQrData.value)
    assertNull(vm.error.value)
    assertFalse(vm.isLoading.value)
    assertTrue(vm.currentTickets.first().isEmpty())
    assertTrue(vm.expiredTickets.first().isEmpty())
  }
}
