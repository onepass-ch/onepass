package ch.onepass.onepass.ui.organizer

import ch.onepass.onepass.model.organization.*
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationEditorViewModelTest {

  private lateinit var repository: FakeEditOrganizationRepository
  private lateinit var viewModel: OrganizationEditorViewModel

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    // Override Dispatchers.Main with test dispatcher
    Dispatchers.setMain(testDispatcher)

    repository = FakeEditOrganizationRepository()
    viewModel = OrganizationEditorViewModel(repository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun loadOrganizationById_found() = runTest {
    val org = testOrganization("org1")
    repository.organizationToReturn = org

    viewModel.loadOrganizationById("org1")
    advanceUntilIdle() // make coroutine complete

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals(org, state.organization)
    assertNull(state.errorMessage)
  }

  @Test
  fun loadOrganizationById_notFound() = runTest {
    repository.organizationToReturn = null

    viewModel.loadOrganizationById("org2")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.organization)
    assertEquals("Organization not found", state.errorMessage)
  }

  @Test
  fun updateOrganization_success() = runTest {
    val org = testOrganization("org1", "Old Name", "Old Description")
    repository.organizationToReturn = org

    viewModel.loadOrganizationById("org1")
    advanceUntilIdle()

    val data =
        OrganizationEditorData(
            id = "org1",
            name = "New Name",
            description = "New Description",
            contactEmail = "email@test.com",
            contactPhone = "987654",
            website = "newsite.com",
            instagram = "insta",
            facebook = "fb",
            tiktok = "tt",
            address = "address")

    viewModel.updateOrganization(data)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.success)
    assertEquals("New Name", state.organization?.name)
    assertEquals("New Description", state.organization?.description)
    assertEquals("email@test.com", state.organization?.contactEmail)
  }

  @Test
  fun updateOrganization_failure() = runTest {
    repository.shouldFailUpdate = true
    val org = testOrganization("org1")
    repository.organizationToReturn = org

    viewModel.loadOrganizationById("org1")
    advanceUntilIdle()

    val data =
        OrganizationEditorData(
            id = "org1",
            name = "New Name",
            description = "New Description",
            contactEmail = "email@test.com",
            contactPhone = "987654",
            website = "newsite.com",
            instagram = "insta",
            facebook = "fb",
            tiktok = "tt",
            address = "address")

    viewModel.updateOrganization(data)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertFalse(state.success)
    assertEquals("Update failed", state.errorMessage)
  }

  @Test
  fun updateOrganization_withoutLoadingOrganization() = runTest {
    val data =
        OrganizationEditorData(
            id = "org1",
            name = "New Name",
            description = "New Description",
            contactEmail = "email@test.com",
            contactPhone = "987654",
            website = "newsite.com",
            instagram = "insta",
            facebook = "fb",
            tiktok = "tt",
            address = "address")

    viewModel.updateOrganization(data)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("Cannot update: organization not loaded", state.errorMessage)
    assertFalse(state.success)
  }

  @Test
  fun clearSuccessFlag_works() = runTest {
    viewModel.clearSuccessFlag()
    val state = viewModel.uiState.value
    assertFalse(state.success)
  }

  @Test
  fun clearError_works() = runTest {
    viewModel.clearError()
    val state = viewModel.uiState.value
    assertNull(state.errorMessage)
  }

  private fun testOrganization(
      id: String,
      name: String = "Test Org",
      description: String = "Description"
  ) =
      Organization(
          id = id,
          name = name,
          description = description,
          ownerId = "owner1",
          status = OrganizationStatus.ACTIVE,
          members = emptyMap(),
          verified = false,
          profileImageUrl = null,
          website = null,
          instagram = null,
          tiktok = null,
          facebook = null,
          contactEmail = null,
          contactPhone = null,
          address = null,
          eventIds = emptyList(),
          followerCount = 0,
          averageRating = 0.0f,
          createdAt = Timestamp.now(),
          updatedAt = null)
}

class FakeEditOrganizationRepository : OrganizationRepository {
  var organizationToReturn: Organization? = null
  var shouldFailUpdate = false

  override fun getOrganizationById(organizationId: String): Flow<Organization?> = flow {
    emit(organizationToReturn)
  }

  override suspend fun updateOrganization(organization: Organization) =
      if (shouldFailUpdate) Result.failure(Exception("Update failed")) else Result.success(Unit)

  // Other methods not needed for tests
  override suspend fun createOrganization(organization: Organization): Result<String> = TODO()

  override suspend fun deleteOrganization(organizationId: String) = TODO()

  override fun getOrganizationsByOwner(ownerId: String) = TODO()

  override fun getOrganizationsByMember(userId: String) = TODO()

  override fun getOrganizationsByStatus(status: OrganizationStatus) = TODO()

  override fun searchOrganizations(query: String) = TODO()

  override fun getVerifiedOrganizations() = TODO()

  override suspend fun addMember(organizationId: String, userId: String, role: OrganizationRole) =
      TODO()

  override suspend fun removeMember(organizationId: String, userId: String) = TODO()

  override suspend fun updateMemberRole(
      organizationId: String,
      userId: String,
      newRole: OrganizationRole
  ) = TODO()

  override suspend fun createInvitation(invitation: OrganizationInvitation) = TODO()

  override fun getPendingInvitations(organizationId: String) = TODO()

  override fun getInvitationsByEmail(email: String) = TODO()

  override suspend fun updateInvitationStatus(invitationId: String, newStatus: InvitationStatus) =
      TODO()

  override suspend fun deleteInvitation(invitationId: String) = TODO()
}
