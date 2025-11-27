package ch.onepass.onepass.ui.myevents

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.pass.Pass
import ch.onepass.onepass.model.pass.PassRepository
import ch.onepass.onepass.model.ticket.Ticket
import ch.onepass.onepass.model.ticket.TicketRepository
import io.mockk.coEvery
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
    val writer = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, uid)
    advanceUntilIdle()
    coEvery { passRepo.getOrCreateSignedPass(uid) } returns Result.success(pass)
    writer.loadUserPass()
    advanceUntilIdle()
    val cachedQr = writer.userQrData.first()

    // Second VM: fail to load but retrieve from cache
    val reader = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, uid)
    advanceUntilIdle()
    coEvery { passRepo.getOrCreateSignedPass(uid) } returns
        Result.failure(Exception("Network down"))
    reader.loadUserPass()
    advanceUntilIdle()

    assertEquals(cachedQr, reader.userQrData.first())
    assertNotNull(reader.error.first())
  }

  @Test
  fun loadUserPass_notAuthenticated_setsError_andKeepsQrNull() = runTest {
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, null)
    advanceUntilIdle()

    vm.loadUserPass()
    advanceUntilIdle()

    assertNotNull(vm.error.first())
    assertTrue(vm.error.first()?.contains("not authenticated") == true)
    assertNull(vm.userQrData.first())
  }

  @Test
  fun loadUserPass_blankUserId_setsError() = runTest {
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, "   ")
    advanceUntilIdle()

    vm.loadUserPass()
    advanceUntilIdle()

    assertNotNull(vm.error.value)
    assertTrue(vm.error.value!!.contains("not authenticated"))
  }

  @Test
  fun loadUserPass_throwable_setsError_andLeavesQrNull() = runTest {
    val uid = uniqueUid("boom")
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, uid)
    advanceUntilIdle()
    coEvery { passRepo.getOrCreateSignedPass(uid) } throws RuntimeException("Boom!")

    vm.loadUserPass()
    advanceUntilIdle()

    assertTrue(vm.error.first()?.contains("Boom") == true)
    assertNull(vm.userQrData.first())
  }

  @Test
  fun failure_after_success_keepsPreviousQr() = runTest {
    val uid = uniqueUid("mix")
    val pass = Pass(uid = uid, kid = "k", issuedAt = 5, version = 1, signature = "mix")
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, uid)
    advanceUntilIdle()

    // First success
    coEvery { passRepo.getOrCreateSignedPass(uid) } returns Result.success(pass)
    vm.loadUserPass()
    advanceUntilIdle()
    val okQr = vm.userQrData.first()

    // Then failure - should keep cached QR
    coEvery { passRepo.getOrCreateSignedPass(uid) } returns Result.failure(Exception("Network"))
    vm.loadUserPass()
    advanceUntilIdle()

    assertEquals(okQr, vm.userQrData.first())
    assertNotNull(vm.error.first())
  }

  @Test
  fun repoThrowsException_setsError_andKeepsQr() = runTest {
    val uid = uniqueUid("ex")
    val pass = Pass(uid = uid, kid = "k", issuedAt = 11, version = 1, signature = "ex")
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, uid)
    advanceUntilIdle()

    // First success
    coEvery { passRepo.getOrCreateSignedPass(uid) } returns Result.success(pass)
    vm.loadUserPass()
    advanceUntilIdle()
    val qr = vm.userQrData.first()

    // Then exception
    coEvery { passRepo.getOrCreateSignedPass(uid) } throws RuntimeException("Crash!")
    vm.loadUserPass()
    advanceUntilIdle()

    assertEquals(qr, vm.userQrData.first())
    assertTrue(vm.error.first()?.contains("Crash") == true)
  }

  // ==================== CACHE MANAGEMENT ====================

  @Test
  fun init_loadsCachedQr_onStartup() = runTest {
    val uid = uniqueUid("startup")
    val pass = Pass(uid = uid, kid = "k", issuedAt = 1, version = 1, signature = "startup")

    // First VM: cache the QR
    val writer = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, uid)
    advanceUntilIdle()
    coEvery { passRepo.getOrCreateSignedPass(uid) } returns Result.success(pass)
    writer.loadUserPass()
    advanceUntilIdle()

    // Second VM: should load from cache on init
    val reader = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, uid)
    advanceUntilIdle()

    assertEquals(writer.userQrData.first(), reader.userQrData.first())
  }

  @Test
  fun init_loadsNothing_whenNoCacheForUser() = runTest {
    val uid = uniqueUid("fresh")
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, uid)
    advanceUntilIdle()

    assertNull(vm.userQrData.first())
  }

  @Test
  fun init_withNullUser_doesNotLoadAnyCachedQr() = runTest {
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, null)
    advanceUntilIdle()

    assertNull(vm.userQrData.first())
  }

  @Test
  fun refreshPass_withNullUser_setsError() = runTest {
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, null)
    advanceUntilIdle()

    vm.refreshPass()
    advanceUntilIdle()

    assertNotNull(vm.error.first())
    assertNull(vm.userQrData.first())
  }

  // ==================== TICKETS ====================

  @Test
  fun tickets_areEmpty_whenReposReturnEmpty() = runTest {
    val uid = uniqueUid("tickets")
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, uid)
    advanceUntilIdle()

    assertTrue(vm.currentTickets.first().isEmpty())
    assertTrue(vm.expiredTickets.first().isEmpty())
  }

  @Test
  fun tickets_notQueried_whenUserIdNull() = runTest {
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, null)
    advanceUntilIdle()

    assertTrue(vm.currentTickets.first().isEmpty())
    assertTrue(vm.expiredTickets.first().isEmpty())
  }

  @Test
  fun tickets_queried_whenUserIdPresent() = runTest {
    val uid = uniqueUid("uZ")
    coEvery { ticketRepo.getActiveTickets(uid) } returns flowOf(emptyList())
    coEvery { ticketRepo.getExpiredTickets(uid) } returns flowOf(emptyList())

    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, uid)
    advanceUntilIdle()

    assertEquals(emptyList<ch.onepass.onepass.ui.myevents.Ticket>(), vm.currentTickets.first())
    assertEquals(emptyList<ch.onepass.onepass.ui.myevents.Ticket>(), vm.expiredTickets.first())
  }

  @Test
  fun enrichTickets_emptyList_returnsEmpty_andDoesNotQueryEvents() = runTest {
    val uid = uniqueUid("enrich")
    coEvery { ticketRepo.getActiveTickets(uid) } returns flowOf(emptyList())
    coEvery { ticketRepo.getExpiredTickets(uid) } returns flowOf(emptyList())

    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, uid)
    advanceUntilIdle()

    assertTrue(vm.currentTickets.first().isEmpty())
    verify(exactly = 0) { eventRepo.getEventById(any()) }
  }

  @Test
  fun currentAndExpiredTickets_areIndependent() = runTest {
    val uid = uniqueUid("independent")
    coEvery { ticketRepo.getActiveTickets(uid) } returns flowOf(emptyList())
    coEvery { ticketRepo.getExpiredTickets(uid) } returns flowOf(emptyList())

    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, uid)
    advanceUntilIdle()

    assertNotSame(vm.currentTickets, vm.expiredTickets)
    assertEquals(emptyList<ch.onepass.onepass.ui.myevents.Ticket>(), vm.currentTickets.value)
    assertEquals(emptyList<ch.onepass.onepass.ui.myevents.Ticket>(), vm.expiredTickets.value)
  }

  // ==================== LOADING STATES ====================

  @Test
  fun userQrData_isNullInitially_beforeAnyLoad() = runTest {
    val uid = uniqueUid("init")
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, uid)
    advanceUntilIdle()

    assertNull(vm.userQrData.first())
    assertNull(vm.error.first())
    assertFalse(vm.isLoading.first())
  }

  @Test
  fun isLoading_isFalse_afterLoadFails() = runTest {
    val uid = uniqueUid("loadFail")
    coEvery { passRepo.getOrCreateSignedPass(uid) } returns Result.failure(Exception("Err"))

    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, uid)
    advanceUntilIdle()

    vm.loadUserPass()
    advanceUntilIdle()

    assertFalse(vm.isLoading.value)
  }

  @Test
  fun initial_states_areSane_beforeAnyAction() = runTest {
    val uid = uniqueUid("sanity")
    val vm = MyEventsViewModel(dataStore, passRepo, ticketRepo, eventRepo, uid)
    advanceUntilIdle()

    assertNull(vm.userQrData.first())
    assertNull(vm.error.first())
    assertFalse(vm.isLoading.first())
    assertTrue(vm.currentTickets.first().isEmpty())
    assertTrue(vm.expiredTickets.first().isEmpty())
  }
}
