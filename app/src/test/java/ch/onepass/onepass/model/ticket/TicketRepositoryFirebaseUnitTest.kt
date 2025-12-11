package ch.onepass.onepass.model.ticket

import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * JVM unit tests for [TicketRepositoryFirebase] with mocked Firestore.
 *
 * These tests focus on marketplace-specific behavior without hitting the real Firebase backend.
 */
class TicketRepositoryFirebaseUnitTest {

  private lateinit var repository: TicketRepositoryFirebase
  private val mockFirestore: FirebaseFirestore = mockk(relaxed = true)
  private val mockCollection: CollectionReference = mockk(relaxed = true)
  private val mockDocument: DocumentReference = mockk(relaxed = true)
  private val mockTransaction: Transaction = mockk(relaxed = true)
  private val mockSnapshot: DocumentSnapshot = mockk(relaxed = true)

  @Before
  fun setUp() {
    mockkObject(Firebase)
    mockkStatic("com.google.firebase.firestore.ktx.FirestoreKt")
    every { Firebase.firestore } returns mockFirestore
    every { mockFirestore.collection("tickets") } returns mockCollection
    every { mockCollection.document(any()) } returns mockDocument
    every { mockTransaction.get(any()) } returns mockSnapshot

    // Route transactions through the provided lambda so we can exercise validation logic.
    every { mockFirestore.runTransaction(any<Transaction.Function<*>>()) } answers
        {
          val function = firstArg<Transaction.Function<*>>()
          val result = function.apply(mockTransaction)
          Tasks.forResult(result)
        }

    repository = spyk(TicketRepositoryFirebase(), recordPrivateCalls = true)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  private fun createTicket(
      ticketId: String,
      ownerId: String = "owner-1",
      eventId: String = "event-1",
      state: TicketState = TicketState.LISTED,
      listingPrice: Double? = 25.0,
      listedAt: Timestamp? = Timestamp.now(),
      version: Int = 1
  ): Ticket {
    return Ticket(
        ticketId = ticketId,
        ownerId = ownerId,
        eventId = eventId,
        state = state,
        listingPrice = listingPrice,
        listedAt = listedAt,
        version = version)
  }

  @Test
  fun getListedTicketsByUser_filtersListedAndSortsByDate() = runTest {
    val listedRecent =
        createTicket(
            ticketId = "listed-recent", listedAt = Timestamp(2_000, 0), state = TicketState.LISTED)
    val listedOlder =
        createTicket(
            ticketId = "listed-old", listedAt = Timestamp(1_000, 0), state = TicketState.LISTED)
    val notListed = createTicket(ticketId = "issued", state = TicketState.ISSUED)

    every { repository["snapshotFlow"](any<() -> Query>()) } returns
        flowOf(listOf(listedOlder, notListed, listedRecent))

    val result = repository.getListedTicketsByUser("owner-1").first()

    assertEquals(listOf("listed-recent", "listed-old"), result.map { it.ticketId })
    assertTrue(result.all { it.state == TicketState.LISTED })
    assertTrue(result[0].listedAt!!.seconds > result[1].listedAt!!.seconds)
  }

  @Test
  fun getListedTickets_filtersByListingPriceAndSorts() = runTest {
    val latestWithPrice =
        createTicket(
            ticketId = "with-price-new", listedAt = Timestamp(3_000, 0), listingPrice = 80.0)
    val earlierWithPrice =
        createTicket(
            ticketId = "with-price-old", listedAt = Timestamp(1_000, 0), listingPrice = 50.0)
    val withoutPrice =
        createTicket(ticketId = "no-price", listedAt = Timestamp(2_000, 0), listingPrice = null)

    every { repository["snapshotFlow"](any<() -> Query>()) } returns
        flowOf(listOf(earlierWithPrice, withoutPrice, latestWithPrice))

    val result = repository.getListedTickets().first()

    assertEquals(listOf("with-price-new", "with-price-old"), result.map { it.ticketId })
    assertTrue(result.none { it.ticketId == "no-price" })
    assertTrue(result.first().listedAt!!.seconds > result.last().listedAt!!.seconds)
  }

  @Test
  fun getListedTicketsByEvent_filtersByEventAndSortsByPrice() = runTest {
    val cheap = createTicket(ticketId = "cheap", listingPrice = 20.0, eventId = "event-42")
    val expensive = createTicket(ticketId = "expensive", listingPrice = 120.0, eventId = "event-42")
    val otherEvent =
        createTicket(ticketId = "other-event", listingPrice = 10.0, eventId = "event-99")

    every { repository["snapshotFlow"](any<() -> Query>()) } returns
        flowOf(listOf(expensive, otherEvent, cheap))

    val result = repository.getListedTicketsByEvent("event-42").first()

    assertEquals(listOf("cheap", "expensive"), result.map { it.ticketId })
    assertTrue(result.all { it.eventId == "event-42" })
    assertTrue(result.zipWithNext().all { it.first.listingPrice!! <= it.second.listingPrice!! })
  }

  @Test
  fun listTicketForSale_updatesListingFields() = runTest {
    val updateSlot = slot<Map<String, Any?>>()
    every { mockDocument.update(capture(updateSlot)) } returns Tasks.forResult(null)

    val result = repository.listTicketForSale("ticket-123", 150.0)

    assertTrue(result.isSuccess)
    assertEquals(TicketState.LISTED.name, updateSlot.captured["state"])
    assertEquals(150.0, updateSlot.captured["listingPrice"] as Double, 0.0)
    assertNotNull(updateSlot.captured["listedAt"])
  }

  @Test
  fun listTicketForSale_rejectsNonPositivePrice() = runTest {
    val result = repository.listTicketForSale("ticket-123", 0.0)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalArgumentException)
  }

