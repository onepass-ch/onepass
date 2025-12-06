package ch.onepass.onepass.ui.event

import ch.onepass.onepass.model.user.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventCardViewModelTest {

  @MockK private lateinit var userRepository: UserRepository
  @MockK private lateinit var auth: FirebaseAuth
  @MockK private lateinit var mockUser: FirebaseUser

  private lateinit var viewModel: EventCardViewModel
  private val testDispatcher = StandardTestDispatcher()
  private val initialLikesFlow = MutableStateFlow<Set<String>>(emptySet())
  private val testUid = "TEST_UID"
  private val testEventId = "EVENT_123"

  @Before
  fun setUp() {
    MockKAnnotations.init(this)
    Dispatchers.setMain(testDispatcher)

    every { auth.currentUser } returns mockUser
    every { mockUser.uid } returns testUid
    every { userRepository.getFavoriteEvents(testUid) } returns initialLikesFlow

    viewModel = EventCardViewModel(userRepository, auth)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun isEventLiked_returns_correct_state() =
      runTest(testDispatcher) {
        initialLikesFlow.value = setOf(testEventId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.isEventLiked(testEventId))
        assertFalse(viewModel.isEventLiked("OTHER_EVENT"))
      }

  @Test
  fun toggleLike_adds_and_removes_optimistically_and_persists() =
      runTest(testDispatcher) {
        initialLikesFlow.value = emptySet()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.isEventLiked(testEventId))

        coEvery { userRepository.addFavoriteEvent(testUid, testEventId) } returns
            Result.success(Unit)

        viewModel.toggleLike(testEventId)
        assertTrue(viewModel.isEventLiked(testEventId))

        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.isEventLiked(testEventId))

        coEvery { userRepository.removeFavoriteEvent(testUid, testEventId) } returns
            Result.success(Unit)

        viewModel.toggleLike(testEventId)
        assertFalse(viewModel.isEventLiked(testEventId))

        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.isEventLiked(testEventId))
      }

  @Test
  fun toggleLike_reverts_state_on_persistence_failure() =
      runTest(testDispatcher) {
        // Mock the static Log class to avoid Android framework dependency
        mockkStatic(android.util.Log::class)
        every { android.util.Log.e(any(), any(), any()) } returns 0

        initialLikesFlow.value = emptySet()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.isEventLiked(testEventId))

        val failure = Result.failure<Unit>(Exception("Network Error"))
        coEvery { userRepository.addFavoriteEvent(testUid, testEventId) } returns failure

        viewModel.toggleLike(testEventId)
        assertTrue(viewModel.isEventLiked(testEventId))

        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.isEventLiked(testEventId))

        // Clean up the static mock
        unmockkStatic(android.util.Log::class)
      }

  @Test
  fun toggleLike_handles_no_logged_in_user() =
      runTest(testDispatcher) {
        every { auth.currentUser } returns null
        viewModel = EventCardViewModel(userRepository, auth)

        val initialSize = viewModel.likedEvents.value.size
        viewModel.toggleLike(testEventId)

        assertTrue(viewModel.likedEvents.value.size == initialSize)
      }
}
