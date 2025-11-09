package ch.onepass.onepass.ui.myinvitations

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.onepass.onepass.model.organization.InvitationStatus
import ch.onepass.onepass.model.organization.Organization
import ch.onepass.onepass.model.organization.OrganizationInvitation
import ch.onepass.onepass.model.organization.OrganizationRepository
import ch.onepass.onepass.model.organization.OrganizationRole
import ch.onepass.onepass.model.organization.OrganizationStatus
import ch.onepass.onepass.ui.theme.OnePassTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Mock OrganizationRepository implementation for Preview purposes.
 *
 * This repository provides test data for previewing the My Invitations screen without requiring
 * actual Firebase connections.
 */
private class PreviewOrganizationRepository(
    private val organizations: Map<String, Organization> = emptyMap()
) : OrganizationRepository {
  override suspend fun createOrganization(organization: Organization): Result<String> =
      Result.success("org-id")

  override suspend fun updateOrganization(organization: Organization): Result<Unit> =
      Result.success(Unit)

  override suspend fun deleteOrganization(organizationId: String): Result<Unit> =
      Result.success(Unit)

  override fun getOrganizationById(organizationId: String): Flow<Organization?> =
      flowOf(organizations[organizationId])

  override fun getOrganizationsByOwner(ownerId: String): Flow<List<Organization>> =
      flowOf(emptyList())

  override fun getOrganizationsByMember(userId: String): Flow<List<Organization>> =
      flowOf(emptyList())

  override fun getOrganizationsByStatus(status: OrganizationStatus): Flow<List<Organization>> =
      flowOf(emptyList())

  override fun searchOrganizations(query: String): Flow<List<Organization>> = flowOf(emptyList())

  override fun getVerifiedOrganizations(): Flow<List<Organization>> = flowOf(emptyList())

  override suspend fun addMember(
      organizationId: String,
      userId: String,
      role: OrganizationRole
  ): Result<Unit> = Result.success(Unit)

  override suspend fun removeMember(organizationId: String, userId: String): Result<Unit> =
      Result.success(Unit)

  override suspend fun updateMemberRole(
      organizationId: String,
      userId: String,
      newRole: OrganizationRole
  ): Result<Unit> = Result.success(Unit)

  override suspend fun createInvitation(invitation: OrganizationInvitation): Result<String> =
      Result.success("invite-id")

  override fun getPendingInvitations(organizationId: String): Flow<List<OrganizationInvitation>> =
      flowOf(emptyList())

  override fun getInvitationsByEmail(email: String): Flow<List<OrganizationInvitation>> =
      flowOf(emptyList())

  override suspend fun updateInvitationStatus(
      invitationId: String,
      newStatus: InvitationStatus
  ): Result<Unit> = Result.success(Unit)

  override suspend fun deleteInvitation(invitationId: String): Result<Unit> = Result.success(Unit)
}

/**
 * Preview composable for My Invitations screen with sample data.
 *
 * This preview displays the screen with multiple invitations to test the UI layout, organization
 * name display, role information, and accept/reject buttons.
 */
@Preview(showBackground = true)
@Composable
fun MyInvitationsScreenPreview() {
  OnePassTheme {
    // Create test organizations
    val org1 =
        Organization(
            id = "org-1",
            name = "Tech Events Lausanne",
            description = "Organizing tech events in Lausanne",
            ownerId = "owner-1",
            status = OrganizationStatus.ACTIVE)

    val org2 =
        Organization(
            id = "org-2",
            name = "Music Festival Geneva",
            description = "Annual music festival in Geneva",
            ownerId = "owner-2",
            status = OrganizationStatus.ACTIVE)

    val org3 =
        Organization(
            id = "org-3",
            name = "Sports Club Zurich",
            description = "Community sports events",
            ownerId = "owner-3",
            status = OrganizationStatus.ACTIVE)

    // Create test invitations
    val invitation1 =
        OrganizationInvitation(
            id = "invite-1",
            orgId = "org-1",
            inviteeEmail = "user@example.com",
            role = OrganizationRole.MEMBER,
            invitedBy = "owner-1",
            status = InvitationStatus.PENDING)

    val invitation2 =
        OrganizationInvitation(
            id = "invite-2",
            orgId = "org-2",
            inviteeEmail = "user@example.com",
            role = OrganizationRole.STAFF,
            invitedBy = "owner-2",
            status = InvitationStatus.PENDING)

    val invitation3 =
        OrganizationInvitation(
            id = "invite-3",
            orgId = "org-3",
            inviteeEmail = "user@example.com",
            role = OrganizationRole.MEMBER,
            invitedBy = "owner-3",
            status = InvitationStatus.PENDING)

    // Create mock repository with organizations
    val orgRepository =
        PreviewOrganizationRepository(
            organizations = mapOf("org-1" to org1, "org-2" to org2, "org-3" to org3))

    // Create UI state with invitations
    val state =
        MyInvitationsUiState(
            invitations = listOf(invitation1, invitation2, invitation3),
            loading = false,
            errorMessage = null,
            successMessage = null)

    MyInvitationsContent(
        state = state,
        onAcceptInvitation = { _ ->
          // Preview callback - can add logging or other actions here
        },
        onRejectInvitation = { _ ->
          // Preview callback - can add logging or other actions here
        },
        onRetry = { /* Preview callback */},
        onNavigateBack = { /* Preview callback */},
        organizationRepository = orgRepository)
  }
}

