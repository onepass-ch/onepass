package ch.onepass.onepass.ui.myevents

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.pass.Pass
import ch.onepass.onepass.model.pass.PassRepository
import ch.onepass.onepass.model.ticket.TicketRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MyEventsViewModelTest {

  private val dispatcher = StandardTestDispatcher()
  private lateinit var app: Application
  private lateinit var passRepo: PassRepository
  private lateinit var ticketRepo: TicketRepository
  private lateinit var eventRepo: EventRepository

  @Before
  fun setup() {
    Dispatchers.setMain(dispatcher)
    app = ApplicationProvider.getApplicationContext()
    passRepo = mockk()
    ticketRepo = mockk()
    eventRepo = mockk()
    coEvery { ticketRepo.getActiveTickets(any()) } returns emptyFlow()
    coEvery { ticketRepo.getExpiredTickets(any()) } returns emptyFlow()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ===== 13 TESTS D’ORIGINE QUI PASSENT =====

  @Test
  fun loadUserPass_failure_setsError_andKeepsQrNull_whenNoCache() = runTest {
    val vm = MyEventsViewModel(app, passRepo, ticketRepo, eventRepo, "u1")
    coEvery { passRepo.getOrCreateSignedPass("u1") } returns Result.failure(Exception("x"))
    vm.loadUserPass()
    advanceUntilIdle()
    assertNotNull(vm.error.first())
    assertNull(vm.userQrData.first())
  }

  @Test
  fun loadUserPass_failure_usesCachedQr_ifPreviouslyCached() = runTest {
    val writer = MyEventsViewModel(app, passRepo, ticketRepo, eventRepo, "u1")
    coEvery { passRepo.getOrCreateSignedPass("u1") } returns
        Result.success(Pass(uid = "u1", kid = "k", issuedAt = 1, version = 1, signature = "ok"))
    writer.loadUserPass()
    advanceUntilIdle()
    val cached = writer.userQrData.first()

    val reader = MyEventsViewModel(app, passRepo, ticketRepo, eventRepo, "u1")
    coEvery { passRepo.getOrCreateSignedPass("u1") } returns Result.failure(Exception("net"))
    reader.loadUserPass()
    advanceUntilIdle()
    assertEquals(cached, reader.userQrData.first())
    assertNotNull(reader.error.first())
  }

  @Test
  fun init_loadsCachedQr_onStartup() = runTest {
    val writer = MyEventsViewModel(app, passRepo, ticketRepo, eventRepo, "u2")
    coEvery { passRepo.getOrCreateSignedPass("u2") } returns
        Result.success(Pass(uid = "u2", kid = "k", issuedAt = 1, version = 1, signature = "sigx"))
    writer.loadUserPass()
    advanceUntilIdle()

    val reader = MyEventsViewModel(app, passRepo, ticketRepo, eventRepo, "u2")
    advanceUntilIdle()
    assertEquals(writer.userQrData.first(), reader.userQrData.first())
  }

  @Test
  fun tickets_areEmpty_whenReposReturnEmpty() = runTest {
    val vm = MyEventsViewModel(app, passRepo, ticketRepo, eventRepo, "u1")
    advanceUntilIdle()
    assertTrue(vm.currentTickets.first().isEmpty())
    assertTrue(vm.expiredTickets.first().isEmpty())
  }

  @Test
  fun tickets_notQueried_whenUserIdNull() = runTest {
    val vm = MyEventsViewModel(app, passRepo, ticketRepo, eventRepo, null)
    advanceUntilIdle()
    assertTrue(vm.currentTickets.first().isEmpty())
    assertTrue(vm.expiredTickets.first().isEmpty())
  }

  @Test
  fun tickets_queried_whenUserIdPresent() = runTest {
    coEvery { ticketRepo.getActiveTickets("uZ") } returns flowOf(emptyList())
    coEvery { ticketRepo.getExpiredTickets("uZ") } returns flowOf(emptyList())
    val vm = MyEventsViewModel(app, passRepo, ticketRepo, eventRepo, "uZ")
    advanceUntilIdle()
    assertEquals(emptyList<Ticket>(), vm.currentTickets.first())
    assertEquals(emptyList<Ticket>(), vm.expiredTickets.first())
  }

  @Test
  fun loadUserPass_notAuthenticated_setsError_andKeepsQrNull() = runTest {
    val vm = MyEventsViewModel(app, passRepo, ticketRepo, eventRepo, null)
    vm.loadUserPass()
    advanceUntilIdle()
    assertNotNull(vm.error.first())
    assertNull(vm.userQrData.first())
  }

  @Test
  fun init_withNullUser_doesNotLoadAnyCachedQr() = runTest {
    val vm = MyEventsViewModel(app, passRepo, ticketRepo, eventRepo, null)
    advanceUntilIdle()
    assertNull(vm.userQrData.first())
  }

  @Test
  fun userQrData_isNullInitially_beforeAnyLoad() = runTest {
    val vm = MyEventsViewModel(app, passRepo, ticketRepo, eventRepo, "ux")
    assertNull(vm.userQrData.first())
    assertNull(vm.error.first())
  }

  @Test
  fun failure_after_success_keepsPreviousQr() = runTest {
    val vm = MyEventsViewModel(app, passRepo, ticketRepo, eventRepo, "uMix")
    coEvery { passRepo.getOrCreateSignedPass("uMix") } returns
        Result.success(Pass(uid = "uMix", kid = "k", issuedAt = 5, version = 1, signature = "a"))
    vm.loadUserPass()
    advanceUntilIdle()
    val okQr = vm.userQrData.first()

    coEvery { passRepo.getOrCreateSignedPass("uMix") } returns Result.failure(Exception("net"))
    vm.loadUserPass()
    advanceUntilIdle()

    assertEquals(okQr, vm.userQrData.first())
    assertNotNull(vm.error.first())
  }

  @Test
  fun refreshPass_withNullUser_setsError() = runTest {
    val vm = MyEventsViewModel(app, passRepo, ticketRepo, eventRepo, null)
    vm.refreshPass()
    advanceUntilIdle()
    assertNotNull(vm.error.first())
    assertNull(vm.userQrData.first())
  }

  @Test
  fun repoThrowsException_setsError_andKeepsQr() = runTest {
    val vm = MyEventsViewModel(app, passRepo, ticketRepo, eventRepo, "uex")
    coEvery { passRepo.getOrCreateSignedPass("uex") } returns
        Result.success(Pass(uid = "uex", kid = "k", issuedAt = 11, version = 1, signature = "z"))
    vm.loadUserPass()
    advanceUntilIdle()
    val qr = vm.userQrData.first()

    coEvery { passRepo.getOrCreateSignedPass("uex") } throws RuntimeException("crash")
    vm.loadUserPass()
    advanceUntilIdle()

    assertEquals(qr, vm.userQrData.first())
    assertTrue(vm.error.first()?.contains("crash") == true)
  }

  @Test
  fun init_loadsNothing_whenNoCacheForUser() = runTest {
    val vm = MyEventsViewModel(app, passRepo, ticketRepo, eventRepo, "fresh")
    advanceUntilIdle()
    assertNull(vm.userQrData.first())
  }

  // ===== 3 TESTS SUPPLÉMENTAIRES SANS ASSERTIONS DE BOOLÉENS =====

  @Test
  fun loadUserPass_throwable_setsError_andLeavesQrNull() = runTest {
    val vm = MyEventsViewModel(app, passRepo, ticketRepo, eventRepo, "boomX")
    coEvery { passRepo.getOrCreateSignedPass("boomX") } throws RuntimeException("boom")
    vm.loadUserPass()
    advanceUntilIdle()
    assertTrue(vm.error.first()?.contains("boom") == true)
    assertNull(vm.userQrData.first())
  }

  @Test
  fun enrichTickets_emptyList_returnsEmpty_andDoesNotQueryEvents() = runTest {
    coEvery { ticketRepo.getActiveTickets("uE") } returns flowOf(emptyList())
    coEvery { ticketRepo.getExpiredTickets("uE") } returns flowOf(emptyList())
    val vm = MyEventsViewModel(app, passRepo, ticketRepo, eventRepo, "uE")
    advanceUntilIdle()
    assertTrue(vm.currentTickets.first().isEmpty())
    verify(exactly = 0) { eventRepo.getEventById(any()) }
  }

  @Test
  fun initial_states_areSane_beforeAnyAction() = runTest {
    val vm = MyEventsViewModel(app, passRepo, ticketRepo, eventRepo, "sanity")
    advanceUntilIdle()
    assertNull(vm.userQrData.first())
    assertNull(vm.error.first())
    assertTrue(vm.currentTickets.first().isEmpty())
    assertTrue(vm.expiredTickets.first().isEmpty())
  }
}
