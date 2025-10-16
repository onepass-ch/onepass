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

const val EVENTS_COLLECTION_PATH = "events"
const val UI_WAIT_TIMEOUT = 5_000L

open class FirestoreTestBase {

  protected lateinit var repository: EventRepository

  init {
    assert(FirebaseEmulator.isRunning) {
      "FirebaseEmulator must be running for Firestore tests. " +
          "Start emulators with: firebase emulators:start"
    }
  }

  protected suspend fun getEventsCount(): Int {
    val user = FirebaseEmulator.auth.currentUser ?: return 0
    return FirebaseEmulator.firestore
        .collection(EVENTS_COLLECTION_PATH)
        .whereEqualTo("organizerId", user.uid)
        .get(Source.SERVER)
        .await()
        .size()
  }

  protected suspend fun getAllEventsFromFirestore(): List<Event> {
    return repository.getAllEvents().first()
  }

  private suspend fun clearTestCollection() {
    val user = FirebaseEmulator.auth.currentUser ?: return
    val col =
        FirebaseEmulator.firestore
            .collection(EVENTS_COLLECTION_PATH)
            .whereEqualTo("organizerId", user.uid)

    repeat(12) { i ->
      val snaps = col.limit(200).get(Source.SERVER).await()
      if (snaps.isEmpty) return
      val batch = FirebaseEmulator.firestore.batch()
      snaps.documents.forEach { batch.delete(it.reference) }
      batch.commit().await()
      delay(100L * (i + 1))
    }

    val remainingCount = getEventsCount()
    assert(remainingCount == 0) {
      "Test collection is not empty after clearing, count: $remainingCount"
    }
  }

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

  @After
  open fun tearDown() {
    runTest { clearTestCollection() }
    FirebaseEmulator.clearFirestoreEmulator()
  }
}
