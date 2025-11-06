package ch.onepass.onepass.model.user

import ch.onepass.onepass.model.staff.StaffSearchResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class UserRepositoryFirebase(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = Firebase.functions
) : UserRepository {
  private val userCollection = db.collection("users")

  // get the current user from Firebase Authentication
  override suspend fun getCurrentUser(): User? {
    val firebaseUser = auth.currentUser ?: return null
    val uid = firebaseUser.uid

    val snapshot = userCollection.document(uid).get().await()
    return snapshot.toObject(User::class.java)?.copy(uid = uid)
  }

  // get the user in Firestore, if the user doesn't exist, create one
  override suspend fun getOrCreateUser(): User? {
    val firebaseUser = auth.currentUser ?: return null
    val uid = firebaseUser.uid
    val docRef = userCollection.document(uid)

    val snapshot = docRef.get().await()
    if (!snapshot.exists()) {
      val newUser =
          User(
              uid = uid,
              email = firebaseUser.email ?: "",
              displayName = firebaseUser.displayName ?: "",
              avatarUrl = firebaseUser.photoUrl?.toString())
      docRef.set(newUser).await()
      return newUser
    } else {
      updateLastLogin(uid)
      return docRef.get().await().toObject(User::class.java)?.copy(uid = uid)
    }
  }

  // Update the lastLoginAt field with the server timestamp
  override suspend fun updateLastLogin(uid: String) {
    val docRef = userCollection.document(uid)
    docRef.update("lastLoginAt", FieldValue.serverTimestamp()).await()
  }

  override suspend fun searchUsersByDisplayName(
      query: String,
      organizationId: String?
  ): Result<List<StaffSearchResult>> = runCatching {
    require(query.isNotBlank()) { "Query cannot be blank" }

    val payload = mutableMapOf<String, Any>("query" to query.trim(), "searchType" to "displayName")
    organizationId?.let { payload["organizationId"] = it }

    val result = functions.getHttpsCallable(FN_SEARCH_USERS).call(payload).await()

    @Suppress("UNCHECKED_CAST")
    val data = result.data as? Map<String, Any?> ?: error("Unexpected response format")

    @Suppress("UNCHECKED_CAST")
    val usersList = data[KEY_USERS] as? List<Map<String, Any?>> ?: emptyList()

    usersList.mapNotNull { userMap ->
      try {
        StaffSearchResult(
            id = userMap[KEY_ID] as? String ?: return@mapNotNull null,
            email = userMap[KEY_EMAIL] as? String ?: "",
            displayName = userMap[KEY_DISPLAY_NAME] as? String ?: "",
            avatarUrl = userMap[KEY_AVATAR_URL] as? String)
      } catch (e: Exception) {
        null
      }
    }
  }

  override suspend fun searchUsersByEmail(
      query: String,
      organizationId: String?
  ): Result<List<StaffSearchResult>> = runCatching {
    require(query.isNotBlank()) { "Query cannot be blank" }

    val payload = mutableMapOf<String, Any>("query" to query.trim(), "searchType" to "email")
    organizationId?.let { payload["organizationId"] = it }

    val result = functions.getHttpsCallable(FN_SEARCH_USERS).call(payload).await()

    @Suppress("UNCHECKED_CAST")
    val data = result.data as? Map<String, Any?> ?: error("Unexpected response format")

    @Suppress("UNCHECKED_CAST")
    val usersList = data[KEY_USERS] as? List<Map<String, Any?>> ?: emptyList()

    usersList.mapNotNull { userMap ->
      try {
        StaffSearchResult(
            id = userMap[KEY_ID] as? String ?: return@mapNotNull null,
            email = userMap[KEY_EMAIL] as? String ?: "",
            displayName = userMap[KEY_DISPLAY_NAME] as? String ?: "",
            avatarUrl = userMap[KEY_AVATAR_URL] as? String)
      } catch (e: Exception) {
        null
      }
    }
  }

  private companion object {
    const val FN_SEARCH_USERS = "searchUsers"

    const val KEY_USERS = "users"
    const val KEY_ID = "id"
    const val KEY_EMAIL = "email"
    const val KEY_DISPLAY_NAME = "displayName"
    const val KEY_AVATAR_URL = "avatarUrl"
  }
}
