package ch.onepass.onepass.ui.organizer

import ch.onepass.onepass.model.organization.InvitationStatus
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationInvitation
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRole
import ch.onepass.onepass.model.organization.OrganizationStatus
import ch.onepass.onepass.model.staff.StaffSearchResult
import ch.onepass.onepass.model.storage.FakeStorageRepository
import ch.onepass.onepass.model.user.User
import ch.onepass.onepass.model.user.UserRepository
import ch.onepass.onepass.model.user.UserSearchType
import ch.onepass.onepass.utils.TestMockMembershipRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationFormViewModelTest {

  private lateinit var repository: FakeOrganizationRepository
  private lateinit var userRepository: FakeUserRepository
  private lateinit var storageRepository: FakeStorageRepository
  private lateinit var membershipRepository: TestMockMembershipRepository
  private lateinit var viewModel: OrganizationFormViewModel

  @Before
  fun setup() {
    repository = FakeOrganizationRepository()
    userRepository = FakeUserRepository()
    storageRepository = FakeStorageRepository()
    membershipRepository = TestMockMembershipRepository()
    viewModel =
        OrganizationFormViewModel(
            repository = repository,
            userRepository = userRepository,
            storageRepository = storageRepository,
            membershipRepository = membershipRepository)
  }

  @Test
  fun updateNameSetsValueCorrectly() = runTest {
    viewModel.updateName("Test Org")
    val state = viewModel.formState.value
    assertEquals("Test Org", state.name.value)
    assertNull(state.name.error)
  }

  @Test
  fun updateDescriptionSetsValueCorrectly() = runTest {
    viewModel.updateDescription("Cool description")
    val state = viewModel.formState.value
    assertEquals("Cool description", state.description.value)
    assertNull(state.description.error)
  }

  @Test
  fun updateContactEmailSetsValueCorrectly() = runTest {
    viewModel.updateContactEmail("test@email.com")
    val state = viewModel.formState.value
    assertEquals("test@email.com", state.contactEmail.value)
    assertNull(state.contactEmail.error)
  }

  @Test
  fun updateContactPhoneSetsValueCorrectly() = runTest {
    viewModel.updateContactPhone("791234567")
    val state = viewModel.formState.value
    assertEquals("791234567", state.contactPhone.value)
    assertNull(state.contactPhone.error)
  }

  @Test
  fun updateWebsiteSetsValueCorrectly() = runTest {
    viewModel.updateWebsite("example.com")
    val state = viewModel.formState.value
    assertEquals("example.com", state.website.value)
    assertNull(state.website.error)
  }

  @Test
  fun validateFormReturnsFalseForEmptyRequiredFields() = runTest {
    val valid = viewModel.createOrganizationValidation()
    assertFalse(valid)
    val state = viewModel.formState.value
    assertEquals("Name is required", state.name.error)
    assertEquals("Description is required", state.description.error)
  }

  @Test
  fun createOrganizationReturnsErrorWhenFormIsInvalid() = runTest {
    viewModel.updateName("")
    viewModel.updateDescription("")

    viewModel.createOrganization("user123")

    val finalState = viewModel.uiState.filter { it.errorMessage != null }.first()

    assertNull(finalState.successOrganizationId)
    assertEquals("Please fix errors", finalState.errorMessage)
  }

  // ===== NEW TESTS FOR IMAGE FUNCTIONALITY =====

  @Test
  fun selectProfileImageUpdatesFormState() = runTest {
    val mockUri = android.net.Uri.parse("content://media/image/123")

    viewModel.selectProfileImage(mockUri)

    val state = viewModel.formState.value
    assertEquals("Profile image URI should be set", mockUri, state.profileImageUri)
  }

  @Test
  fun selectCoverImageUpdatesFormState() = runTest {
    val mockUri = android.net.Uri.parse("content://media/image/456")

    viewModel.selectCoverImage(mockUri)

    val state = viewModel.formState.value
    assertEquals("Cover image URI should be set", mockUri, state.coverImageUri)
  }

  @Test
  fun canSelectBothProfileAndCoverImages() = runTest {
    val profileUri = android.net.Uri.parse("content://media/image/123")
    val coverUri = android.net.Uri.parse("content://media/image/456")

    viewModel.selectProfileImage(profileUri)
    viewModel.selectCoverImage(coverUri)

    val state = viewModel.formState.value
    assertEquals("Profile image URI should be set", profileUri, state.profileImageUri)
    assertEquals("Cover image URI should be set", coverUri, state.coverImageUri)
  }

  @Test
  fun selectingNewProfileImageReplacesOld() = runTest {
    val firstUri = android.net.Uri.parse("content://media/image/123")
    val secondUri = android.net.Uri.parse("content://media/image/789")

    viewModel.selectProfileImage(firstUri)
    viewModel.selectProfileImage(secondUri)

    val state = viewModel.formState.value
    assertEquals("Should have the second image", secondUri, state.profileImageUri)
  }

  @Test
  fun selectingNewCoverImageReplacesOld() = runTest {
    val firstUri = android.net.Uri.parse("content://media/image/456")
    val secondUri = android.net.Uri.parse("content://media/image/999")

    viewModel.selectCoverImage(firstUri)
    viewModel.selectCoverImage(secondUri)

    val state = viewModel.formState.value
    assertEquals("Should have the second image", secondUri, state.coverImageUri)
  }

  @Test
  fun createOrganizationWithProfileImageUploadsSuccessfully() = runTest {
    val profileUri = android.net.Uri.parse("content://media/image/profile123")
    storageRepository.shouldSucceed = true

    viewModel.updateName("Test Org")
    viewModel.updateDescription("Test Description")
    viewModel.selectProfileImage(profileUri)
    viewModel.updateContactPhone("791234567")

    viewModel.createOrganization("user123")

    val finalState = viewModel.uiState.filter { it.successOrganizationId != null }.first()

    assertNotNull(finalState.successOrganizationId)
    assertNull(finalState.errorMessage)
  }

  @Test
  fun createOrganizationWithCoverImageUploadsSuccessfully() = runTest {
    val coverUri = android.net.Uri.parse("content://media/image/cover456")
    storageRepository.shouldSucceed = true

    viewModel.updateName("Test Org")
    viewModel.updateDescription("Test Description")
    viewModel.selectCoverImage(coverUri)
    viewModel.updateContactPhone("791234567")

    viewModel.createOrganization("user123")

    val finalState = viewModel.uiState.filter { it.successOrganizationId != null }.first()

    assertNotNull(finalState.successOrganizationId)
    assertNull(finalState.errorMessage)
  }

  @Test
  fun createOrganizationWithBothImagesUploadsSuccessfully() = runTest {
    val profileUri = android.net.Uri.parse("content://media/image/profile123")
    val coverUri = android.net.Uri.parse("content://media/image/cover456")
    storageRepository.shouldSucceed = true

    viewModel.updateName("Test Org")
    viewModel.updateDescription("Test Description")
    viewModel.selectProfileImage(profileUri)
    viewModel.selectCoverImage(coverUri)
    viewModel.updateContactPhone("791234567")

    viewModel.createOrganization("user123")

    val finalState = viewModel.uiState.filter { it.successOrganizationId != null }.first()

    assertNotNull(finalState.successOrganizationId)
    assertNull(finalState.errorMessage)
  }

  @Test
  fun createOrganizationWithProfileImageFailsWhenUploadFails() = runTest {
    val profileUri = android.net.Uri.parse("content://media/image/profile123")
    storageRepository.shouldSucceed = false
    storageRepository.failureMessage = "Failed to upload profile image"

    viewModel.updateName("Test Org")
    viewModel.updateDescription("Test Description")
    viewModel.selectProfileImage(profileUri)
    viewModel.updateContactPhone("791234567")

    viewModel.createOrganization("user123")

    val finalState = viewModel.uiState.filter { it.errorMessage != null }.first()

    assertNull(finalState.successOrganizationId)
    assertTrue(finalState.errorMessage?.contains("Failed to upload profile image") ?: false)
  }

  @Test
  fun createOrganizationWithCoverImageFailsWhenUploadFails() = runTest {
    val coverUri = android.net.Uri.parse("content://media/image/cover456")
    storageRepository.shouldSucceed = false
    storageRepository.failureMessage = "Failed to upload cover image"

    viewModel.updateName("Test Org")
    viewModel.updateDescription("Test Description")
    viewModel.selectCoverImage(coverUri)
    viewModel.updateContactPhone("791234567")

    viewModel.createOrganization("user123")

    val finalState = viewModel.uiState.filter { it.errorMessage != null }.first()

    assertNull(finalState.successOrganizationId)
    assertTrue(finalState.errorMessage?.contains("Failed to upload cover image") ?: false)
  }

  @Test
  fun createOrganizationWithoutImagesSucceeds() = runTest {
    storageRepository.shouldSucceed = true

    viewModel.updateName("Test Org")
    viewModel.updateDescription("Test Description")
    viewModel.updateContactPhone("791234567")

    viewModel.createOrganization("user123")

    val finalState = viewModel.uiState.filter { it.successOrganizationId != null }.first()

    assertNotNull(finalState.successOrganizationId)
    assertNull(finalState.errorMessage)
  }

  private fun OrganizationFormViewModel.createOrganizationValidation(): Boolean {
    val method = OrganizationFormViewModel::class.java.getDeclaredMethod("validateForm")
    method.isAccessible = true
    return method.invoke(this) as Boolean
  }
}

