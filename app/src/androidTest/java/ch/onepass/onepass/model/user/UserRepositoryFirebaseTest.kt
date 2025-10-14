package ch.onepass.onepass.model.user

import ch.onepass.onepass.utils.FirebaseEmulator
import ch.onepass.onepass.utils.FirestoreTestBase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for UserRepositoryFirebase using Firebase Emulator.
 *
 * These tests verify user creation, retrieval, and last login update logic.
 *
 * Requirements:
 * - Firebase emulator must be running (`firebase emulators:start`)
 * - Firestore and Auth emulators properly configured in FirebaseEmulator
 */
class UserRepositoryFirebaseTest : FirestoreTestBase() {

  private lateinit var userRepository: UserRepositoryFirebase
  private lateinit var auth: FirebaseAuth
  private lateinit var db: FirebaseFirestore

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      // Use the emulator's FirebaseAuth and Firestore instances
      auth = FirebaseEmulator.auth
      db = FirebaseEmulator.firestore

      // Sign in a real (but test) user using email & password
      // This will still return a FirebaseUser even we don't use Google OAuth2
      val email = "testuser@example.com"
      val password = "password123"

      // Try to create the user if it doesn't exist
      try {
        auth.createUserWithEmailAndPassword(email, password).await()
      } catch (e: Exception) {
        // User might already exist, ignore
      }

      // Sign in
      auth.signInWithEmailAndPassword(email, password).await()

      userRepository = UserRepositoryFirebase(auth, db)
    }
  }

  @Test
  fun canGetOrCreateUser_createsUserIfNotExists() = runTest {
    val currentUser = auth.currentUser
    assertNotNull("User should be signed in", currentUser)

    val uid = currentUser!!.uid

    // Ensure Firestore doesn't have the user yet
    db.collection("users").document(uid).delete().await()

    val user = userRepository.getOrCreateUser()
    assertNotNull("Should create and return user", user)
    assertEquals("UID should match", uid, user!!.uid)
    assertEquals("Email should match", currentUser.email, user.email)

    // Verify Firestore document exists
    val snapshot = db.collection("users").document(uid).get().await()
    assertTrue("User document should exist", snapshot.exists())
  }

  @Test
  fun getOrCreateUser_returnsExistingUserIfExists() = runTest {
    val firstCall = userRepository.getOrCreateUser()
    assertNotNull(firstCall)

    val secondCall = userRepository.getOrCreateUser()
    assertNotNull(secondCall)

    assertEquals("Should return same user", firstCall!!.uid, secondCall!!.uid)
  }

  @Test
  fun updateLastLogin_updatesTimestamp() = runTest {
    val user = userRepository.getOrCreateUser()
    assertNotNull(user)

    val uid = user!!.uid
    userRepository.updateLastLogin(uid)

    val snapshot = db.collection("users").document(uid).get().await()
    val lastLoginAt = snapshot.getTimestamp("lastLoginAt")

    assertNotNull("lastLoginAt should be updated", lastLoginAt)
  }

  @Test
  fun getCurrentUser_returnsUserFromFirestore() = runTest {
    val user = userRepository.getOrCreateUser()
    assertNotNull(user)

    val retrieved = userRepository.getCurrentUser()

    assertNotNull("Should retrieve current user", retrieved)
    assertEquals("UID should match", user!!.uid, retrieved!!.uid)
  }
}
