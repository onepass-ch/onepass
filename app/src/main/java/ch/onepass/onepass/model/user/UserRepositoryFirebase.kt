package ch.onepass.onepass.model.user

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class UserRepositoryFirebase(private val db: FirebaseFirestore = Firebase.firestore) :
    UserRepository {
  private val collection = db.collection("user")

  override suspend fun getUser(userId: String): User? {
    return collection.document(userId).get().await().toObject(User::class.java)
  }

  override suspend fun getOrCreateUser(user: FirebaseUser): User {
    val docRef = collection.document(user.uid)
    val snapshot = docRef.get().await()

    if (snapshot.exists()) {
      // User exists, update last login time
      docRef.update("lastLoginAt", FieldValue.serverTimestamp()).await()
    } else {
      // User does not exist, create it
      val newUser = User(email = user.email!!, displayName = user.displayName!!)
      docRef.set(newUser).await()
    }

    // Return the latest user data
    return docRef.get().await().toObject(User::class.java)!!
  }
}
