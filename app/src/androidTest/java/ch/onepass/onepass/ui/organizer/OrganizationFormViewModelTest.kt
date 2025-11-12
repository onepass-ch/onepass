package ch.onepass.onepass.ui.organizer

import ch.onepass.onepass.model.organization.InvitationStatus
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationInvitation
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRole
import ch.onepass.onepass.model.organization.OrganizationStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationFormViewModelTest {

  private lateinit var repository: FakeOrganizationRepository
  private lateinit var viewModel: OrganizationFormViewModel

  @Before
  fun setup() {
    repository = FakeOrganizationRepository()
    viewModel = OrganizationFormViewModel(repository)
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

    val uiStates = mutableListOf<OrganizationFormUiState>()
    val job = launch { viewModel.uiState.take(1).toList(uiStates) }

    viewModel.createOrganization("user123")
    runCurrent()

    job.cancel()

    val state = uiStates[0]
    assertNull(state.successOrganizationId)
    assertEquals("Please fix errors", state.errorMessage)
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