  @Test
  fun cancelTicketListing_resetsListingFields() = runTest {
    val updateSlot = slot<Map<String, Any?>>()
    every { mockDocument.update(capture(updateSlot)) } returns Tasks.forResult(null)

    val result = repository.cancelTicketListing("ticket-123")

    assertTrue(result.isSuccess)
    assertEquals(TicketState.ISSUED.name, updateSlot.captured["state"])
    assertNull(updateSlot.captured["listingPrice"])
    assertNull(updateSlot.captured["listedAt"])
  }

  @Test
  fun purchaseListedTicket_transfersOwnershipAndClearsListing() = runTest {
    val listedTicket =
        createTicket(
            ticketId = "ticket-42",
            ownerId = "seller",
            state = TicketState.LISTED,
            listingPrice = 40.0,
            version = 3)

    val updateSlot = slot<Map<String, Any?>>()
    every { mockSnapshot.toObject(Ticket::class.java) } returns listedTicket
    every { mockTransaction.update(any<DocumentReference>(), capture(updateSlot)) } returns
        mockTransaction

    val result = repository.purchaseListedTicket(listedTicket.ticketId, "buyer-99")

    assertTrue(result.isSuccess)
    assertEquals("buyer-99", updateSlot.captured["ownerId"])
    assertEquals(TicketState.TRANSFERRED.name, updateSlot.captured["state"])
    assertNull(updateSlot.captured["listingPrice"])
    assertNull(updateSlot.captured["listedAt"])
    assertEquals(listedTicket.version + 1, updateSlot.captured["version"])
  }

  @Test
  fun purchaseListedTicket_rejectsNonListedTicket() = runTest {
    val notListedTicket =
        createTicket(
            ticketId = "ticket-99",
            ownerId = "seller",
            state = TicketState.ISSUED,
            listingPrice = null)

    every { mockSnapshot.toObject(Ticket::class.java) } returns notListedTicket
    every { mockTransaction.update(any<DocumentReference>(), any<Map<String, Any?>>()) } returns
        mockTransaction

    val result = repository.purchaseListedTicket(notListedTicket.ticketId, "buyer-99")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("not listed") == true)
  }

  @Test
  fun purchaseListedTicket_rejectsPurchasingOwnTicket() = runTest {
    val listedTicket =
        createTicket(
            ticketId = "ticket-100",
            ownerId = "buyer-99",
            state = TicketState.LISTED,
            listingPrice = 60.0)

    every { mockSnapshot.toObject(Ticket::class.java) } returns listedTicket
    every { mockTransaction.update(any<DocumentReference>(), any<Map<String, Any?>>()) } returns
        mockTransaction

    val result = repository.purchaseListedTicket(listedTicket.ticketId, "buyer-99")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("own ticket") == true)
  }
}
