package ch.onepass.onepass.ui.profile

import ch.onepass.onepass.model.user.User
import ch.onepass.onepass.model.user.UserRepository
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class ProfileViewModelTest {

  // ---------- Simple fake repository ----------
  private class FakeUserRepository(
      private val currentUser: User? = null,
      private val createdUser: User? = null,
      private val throwOnLoad: Boolean = false
  ) : UserRepository {

    override suspend fun getCurrentUser(): User? {
      if (throwOnLoad) throw RuntimeException("boom")
      return currentUser
    }

    override suspend fun getOrCreateUser(): User? {
      if (throwOnLoad) throw RuntimeException("boom")
      return createdUser
    }

    override suspend fun updateLastLogin(uid: String) {
      /* no-op */
    }
  }

  private fun makeUser(
      uid: String = "u1",
      email: String = "jane@example.com",
      display: String = "Jane Doe",
      avatar: String? = null
  ) = User(uid = uid, email = email, displayName = display, avatarUrl = avatar)

  // ---------- Tests ----------

  @Test
  fun initLoadsUseAndMapsToUIState() = runBlocking {
    val repo = FakeUserRepository(currentUser = makeUser())
    val vm = ProfileViewModel(userRepository = repo)

    // Wait until loading finishes then grab state
    val state = vm.state.filter { !it.loading }.first()

    assertEquals("Jane Doe", state.displayName)
    assertEquals("jane@example.com", state.email)
    assertEquals("JD", state.initials)
    assertNull(state.errorMessage)
  }

  @Test
  fun whenNoUserFoundSetsErrorState() = runBlocking {
    val repo = FakeUserRepository(currentUser = null, createdUser = null)
    val vm = ProfileViewModel(userRepository = repo)

    val state = vm.state.filter { !it.loading }.first()

    assertNotNull("Expected an error message", state.errorMessage)
  }

  @Test
  fun exceptionFromRepoSurfacesAsErrorState() = runBlocking {
    val repo = FakeUserRepository(throwOnLoad = true)
    val vm = ProfileViewModel(userRepository = repo)

    val state = vm.state.filter { !it.loading }.first()

    assertNotNull("Expected an error message", state.errorMessage)
  }

  @Test
  fun onCreateEventClickedEmitsOnboardingWhenNotOrganizer() = runBlocking {
    val repo = FakeUserRepository(currentUser = makeUser())
    val vm = ProfileViewModel(userRepository = repo)

    // Ensure state is ready before triggering effect
    vm.state.filter { !it.loading }.first()

    val effect =
        withTimeout(1000) {
          vm.onCreateEventClicked()
          vm.effects.first()
        }
    assertEquals(ProfileEffect.NavigateToOrganizerOnboarding, effect)
  }

  @Test
  fun createEventEmitsOrganizerOnboarding_withoutTimeout() = runBlocking {
    val repo = FakeUserRepository(currentUser = makeUser())
    val vm = ProfileViewModel(userRepository = repo)

    // Wait until the initial load finishes (no timeout used)
    vm.state.first { !it.loading }

    // Trigger and assert the only active effect
    vm.onCreateEventClicked()
    val effect = vm.effects.first()
    assertEquals(ProfileEffect.NavigateToOrganizerOnboarding, effect)
  }
}
