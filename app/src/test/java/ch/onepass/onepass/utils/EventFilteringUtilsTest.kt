package ch.onepass.onepass.utils

import ch.onepass.onepass.model.eventfilters.EventFilters
import com.google.firebase.Timestamp
import java.util.*
import org.junit.Assert.*
import org.junit.Test

class EventFilteringUtilsTest {

  @Test
  fun applyFiltersLocally_withNoFilters_returnsAllEvents() {
    val events =
        listOf(
            EventTestData.createPublishedEvent(),
            EventTestData.createSoldOutEvent(),
            EventTestData.createDraftEvent())

    val filters = EventFilters()
    val result = EventFilteringUtils.applyFiltersLocally(events, filters)

    assertEquals(events.size, result.size)
  }

  @Test
  fun applyFiltersLocally_withRegionFilter_matchesRegion() {
    val zurichEvent =
        EventTestData.createPublishedEvent()
            .copy(location = EventTestData.createTestLocation(region = "Zurich"))
    val vaudEvent =
        EventTestData.createPublishedEvent()
            .copy(location = EventTestData.createTestLocation(region = "Vaud"))
    val events = listOf(zurichEvent, vaudEvent)

    val filters = EventFilters(region = "Zurich")
    val result = EventFilteringUtils.applyFiltersLocally(events, filters)

    assertEquals(1, result.size)
    assertEquals("Zurich", result[0].location?.region)
  }

  @Test
  fun applyFiltersLocally_withRegionFilter_matchesLocationName() {
    val event =
        EventTestData.createPublishedEvent()
            .copy(
                location =
                    EventTestData.createTestLocation(
                        name = "Zurich Convention Center", region = "Zurich"))
    val events = listOf(event)

    val filters = EventFilters(region = "Convention Center")
    val result = EventFilteringUtils.applyFiltersLocally(events, filters)

    assertEquals(1, result.size)
  }

  @Test
  fun applyFiltersLocally_withDateRangeFilter_matchesEventsInRange() {
    val now = Calendar.getInstance()
    val futureDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 10) }

    val pastEvent =
        EventTestData.createPublishedEvent()
            .copy(startTime = EventTestData.createPastTimestamp(daysAgo = 5))
    val futureEvent =
        EventTestData.createPublishedEvent().copy(startTime = Timestamp(futureDate.time))
    val events = listOf(pastEvent, futureEvent)

    val dateRange =
        EventTestData.createPastTimestamp(daysAgo = 7).toDate().time..EventTestData
                .createPastTimestamp(daysAgo = 3)
                .toDate()
                .time
    val filters = EventFilters(dateRange = dateRange)
    val result = EventFilteringUtils.applyFiltersLocally(events, filters)

    assertEquals(1, result.size)
  }

  @Test
  fun applyFiltersLocally_withTagFilter_matchesTaggedEvents() {
    val techEvent =
        EventTestData.createPublishedEvent().copy(tags = listOf("TECHNOLOGY", "Networking"))
    val musicEvent = EventTestData.createPublishedEvent().copy(tags = listOf("Music", "Arts"))
    val events = listOf(techEvent, musicEvent)

    val filters = EventFilters(selectedTags = setOf("Technology"))
    val result = EventFilteringUtils.applyFiltersLocally(events, filters)

    assertEquals(1, result.size)
    assertTrue(result[0].tags.contains("TECHNOLOGY"))
  }

  @Test
  fun applyFiltersLocally_withHideSoldOutFilter_excludesSoldOutEvents() {
    val availableEvent = EventTestData.createPublishedEvent(capacity = 100)
    val soldOutEvent = EventTestData.createSoldOutEvent(capacity = 50)
    val events = listOf(availableEvent, soldOutEvent)

    val filters = EventFilters(hideSoldOut = true)
    val result = EventFilteringUtils.applyFiltersLocally(events, filters)

    assertEquals(1, result.size)
    assertFalse(result[0].isSoldOut)
  }

  @Test
  fun applyFiltersLocally_withCombinedFilters_appliesAllFilters() {
    val zurichTechEvent =
        EventTestData.createPublishedEvent()
            .copy(
                location = EventTestData.createTestLocation(region = "Zurich"),
                tags = listOf("Technology"),
                startTime = EventTestData.createFutureTimestamp(daysFromNow = 5))
    val zurichMusicEvent =
        EventTestData.createPublishedEvent()
            .copy(
                location = EventTestData.createTestLocation(region = "Zurich"),
                tags = listOf("Music"),
                startTime = EventTestData.createFutureTimestamp(daysFromNow = 15))
    val vaudTechEvent =
        EventTestData.createSoldOutEvent()
            .copy(
                location = EventTestData.createTestLocation(region = "Vaud"),
                tags = listOf("Technology"))
    val events = listOf(zurichTechEvent, zurichMusicEvent, vaudTechEvent)

    val dateRangeStart = EventTestData.createFutureTimestamp(daysFromNow = 1).toDate().time
    val dateRangeEnd = EventTestData.createFutureTimestamp(daysFromNow = 10).toDate().time
    val filters =
        EventFilters(
            region = "Zurich",
            dateRange = dateRangeStart..dateRangeEnd,
            selectedTags = setOf("Technology"),
            hideSoldOut = true)

    val result = EventFilteringUtils.applyFiltersLocally(events, filters)

    assertEquals(1, result.size)
    assertEquals("Zurich", result[0].location?.region)
    assertTrue(result[0].tags.contains("Technology"))
    assertFalse(result[0].isSoldOut)
  }

  @Test
  fun applyFiltersLocally_withNullLocation_handlesGracefully() {
    val event = EventTestData.createPublishedEvent().copy(location = null)
    val events = listOf(event)

    val filters = EventFilters(region = "Zurich")
    val result = EventFilteringUtils.applyFiltersLocally(events, filters)

    assertEquals(0, result.size)
  }

  @Test
  fun applyFiltersLocally_withNullStartTime_handlesGracefully() {
    val event = EventTestData.createPublishedEvent().copy(startTime = null)
    val events = listOf(event)

    val dateRange = 0L..System.currentTimeMillis()
    val filters = EventFilters(dateRange = dateRange)
    val result = EventFilteringUtils.applyFiltersLocally(events, filters)

    assertEquals(0, result.size)
  }
}