/**
 * Preview composable for empty state.
 *
 * This preview shows the screen when there are no pending invitations.
 */
@Preview(showBackground = true)
@Composable
fun MyInvitationsScreenEmptyPreview() {
  OnePassTheme {
    val state =
        MyInvitationsUiState(
            invitations = emptyList(), loading = false, errorMessage = null, successMessage = null)

    MyInvitationsContent(
        state = state,
        onAcceptInvitation = {},
        onRejectInvitation = {},
        onRetry = {},
        onNavigateBack = { /* Preview callback */},
        organizationRepository = PreviewOrganizationRepository())
  }
}

/**
 * Preview composable for loading state.
 *
 * This preview shows the loading indicator while invitations are being fetched.
 */
@Preview(showBackground = true)
@Composable
fun MyInvitationsScreenLoadingPreview() {
  OnePassTheme {
    val state =
        MyInvitationsUiState(
            invitations = emptyList(), loading = true, errorMessage = null, successMessage = null)

    MyInvitationsContent(
        state = state,
        onAcceptInvitation = {},
        onRejectInvitation = {},
        onRetry = {},
        onNavigateBack = { /* Preview callback */},
        organizationRepository = PreviewOrganizationRepository())
  }
}

/**
 * Preview composable for error state.
 *
 * This preview shows the screen when there's an error loading invitations.
 */
@Preview(showBackground = true)
@Composable
fun MyInvitationsScreenErrorPreview() {
  OnePassTheme {
    val state =
        MyInvitationsUiState(
            invitations = emptyList(),
            loading = false,
            errorMessage =
                "Failed to load invitations. Please check your connection and try again.",
            successMessage = null)

    MyInvitationsContent(
        state = state,
        onAcceptInvitation = {},
        onRejectInvitation = {},
        onRetry = {},
        onNavigateBack = { /* Preview callback */},
        organizationRepository = PreviewOrganizationRepository())
  }
}

/**
 * Preview composable for success message state.
 *
 * This preview shows the screen with a success message displayed.
 */
@Preview(showBackground = true)
@Composable
fun MyInvitationsScreenSuccessPreview() {
  OnePassTheme {
    // Create test data
    val org1 =
        Organization(
            id = "org-1",
            name = "Tech Events Lausanne",
            description = "Organizing tech events in Lausanne",
            ownerId = "owner-1",
            status = OrganizationStatus.ACTIVE)

    val invitation1 =
        OrganizationInvitation(
            id = "invite-1",
            orgId = "org-1",
            inviteeEmail = "user@example.com",
            role = OrganizationRole.MEMBER,
            invitedBy = "owner-1",
            status = InvitationStatus.PENDING)

    val orgRepository = PreviewOrganizationRepository(organizations = mapOf("org-1" to org1))

    val state =
        MyInvitationsUiState(
            invitations = listOf(invitation1),
            loading = false,
            errorMessage = null,
            successMessage = "Invitation accepted successfully. You are now a member.")

    MyInvitationsContent(
        state = state,
        onAcceptInvitation = {},
        onRejectInvitation = {},
        onRetry = {},
        onNavigateBack = { /* Preview callback */},
        organizationRepository = orgRepository)
  }
}
