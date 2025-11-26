package ch.onepass.onepass.ui.profile

import ch.onepass.onepass.model.membership.Membership
import ch.onepass.onepass.model.organization.OrganizationRole
import ch.onepass.onepass.model.user.FakeUserRepository
import ch.onepass.onepass.model.user.User
import ch.onepass.onepass.utils.TestMockMembershipRepository
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
    val membershipRepo = TestMockMembershipRepository()
    val vm = ProfileViewModel(userRepository = repo, membershipRepository = membershipRepo)

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
    val membershipRepo = TestMockMembershipRepository()
    val vm = ProfileViewModel(userRepository = repo, membershipRepository = membershipRepo)

    val state = vm.state.filter { !it.loading }.first()

    assertNotNull("Expected an error message", state.errorMessage)
  }

  @Test
  fun exceptionFromRepoSurfacesAsErrorState() = runBlocking {
    val repo = FakeUserRepository(throwOnLoad = true)
    val membershipRepo = TestMockMembershipRepository()
    val vm = ProfileViewModel(userRepository = repo, membershipRepository = membershipRepo)

    val state = vm.state.filter { !it.loading }.first()

    assertNotNull("Expected an error message", state.errorMessage)
  }

  @Test
  fun onOrganizationButtonEmitsNavigateToMyOrganizations() = runBlocking {
    val repo = FakeUserRepository(currentUser = makeUser())
    val membershipRepo = TestMockMembershipRepository()
    val vm = ProfileViewModel(userRepository = repo, membershipRepository = membershipRepo)

    // Ensure state is ready before triggering effect
    vm.state.filter { !it.loading }.first()

    val effect =
        withTimeout(1000) {
          vm.onOrganizationButton()
          vm.effects.first()
        }
    // Note: The current implementation always navigates to MyOrganizations
    assertEquals(ProfileEffect.NavigateToMyOrganizations, effect)
  }

  @Test
  fun isOrganizerIsTrueWhenUserHasOwnerRoleMembership() = runBlocking {
    val user = makeUser(uid = "user-1")
    val repo = FakeUserRepository(currentUser = user)
    val membership = Membership(userId = "user-1", orgId = "org-1", role = OrganizationRole.OWNER)
    val membershipRepo =
        TestMockMembershipRepository(organizationsByUser = mapOf("user-1" to listOf(membership)))
    val vm = ProfileViewModel(userRepository = repo, membershipRepository = membershipRepo)

    val state = vm.state.filter { !it.loading }.first()

    assertTrue("User with OWNER role membership should be organizer", state.isOrganizer)
  }

  @Test
  fun isOrganizerIsFalseWhenUserHasNoMemberships() = runBlocking {
    val user = makeUser(uid = "user-1")
    val repo = FakeUserRepository(currentUser = user)
    val membershipRepo = TestMockMembershipRepository()
    val vm = ProfileViewModel(userRepository = repo, membershipRepository = membershipRepo)

    val state = vm.state.filter { !it.loading }.first()

    assertFalse("User with no memberships should not be organizer", state.isOrganizer)
  }

  @Test
  fun isOrganizerIsFalseWhenUserHasOnlyMemberRoleMembership() = runBlocking {
    val user = makeUser(uid = "user-1")
    val repo = FakeUserRepository(currentUser = user)
    val membership = Membership(userId = "user-1", orgId = "org-1", role = OrganizationRole.MEMBER)
    val membershipRepo =
        TestMockMembershipRepository(organizationsByUser = mapOf("user-1" to listOf(membership)))
    val vm = ProfileViewModel(userRepository = repo, membershipRepository = membershipRepo)

    val state = vm.state.filter { !it.loading }.first()

    assertFalse("User with only MEMBER role should not be organizer", state.isOrganizer)
  }

  @Test
  fun isOrganizerIsTrueWhenUserHasOwnerRoleAmongMultipleMemberships() = runBlocking {
    val user = makeUser(uid = "user-1")
    val repo = FakeUserRepository(currentUser = user)
    val memberships =
        listOf(
            Membership(userId = "user-1", orgId = "org-1", role = OrganizationRole.MEMBER),
            Membership(userId = "user-1", orgId = "org-2", role = OrganizationRole.OWNER),
            Membership(userId = "user-1", orgId = "org-3", role = OrganizationRole.STAFF))
    val membershipRepo =
        TestMockMembershipRepository(organizationsByUser = mapOf("user-1" to memberships))
    val vm = ProfileViewModel(userRepository = repo, membershipRepository = membershipRepo)

    val state = vm.state.filter { !it.loading }.first()

    assertTrue("User with at least one OWNER role should be organizer", state.isOrganizer)
  }

  @Test
  fun isOrganizerIsFalseWhenMembershipRepositoryReturnsError() = runBlocking {
    val user = makeUser(uid = "user-1")
    val repo = FakeUserRepository(currentUser = user)
    val membershipRepo =
        TestMockMembershipRepository(
            getOrganizationsByUserError = RuntimeException("Network error"))
    val vm = ProfileViewModel(userRepository = repo, membershipRepository = membershipRepo)

    val state = vm.state.filter { !it.loading }.first()

    // When getOrganizationsByUser fails, getOrNull() returns null, which becomes emptyList(),
    // so isOrganizer should be false
    assertFalse("User should not be organizer when membership check fails", state.isOrganizer)
  }

  @Test
  fun onInvitationsEmitsNavigateToMyInvitations() = runBlocking {
    val repo = FakeUserRepository(currentUser = makeUser())
    val membershipRepo = TestMockMembershipRepository()
    val vm = ProfileViewModel(userRepository = repo, membershipRepository = membershipRepo)

    vm.state.filter { !it.loading }.first()

    vm.onInvitations()
    val effect = vm.effects.first()

    assertEquals(ProfileEffect.NavigateToMyInvitations, effect)
  }
}
