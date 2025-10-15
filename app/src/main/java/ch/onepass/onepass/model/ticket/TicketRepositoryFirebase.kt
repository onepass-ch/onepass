package ch.onepass.onepass.model.ticket

import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/** Firestore-backed implementation of [TicketRepository]. */
class TicketRepositoryFirebase : TicketRepository {
  /** Firestore collection reference for tickets */
  private val ticketsCollection = Firebase.firestore.collection("tickets")

  /** Get all tickets for a specific user (the owner). */
  override fun getTicketsByUser(userId: String): Flow<List<Ticket>> =
      snapshotFlow { ticketsCollection.whereEqualTo("ownerId", userId) }
          .map { tickets -> tickets.sortedByDescending { it.issuedAt?.seconds ?: 0 } }

  /** Get only active (not expired/redeemed/revoked) tickets. */
  override fun getActiveTickets(userId: String): Flow<List<Ticket>> =
      snapshotFlow { ticketsCollection.whereEqualTo("ownerId", userId) }
          .map { tickets ->
            tickets
                .filter { ticket ->
                  ticket.state in
                      listOf(TicketState.ISSUED, TicketState.LISTED, TicketState.TRANSFERRED)
                }
                .sortedByDescending { it.issuedAt?.seconds ?: 0 }
          }

  /** Get expired or redeemed tickets. */
  override fun getExpiredTickets(userId: String): Flow<List<Ticket>> =
      snapshotFlow { ticketsCollection.whereEqualTo("ownerId", userId) }
          .map { tickets ->
            tickets
                .filter { ticket ->
                  ticket.state in listOf(TicketState.REDEEMED, TicketState.REVOKED)
                }
                .sortedByDescending { it.issuedAt?.seconds ?: 0 }
          }

  /** Get a single ticket by ID. */
  override fun getTicketById(ticketId: String): Flow<Ticket?> = callbackFlow {
    val listener =
        ticketsCollection.document(ticketId).addSnapshotListener { snap, error ->
          if (error != null) {
            close(error)
            return@addSnapshotListener
          }
          trySend(snap?.toObject(Ticket::class.java))
        }
    awaitClose { listener.remove() }
  }

  /** Create a ticket document (when purchased). */
  override suspend fun createTicket(ticket: Ticket): Result<String> = runCatching {
    val docRef = ticketsCollection.document()
    val withId = ticket.copy(ticketId = docRef.id)
    docRef.set(withId).await()
    docRef.id
  }

  /** Update ticket (e.g., state change). */
  override suspend fun updateTicket(ticket: Ticket): Result<Unit> = runCatching {
    ticketsCollection.document(ticket.ticketId).set(ticket).await()
  }

  /** Delete ticket (soft delete). */
  override suspend fun deleteTicket(ticketId: String): Result<Unit> = runCatching {
    ticketsCollection.document(ticketId).delete().await()
  }

  /** Helper to create a [Flow] from a Firestore query using a snapshot listener. */
  private fun snapshotFlow(queryBuilder: () -> Query): Flow<List<Ticket>> = callbackFlow {
    val query = queryBuilder()
    val listener =
        query.addSnapshotListener { snap, error ->
          if (error != null) {
            close(error)
            return@addSnapshotListener
          }
          val tickets =
              snap?.documents?.mapNotNull { it.toObject(Ticket::class.java) } ?: emptyList()

          trySend(tickets)
        }
    awaitClose { listener.remove() }
  }
}