class FakeOrganizationRepository : OrganizationRepository {

  var createdOrganization: Organization? = null
  var shouldFail = false

  override suspend fun createOrganization(organization: Organization): Result<String> {
    return if (shouldFail) Result.failure(Exception("Failed"))
    else {
      this.createdOrganization = organization
      Result.success("org123")
    }
  }

  override suspend fun updateOrganization(organization: Organization) = TODO()

  override suspend fun deleteOrganization(organizationId: String) = TODO()

  override fun getOrganizationById(organizationId: String) = TODO()

  override fun getOrganizationsByOwner(ownerId: String) = TODO()

  override fun getOrganizationsByMember(userId: String) = TODO()

  override fun getOrganizationsByStatus(status: OrganizationStatus) = TODO()

  override fun searchOrganizations(query: String) = TODO()

  override fun getVerifiedOrganizations() = TODO()

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

  override suspend fun addMember(
      organizationId: String,
      userId: String,
      role: OrganizationRole
  ): Result<Unit> = Result.success(Unit)
}

class FakeUserRepository : UserRepository {
  var userToReturn: User? = null
  var shouldFailAddOrganization = false

  override suspend fun getCurrentUser(): User? = userToReturn

  override suspend fun getOrCreateUser(): User? = userToReturn

  override suspend fun updateLastLogin(uid: String) {}

  override suspend fun getUserById(uid: String): Result<StaffSearchResult?> {
    return Result.success(null)
  }

  override suspend fun searchUsers(
      query: String,
      searchType: UserSearchType,
      organizationId: String?
  ): Result<List<StaffSearchResult>> = Result.success(emptyList())

  suspend fun isOrganizer(): Boolean = false

  suspend fun addOrganizationToUser(userId: String, orgId: String) {
    if (shouldFailAddOrganization) {
      throw Exception("Failed to add organization")
    }
  }

  suspend fun removeOrganizationFromUser(userId: String, orgId: String) {}
}
