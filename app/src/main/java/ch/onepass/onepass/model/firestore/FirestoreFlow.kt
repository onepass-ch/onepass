package ch.onepass.onepass.model.firestore

import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Helper to convert a Firestore [Query] into a cold [Flow] emitting typed snapshots.
 *
 * @param T The model type to deserialize documents into.
 * @param queryBuilder Lambda returning the configured [Query] to observe.
 */
inline fun <reified T> firestoreFlow(noinline queryBuilder: () -> Query): Flow<List<T>> =
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
