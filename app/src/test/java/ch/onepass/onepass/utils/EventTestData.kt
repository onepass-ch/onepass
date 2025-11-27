package ch.onepass.onepass.utils

import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.event.PricingTier
import ch.onepass.onepass.model.map.Location
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.util.Calendar

/**
 * Helper object for creating test event data. Provides factory methods for common test scenarios.
 */
object EventTestData {

  /** Creates a basic test event with customizable parameters. */
  fun createTestEvent(
      eventId: String = "test-event-${System.currentTimeMillis()}",
      title: String = "Test Event",
      description: String = "Test Description",
      organizerId: String = "test-organizer",
      organizerName: String = "Test Organizer",
      status: EventStatus = EventStatus.PUBLISHED,
      location: Location? = createTestLocation(),
      startTime: Timestamp? = createFutureTimestamp(daysFromNow = 30),
      endTime: Timestamp? = createFutureTimestamp(daysFromNow = 30, addHours = 2),
      capacity: Int = 100,
      ticketsRemaining: Int = 100,
      ticketsIssued: Int = 0,
      ticketsRedeemed: Int = 0,
      currency: String = "CHF",
      pricingTiers: List<PricingTier> =
          listOf(PricingTier("General", 25.0, 80, 80), PricingTier("VIP", 50.0, 20, 20)),
      images: List<String> = listOf("https://example.com/image.jpg"),
      tags: List<String> = listOf("test", "event"),
      createdAt: Timestamp? = Timestamp.now(),
      updatedAt: Timestamp? = Timestamp.now(),
      deletedAt: Timestamp? = null
  ): Event {
    return Event(
        eventId = eventId,
        title = title,
        description = description,
        organizerId = organizerId,
        organizerName = organizerName,
        status = status,
        location = location,
        startTime = startTime,
        endTime = endTime,
        capacity = capacity,
        ticketsRemaining = ticketsRemaining,
        ticketsIssued = ticketsIssued,
        ticketsRedeemed = ticketsRedeemed,
        currency = currency,
        pricingTiers = pricingTiers,
        images = images,
        tags = tags,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt)
  }

  /** Creates a published event that's ready for ticket sales. */
  fun createPublishedEvent(
      eventId: String = "published-${System.currentTimeMillis()}",
      organizerId: String = "test-organizer",
      capacity: Int = 100
  ): Event {
    return createTestEvent(
        eventId = eventId,
        title = "Published Test Event",
        organizerId = organizerId,
        status = EventStatus.PUBLISHED,
        capacity = capacity,
        ticketsRemaining = capacity)
  }

  /** Creates a draft event that hasn't been published yet. */
  fun createDraftEvent(
      eventId: String = "draft-${System.currentTimeMillis()}",
      organizerId: String = "test-organizer"
  ): Event {
    return createTestEvent(
        eventId = eventId,
        title = "Draft Test Event",
        organizerId = organizerId,
        status = EventStatus.DRAFT)
  }

  /** Creates a sold-out event. */
  fun createSoldOutEvent(
      eventId: String = "soldout-${System.currentTimeMillis()}",
      organizerId: String = "test-organizer",
      capacity: Int = 50
  ): Event {
    return createTestEvent(
        eventId = eventId,
        title = "Sold Out Event",
        organizerId = organizerId,
        status = EventStatus.PUBLISHED,
        capacity = capacity,
        ticketsRemaining = 0,
        ticketsIssued = capacity)
  }

  /** Creates a cancelled event. */
  fun createCancelledEvent(
      eventId: String = "cancelled-${System.currentTimeMillis()}",
      organizerId: String = "test-organizer"
  ): Event {
    return createTestEvent(
        eventId = eventId,
        title = "Cancelled Event",
        organizerId = organizerId,
        status = EventStatus.CANCELLED)
  }

  /** Creates a closed (past) event. */
  fun createClosedEvent(
      eventId: String = "closed-${System.currentTimeMillis()}",
      organizerId: String = "test-organizer"
  ): Event {
    return createTestEvent(
        eventId = eventId,
        title = "Closed Event",
        organizerId = organizerId,
        status = EventStatus.CLOSED,
        startTime = createPastTimestamp(daysAgo = 7),
        endTime = createPastTimestamp(daysAgo = 7, addHours = 2))
  }

