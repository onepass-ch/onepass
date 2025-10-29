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

  @Test
  fun loadUserPass_failure_setsError_andKeepsQrNull_whenNoCache() = runTest {
    val vm = MyEventsViewModel(app, passRepo, ticketRepo, eventRepo, "u1")
    coEvery { passRepo.getOrCreateSignedPass("u1") } returns Result.failure(Exception("x"))
    vm.loadUserPass()
    advanceUntilIdle()
    assertNotNull(vm.error.first())
    assertNull(vm.userQrData.first())
    assertFalse(vm.isLoading.first())
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
  fun cache_isIsolated_perUser_noLeakBetweenDifferentUids() = runTest {
    val writerA = MyEventsViewModel(app, passRepo, ticketRepo, eventRepo, "uA")
    coEvery { passRepo.getOrCreateSignedPass("uA") } returns
        Result.success(Pass(uid = "uA", kid = "k", issuedAt = 1, version = 1, signature = "sigA"))
    writerA.loadUserPass()
    advanceUntilIdle()
    val cachedA = writerA.userQrData.first()
    val readerB = MyEventsViewModel(app, passRepo, ticketRepo, eventRepo, "uB")
    coEvery { passRepo.getOrCreateSignedPass("uB") } returns Result.failure(Exception("net"))
    readerB.loadUserPass()
    advanceUntilIdle()
    assertNull(readerB.userQrData.first())
    assertNotNull(readerB.error.first())
  }

  @Test
  fun loadUserPass_notAuthenticated_setsError_andKeepsQrNull() = runTest {
    val vm = MyEventsViewModel(app, passRepo, ticketRepo, eventRepo, null)
    vm.loadUserPass()
    advanceUntilIdle()
    assertNotNull(vm.error.first())
    assertNull(vm.userQrData.first())
    assertFalse(vm.isLoading.first())
  }

  @Test
  fun init_withNullUser_doesNotLoadAnyCachedQr() = runTest {
    val vm = MyEventsViewModel(app, passRepo, ticketRepo, eventRepo, null)
    advanceUntilIdle()
    assertNull(vm.userQrData.first())
  }
}
