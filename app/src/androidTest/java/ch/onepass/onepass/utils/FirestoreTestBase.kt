package ch.onepass.onepass.utils

import android.util.Log
import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import com.google.firebase.firestore.Source
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before

// --- Constants for test configuration ---
private const val EVENTS_COLLECTION_PATH = "events"
private const val MAX_BATCH_RETRIES = 12 // Maximum retry attempts for batch deletion
private const val MAX_BATCH_SIZE =
    200L // Max number of documents deleted per batch (Long required by Firestore)
private const val BASE_DELAY_MS = 100L // Base delay (ms) between retries

/**
 * Base class for Firestore integration tests using Firebase Emulator.
 *
 * Ensures a clean test environment by clearing test data before and after each test. Provides
 * helper functions for Firestore event queries and cleanup.
 */
open class FirestoreTestBase {

  protected lateinit var repository: EventRepository

  init {
    // Ensure that the Firebase emulator is running before tests
    assert(FirebaseEmulator.isRunning) {
      "FirebaseEmulator must be running for Firestore tests. " +
          "Start emulators with: firebase emulators:start"
    }
  }

  /**
   * Returns the number of events belonging to the current user from the Firestore emulator. Always
   * uses Source.SERVER to avoid stale cache data.
   */
  protected suspend fun getEventsCount(): Int {
    val user = FirebaseEmulator.auth.currentUser ?: return 0
    return FirebaseEmulator.firestore
        .collection(EVENTS_COLLECTION_PATH)
        .whereEqualTo("organizerId", user.uid)
        .get(Source.SERVER)
        .await()
        .size()
  }

  /** Retrieves all events for the current user through the repository layer. */
  protected suspend fun getAllEventsFromFirestore(): List<Event> {
    return repository.getAllEvents().first()
  }

  /**
   * Clears all test events associated with the current user from Firestore. Uses batched deletions
   * and retries to handle potential Firestore propagation delays.
   */
  private suspend fun clearTestCollection() {
    val user = FirebaseEmulator.auth.currentUser ?: return
    val col =
        FirebaseEmulator.firestore
            .collection(EVENTS_COLLECTION_PATH)
            .whereEqualTo("organizerId", user.uid)

    // Retry up to MAX_BATCH_RETRIES to ensure complete cleanup
    repeat(MAX_BATCH_RETRIES) { attempt ->
      val snaps = col.limit(MAX_BATCH_SIZE).get(Source.SERVER).await()
      if (snaps.isEmpty) return

      val batch = FirebaseEmulator.firestore.batch()
      snaps.documents.forEach { batch.delete(it.reference) }
      batch.commit().await()

      // Gradual delay to allow Firestore to process deletions
      delay(BASE_DELAY_MS * (attempt + 1).toLong())
    }

    val remainingCount = getEventsCount()
    assert(remainingCount == 0) {
      "Test collection is not empty after clearing, count: $remainingCount"
    }
  }

  /** Sets up a clean Firestore state before each test. */
  @Before
  open fun setUp() {
    repository = EventRepositoryFirebase()
    runTest {
      val eventsCount = getEventsCount()
      if (eventsCount > 0) {
        Log.w(
            "FirestoreTestBase",
            "Warning: Test collection is not empty at the beginning of the test, count: $eventsCount. Clearing...")
        clearTestCollection()
      }
    }
  }
  /** Cleans up all Firestore data and resets the emulator after each test. */
  @After
  open fun tearDown() {
    runTest { clearTestCollection() }
    FirebaseEmulator.clearFirestoreEmulator()
  }
}
