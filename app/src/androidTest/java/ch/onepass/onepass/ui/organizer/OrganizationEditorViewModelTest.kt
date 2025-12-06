package ch.onepass.onepass.ui.organizer

import ch.onepass.onepass.model.organization.*
import ch.onepass.onepass.model.storage.FakeStorageRepository
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
  private lateinit var storageRepository: FakeStorageRepository
  private lateinit var viewModel: OrganizationEditorViewModel

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    // Override Dispatchers.Main with test dispatcher
    Dispatchers.setMain(testDispatcher)

    repository = FakeEditOrganizationRepository()
    storageRepository = FakeStorageRepository()
    viewModel = OrganizationEditorViewModel(repository, storageRepository)
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
            phonePrefix = "1",
            website = "newsite.com",
            instagram = "insta",
            facebook = "fb",
            tiktok = "tt",
            address = "address",
            profileImageUri = null,
            coverImageUri = null)

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
            phonePrefix = "1",
            website = "newsite.com",
            instagram = "insta",
            facebook = "fb",
            tiktok = "tt",
            address = "address",
            profileImageUri = null,
            coverImageUri = null)

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
            phonePrefix = "1",
            website = "newsite.com",
            instagram = "insta",
            facebook = "fb",
            tiktok = "tt",
            address = "address",
            profileImageUri = null,
            coverImageUri = null)

    viewModel.updateOrganization(data)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("Cannot update: organization not loaded", state.errorMessage)
    assertFalse(state.success)
  }

  // ===== NEW TESTS FOR IMAGE FUNCTIONALITY =====

  @Test
  fun updateOrganizationWithProfileImage() = runTest {
    val org = testOrganization("org1")
    repository.organizationToReturn = org

    viewModel.loadOrganizationById("org1")
    advanceUntilIdle()

    val profileUri = android.net.Uri.parse("content://media/image/profile123")
    val data =
        OrganizationEditorData(
            id = "org1",
            name = "Updated Name",
            description = "Updated Description",
            contactEmail = "email@test.com",
            contactPhone = "987654",
            phonePrefix = "1",
            website = "website.com",
            instagram = "insta",
            facebook = "fb",
            tiktok = "tt",
            address = "address",
            profileImageUri = profileUri,
            coverImageUri = null)

    viewModel.updateOrganization(data)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.success)
  }

  @Test
  fun updateOrganizationWithCoverImage() = runTest {
    val org = testOrganization("org1")
    repository.organizationToReturn = org

    viewModel.loadOrganizationById("org1")
    advanceUntilIdle()

    val coverUri = android.net.Uri.parse("content://media/image/cover456")
    val data =
        OrganizationEditorData(
            id = "org1",
            name = "Updated Name",
            description = "Updated Description",
            contactEmail = "email@test.com",
            contactPhone = "987654",
            phonePrefix = "1",
            website = "website.com",
            instagram = "insta",
            facebook = "fb",
            tiktok = "tt",
            address = "address",
            profileImageUri = null,
            coverImageUri = coverUri)

    viewModel.updateOrganization(data)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.success)
  }

  @Test
  fun updateOrganizationWithBothImages() = runTest {
    val org = testOrganization("org1")
    repository.organizationToReturn = org

    viewModel.loadOrganizationById("org1")
    advanceUntilIdle()

    val profileUri = android.net.Uri.parse("content://media/image/profile123")
    val coverUri = android.net.Uri.parse("content://media/image/cover456")
    val data =
        OrganizationEditorData(
            id = "org1",
            name = "Updated Name",
            description = "Updated Description",
            contactEmail = "email@test.com",
            contactPhone = "987654",
            phonePrefix = "1",
            website = "website.com",
            instagram = "insta",
            facebook = "fb",
            tiktok = "tt",
            address = "address",
            profileImageUri = profileUri,
            coverImageUri = coverUri)

    viewModel.updateOrganization(data)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.success)
  }

  @Test
  fun updateOrganizationWithoutNewImages() = runTest {
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
            phonePrefix = "1",
            website = "newsite.com",
            instagram = "insta",
            facebook = "fb",
            tiktok = "tt",
            address = "address",
            profileImageUri = null,
            coverImageUri = null)

    viewModel.updateOrganization(data)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.success)
    // Existing images should be preserved (not replaced with null)
    assertEquals("New Name", state.organization?.name)
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

  // ===== TESTS FOR OrganizationEditorData.fromForm METHOD =====

  @Test
  fun fromFormCreatesDataWithAllFields() = runTest {
    val formState =
        OrganizationFormState(
            name = FieldState(value = "Test Org"),
            description = FieldState(value = "Test Description"),
            contactEmail = FieldState(value = "test@example.com"),
            contactPhone = FieldState(value = "1234567890"),
            contactPhonePrefix = androidx.compose.runtime.mutableStateOf("+1"),
            website = FieldState(value = "https://example.com"),
            instagram = FieldState(value = "test_instagram"),
            facebook = FieldState(value = "test_facebook"),
            tiktok = FieldState(value = "test_tiktok"),
            address = FieldState(value = "123 Test St"),
            profileImageUri = null,
            coverImageUri = null)

    val data = OrganizationEditorData.fromForm("org123", formState)

    assertEquals("org123", data.id)
    assertEquals("Test Org", data.name)
    assertEquals("Test Description", data.description)
    assertEquals("test@example.com", data.contactEmail)
    assertEquals("1234567890", data.contactPhone)
    assertEquals("+1", data.phonePrefix)
    assertEquals("https://example.com", data.website)
    assertEquals("test_instagram", data.instagram)
    assertEquals("test_facebook", data.facebook)
    assertEquals("test_tiktok", data.tiktok)
    assertEquals("123 Test St", data.address)
    assertNull(data.profileImageUri)
    assertNull(data.coverImageUri)
  }

  @Test
  fun fromFormHandlesBlankFieldsAsNull() = runTest {
    val formState =
        OrganizationFormState(
            name = FieldState(value = "Test Org"),
            description = FieldState(value = "Test Description"),
            contactEmail = FieldState(value = ""),
            contactPhone = FieldState(value = ""),
            contactPhonePrefix = androidx.compose.runtime.mutableStateOf(""),
            website = FieldState(value = ""),
            instagram = FieldState(value = ""),
            facebook = FieldState(value = ""),
            tiktok = FieldState(value = ""),
            address = FieldState(value = ""),
            profileImageUri = null,
            coverImageUri = null)

    val data = OrganizationEditorData.fromForm("org123", formState)

    assertEquals("org123", data.id)
    assertEquals("Test Org", data.name)
    assertEquals("Test Description", data.description)
    assertNull(data.contactEmail)
    assertNull(data.contactPhone)
    assertNull(data.phonePrefix)
    assertNull(data.website)
    assertNull(data.instagram)
    assertNull(data.facebook)
    assertNull(data.tiktok)
    assertNull(data.address)
  }

  @Test
  fun fromFormIncludesImageUris() = runTest {
    val profileUri = android.net.Uri.parse("content://media/image/profile123")
    val coverUri = android.net.Uri.parse("content://media/image/cover456")

    val formState =
        OrganizationFormState(
            name = FieldState(value = "Test Org"),
            description = FieldState(value = "Test Description"),
            contactEmail = FieldState(value = "test@example.com"),
            contactPhone = FieldState(value = "1234567890"),
            contactPhonePrefix = androidx.compose.runtime.mutableStateOf("+1"),
            website = FieldState(value = "https://example.com"),
            instagram = FieldState(value = "test_instagram"),
            facebook = FieldState(value = "test_facebook"),
            tiktok = FieldState(value = "test_tiktok"),
            address = FieldState(value = "123 Test St"),
            profileImageUri = profileUri,
            coverImageUri = coverUri)

    val data = OrganizationEditorData.fromForm("org123", formState)

    assertEquals(profileUri, data.profileImageUri)
    assertEquals(coverUri, data.coverImageUri)
  }

  @Test
  fun fromFormHandlesOnlyProfileImage() = runTest {
    val profileUri = android.net.Uri.parse("content://media/image/profile123")

    val formState =
        OrganizationFormState(
            name = FieldState(value = "Test Org"),
            description = FieldState(value = "Test Description"),
            contactEmail = FieldState(value = "test@example.com"),
            contactPhone = FieldState(value = "1234567890"),
            contactPhonePrefix = androidx.compose.runtime.mutableStateOf("+1"),
            website = FieldState(value = "https://example.com"),
            instagram = FieldState(value = "test_instagram"),
            facebook = FieldState(value = "test_facebook"),
            tiktok = FieldState(value = "test_tiktok"),
            address = FieldState(value = "123 Test St"),
            profileImageUri = profileUri,
            coverImageUri = null)

    val data = OrganizationEditorData.fromForm("org123", formState)

    assertEquals(profileUri, data.profileImageUri)
    assertNull(data.coverImageUri)
  }

  @Test
  fun fromFormHandlesOnlyCoverImage() = runTest {
    val coverUri = android.net.Uri.parse("content://media/image/cover456")

    val formState =
        OrganizationFormState(
            name = FieldState(value = "Test Org"),
            description = FieldState(value = "Test Description"),
            contactEmail = FieldState(value = "test@example.com"),
            contactPhone = FieldState(value = "1234567890"),
            contactPhonePrefix = androidx.compose.runtime.mutableStateOf("+1"),
            website = FieldState(value = "https://example.com"),
            instagram = FieldState(value = "test_instagram"),
            facebook = FieldState(value = "test_facebook"),
            tiktok = FieldState(value = "test_tiktok"),
            address = FieldState(value = "123 Test St"),
            profileImageUri = null,
            coverImageUri = coverUri)

    val data = OrganizationEditorData.fromForm("org123", formState)

    assertNull(data.profileImageUri)
    assertEquals(coverUri, data.coverImageUri)
  }

  @Test
  fun fromFormHandlesMinimalData() = runTest {
    val formState =
        OrganizationFormState(
            name = FieldState(value = "Minimal Org"),
            description = FieldState(value = "Minimal Description"),
            contactEmail = FieldState(value = ""),
            contactPhone = FieldState(value = ""),
            contactPhonePrefix = androidx.compose.runtime.mutableStateOf(""),
            website = FieldState(value = ""),
            instagram = FieldState(value = ""),
            facebook = FieldState(value = ""),
            tiktok = FieldState(value = ""),
            address = FieldState(value = ""),
            profileImageUri = null,
            coverImageUri = null)

    val data = OrganizationEditorData.fromForm("org456", formState)

    assertEquals("org456", data.id)
    assertEquals("Minimal Org", data.name)
    assertEquals("Minimal Description", data.description)
    assertNull(data.contactEmail)
    assertNull(data.contactPhone)
    assertNull(data.phonePrefix)
    assertNull(data.website)
    assertNull(data.instagram)
    assertNull(data.facebook)
    assertNull(data.tiktok)
    assertNull(data.address)
    assertNull(data.profileImageUri)
    assertNull(data.coverImageUri)
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

  override suspend fun updateProfileImage(organizationId: String, imageUrl: String?) =
      Result.success(Unit)

  override suspend fun updateCoverImage(organizationId: String, imageUrl: String?) =
      Result.success(Unit)
}
