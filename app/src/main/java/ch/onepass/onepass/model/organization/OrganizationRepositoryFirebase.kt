package ch.onepass.onepass.model.organization

import android.util.Log
import ch.onepass.onepass.model.firestore.firestoreFlow
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Firestore-backed implementation of [OrganizationRepository]. */
class OrganizationRepositoryFirebase : OrganizationRepository {
  private val organizationsCollection = Firebase.firestore.collection("organizations")
  private val invitationsCollection = Firebase.firestore.collection("organization_invitations")

  override suspend fun createOrganization(organization: Organization): Result<String> =
      runCatching {
        val docRef = organizationsCollection.document()
        val orgWithMetadata = organization.copy(id = docRef.id, createdAt = null, updatedAt = null)
        docRef.set(orgWithMetadata).await()
        docRef.id
      }

  override suspend fun updateOrganization(organization: Organization): Result<Unit> = runCatching {
    val updated = organization.copy(updatedAt = null)
    organizationsCollection.document(organization.id).set(updated).await()
  }

  override suspend fun deleteOrganization(organizationId: String): Result<Unit> = runCatching {
    organizationsCollection.document(organizationId).delete().await()
  }

  override fun getOrganizationById(organizationId: String): Flow<Organization?> = callbackFlow {
    val listener =
        organizationsCollection.document(organizationId).addSnapshotListener { snap, error ->
          if (error != null) {
            Log.w(
                "OrganizationRepository",
                "Failed to load organization details for orgId: $organizationId",
                error)
            close(error)
            return@addSnapshotListener
          }
          trySend(snap?.toObject(Organization::class.java))
        }
    awaitClose { listener.remove() }
  }

  override fun getOrganizationsByOwner(ownerId: String): Flow<List<Organization>> = firestoreFlow {
    organizationsCollection
        .whereEqualTo("ownerId", ownerId)
        .orderBy("createdAt", Query.Direction.DESCENDING)
  }

  override fun getOrganizationsByMember(userId: String): Flow<List<Organization>> = firestoreFlow {
    organizationsCollection.whereIn(
        "members.${userId}.role", OrganizationRole.entries.map { it.name })
    // Removed orderBy to avoid composite index requirement - sorting done in-memory instead
  }

  override fun getOrganizationsByStatus(status: OrganizationStatus): Flow<List<Organization>> =
      firestoreFlow {
        organizationsCollection
            .whereEqualTo("status", status.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
      }

  override fun searchOrganizations(query: String): Flow<List<Organization>> = firestoreFlow {
    val lowerQuery = query.lowercase()
    organizationsCollection.orderBy("nameLower").startAt(lowerQuery).endAt("$lowerQuery\uFFFF")
  }

  override fun getVerifiedOrganizations(): Flow<List<Organization>> = firestoreFlow {
    organizationsCollection
        .whereEqualTo("verified", true)
        .orderBy("followerCount", Query.Direction.DESCENDING)
  }

  override suspend fun addMember(
      organizationId: String,
      userId: String,
      role: OrganizationRole
  ): Result<Unit> = runCatching {
    val member = OrganizationMember(role = role, joinedAt = null)
    organizationsCollection
        .document(organizationId)
        .update(mapOf("members.$userId" to member, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
  }

  override suspend fun removeMember(organizationId: String, userId: String): Result<Unit> =
      runCatching {
        organizationsCollection
            .document(organizationId)
            .update(
                mapOf(
                    "members.$userId" to FieldValue.delete(),
                    "updatedAt" to FieldValue.serverTimestamp()))
            .await()
      }

  override suspend fun updateMemberRole(
      organizationId: String,
      userId: String,
      newRole: OrganizationRole
  ): Result<Unit> = runCatching {
    val org =
        organizationsCollection
            .document(organizationId)
            .get()
            .await()
            .toObject(Organization::class.java)
            ?: throw IllegalStateException("Organization not found")

    val existingMember = org.members[userId] ?: throw IllegalStateException("Member not found")
    val updatedMember = existingMember.copy(role = newRole)

    organizationsCollection
        .document(organizationId)
        .update(
            mapOf("members.$userId" to updatedMember, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
  }

  override suspend fun createInvitation(invitation: OrganizationInvitation): Result<String> =
      runCatching {
        val docRef = invitationsCollection.document()
        val invitationWithMetadata =
            invitation.copy(id = docRef.id, createdAt = null, updatedAt = null)
        docRef.set(invitationWithMetadata).await()
        docRef.id
      }

  override fun getPendingInvitations(organizationId: String): Flow<List<OrganizationInvitation>> =
      firestoreFlow {
        invitationsCollection
            .whereEqualTo("orgId", organizationId)
            .whereEqualTo("status", InvitationStatus.PENDING.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
      }

  override fun getInvitationsByEmail(email: String): Flow<List<OrganizationInvitation>> =
      firestoreFlow {
        invitationsCollection
            .whereEqualTo("inviteeEmail", email)
            .orderBy("createdAt", Query.Direction.DESCENDING)
      }

  override suspend fun updateInvitationStatus(
      invitationId: String,
      newStatus: InvitationStatus
  ): Result<Unit> = runCatching {
    invitationsCollection
        .document(invitationId)
        .update(mapOf("status" to newStatus.name, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
  }

  override suspend fun deleteInvitation(invitationId: String): Result<Unit> = runCatching {
    invitationsCollection.document(invitationId).delete().await()
  }

  override suspend fun updateProfileImage(
      organizationId: String,
      imageUrl: String?
  ): Result<Unit> = runCatching {
    organizationsCollection
        .document(organizationId)
        .update(
            mapOf("profileImageUrl" to imageUrl, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
  }

  override suspend fun updateCoverImage(
      organizationId: String,
      imageUrl: String?
  ): Result<Unit> = runCatching {
    organizationsCollection
        .document(organizationId)
        .update(
            mapOf("coverImageUrl" to imageUrl, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
  }
}
