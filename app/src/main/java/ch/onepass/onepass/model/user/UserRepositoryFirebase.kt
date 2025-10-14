package ch.onepass.onepass.model.user

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepositoryFirebase(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {
  private val userCollection = db.collection("users")

  // get the current user from Firebase Authentication
  override suspend fun getCurrentUser(): User? {
    val firebaseUser = auth.currentUser ?: return null
    val uid = firebaseUser.uid

    val snapshot = userCollection.document(uid).get().await()
    return snapshot.toObject(User::class.java)
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
      return docRef.get().await().toObject(User::class.java)
    }
  }

  // Update the lastLoginAt field with the server timestamp
  override suspend fun updateLastLogin(uid: String) {
    val docRef = userCollection.document(uid)
    docRef.update("lastLoginAt", FieldValue.serverTimestamp()).await()
  }
}
