package ch.onepass.onepass.model.eventFilters

import java.util.Calendar
import org.junit.Assert.*
import org.junit.Test

/** Unit tests for the EventFilters data class and related functionality. */
class EventFiltersTest {
  @Test
  fun eventFiltersHasCorrectDefaults() {
    val filters = EventFilters()
    assertNull(filters.region)
    assertNull(filters.dateRange)
    assertFalse(filters.hideSoldOut)
  }

  @Test
  fun eventFiltersCanBeCreatedWithValues() {
    val dateRange = 1000L..2000L
    val filters = EventFilters(region = "Zurich", dateRange = dateRange, hideSoldOut = true)
    assertEquals("Zurich", filters.region)
    assertEquals(dateRange, filters.dateRange)
    assertTrue(filters.hideSoldOut)
  }

  @Test
  fun hasActiveFiltersReturnsFalseWhenNoFiltersSet() {
    val filters = EventFilters()
    assertFalse(filters.hasActiveFilters)
  }

  @Test
  fun hasActiveFiltersReturnsTrueWhenRegionIsSet() {
    val filters = EventFilters(region = "Bern")
    assertTrue(filters.hasActiveFilters)
  }

  @Test
  fun hasActiveFiltersReturnsTrueWhenDateRangeIsSet() {
    val filters = EventFilters(dateRange = 1000L..2000L)
    assertTrue(filters.hasActiveFilters)
  }

  @Test
  fun hasActiveFiltersReturnsTrueWhenHideSoldOutIsTrue() {
    val filters = EventFilters(hideSoldOut = true)
    assertTrue(filters.hasActiveFilters)
  }

  @Test
  fun hasActiveFiltersReturnsTrueWhenMultipleFiltersAreSet() {
    val filters = EventFilters(region = "Geneva", dateRange = 1000L..2000L, hideSoldOut = true)
    assertTrue(filters.hasActiveFilters)
  }

  @Test
  fun swissRegionsContainsAllExpectedRegions() {
    val expectedRegions =
        listOf(
            "Zurich",
            "Bern",
            "Lucerne",
            "Uri",
            "Schwyz",
            "Obwalden",
            "Nidwalden",
            "Glarus",
            "Zug",
            "Fribourg",
            "Solothurn",
            "Basel-Stadt",
            "Basel-Landschaft",
            "Schaffhausen",
            "Appenzell Ausserrhoden",
            "Appenzell Innerrhoden",
            "St. Gallen",
            "Graubünden",
            "Aargau",
            "Thurgau",
            "Ticino",
            "Vaud",
            "Valais",
            "Neuchâtel",
            "Geneva",
            "Jura",
        )
    assertEquals(expectedRegions, SwissRegions.REGIONS)
    assertEquals("All Regions", SwissRegions.ALL_REGIONS)
    assertEquals(26, SwissRegions.REGIONS.size)
  }

  @Test
  fun dateRangePresetsGetTodayRangeReturnsCorrectRange() {
    val todayRange = DateRangePresets.getTodayRange()
    val calendar =
        Calendar.getInstance().apply {
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
    val expectedStart = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_MONTH, 1)
    val expectedEnd = calendar.timeInMillis - 1
    assertEquals(expectedStart, todayRange.start)
    assertEquals(expectedEnd, todayRange.endInclusive)
    assertTrue(todayRange.start <= todayRange.endInclusive)
  }

  @Test
  fun dateRangePresetsGetNextWeekendRangeReturnsCorrectRange() {
    val weekendRange = DateRangePresets.getNextWeekendRange()
    // Verify it's a valid range
    assertTrue(weekendRange.start <= weekendRange.endInclusive)
    // The range should be 2 days (Saturday to Sunday)
    val duration = weekendRange.endInclusive - weekendRange.start
    val twoDaysInMillis = 2 * 24 * 60 * 60 * 1000L - 1 // 2 days minus 1 millisecond
    assertTrue(duration <= twoDaysInMillis)
  }

  @Test
  fun dateRangePresetsGetNext7DaysRangeReturnsCorrectRange() {
    val next7DaysRange = DateRangePresets.getNext7DaysRange()
    val calendar =
        Calendar.getInstance().apply {
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
    val expectedStart = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_MONTH, 7)
    val expectedEnd = calendar.timeInMillis - 1
    assertEquals(expectedStart, next7DaysRange.start)
    assertEquals(expectedEnd, next7DaysRange.endInclusive)
    // Verify the range spans exactly 7 days minus 1 millisecond
    val duration = next7DaysRange.endInclusive - next7DaysRange.start
    val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L - 1
    assertEquals(sevenDaysInMillis, duration)
  }

