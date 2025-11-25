package ch.onepass.onepass.ui.notification

import ch.onepass.onepass.model.notification.Notification
import ch.onepass.onepass.model.notification.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var repository: NotificationRepository
  private lateinit var auth: FirebaseAuth
  private lateinit var user: FirebaseUser
  private lateinit var viewModel: NotificationsViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    repository = mockk(relaxed = true)
    auth = mockk(relaxed = true)
    user = mockk(relaxed = true)

    every { auth.currentUser } returns user
    every { user.uid } returns "testUser"
    every { repository.getUserNotifications("testUser") } returns flowOf(emptyList())
    every { repository.getUnreadCount("testUser") } returns flowOf(0)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `loadNotifications updates state`() = runTest {
    val notifs = listOf(Notification(title = "Test"))
    every { repository.getUserNotifications("testUser") } returns flowOf(notifs)
    every { repository.getUnreadCount("testUser") } returns flowOf(5)

    viewModel = NotificationsViewModel(repository, auth)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    Assert.assertEquals(notifs, state.notifications)
    Assert.assertEquals(5, state.unreadCount)
    Assert.assertFalse(state.isLoading)
  }

  @Test
  fun `markAsRead calls repository`() = runTest {
    viewModel = NotificationsViewModel(repository, auth)

    viewModel.markAsRead("123")
    advanceUntilIdle()

    coVerify { repository.markAsRead("123") }
  }

  @Test
  fun `markAllAsRead calls repository`() = runTest {
    viewModel = NotificationsViewModel(repository, auth)

    viewModel.markAllAsRead()
    advanceUntilIdle()

    coVerify { repository.markAllAsRead("testUser") }
  }

  @Test
  fun `deleteNotification calls repository and reloads`() = runTest {
    viewModel = NotificationsViewModel(repository, auth)

    viewModel.deleteNotification("123")
    advanceUntilIdle()

    coVerify { repository.deleteNotification("123") }
    // Verify getUserNotifications is called again (initial init + reload)
    verify(atLeast = 2) { repository.getUserNotifications("testUser") }
  }

  @Test
  fun `no user stops loading`() = runTest {
    every { auth.currentUser } returns null
    viewModel = NotificationsViewModel(repository, auth)
    advanceUntilIdle()

    // Verify we didn't call the repo with a null ID or crash
    verify(exactly = 0) { repository.getUserNotifications(any()) }
  }
}
