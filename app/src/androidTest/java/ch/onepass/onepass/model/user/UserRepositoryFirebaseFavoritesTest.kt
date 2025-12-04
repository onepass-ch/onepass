package ch.onepass.onepass.model.user

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryFirebaseFavoritesTest {

  private lateinit var repository: UserRepositoryFirebase
  private val mockAuth: FirebaseAuth = mockk()
  private val mockDb: FirebaseFirestore = mockk()
  private val mockCollection: CollectionReference = mockk()
  private val mockDocument: DocumentReference = mockk()

  private val testUserId = "testUser123"
  private val testEventId = "testEvent456"

  @Before
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)

    every { mockDb.collection("users") } returns mockCollection
    every { mockCollection.document(any()) } returns mockDocument

    repository = UserRepositoryFirebase(mockAuth, mockDb, mockk())
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  @Test
  fun addFavoriteEvent_callsCorrectFirestoreMethod() = runTest {
    every { mockDocument.update("favoriteEventIds", any<FieldValue>()) } returns
        Tasks.forResult(null)

    val result = repository.addFavoriteEvent(testUserId, testEventId)

    assertTrue(result.isSuccess)
    verify(exactly = 1) { mockDocument.update("favoriteEventIds", any<FieldValue>()) }
  }

  @Test
  fun removeFavoriteEvent_callsCorrectFirestoreMethod() = runTest {
    every { mockDocument.update("favoriteEventIds", any<FieldValue>()) } returns
        Tasks.forResult(null)

    val result = repository.removeFavoriteEvent(testUserId, testEventId)

    assertTrue(result.isSuccess)
    verify(exactly = 1) { mockDocument.update("favoriteEventIds", any<FieldValue>()) }
  }

  @Test
  fun addFavoriteEvent_handlesFailure() = runTest {
    val exception = Exception("Firestore error")
    every { mockDocument.update("favoriteEventIds", any<FieldValue>()) } returns
        Tasks.forException(exception)

    val result = repository.addFavoriteEvent(testUserId, testEventId)

    assertTrue(result.isFailure)
  }

  @Test
  fun removeFavoriteEvent_handlesFailure() = runTest {
    val exception = Exception("Firestore error")
    every { mockDocument.update("favoriteEventIds", any<FieldValue>()) } returns
        Tasks.forException(exception)

    val result = repository.removeFavoriteEvent(testUserId, testEventId)

    assertTrue(result.isFailure)
  }

  @Test
  fun getFavoriteEvents_emitsDataFromSnapshot() = runTest {
    val mockSnapshot = mockk<DocumentSnapshot>()
    val mockUser = User(uid = testUserId, favoriteEventIds = listOf("event1", "event2"))

    // Mock the snapshot listener
    every { mockDocument.addSnapshotListener(any()) } answers
        {
          val listener = firstArg<EventListener<DocumentSnapshot>>()
          every { mockSnapshot.exists() } returns true
          every { mockSnapshot.toObject(User::class.java) } returns mockUser
          listener.onEvent(mockSnapshot, null)
          mockk<ListenerRegistration>(relaxed = true)
        }

    val flow = repository.getFavoriteEvents(testUserId)
    val result = flow.first()

    assertEquals(2, result.size)
    assertTrue(result.contains("event1"))
    assertTrue(result.contains("event2"))
  }

  @Test
  fun getFavoriteEvents_emitsEmptySet_whenUserDoesNotExist() = runTest {
    // Simulate a snapshot where the document does not exist
    val mockSnapshot = mockk<DocumentSnapshot>()
    every { mockSnapshot.exists() } returns false

    // Mock the snapshot listener to emit the non-existent snapshot
    every { mockDocument.addSnapshotListener(any()) } answers
        {
          val listener = firstArg<EventListener<DocumentSnapshot>>()
          listener.onEvent(mockSnapshot, null)
          mockk<ListenerRegistration>(relaxed = true)
        }

    val flow = repository.getFavoriteEvents(testUserId)
    val result = flow.first()

    assertTrue("Should return empty set for non-existent user", result.isEmpty())
  }
}
