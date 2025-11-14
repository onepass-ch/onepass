package ch.onepass.onepass.model.event

import ch.onepass.onepass.model.map.Location
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.util.Calendar
import java.util.Date
import org.junit.Assert.*
import org.junit.Test

class EventTest {

  private fun createTestEvent(
      status: EventStatus = EventStatus.PUBLISHED,
      ticketsRemaining: Int = 50,
      ticketsIssued: Int = 50,
      ticketsRedeemed: Int = 0,
      pricingTiers: List<PricingTier> = listOf(PricingTier("General", 25.0, 100, 100)),
      images: List<String> = listOf("https://example.com/image.jpg")
  ): Event {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.DECEMBER, 25, 18, 0, 0)

    return Event(
        eventId = "test-event-1",
        title = "Test Event",
        description = "Test Description",
        organizerId = "org-123",
        organizerName = "Test Organizer",
        status = status,
        location =
            Location(coordinates = GeoPoint(46.5191, 6.5668), name = "EPFL", region = "Vaud"),
        startTime = Timestamp(calendar.time),
        endTime = Timestamp(Date(calendar.timeInMillis + 3600000)),
        capacity = 100,
        ticketsRemaining = ticketsRemaining,
        ticketsIssued = ticketsIssued,
        ticketsRedeemed = ticketsRedeemed,
        pricingTiers = pricingTiers,
        images = images,
        tags = listOf("tech", "networking"))
  }

  @Test
  fun displayLocationReturnsLocationName() {
    val event = createTestEvent()
    assertEquals("EPFL", event.displayLocation)
  }

  @Test
  fun displayLocationReturnsUnknownWhenLocationIsNull() {
    val event = createTestEvent().copy(location = null)
    assertEquals("Unknown Location", event.displayLocation)
  }

  @Test
  fun displayDateTimeFormatsCorrectly() {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.DECEMBER, 25, 18, 0, 0)

    val event = createTestEvent().copy(startTime = Timestamp(calendar.time))

    val displayDateTime = event.displayDateTime
    assertTrue(displayDateTime.contains("December 25, 2025"))
    assertTrue(displayDateTime.contains("6:00 PM"))
  }

  @Test
  fun displayDateTimeHandlesNullStartTime() {
    val event = createTestEvent().copy(startTime = null)
    assertEquals("Date not set", event.displayDateTime)
  }

  @Test
  fun lowestPriceReturnsMinimumTierPrice() {
    val event =
        createTestEvent(
            pricingTiers =
                listOf(
                    PricingTier("Early Bird", 15.0, 20, 20),
                    PricingTier("General", 25.0, 50, 50),
                    PricingTier("VIP", 50.0, 30, 30)))
    assertEquals(15u, event.lowestPrice)
  }

  @Test
  fun lowestPriceReturnsZeroForEmptyTiers() {
    val event = createTestEvent(pricingTiers = emptyList())
    assertEquals(0u, event.lowestPrice)
  }

  @Test
  fun imageUrlReturnsFirstImage() {
    val event =
        createTestEvent(
            images = listOf("https://example.com/image1.jpg", "https://example.com/image2.jpg"))
    assertEquals("https://example.com/image1.jpg", event.imageUrl)
  }

  @Test
  fun imageUrlReturnsEmptyStringWhenNoImages() {
    val event = createTestEvent(images = emptyList())
    assertEquals("", event.imageUrl)
  }

  @Test
  fun isSoldOutReturnsTrueWhenNoTicketsRemaining() {
    val event = createTestEvent(ticketsRemaining = 0)
    assertTrue(event.isSoldOut)
  }

  @Test
  fun isSoldOutReturnsFalseWhenTicketsRemaining() {
    val event = createTestEvent(ticketsRemaining = 10)
    assertFalse(event.isSoldOut)
  }

  @Test
  fun isPublishedReturnsTrueForPublishedStatus() {
    val event = createTestEvent(status = EventStatus.PUBLISHED)
    assertTrue(event.isPublished)
  }

  @Test
  fun isPublishedReturnsFalseForNonPublishedStatus() {
    val draftEvent = createTestEvent(status = EventStatus.DRAFT)
    assertFalse(draftEvent.isPublished)

    val closedEvent = createTestEvent(status = EventStatus.CLOSED)
    assertFalse(closedEvent.isPublished)
  }

  @Test
  fun canResellReturnsTrueWhenConditionsMet() {
    val event =
        createTestEvent(status = EventStatus.PUBLISHED, ticketsIssued = 50, ticketsRedeemed = 30)
    assertTrue(event.canResell)
  }

  @Test
  fun canResellReturnsFalseWhenNotPublished() {
    val event =
        createTestEvent(status = EventStatus.DRAFT, ticketsIssued = 50, ticketsRedeemed = 30)
    assertFalse(event.canResell)
  }

  @Test
  fun canResellReturnsFalseWhenAllTicketsRedeemed() {
    val event =
        createTestEvent(status = EventStatus.PUBLISHED, ticketsIssued = 50, ticketsRedeemed = 50)
    assertFalse(event.canResell)
  }

  @Test
  fun eventStatusEnumHasAllExpectedValues() {
    val statuses = EventStatus.values()
    assertEquals(4, statuses.size)
    assertTrue(statuses.contains(EventStatus.DRAFT))
    assertTrue(statuses.contains(EventStatus.PUBLISHED))
    assertTrue(statuses.contains(EventStatus.CLOSED))
    assertTrue(statuses.contains(EventStatus.CANCELLED))
  }

  @Test
  fun pricingTierHasCorrectDefaults() {
    val tier = PricingTier()
    assertEquals("", tier.name)
    assertEquals(0.0, tier.price, 0.01)
    assertEquals(0, tier.quantity)
    assertEquals(0, tier.remaining)
  }

  @Test
  fun eventHasCorrectDefaults() {
    val event = Event()
    assertEquals("", event.eventId)
    assertEquals("", event.title)
    assertEquals("", event.description)
    assertEquals("", event.organizerId)
    assertEquals("", event.organizerName)
    assertEquals(EventStatus.PUBLISHED, event.status)
    assertNull(event.location)
    assertNull(event.startTime)
    assertNull(event.endTime)
    assertEquals(0, event.capacity)
    assertEquals(0, event.ticketsRemaining)
    assertEquals(0, event.ticketsIssued)
    assertEquals(0, event.ticketsRedeemed)
    assertEquals("CHF", event.currency)
    assertTrue(event.pricingTiers.isEmpty())
    assertTrue(event.images.isEmpty())
    assertTrue(event.tags.isEmpty())
    assertNull(event.createdAt)
    assertNull(event.updatedAt)
    assertNull(event.deletedAt)
  }

  private fun createEventWithTitle(title: String): Event {
    return Event(
        eventId = "test-1",
        title = title,
        description = "Test description",
        organizerId = "org-1",
        organizerName = "Test Org",
        status = EventStatus.PUBLISHED,
        location =
            Location(
                name = "Test Location", coordinates = GeoPoint(0.0, 0.0), region = "Test Region"),
        startTime = Timestamp(Date()),
        endTime = Timestamp(Date()),
        capacity = 100,
        ticketsIssued = 50,
        ticketsRedeemed = 10,
        pricingTiers = emptyList(),
        images = emptyList(),
        tags = emptyList())
  }

  @Test
  fun titleLower_convertsTitleToLowercaseCorrectly() {
    val event = createEventWithTitle("This is a Test Title")
    assertEquals("this is a test title", event.titleLower)
  }

  @Test
  fun titleLower_handlesAlreadyLowercaseTitles() {
    val event = createEventWithTitle("already lower")
    assertEquals("already lower", event.titleLower)
  }

  @Test
  fun titleLower_handlesMixedCaseTitles() {
    val event = createEventWithTitle("MixedCase AND UPPER")
    assertEquals("mixedcase and upper", event.titleLower)
  }

  @Test
  fun titleLower_handlesEmptyTitles() {
    val event = createEventWithTitle("")
    assertEquals("", event.titleLower)
  }

  @Test
  fun titleLower_handlesTitlesWithNumbersAndSymbols() {
    val event = createEventWithTitle("Event 123!@#")
    assertEquals("event 123!@#", event.titleLower)
  }
}
