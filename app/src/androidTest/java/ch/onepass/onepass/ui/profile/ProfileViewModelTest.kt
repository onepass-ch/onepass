package ch.onepass.onepass.ui.profile

import ch.onepass.onepass.model.user.FakeUserRepository
import ch.onepass.onepass.model.user.User
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class ProfileViewModelTest {

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
          vm.onOrganizationButton()
          vm.effects.first()
        }
    assertEquals(ProfileEffect.NavigateToBecomeOrganizer, effect)
  }

  @Test
  fun onInvitationsEmitsNavigateToMyInvitations() = runBlocking {
    val repo = FakeUserRepository(currentUser = makeUser())
    val vm = ProfileViewModel(userRepository = repo)

    vm.state.filter { !it.loading }.first()

    vm.onInvitations()
    val effect = vm.effects.first()

    assertEquals(ProfileEffect.NavigateToMyInvitations, effect)
  }
}
