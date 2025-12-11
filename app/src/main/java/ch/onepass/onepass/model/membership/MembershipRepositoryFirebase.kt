package ch.onepass.onepass.model.membership

import ch.onepass.onepass.model.firestore.firestoreFlow
import ch.onepass.onepass.model.organization.OrganizationRole
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/** Firestore-backed implementation of [MembershipRepository]. */
class MembershipRepositoryFirebase : MembershipRepository {
  private val membershipsCollection = Firebase.firestore.collection("memberships")

  override suspend fun addMembership(
      userId: String,
      orgId: String,
      role: OrganizationRole
  ): Result<String> = runCatching {
    val membershipId = "${orgId}_${userId}"
    val docRef = membershipsCollection.document(membershipId)

    // Check if membership already exists
    val snapshot = docRef.get().await()
    if (snapshot.exists()) {
      throw IllegalStateException(
          "Membership already exists for user $userId in organization $orgId")
    }

    val membership =
        Membership(
            membershipId = membershipId,
            userId = userId,
            orgId = orgId,
            role = role,
            createdAt = null,
            updatedAt = null)
    docRef.set(membership).await()
    membershipId
  }

  override suspend fun removeMembership(userId: String, orgId: String): Result<Unit> = runCatching {
    val membershipId = "${orgId}_${userId}"
    val docRef = membershipsCollection.document(membershipId)

    // We can verify existence first if we want to throw specific error,
    // or just delete. Previous logic threw error, so we keep that behavior.
    val snapshot = docRef.get().await()
    if (!snapshot.exists()) {
      throw NoSuchElementException("Membership not found for user $userId in organization $orgId")
    }

    docRef.delete().await()
  }

  override suspend fun updateMembership(
      userId: String,
      orgId: String,
      newRole: OrganizationRole
  ): Result<Unit> = runCatching {
    val membershipId = "${orgId}_${userId}"
    val docRef = membershipsCollection.document(membershipId)

    val snapshot = docRef.get().await()
    if (!snapshot.exists()) {
      throw NoSuchElementException("Membership not found for user $userId in organization $orgId")
    }

    docRef
        .update(mapOf("role" to newRole.name, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
  }

  override suspend fun getUsersByOrganization(orgId: String): Result<List<Membership>> =
      runCatching {
        membershipsCollection
            .whereEqualTo("orgId", orgId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(Membership::class.java) }
      }

  override suspend fun getOrganizationsByUser(userId: String): Result<List<Membership>> =
      runCatching {
        membershipsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(Membership::class.java) }
      }

  override suspend fun hasMembership(
      userId: String,
      orgId: String,
      roles: List<OrganizationRole>
  ): Boolean =
      runCatching {
            val membershipId = "${orgId}_${userId}"
            val snapshot = membershipsCollection.document(membershipId).get().await()

            if (!snapshot.exists()) return@runCatching false

            val roleStr = snapshot.getString("role") ?: return@runCatching false
            val role =
                try {
                  OrganizationRole.valueOf(roleStr)
                } catch (e: IllegalArgumentException) {
                  return@runCatching false
                }

            roles.contains(role)
          }
          .getOrDefault(false)

  override fun getUsersByOrganizationFlow(orgId: String): Flow<List<Membership>> = firestoreFlow {
    membershipsCollection
        .whereEqualTo("orgId", orgId)
        .orderBy("createdAt", Query.Direction.DESCENDING)
  }

  override fun getOrganizationsByUserFlow(userId: String): Flow<List<Membership>> = firestoreFlow {
    membershipsCollection
        .whereEqualTo("userId", userId)
        .orderBy("createdAt", Query.Direction.DESCENDING)
  }
}
