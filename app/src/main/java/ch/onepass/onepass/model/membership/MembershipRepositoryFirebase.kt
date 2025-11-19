package ch.onepass.onepass.model.membership

import ch.onepass.onepass.model.organization.OrganizationRole
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Firestore-backed implementation of [MembershipRepository]. */
class MembershipRepositoryFirebase : MembershipRepository {
  private val membershipsCollection = Firebase.firestore.collection("memberships")

  override suspend fun addMembership(
      userId: String,
      orgId: String,
      role: OrganizationRole
  ): Result<String> = runCatching {
    // Check if membership already exists
    val existingMembership =
        membershipsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("orgId", orgId)
            .limit(1)
            .get()
            .await()

    if (!existingMembership.isEmpty) {
      throw IllegalStateException(
          "Membership already exists for user $userId in organization $orgId")
    }

    val docRef = membershipsCollection.document()
    val membership =
        Membership(
            membershipId = docRef.id,
            userId = userId,
            orgId = orgId,
            role = role,
            createdAt = null,
            updatedAt = null)
    docRef.set(membership).await()
    docRef.id
  }

  override suspend fun removeMembership(userId: String, orgId: String): Result<Unit> = runCatching {
    val querySnapshot =
        membershipsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("orgId", orgId)
            .limit(1)
            .get()
            .await()

    if (querySnapshot.isEmpty) {
      throw IllegalStateException("Membership not found for user $userId in organization $orgId")
    }

    querySnapshot.documents.first().reference.delete().await()
  }

  override suspend fun updateMembership(
      userId: String,
      orgId: String,
      newRole: OrganizationRole
  ): Result<Unit> = runCatching {
    val querySnapshot =
        membershipsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("orgId", orgId)
            .limit(1)
            .get()
            .await()

    if (querySnapshot.isEmpty) {
      throw IllegalStateException("Membership not found for user $userId in organization $orgId")
    }

    querySnapshot.documents
        .first()
        .reference
        .update(mapOf("role" to newRole.name, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
  }

  override suspend fun getUsersByOrganization(orgId: String): List<Membership> =
      runCatching {
            membershipsCollection
                .whereEqualTo("orgId", orgId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Membership::class.java) }
          }
          .getOrDefault(emptyList())

  override suspend fun getOrganizationsByUser(userId: String): List<Membership> =
      runCatching {
            membershipsCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Membership::class.java) }
          }
          .getOrDefault(emptyList())

  override suspend fun hasMembership(
      userId: String,
      orgId: String,
      roles: List<OrganizationRole>
  ): Boolean =
      runCatching {
            val roleNames = roles.map { it.name }

            val snapshot =
                membershipsCollection
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("orgId", orgId)
                    .whereIn("role", roleNames)
                    .limit(1)
                    .get()
                    .await()

            !snapshot.isEmpty
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

  /**
   * Helper function to create a [Flow] from a Firestore query using a snapshot listener.
   *
   * @param T The type of objects to emit in the Flow
   * @param queryBuilder Lambda that returns a configured [Query].
   * @return A [Flow] emitting a list of objects of type [T].
   */
  private inline fun <reified T> firestoreFlow(noinline queryBuilder: () -> Query): Flow<List<T>> =
      callbackFlow {
        val query = queryBuilder()
        val listener =
            query.addSnapshotListener { snap, error ->
              if (error != null) {
                close(error)
                return@addSnapshotListener
              }
              val list = snap?.documents?.mapNotNull { it.toObject(T::class.java) } ?: emptyList()
              trySend(list)
            }
        awaitClose { listener.remove() }
      }
}
