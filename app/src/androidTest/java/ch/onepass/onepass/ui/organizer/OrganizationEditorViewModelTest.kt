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
  private lateinit var formViewModel: OrganizationFormViewModel
  private lateinit var editorViewModel: OrganizationEditorViewModel

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    repository = FakeEditOrganizationRepository()
    storageRepository = FakeStorageRepository()
    formViewModel = OrganizationFormViewModel()
    editorViewModel = OrganizationEditorViewModel(repository, storageRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun loadOrganizationById_found() = runTest {
    val org = testOrganization("org1")
    repository.organizationToReturn = org

    editorViewModel.loadOrganizationById("org1")
    advanceUntilIdle()

    val state = editorViewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals(org, state.organization)
    assertNull(state.errorMessage)
  }

  @Test
  fun loadOrganizationById_notFound() = runTest {
    repository.organizationToReturn = null

    editorViewModel.loadOrganizationById("org2")
    advanceUntilIdle()

    val state = editorViewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.organization)
    assertEquals("Organization not found", state.errorMessage)
  }

  @Test
  fun updateNameSanitizesInput() = runTest {
    val org = testOrganization("org1")
    repository.organizationToReturn = org

    editorViewModel.loadOrganizationById("org1")
    advanceUntilIdle()

    formViewModel.updateName("Test <script>alert('xss')</script> Org")
    val state = formViewModel.formState.value
    assertFalse("Should not contain script tags", state.name.value.contains("<script>"))
  }

  @Test
  fun updateNameTruncatesToMaxLength() = runTest {
    val org = testOrganization("org1")
    repository.organizationToReturn = org

    editorViewModel.loadOrganizationById("org1")
    advanceUntilIdle()

    val longName = "A".repeat(100)
    formViewModel.updateName(longName)
    val state = formViewModel.formState.value
    assertEquals("Should truncate to MAX_NAME_LENGTH", 50, state.name.value.length)
  }

  @Test
  fun updateDescriptionSanitizesInput() = runTest {
    val org = testOrganization("org1")
    repository.organizationToReturn = org

    editorViewModel.loadOrganizationById("org1")
    advanceUntilIdle()

    formViewModel.updateDescription("Test'; DROP TABLE users; -- Description")
    val state = formViewModel.formState.value
    assertNotNull("Description should be set", state.description.value)
    assertTrue("Description length should be within limits", state.description.value.length <= 200)
  }

  @Test
  fun updateContactEmailTruncatesToMaxLength() = runTest {
    val org = testOrganization("org1")
    repository.organizationToReturn = org

    editorViewModel.loadOrganizationById("org1")
    advanceUntilIdle()

    val longEmail = "a".repeat(50) + "@test.com"
    formViewModel.updateContactEmail(longEmail)
    val state = formViewModel.formState.value
    assertTrue("Email should not exceed MAX_EMAIL_LENGTH", state.contactEmail.value.length <= 100)
  }

  @Test
  fun updateContactPhoneSanitizesInput() = runTest {
    val org = testOrganization("org1")
    repository.organizationToReturn = org

    editorViewModel.loadOrganizationById("org1")
    advanceUntilIdle()

    formViewModel.updateContactPhone("79-123-456<script>")
    val state = formViewModel.formState.value
    assertFalse("Should remove dangerous characters", state.contactPhone.value.contains("<"))
  }

  @Test
  fun updateWebsiteTruncatesToMaxLength() = runTest {
    val org = testOrganization("org1")
    repository.organizationToReturn = org

    editorViewModel.loadOrganizationById("org1")
    advanceUntilIdle()

    val longUrl = "https://" + "a".repeat(200) + ".com"
    formViewModel.updateWebsite(longUrl)
    val state = formViewModel.formState.value
    assertTrue("Website should not exceed MAX_WEBSITE_LENGTH", state.website.value.length <= 200)
  }

  @Test
  fun updateInstagramTruncatesToMaxLength() = runTest {
    val org = testOrganization("org1")
    repository.organizationToReturn = org

    editorViewModel.loadOrganizationById("org1")
    advanceUntilIdle()

    val longHandle = "@" + "a".repeat(100)
    formViewModel.updateInstagram(longHandle)
    val state = formViewModel.formState.value
    assertTrue(
        "Instagram handle should not exceed MAX_SOCIAL_LENGTH", state.instagram.value.length <= 100)
  }

  @Test
  fun updateFacebookTruncatesToMaxLength() = runTest {
    val org = testOrganization("org1")
    repository.organizationToReturn = org

    editorViewModel.loadOrganizationById("org1")
    advanceUntilIdle()

    val longHandle = "a".repeat(120)
    formViewModel.updateFacebook(longHandle)
    val state = formViewModel.formState.value
    assertTrue(
        "Facebook handle should not exceed MAX_SOCIAL_LENGTH", state.facebook.value.length <= 100)
  }

  @Test
  fun updateTiktokTruncatesToMaxLength() = runTest {
    val org = testOrganization("org1")
    repository.organizationToReturn = org

    editorViewModel.loadOrganizationById("org1")
    advanceUntilIdle()

    val longHandle = "a".repeat(120)
    formViewModel.updateTiktok(longHandle)
    val state = formViewModel.formState.value
    assertTrue(
        "TikTok handle should not exceed MAX_SOCIAL_LENGTH", state.tiktok.value.length <= 100)
  }

  @Test
  fun updateAddressTruncatesToMaxLength() = runTest {
    val org = testOrganization("org1")
    repository.organizationToReturn = org

    editorViewModel.loadOrganizationById("org1")
    advanceUntilIdle()

    val longAddress = "a".repeat(300)
    formViewModel.updateAddress(longAddress)
    val state = formViewModel.formState.value
    assertTrue("Address should not exceed MAX_ADDRESS_LENGTH", state.address.value.length <= 200)
  }

  @Test
  fun updateOrganization_success() = runTest {
    val org = testOrganization("org1", "Old Name", "Old Description")
    repository.organizationToReturn = org

    editorViewModel.loadOrganizationById("org1")
    advanceUntilIdle()

      formViewModel.initializeFrom(org)

      formViewModel.updateName("New Name")
      formViewModel.updateDescription("New Description")
      formViewModel.updateContactEmail("email@test.com")
      formViewModel.updateContactPhone("987654")
      formViewModel.formState.value.contactPhonePrefix.value = "1"
      formViewModel.updateWebsite("newsite.com")
      formViewModel.updateInstagram("insta")
      formViewModel.updateFacebook("fb")
      formViewModel.updateTiktok("tt")
      formViewModel.updateAddress("address")

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

    editorViewModel.updateOrganization(data, formViewModel)
    advanceUntilIdle()

    val state = editorViewModel.uiState.value
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

    editorViewModel.loadOrganizationById("org1")
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

    editorViewModel.updateOrganization(data, formViewModel)
    advanceUntilIdle()

    val state = editorViewModel.uiState.value
    assertFalse(state.isLoading)
    assertFalse(state.success)
    assertEquals("Please fix all errors before submitting", state.errorMessage)
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

    editorViewModel.updateOrganization(data, formViewModel)
    advanceUntilIdle()

    val state = editorViewModel.uiState.value
    assertEquals("Cannot update: organization not loaded", state.errorMessage)
    assertFalse(state.success)
  }

  @Test
  fun updateOrganizationWithProfileImage() = runTest {
    val org = testOrganization("org1")
    repository.organizationToReturn = org

    editorViewModel.loadOrganizationById("org1")
    advanceUntilIdle()

      formViewModel.initializeFrom(org)

      val profileUri = android.net.Uri.parse("content://media/image/profile123")

      formViewModel.updateName("Updated Name")
      formViewModel.updateDescription("Updated Description")
      formViewModel.updateContactEmail("email@test.com")
      formViewModel.updateContactPhone("987654")
      formViewModel.formState.value.contactPhonePrefix.value = "1"
      formViewModel.updateWebsite("website.com")
      formViewModel.updateInstagram("insta")
      formViewModel.updateFacebook("fb")
      formViewModel.updateTiktok("tt")
      formViewModel.updateAddress("address")
      formViewModel.selectProfileImage(profileUri)

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

    editorViewModel.updateOrganization(data, formViewModel)
    advanceUntilIdle()

    val state = editorViewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.success)
  }

  @Test
  fun updateOrganizationWithCoverImage() = runTest {
    val org = testOrganization("org1")
    repository.organizationToReturn = org

    editorViewModel.loadOrganizationById("org1")
    advanceUntilIdle()

      formViewModel.initializeFrom(org)

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

    editorViewModel.updateOrganization(data, formViewModel)
    advanceUntilIdle()

    val state = editorViewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.success)
  }

  @Test
  fun updateOrganizationWithBothImages() = runTest {
    val org = testOrganization("org1")
    repository.organizationToReturn = org

    editorViewModel.loadOrganizationById("org1")
    advanceUntilIdle()

      formViewModel.initializeFrom(org)

    val profileUri = android.net.Uri.parse("content://media/image/profile123")
    val coverUri = android.net.Uri.parse("content://media/image/cover456")

      formViewModel.updateName("Updated Name")
      formViewModel.updateDescription("Updated Description")
      formViewModel.updateContactEmail("email@test.com")
      formViewModel.updateContactPhone("987654")
      formViewModel.formState.value.contactPhonePrefix.value = "1"
      formViewModel.updateWebsite("website.com")
      formViewModel.updateInstagram("insta")
      formViewModel.updateFacebook("fb")
      formViewModel.updateTiktok("tt")
      formViewModel.updateAddress("address")
      formViewModel.selectProfileImage(profileUri)
      formViewModel.selectCoverImage(coverUri)

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

    editorViewModel.updateOrganization(data, formViewModel)
    advanceUntilIdle()

    val state = editorViewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.success)
  }

  @Test
  fun updateOrganizationWithoutNewImages() = runTest {
    val org = testOrganization("org1", "Old Name", "Old Description")
    repository.organizationToReturn = org

    editorViewModel.loadOrganizationById("org1")
    advanceUntilIdle()

      formViewModel.initializeFrom(org)

      formViewModel.updateName("New Name")
      formViewModel.updateDescription("New Description")
      formViewModel.updateContactEmail("email@test.com")
      formViewModel.updateContactPhone("987654")
      formViewModel.formState.value.contactPhonePrefix.value = "1"
      formViewModel.updateWebsite("newsite.com")
      formViewModel.updateInstagram("insta")
      formViewModel.updateFacebook("fb")
      formViewModel.updateTiktok("tt")
      formViewModel.updateAddress("address")

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

    editorViewModel.updateOrganization(data, formViewModel)
    advanceUntilIdle()

    val state = editorViewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.success)
    assertEquals("New Name", state.organization?.name)
  }

  @Test
  fun clearSuccessFlag_works() = runTest {
    editorViewModel.clearSuccessFlag()
    val state = editorViewModel.uiState.value
    assertFalse(state.success)
  }

  @Test
  fun clearError_works() = runTest {
    editorViewModel.clearError()
    val state = editorViewModel.uiState.value
    assertNull(state.errorMessage)
  }

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
            contactEmail = FieldState(value = "123@gmail.com"),
            contactPhone = FieldState(value = "1234567890"),
            contactPhonePrefix = androidx.compose.runtime.mutableStateOf("+41"),
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
    assertEquals("123@gmail.com", data.contactEmail)
    assertEquals("1234567890", data.contactPhone)
    assertEquals("+41", data.phonePrefix)
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
            contactEmail = FieldState(value = "123@gmail.com"),
            contactPhone = FieldState(value = "1234567890"),
            contactPhonePrefix = androidx.compose.runtime.mutableStateOf("+41"),
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
    assertEquals("123@gmail.com", data.contactEmail)
    assertEquals("1234567890", data.contactPhone)
    assertEquals("+41", data.phonePrefix)
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
        description: String = "Description",
        contactEmail: String = "test@example.com",
        contactPhone: String = "1234567890"
    ) =
        Organization(
            id = id,
            name = name,
            description = description,
            ownerId = "owner1",
            status = OrganizationStatus.ACTIVE,
            verified = false,
            profileImageUrl = null,
            website = null,
            instagram = null,
            tiktok = null,
            facebook = null,
            contactEmail = contactEmail,
            contactPhone = contactPhone,
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

  override suspend fun createOrganization(organization: Organization): Result<String> = TODO()

  override suspend fun deleteOrganization(organizationId: String) = TODO()

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
      Result.success(Unit)

  override suspend fun updateCoverImage(organizationId: String, imageUrl: String?) =
      Result.success(Unit)
}
