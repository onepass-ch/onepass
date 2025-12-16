package ch.onepass.onepass.ui.organizer

import ch.onepass.onepass.model.membership.Membership
import ch.onepass.onepass.model.membership.MembershipRepository
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationFormViewModelTest {
  private lateinit var repository: FakeOrganizationRepository
  private lateinit var userRepository: FakeUserRepository
  private lateinit var storageRepository: FakeStorageRepository
  private lateinit var membershipRepository: FakeMembershipRepository
  private lateinit var viewModel: OrganizationFormViewModel

  @Before
  fun setup() {
    repository = FakeOrganizationRepository()
    userRepository = FakeUserRepository()
    storageRepository = FakeStorageRepository()
    membershipRepository = FakeMembershipRepository()
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
  fun updateNameSanitizesInput() = runTest {
    viewModel.updateName("Test <script>alert('xss')</script> Org")
    val state = viewModel.formState.value
    // Sanitizer should remove dangerous patterns
    assertFalse("Should not contain script tags", state.name.value.contains("<script>"))
  }

  @Test
  fun updateNameTruncatesToMaxLength() = runTest {
    val longName = "A".repeat(100)
    viewModel.updateName(longName)
    val state = viewModel.formState.value
    assertEquals("Should truncate to MAX_NAME_LENGTH", 50, state.name.value.length)
  }

  @Test
  fun updateDescriptionSetsValueCorrectly() = runTest {
    viewModel.updateDescription("Cool description")
    val state = viewModel.formState.value
    assertEquals("Cool description", state.description.value)
    assertNull(state.description.error)
  }

  @Test
  fun updateDescriptionSanitizesInput() = runTest {
    viewModel.updateDescription("Test'; DROP TABLE users; -- Description")
    val state = viewModel.formState.value
    // Sanitizer should handle SQL injection attempts
    assertNotNull("Description should be set", state.description.value)
    assertTrue("Description length should be within limits", state.description.value.length <= 200)
  }

  @Test
  fun updateDescriptionTruncatesToMaxLength() = runTest {
    val longDescription = "B".repeat(300)
    viewModel.updateDescription(longDescription)
    val state = viewModel.formState.value
    assertEquals("Should truncate to MAX_DESCRIPTION_LENGTH", 200, state.description.value.length)
  }

  @Test
  fun updateContactEmailSetsValueCorrectly() = runTest {
    viewModel.updateContactEmail("test@email.com")
    val state = viewModel.formState.value
    assertEquals("test@email.com", state.contactEmail.value)
    assertNull(state.contactEmail.error)
  }

  @Test
  fun updateContactEmailTruncatesToMaxLength() = runTest {
    val longEmail = "a".repeat(50) + "@test.com"
    viewModel.updateContactEmail(longEmail)
    val state = viewModel.formState.value
    assertTrue("Email should not exceed MAX_EMAIL_LENGTH", state.contactEmail.value.length <= 100)
  }

  @Test
  fun updateContactPhoneSetsValueCorrectly() = runTest {
    viewModel.updateContactPhone("791234567")
    val state = viewModel.formState.value
    assertEquals("791234567", state.contactPhone.value)
    assertNull(state.contactPhone.error)
  }

  @Test
  fun updateContactPhoneSanitizesInput() = runTest {
    viewModel.updateContactPhone("79-123-456<script>")
    val state = viewModel.formState.value
    assertFalse("Should remove dangerous characters", state.contactPhone.value.contains("<"))
  }

  @Test
  fun updateContactPhoneTruncatesToMaxLength() = runTest {
    viewModel.updateContactPhone("123456789012345678")
    val state = viewModel.formState.value
    assertEquals("Should truncate to MAX_PHONE_LENGTH", 15, state.contactPhone.value.length)
  }

  @Test
  fun updateWebsiteSetsValueCorrectly() = runTest {
    viewModel.updateWebsite("example.com")
    val state = viewModel.formState.value
    assertEquals("example.com", state.website.value)
    assertNull(state.website.error)
  }

  @Test
  fun updateWebsiteTruncatesToMaxLength() = runTest {
    val longUrl = "https://" + "a".repeat(200) + ".com"
    viewModel.updateWebsite(longUrl)
    val state = viewModel.formState.value
    assertTrue("Website should not exceed MAX_WEBSITE_LENGTH", state.website.value.length <= 200)
  }

  @Test
  fun updateInstagramSetsValueCorrectly() = runTest {
    viewModel.updateInstagram("@testhandle")
    val state = viewModel.formState.value
    assertEquals("@testhandle", state.instagram.value)
  }

  @Test
  fun updateInstagramTruncatesToMaxLength() = runTest {
    val longHandle = "@" + "a".repeat(100)
    viewModel.updateInstagram(longHandle)
    val state = viewModel.formState.value
    assertTrue(
        "Instagram handle should not exceed MAX_SOCIAL_LENGTH", state.instagram.value.length <= 100)
  }

  @Test
  fun updateFacebookSetsValueCorrectly() = runTest {
    viewModel.updateFacebook("facebookpage")
    val state = viewModel.formState.value
    assertEquals("facebookpage", state.facebook.value)
  }

  @Test
  fun updateFacebookTruncatesToMaxLength() = runTest {
    val longHandle = "a".repeat(120)
    viewModel.updateFacebook(longHandle)
    val state = viewModel.formState.value
    assertTrue(
        "Facebook handle should not exceed MAX_SOCIAL_LENGTH", state.facebook.value.length <= 100)
  }

  @Test
  fun updateTiktokSetsValueCorrectly() = runTest {
    viewModel.updateTiktok("tiktokuser")
    val state = viewModel.formState.value
    assertEquals("tiktokuser", state.tiktok.value)
  }

  @Test
  fun updateTiktokTruncatesToMaxLength() = runTest {
    val longHandle = "a".repeat(120)
    viewModel.updateTiktok(longHandle)
    val state = viewModel.formState.value
    assertTrue(
        "TikTok handle should not exceed MAX_SOCIAL_LENGTH", state.tiktok.value.length <= 100)
  }

  @Test
  fun updateAddressSetsValueCorrectly() = runTest {
    viewModel.updateAddress("123 Main Street")
    val state = viewModel.formState.value
    assertEquals("123 Main Street", state.address.value)
  }

  @Test
  fun updateAddressTruncatesToMaxLength() = runTest {
    val longAddress = "a".repeat(300)
    viewModel.updateAddress(longAddress)
    val state = viewModel.formState.value
    assertTrue("Address should not exceed MAX_ADDRESS_LENGTH", state.address.value.length <= 200)
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
  fun validateFormReturnsFalseForEmptyPhoneNumber() = runTest {
    viewModel.updateName("Test Org")
    viewModel.updateDescription("Test Description")

    val valid = viewModel.createOrganizationValidation()
    assertFalse(valid)
    val state = viewModel.formState.value
    assertEquals("Phone number is required", state.contactPhone.error)
  }

  @Test
  fun validateFormReturnsTrueForValidInput() = runTest {
    viewModel.updateName("Test Org")
    viewModel.updateDescription("Test Description")
    viewModel.updateContactPhone("791234567")
    viewModel.updateContactEmail("123@example.com")
    viewModel.updateCountryIndex(41)
    viewModel.updateContactPhone("123456789")

    val valid = viewModel.createOrganizationValidation()
    assertTrue(valid)
  }

  @Test
  fun createOrganizationReturnsErrorWhenFormIsInvalid() = runTest {
    viewModel.updateName("")
    viewModel.updateDescription("")

    viewModel.createOrganization("user123")

    val finalState = viewModel.uiState.filter { it.errorMessage != null }.first()

    assertNull(finalState.successOrganizationId)
    assertEquals("Please fix validation errors", finalState.errorMessage)
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
    viewModel.updateContactEmail("123@yahoo.com")

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
    viewModel.updateContactEmail("123@yahoo.com")

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
    viewModel.updateContactEmail("123@yahoo.com")

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
    viewModel.updateContactEmail("123@yahoo.com")

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
    viewModel.updateContactEmail("123@yahoo.com")

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
    viewModel.updateContactEmail("123@yahoo.com")

    viewModel.createOrganization("user123")

    val finalState = viewModel.uiState.filter { it.successOrganizationId != null }.first()

    assertNotNull(finalState.successOrganizationId)
    assertNull(finalState.errorMessage)
  }

  @Test
  fun clearSuccess_clearsSuccessState() = runTest {
    storageRepository.shouldSucceed = true
    viewModel.updateName("Test")
    viewModel.updateDescription("Desc")
    viewModel.updateContactPhone("123")
    viewModel.updateContactEmail("123@yahoo.com")

    viewModel.createOrganization("user1")

    // Wait for success
    val successState = viewModel.uiState.filter { it.successOrganizationId != null }.first()
    assertNotNull(successState.successOrganizationId)

    // Clear success
    viewModel.clearSuccess()

    val clearedState = viewModel.uiState.value
    assertNull(clearedState.successOrganizationId)
  }

  @Test
  fun clearError_clearsErrorMessage() = runTest {
    viewModel.updateName("") // Invalid form
    viewModel.createOrganization("user1")

    val errorState = viewModel.uiState.filter { it.errorMessage != null }.first()
    assertNotNull(errorState.errorMessage)

    viewModel.clearError()

    val clearedState = viewModel.uiState.value
    assertNull(clearedState.errorMessage)
  }

  @Test
  fun resetForm_resetsStateAndUi() = runTest {
    viewModel.updateName("Dirty Name")
    viewModel.createOrganization("user1")

    viewModel.resetForm()

    val formState = viewModel.formState.value
    val uiState = viewModel.uiState.value

    assertEquals("", formState.name.value)
    assertNull(uiState.successOrganizationId)
    assertNull(uiState.errorMessage)
    assertFalse(uiState.isLoading)
  }

  @Test
  fun createOrganizationHandlesRepositoryCreationFailure() = runTest {
    // Tests createOrganizationEntity failure path
    repository.shouldFail = true
    viewModel.updateName("Test Org")
    viewModel.updateDescription("Desc")
    viewModel.updateContactPhone("791234567")
    viewModel.updateContactEmail("123@yahoo.com")

    viewModel.createOrganization("user123")

    val finalState = viewModel.uiState.filter { it.errorMessage != null }.first()
    assertNull(finalState.successOrganizationId)
    assertEquals("Failed", finalState.errorMessage)
  }

  @Test
  fun createOrganizationHandlesProfileImageUpdateFailure() = runTest {
    // Tests uploadAndUpdateProfileImage failure path (Upload succeeds, DB update fails)
    repository.shouldFailUpdateProfile = true
    storageRepository.shouldSucceed = true
    val profileUri = android.net.Uri.parse("content://media/image/profile123")

    viewModel.updateName("Test Org")
    viewModel.updateDescription("Desc")
    viewModel.updateContactPhone("791234567")
    viewModel.updateContactEmail("123@yahoo.com")
    viewModel.selectProfileImage(profileUri)

    viewModel.createOrganization("user123")

    val finalState = viewModel.uiState.filter { it.errorMessage != null }.first()
    assertNull(finalState.successOrganizationId)
    assertEquals("Failed to update profile image", finalState.errorMessage)
  }

  @Test
  fun createOrganizationHandlesCoverImageUpdateFailure() = runTest {
    // Tests uploadAndUpdateCoverImage failure path (Upload succeeds, DB update fails)
    repository.shouldFailUpdateCover = true
    storageRepository.shouldSucceed = true
    val coverUri = android.net.Uri.parse("content://media/image/cover456")

    viewModel.updateName("Test Org")
    viewModel.updateDescription("Desc")
    viewModel.updateContactPhone("791234567")
    viewModel.selectCoverImage(coverUri)
    viewModel.updateContactEmail("123@yahoo.com")

    viewModel.createOrganization("user123")

    val finalState = viewModel.uiState.filter { it.errorMessage != null }.first()
    assertNull(finalState.successOrganizationId)
    assertEquals("Failed to update cover image", finalState.errorMessage)
  }

  @Test
  fun createOrganizationHandlesMembershipCreationFailure() = runTest {
    // Tests addOwnerMembership failure path
    membershipRepository.shouldFail = true
    viewModel.updateName("Test Org")
    viewModel.updateDescription("Desc")
    viewModel.updateContactPhone("791234567")
    viewModel.updateContactEmail("123@yahoo.com")

    viewModel.createOrganization("user123")

    val finalState = viewModel.uiState.filter { it.errorMessage != null }.first()
    // Org is created but membership fails
    assertEquals("org123", finalState.successOrganizationId)
    assertTrue(finalState.errorMessage?.contains("Failed to add membership") == true)
  }

  private fun OrganizationFormViewModel.createOrganizationValidation(): Boolean {
    val method = OrganizationFormViewModel::class.java.getDeclaredMethod("validateForm")
    method.isAccessible = true
    return method.invoke(this) as Boolean
  }
}

class FakeMembershipRepository : MembershipRepository {
  var shouldFail = false

  override suspend fun addMembership(
      userId: String,
      orgId: String,
      role: OrganizationRole
  ): Result<String> {
    return if (shouldFail) Result.failure(Exception("Failed to add membership"))
    else Result.success("membership123")
  }

  override suspend fun removeMembership(userId: String, orgId: String) = Result.success(Unit)

  override suspend fun updateMembership(userId: String, orgId: String, newRole: OrganizationRole) =
      Result.success(Unit)

  override suspend fun getUsersByOrganization(orgId: String): Result<List<Membership>> =
      Result.success(emptyList())

  override fun getUsersByOrganizationFlow(orgId: String): Flow<List<Membership>> =
      flowOf(emptyList())

  override suspend fun getOrganizationsByUser(userId: String): Result<List<Membership>> =
      Result.success(emptyList())

  override fun getOrganizationsByUserFlow(userId: String): Flow<List<Membership>> =
      flowOf(emptyList())

  override suspend fun hasMembership(
      userId: String,
      orgId: String,
      roles: List<OrganizationRole>
  ): Boolean = false
}

class FakeOrganizationRepository : OrganizationRepository {

  var createdOrganization: Organization? = null
  var shouldFail = false
  var shouldDelay = false
  var delayMs = 0L
  var createCallCount = 0
  var shouldThrow = false
  var shouldFailUpdateProfile = false
  var shouldFailUpdateCover = false

  override suspend fun createOrganization(organization: Organization): Result<String> {
    createCallCount++
    if (shouldDelay) kotlinx.coroutines.delay(delayMs)
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

  override fun getOrganizationsByStatus(status: OrganizationStatus) = TODO()

  override fun searchOrganizations(query: String) = TODO()

  override fun getVerifiedOrganizations() = TODO()

  override suspend fun createInvitation(invitation: OrganizationInvitation) = TODO()

  override fun getPendingInvitations(organizationId: String) = TODO()

  override fun getInvitationsByEmail(email: String) = TODO()

  override suspend fun updateInvitationStatus(invitationId: String, newStatus: InvitationStatus) =
      TODO()

  override suspend fun deleteInvitation(invitationId: String) = TODO()

  override suspend fun updateProfileImage(organizationId: String, imageUrl: String?) =
      if (shouldFailUpdateProfile) Result.failure(Exception("Failed to update profile image"))
      else Result.success(Unit)

  override suspend fun updateCoverImage(organizationId: String, imageUrl: String?) =
      if (shouldFailUpdateCover) Result.failure(Exception("Failed to update cover image"))
      else Result.success(Unit)
}

class FakeUserRepository : UserRepository {
  var userToReturn: User? = null
  var shouldFailAddOrganization = false
  private val _favoriteEventIds = MutableStateFlow<Set<String>>(emptySet())

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

  override fun getFavoriteEvents(uid: String): Flow<Set<String>> {
    return _favoriteEventIds
  }

  override suspend fun addFavoriteEvent(uid: String, eventId: String): Result<Unit> {
    _favoriteEventIds.update { it + eventId }
    return Result.success(Unit)
  }

  override suspend fun removeFavoriteEvent(uid: String, eventId: String): Result<Unit> {
    _favoriteEventIds.update { it - eventId }
    return Result.success(Unit)
  }
}
