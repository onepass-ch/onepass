package ch.onepass.onepass.model.ticket

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
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

  /**
   * Get only active (not expired/redeemed/revoked/listed) tickets that can be used.
   * Excludes LISTED tickets since they are displayed in their own section.
   */
  override fun getActiveTickets(userId: String): Flow<List<Ticket>> =
      snapshotFlow { ticketsCollection.whereEqualTo("ownerId", userId) }
          .map { tickets ->
            val now = Timestamp.now()
            tickets
                .filter { ticket ->
                  // Include tickets that are ISSUED or TRANSFERRED and not expired by time
                  ticket.state in listOf(TicketState.ISSUED, TicketState.TRANSFERRED) &&
                      (ticket.expiresAt == null || now.seconds <= ticket.expiresAt.seconds)
                }
                .sortedByDescending { it.issuedAt?.seconds ?: 0 }
          }

  /** Get expired or redeemed tickets. */
  override fun getExpiredTickets(userId: String): Flow<List<Ticket>> =
      snapshotFlow { ticketsCollection.whereEqualTo("ownerId", userId) }
          .map { tickets ->
            val now = Timestamp.now()
            tickets
                .filter { ticket ->
                  // Include tickets that are REDEEMED/REVOKED or have passed their expiration time
                  ticket.state in listOf(TicketState.REDEEMED, TicketState.REVOKED) ||
                      (ticket.expiresAt != null && now.seconds > ticket.expiresAt.seconds)
                }
                .sortedByDescending { it.issuedAt?.seconds ?: 0 }
          }

  /** Get tickets that the user has listed for sale. */
  override fun getListedTicketsByUser(userId: String): Flow<List<Ticket>> =
      snapshotFlow { ticketsCollection.whereEqualTo("ownerId", userId) }
          .map { tickets ->
            tickets
                .filter { ticket -> ticket.state == TicketState.LISTED }
                .sortedByDescending { it.listedAt?.seconds ?: 0 }
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
    ticketsCollection.document(ticketId).update("deletedAt", FieldValue.serverTimestamp()).await()
  }

  // -------------------- Market Operations --------------------

  /**
   * Get all tickets listed for sale on the marketplace.
   *
   * Note: Uses client-side sorting to avoid requiring a composite Firestore index.
   */
  override fun getListedTickets(): Flow<List<Ticket>> =
      snapshotFlow { ticketsCollection.whereEqualTo("state", TicketState.LISTED.name) }
          .map { tickets ->
            tickets
                .filter { it.listingPrice != null }
                .sortedByDescending { it.listedAt?.seconds ?: 0 }
          }

  /**
   * Get listed tickets for a specific event.
   *
   * Note: Uses client-side sorting to avoid requiring a composite Firestore index.
   */
  override fun getListedTicketsByEvent(eventId: String): Flow<List<Ticket>> =
      snapshotFlow { ticketsCollection.whereEqualTo("state", TicketState.LISTED.name) }
          .map { tickets ->
            tickets
                .filter { it.listingPrice != null && it.eventId == eventId }
                .sortedBy { it.listingPrice }
          }

  /** List a ticket for sale on the marketplace. */
  override suspend fun listTicketForSale(ticketId: String, askingPrice: Double): Result<Unit> =
      runCatching {
        require(askingPrice > 0) { "Asking price must be positive" }

        ticketsCollection
            .document(ticketId)
            .update(
                mapOf(
                    "state" to TicketState.LISTED.name,
                    "listingPrice" to askingPrice,
                    "listedAt" to Timestamp.now()))
            .await()
      }

  /** Cancel a ticket listing and return it to ISSUED state. */
  override suspend fun cancelTicketListing(ticketId: String): Result<Unit> = runCatching {
    ticketsCollection
        .document(ticketId)
        .update(
            mapOf("state" to TicketState.ISSUED.name, "listingPrice" to null, "listedAt" to null))
        .await()
  }

  /**
   * Purchase a listed ticket from the marketplace.
   *
   * Uses a Firestore transaction to ensure atomic transfer of ownership.
   */
  override suspend fun purchaseListedTicket(ticketId: String, buyerId: String): Result<Unit> =
      runCatching {
        Firebase.firestore
            .runTransaction { transaction ->
              val ticketRef = ticketsCollection.document(ticketId)
              val ticketSnapshot = transaction.get(ticketRef)
              val ticket = ticketSnapshot.toObject(Ticket::class.java)

              requireNotNull(ticket) { "Ticket not found" }
              require(ticket.state == TicketState.LISTED) { "Ticket is not listed for sale" }
              require(ticket.ownerId != buyerId) { "Cannot purchase your own ticket" }

              transaction.update(
                  ticketRef,
                  mapOf(
                      "ownerId" to buyerId,
                      "state" to TicketState.TRANSFERRED.name,
                      "listingPrice" to null,
                      "listedAt" to null,
                      "version" to (ticket.version + 1)))
            }
            .await()
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