  /** Creates a free event with no pricing. */
  fun createFreeEvent(
      eventId: String = "free-${System.currentTimeMillis()}",
      organizerId: String = "test-organizer",
      capacity: Int = 200
  ): Event {
    return createTestEvent(
        eventId = eventId,
        title = "Free Event",
        organizerId = organizerId,
        pricingTiers = listOf(PricingTier("Free", 0.0, capacity, capacity)),
        capacity = capacity,
        tags = listOf("free", "community"))
  }

  /** Creates an event with partial ticket sales. */
  fun createPartiallyBookedEvent(
      eventId: String = "partial-${System.currentTimeMillis()}",
      organizerId: String = "test-organizer",
      capacity: Int = 100,
      ticketsSold: Int = 40,
      ticketsRedeemed: Int = 20
  ): Event {
    return createTestEvent(
        eventId = eventId,
        title = "Partially Booked Event",
        organizerId = organizerId,
        status = EventStatus.PUBLISHED,
        capacity = capacity,
        ticketsRemaining = capacity - ticketsSold,
        ticketsIssued = ticketsSold,
        ticketsRedeemed = ticketsRedeemed)
  }

  /** Creates a test location. */
  fun createTestLocation(
      latitude: Double = 46.5191,
      longitude: Double = 6.5668,
      name: String = "EPFL",
      region: String = "Vaud"
  ): Location {
    return Location(coordinates = GeoPoint(latitude, longitude), name = name, region = region)
  }

  /** Creates a list of test locations for testing location-based queries. */
  fun createTestLocations(): List<Location> {
    return listOf(
        Location(GeoPoint(46.5191, 6.5668), "EPFL", "Vaud"),
        Location(GeoPoint(46.5210, 6.5790), "Lausanne Center", "Vaud"),
        Location(GeoPoint(46.5200, 6.5800), "City Library", "Vaud"),
        Location(GeoPoint(47.3769, 8.5417), "Zurich HB", "Zurich"),
        Location(GeoPoint(46.2044, 6.1432), "Geneva Airport", "Geneva"))
  }

  /** Creates a timestamp for a future date. */
  fun createFutureTimestamp(daysFromNow: Int = 30, addHours: Int = 0): Timestamp {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, daysFromNow)
    calendar.add(Calendar.HOUR_OF_DAY, addHours)
    return Timestamp(calendar.time)
  }

  /** Creates a timestamp for a past date. */
  fun createPastTimestamp(daysAgo: Int = 7, addHours: Int = 0): Timestamp {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, -daysAgo)
    calendar.add(Calendar.HOUR_OF_DAY, addHours)
    return Timestamp(calendar.time)
  }

  /** Creates a specific timestamp from date components. */
  fun createTimestamp(
      year: Int,
      month: Int, // 0-based (Calendar.JANUARY = 0)
      day: Int,
      hour: Int = 0,
      minute: Int = 0,
      second: Int = 0
  ): Timestamp {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, day, hour, minute, second)
    calendar.set(Calendar.MILLISECOND, 0)
    return Timestamp(calendar.time)
  }

  /** Creates a list of test pricing tiers. */
  fun createTestPricingTiers(): List<PricingTier> {
    return listOf(
        PricingTier("Early Bird", 15.0, 20, 20),
        PricingTier("General", 25.0, 60, 60),
        PricingTier("VIP", 50.0, 20, 20))
  }

  /** Creates a list of multiple test events for bulk testing. */
  fun createTestEvents(
      count: Int = 10,
      organizerId: String = "test-organizer",
      baseTitle: String = "Test Event"
  ): List<Event> {
    return (1..count).map { index ->
      createTestEvent(
          eventId = "test-event-$index",
          title = "$baseTitle $index",
          organizerId = organizerId,
          startTime = createFutureTimestamp(daysFromNow = index * 7),
          tags = if (index % 2 == 0) listOf("tech", "networking") else listOf("music", "arts"))
    }
  }

  /** Creates events with different statuses for testing status-based queries. */
  fun createEventsWithDifferentStatuses(organizerId: String = "test-organizer"): List<Event> {
    return listOf(
        createDraftEvent(organizerId = organizerId),
        createPublishedEvent(organizerId = organizerId),
        createClosedEvent(organizerId = organizerId),
        createCancelledEvent(organizerId = organizerId))
  }

  /** Creates events at different locations for testing location-based queries. */
  fun createEventsAtDifferentLocations(organizerId: String = "test-organizer"): List<Event> {
    val locations = createTestLocations()
    return locations.mapIndexed { index, location ->
      createTestEvent(
          eventId = "location-event-$index",
          title = "Event at ${location.name}",
          organizerId = organizerId,
          location = location)
    }
  }
}
