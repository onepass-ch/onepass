package ch.onepass.onepass.utils

import ch.onepass.onepass.model.organization.InvitationStatus
import ch.onepass.onepass.model.organization.OrganizationInvitation
import ch.onepass.onepass.model.organization.OrganizationRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Extended MockOrganizationRepository with specific implementations for invitation testing.
 *
 * This class provides a mock implementation that can be used in tests where you need to test
 * invitation-related functionality with configurable behavior.
 */
class TestMockOrganizationRepository(
    private val invitationsByEmail: Map<String, List<OrganizationInvitation>> = emptyMap(),
    private val updateInvitationStatusResult: Result<Unit> = Result.success(Unit),
    private val addMemberResult: Result<Unit> = Result.success(Unit),
    private val shouldThrowOnGetInvitations: Boolean = false,
    private val shouldThrowOnUpdateStatus: Boolean = false,
    private val exceptionMessage: String? = "Test error"
) : MockOrganizationRepository() {
  private val _invitationsFlowsByEmail =
      mutableMapOf<String, MutableStateFlow<List<OrganizationInvitation>>>()

  // Track addMember calls for testing
  val addMemberCalls = mutableListOf<Triple<String, String, OrganizationRole>>()

  init {
    // Initialize flows with data from invitationsByEmail map
    invitationsByEmail.forEach { (email, invitations) ->
      _invitationsFlowsByEmail[email] = MutableStateFlow(invitations)
    }
  }

  override fun getInvitationsByEmail(email: String): Flow<List<OrganizationInvitation>> {
    if (shouldThrowOnGetInvitations) {
      return flow {
        if (exceptionMessage != null) {
          throw Exception(exceptionMessage)
        } else {
          throw Exception()
        }
      }
    }
    // Get or create a StateFlow for this email
    val flow =
        _invitationsFlowsByEmail.getOrPut(email) {
          MutableStateFlow(invitationsByEmail[email] ?: emptyList())
        }
    return flow
  }

  override suspend fun updateInvitationStatus(
      invitationId: String,
      newStatus: InvitationStatus
  ): Result<Unit> {
    if (shouldThrowOnUpdateStatus) {
      throw Exception("Update invitation status failed")
    }
    // Update the mock state to reflect the status change for all email flows
    _invitationsFlowsByEmail.values.forEach { flow ->
      val currentInvitations = flow.value.toMutableList()
      val index = currentInvitations.indexOfFirst { it.id == invitationId }
      if (index >= 0) {
        currentInvitations[index] = currentInvitations[index].copy(status = newStatus)
        flow.value = currentInvitations
      }
    }
    return updateInvitationStatusResult
  }

  override suspend fun addMember(
      organizationId: String,
      userId: String,
      role: OrganizationRole
  ): Result<Unit> {
    // Track the call for testing
    addMemberCalls.add(Triple(organizationId, userId, role))
    return addMemberResult
  }
}

