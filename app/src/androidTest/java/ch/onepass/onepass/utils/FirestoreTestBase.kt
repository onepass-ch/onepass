package ch.onepass.onepass.utils

import android.util.Log
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before

const val EVENTS_COLLECTION_PATH = "events"
const val UI_WAIT_TIMEOUT = 5_000L

/**
 * Base class for Firebase Firestore emulated tests for the OnePass app.
 *
 * This class handles setup and teardown of the Firebase emulator connection and provides utilities
 * for testing with Firestore.
 *
 * Usage:
 * ```kotlin
 * class MyEventTest : FirestoreTestBase() {
 *     @Before
 *     override fun setUp() {
 *         super.setUp()
 *         runTest {
 *             FirebaseEmulator.auth.signInAnonymously().await()
 *         }
 *     }
 *
 *     @Test
 *     fun myTest() = runTest {
 *         // Your test code
 *     }
 * }
 * ```
 */
open class FirestoreTestBase {

  protected lateinit var repository: EventRepository

  init {
    assert(FirebaseEmulator.isRunning) {
      "FirebaseEmulator must be running for Firestore tests. " +
          "Start emulators with: firebase emulators:start"
    }
  }

  /**
   * Gets the count of events for the current user in Firestore. This is useful for verifying the
   * number of events after operations.
   */
  protected suspend fun getEventsCount(): Int {
    val user = FirebaseEmulator.auth.currentUser ?: return 0
    return FirebaseEmulator.firestore
        .collection(EVENTS_COLLECTION_PATH)
        .whereEqualTo("organizerId", user.uid)
        .get()
        .await()
        .size()
  }

  /**
   * Gets all events for the current user from Firestore using the repository. This is a convenience
   * method that collects the Flow into a List.
   */
  protected suspend fun getAllEventsFromFirestore(): List<Event> {
    return repository.getAllEvents().first()
  }

  /**
   * Clears all test data from the events collection for the current user. This ensures test
   * isolation by removing all documents created during tests.
   */
  private suspend fun clearTestCollection() {
    val user = FirebaseEmulator.auth.currentUser ?: return

    repeat(5) { attempt ->
      val events =
          FirebaseEmulator.firestore
              .collection(EVENTS_COLLECTION_PATH)
              .whereEqualTo("organizerId", user.uid)
              .get()
              .await()

      val batch = FirebaseEmulator.firestore.batch()
      events.documents.forEach { batch.delete(it.reference) }
      batch.commit().await()

      delay(100L * (attempt + 1))

      val remainingCount = getEventsCount()
      if (remainingCount == 0) {
        return
      }
    }

    val remainingCount = getEventsCount()
    assert(remainingCount == 0) {
      "Test collection is not empty after clearing, count: $remainingCount"
    }
  }

  /**
   * Sets up the test environment before each test. Initializes the repository and clears any
   * existing test data.
   *
   * Override this method to add your own setup logic, but always call super.setUp():
   * ```kotlin
   * @Before
   * override fun setUp() {
   *     super.setUp()
   *     // Your setup code
   * }
   * ```
   */
  @Before
  open fun setUp() {
    repository = EventRepositoryFirebase()
    runTest {
      val eventsCount = getEventsCount()
      if (eventsCount > 0) {
        Log.w(
            "FirestoreTestBase",
            "Warning: Test collection is not empty at the beginning of the test, count: $eventsCount. Clearing...",
        )
        clearTestCollection()
      }
    }
  }

  /**
   * Cleans up the test environment after each test. Clears all test data and resets the Firestore
   * emulator.
   *
   * Override this method to add your own cleanup logic, but always call super.tearDown():
   * ```kotlin
   * @After
   * override fun tearDown() {
   *     // Your cleanup code
   *     super.tearDown()
   * }
   * ```
   */
  @After
  open fun tearDown() {
    runTest { clearTestCollection() }
    FirebaseEmulator.clearFirestoreEmulator()
  }
}
