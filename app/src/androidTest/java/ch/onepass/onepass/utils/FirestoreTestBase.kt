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

// --- Public constants shared by tests and utilities ---
const val EVENTS_COLLECTION_PATH = "events" // may be imported by other test utils
const val UI_WAIT_TIMEOUT = 5_000L // common UI wait timeout for instrumentation tests

// --- Internal tuning knobs for cleanup behavior ---
private const val MAX_BATCH_RETRIES = 12 // max retry attempts for batched deletion
private const val MAX_BATCH_SIZE = 200L // Firestore limit(...) expects Long
private const val BASE_DELAY_MS = 100L // base delay (ms) between retries

/**
 * Base class for Firestore integration tests using the Firebase Emulator.
 *
 * Responsibilities:
 * - Ensures emulator is running before tests.
 * - Provides helpers to count/fetch events for the authenticated test user.
 * - Clears test data before/after each test with robust batched deletion + retry.
 */
open class FirestoreTestBase {

  protected lateinit var repository: EventRepository

  init {
    // Fail fast if emulator is not available.
    assert(FirebaseEmulator.isRunning) {
      "FirebaseEmulator must be running for Firestore tests. Start with: firebase emulators:start"
    }
  }

  /** Counts events for the current user (server source to avoid cache). */
  protected suspend fun getEventsCount(): Int {
    val user = FirebaseEmulator.auth.currentUser ?: return 0
    return FirebaseEmulator.firestore
        .collection(EVENTS_COLLECTION_PATH)
        .whereEqualTo("organizerId", user.uid)
        .get(Source.SERVER)
        .await()
        .size()
  }

  /** Retrieves all events via the repository layer (useful for integration assertions). */
  protected suspend fun getAllEventsFromFirestore(): List<Event> {
    return repository.getAllEvents().first()
  }

  /**
   * Clears all test events belonging to the current user.
   *
   * Uses batched deletes (MAX_BATCH_SIZE) and retries (MAX_BATCH_RETRIES) with a growing delay to
   * absorb Firestore emulator propagation latency.
   */
  private suspend fun clearTestCollection() {
    val user = FirebaseEmulator.auth.currentUser ?: return
    val col =
        FirebaseEmulator.firestore
            .collection(EVENTS_COLLECTION_PATH)
            .whereEqualTo("organizerId", user.uid)

    repeat(MAX_BATCH_RETRIES) { attempt ->
      val snaps = col.limit(MAX_BATCH_SIZE).get(Source.SERVER).await()
      if (snaps.isEmpty) return

      val batch = FirebaseEmulator.firestore.batch()
      snaps.documents.forEach { batch.delete(it.reference) }
      batch.commit().await()

      // Gradual delay (100ms, 200ms, …) to let the emulator settle between passes.
      delay(BASE_DELAY_MS * (attempt + 1).toLong())
    }

    val remainingCount = getEventsCount()
    assert(remainingCount == 0) {
      "Test collection is not empty after clearing, count: $remainingCount"
    }
  }

  /** Prepares a clean state and a concrete repository before each test. */
  @Before
  open fun setUp() {
    repository = EventRepositoryFirebase()
    runTest {
      val eventsCount = getEventsCount()
      if (eventsCount > 0) {
        Log.w(
            "FirestoreTestBase",
            "Warning: initial test collection not empty (count=$eventsCount). Clearing…")
        clearTestCollection()
      }
    }
  }

  /** Cleans up any leftover data and resets the emulator after each test. */
  @After
  open fun tearDown() {
    runTest { clearTestCollection() }
    FirebaseEmulator.clearFirestoreEmulator()
  }
}