  @Test
  fun eventFiltersCopyWithNewValues() {
    val original = EventFilters(region = "Zurich", dateRange = 1000L..2000L, hideSoldOut = false)
    val updated = original.copy(region = "Geneva", dateRange = 3000L..4000L, hideSoldOut = true)
    assertEquals("Geneva", updated.region)
    assertEquals(3000L..4000L, updated.dateRange)
    assertTrue(updated.hideSoldOut)
  }

  @Test
  fun eventFiltersEqualityBasedOnAllFields() {
    val filters1 = EventFilters(region = "Bern", dateRange = 1000L..2000L, hideSoldOut = true)
    val filters2 = EventFilters(region = "Bern", dateRange = 1000L..2000L, hideSoldOut = true)
    val filters3 =
        EventFilters(
            region = "Zurich", // Different region
            dateRange = 1000L..2000L,
            hideSoldOut = true,
        )
    assertEquals("EventFilters with same values should be equal", filters1, filters2)
    assertNotEquals("EventFilters with different regions should not be equal", filters1, filters3)
  }

  @Test
  fun eventFiltersToStringIncludesImportantFields() {
    val filters = EventFilters(region = "Zurich", dateRange = 1000L..2000L, hideSoldOut = true)
    val stringRepresentation = filters.toString()
    assertTrue("Should include region", stringRepresentation.contains("Zurich"))
    assertTrue("Should include date range", stringRepresentation.contains("1000..2000"))
    assertTrue("Should include hideSoldOut", stringRepresentation.contains("hideSoldOut=true"))
  }

  @Test
  fun eventFiltersWithNullRegion() {
    val filters = EventFilters(region = null)

    assertNull(filters.region)
    assertFalse(filters.hasActiveFilters)
  }

  @Test
  fun eventFiltersWithNullDateRange() {
    val filters = EventFilters(dateRange = null)

    assertNull(filters.dateRange)
    assertFalse(filters.hasActiveFilters)
  }

  @Test
  fun eventFiltersWithEmptySwissRegions() {
    // Verify that SwissRegions object is properly initialized
    assertNotNull(SwissRegions.REGIONS)
    assertTrue(SwissRegions.REGIONS.isNotEmpty())
    assertTrue(SwissRegions.REGIONS.contains("Zurich"))
    assertTrue(SwissRegions.REGIONS.contains("Geneva"))
  }

  @Test
  fun dateRangePresetsProduceValidRanges() {
    val todayRange = DateRangePresets.getTodayRange()
    val weekendRange = DateRangePresets.getNextWeekendRange()
    val next7DaysRange = DateRangePresets.getNext7DaysRange()
    // All ranges should have start <= end
    assertTrue(todayRange.start <= todayRange.endInclusive)
    assertTrue(weekendRange.start <= weekendRange.endInclusive)
    assertTrue(next7DaysRange.start <= next7DaysRange.endInclusive)
    // Ranges should not be negative
    assertTrue(todayRange.start >= 0)
    assertTrue(weekendRange.start >= 0)
    assertTrue(next7DaysRange.start >= 0)
  }

  @Test
  fun eventFiltersHashCodeConsistency() {
    val filters1 = EventFilters(region = "Zurich", hideSoldOut = true)
    val filters2 = EventFilters(region = "Zurich", hideSoldOut = true)
    assertEquals(filters1.hashCode(), filters2.hashCode())
  }

  @Test
  fun eventFiltersComponentFunctions() {
    val filters = EventFilters(region = "Bern", dateRange = 1000L..2000L, hideSoldOut = true)
    assertEquals("Bern", filters.component1())
    assertEquals(1000L..2000L, filters.component2())
    assertTrue(filters.component3())
  }

  @Test
  fun swissRegionsConstantsAreAccessible() {
    // Test that the constants can be accessed without issues
    assertNotNull(SwissRegions.ALL_REGIONS)
    assertNotNull(SwissRegions.REGIONS)
    // Test that ALL_REGIONS is not included in the REGIONS list
    assertFalse(SwissRegions.REGIONS.contains(SwissRegions.ALL_REGIONS))
  }
}
