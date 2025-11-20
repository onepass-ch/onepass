package ch.onepass.onepass.model.event

import ch.onepass.onepass.model.map.Location
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.util.Calendar
import java.util.Date
import org.junit.Assert
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
    Assert.assertEquals("EPFL", event.displayLocation)
  }

  @Test
  fun displayLocationReturnsUnknownWhenLocationIsNull() {
    val event = createTestEvent().copy(location = null)
    Assert.assertEquals("Unknown Location", event.displayLocation)
  }

  @Test
  fun displayDateTimeFormatsCorrectly() {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.DECEMBER, 25, 18, 0, 0)

    val event = createTestEvent().copy(startTime = Timestamp(calendar.time))

    val displayDateTime = event.displayDateTime
    Assert.assertTrue(displayDateTime.contains("December 25, 2025"))
    Assert.assertTrue(displayDateTime.contains("6:00 PM"))
  }

  @Test
  fun displayDateTimeHandlesNullStartTime() {
    val event = createTestEvent().copy(startTime = null)
    Assert.assertEquals("Date not set", event.displayDateTime)
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
    Assert.assertEquals(15u, event.lowestPrice)
  }

  @Test
  fun lowestPriceReturnsZeroForEmptyTiers() {
    val event = createTestEvent(pricingTiers = emptyList())
    Assert.assertEquals(0u, event.lowestPrice)
  }

  @Test
  fun imageUrlReturnsFirstImage() {
    val event =
        createTestEvent(
            images = listOf("https://example.com/image1.jpg", "https://example.com/image2.jpg"))
    Assert.assertEquals("https://example.com/image1.jpg", event.imageUrl)
  }

  @Test
  fun imageUrlReturnsEmptyStringWhenNoImages() {
    val event = createTestEvent(images = emptyList())
    Assert.assertEquals("", event.imageUrl)
  }

  @Test
  fun isSoldOutReturnsTrueWhenNoTicketsRemaining() {
    val event = createTestEvent(ticketsRemaining = 0)
    Assert.assertTrue(event.isSoldOut)
  }

  @Test
  fun isSoldOutReturnsFalseWhenTicketsRemaining() {
    val event = createTestEvent(ticketsRemaining = 10)
    Assert.assertFalse(event.isSoldOut)
  }

  @Test
  fun isPublishedReturnsTrueForPublishedStatus() {
    val event = createTestEvent(status = EventStatus.PUBLISHED)
    Assert.assertTrue(event.isPublished)
  }

  @Test
  fun isPublishedReturnsFalseForNonPublishedStatus() {
    val draftEvent = createTestEvent(status = EventStatus.DRAFT)
    Assert.assertFalse(draftEvent.isPublished)

    val closedEvent = createTestEvent(status = EventStatus.CLOSED)
    Assert.assertFalse(closedEvent.isPublished)
  }

  @Test
  fun canResellReturnsTrueWhenConditionsMet() {
    val event =
        createTestEvent(status = EventStatus.PUBLISHED, ticketsIssued = 50, ticketsRedeemed = 30)
    Assert.assertTrue(event.canResell)
  }

  @Test
  fun canResellReturnsFalseWhenNotPublished() {
    val event =
        createTestEvent(status = EventStatus.DRAFT, ticketsIssued = 50, ticketsRedeemed = 30)
    Assert.assertFalse(event.canResell)
  }

  @Test
  fun canResellReturnsFalseWhenAllTicketsRedeemed() {
    val event =
        createTestEvent(status = EventStatus.PUBLISHED, ticketsIssued = 50, ticketsRedeemed = 50)
    Assert.assertFalse(event.canResell)
  }

  @Test
  fun eventStatusEnumHasAllExpectedValues() {
    val statuses = EventStatus.values()
    Assert.assertEquals(4, statuses.size)
    Assert.assertTrue(statuses.contains(EventStatus.DRAFT))
    Assert.assertTrue(statuses.contains(EventStatus.PUBLISHED))
    Assert.assertTrue(statuses.contains(EventStatus.CLOSED))
    Assert.assertTrue(statuses.contains(EventStatus.CANCELLED))
  }

  @Test
  fun pricingTierHasCorrectDefaults() {
    val tier = PricingTier()
    Assert.assertEquals("", tier.name)
    Assert.assertEquals(0.0, tier.price, 0.01)
    Assert.assertEquals(0, tier.quantity)
    Assert.assertEquals(0, tier.remaining)
  }

  @Test
  fun eventHasCorrectDefaults() {
    val event = Event()
    Assert.assertEquals("", event.eventId)
    Assert.assertEquals("", event.title)
    Assert.assertEquals("", event.description)
    Assert.assertEquals("", event.organizerId)
    Assert.assertEquals("", event.organizerName)
    Assert.assertEquals(EventStatus.PUBLISHED, event.status)
    Assert.assertNull(event.location)
    Assert.assertNull(event.startTime)
    Assert.assertNull(event.endTime)
    Assert.assertEquals(0, event.capacity)
    Assert.assertEquals(0, event.ticketsRemaining)
    Assert.assertEquals(0, event.ticketsIssued)
    Assert.assertEquals(0, event.ticketsRedeemed)
    Assert.assertEquals("CHF", event.currency)
    Assert.assertTrue(event.pricingTiers.isEmpty())
    Assert.assertTrue(event.images.isEmpty())
    Assert.assertTrue(event.tags.isEmpty())
    Assert.assertNull(event.createdAt)
    Assert.assertNull(event.updatedAt)
    Assert.assertNull(event.deletedAt)
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
    Assert.assertEquals("this is a test title", event.titleLower)
  }

  @Test
  fun titleLower_handlesAlreadyLowercaseTitles() {
    val event = createEventWithTitle("already lower")
    Assert.assertEquals("already lower", event.titleLower)
  }

  @Test
  fun titleLower_handlesMixedCaseTitles() {
    val event = createEventWithTitle("MixedCase AND UPPER")
    Assert.assertEquals("mixedcase and upper", event.titleLower)
  }

  @Test
  fun titleLower_handlesEmptyTitles() {
    val event = createEventWithTitle("")
    Assert.assertEquals("", event.titleLower)
  }

  @Test
  fun titleLower_handlesTitlesWithNumbersAndSymbols() {
    val event = createEventWithTitle("Event 123!@#")
    Assert.assertEquals("event 123!@#", event.titleLower)
  }
}
