package ch.onepass.onepass.model.event

import ch.onepass.onepass.model.eventfilters.DateRangePresets
import ch.onepass.onepass.model.eventfilters.EventFilters
import ch.onepass.onepass.model.eventfilters.SwissRegions
import java.util.Calendar
import org.junit.Assert
import org.junit.Test

/** Unit tests for the EventFilters data class and related functionality. */
class EventFiltersTest {
  @Test
  fun eventFiltersHasCorrectDefaults() {
    val filters = EventFilters()
    Assert.assertNull(filters.region)
    Assert.assertNull(filters.dateRange)
    Assert.assertFalse(filters.hideSoldOut)
  }

  @Test
  fun eventFiltersCanBeCreatedWithValues() {
    val dateRange = 1000L..2000L
    val filters = EventFilters(region = "Zurich", dateRange = dateRange, hideSoldOut = true)
    Assert.assertEquals("Zurich", filters.region)
    Assert.assertEquals(dateRange, filters.dateRange)
    Assert.assertTrue(filters.hideSoldOut)
  }

  @Test
  fun hasActiveFiltersReturnsFalseWhenNoFiltersSet() {
    val filters = EventFilters()
    Assert.assertFalse(filters.hasActiveFilters)
  }

  @Test
  fun hasActiveFiltersReturnsTrueWhenRegionIsSet() {
    val filters = EventFilters(region = "Bern")
    Assert.assertTrue(filters.hasActiveFilters)
  }

  @Test
  fun hasActiveFiltersReturnsTrueWhenDateRangeIsSet() {
    val filters = EventFilters(dateRange = 1000L..2000L)
    Assert.assertTrue(filters.hasActiveFilters)
  }

  @Test
  fun hasActiveFiltersReturnsTrueWhenHideSoldOutIsTrue() {
    val filters = EventFilters(hideSoldOut = true)
    Assert.assertTrue(filters.hasActiveFilters)
  }

  @Test
  fun hasActiveFiltersReturnsTrueWhenMultipleFiltersAreSet() {
    val filters = EventFilters(region = "Geneva", dateRange = 1000L..2000L, hideSoldOut = true)
    Assert.assertTrue(filters.hasActiveFilters)
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
    Assert.assertEquals(expectedRegions, SwissRegions.REGIONS)
    Assert.assertEquals("All Regions", SwissRegions.ALL_REGIONS)
    Assert.assertEquals(26, SwissRegions.REGIONS.size)
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
    Assert.assertEquals(expectedStart, todayRange.start)
    Assert.assertEquals(expectedEnd, todayRange.endInclusive)
    Assert.assertTrue(todayRange.start <= todayRange.endInclusive)
  }

  @Test
  fun dateRangePresetsGetNextWeekendRangeReturnsCorrectRange() {
    val weekendRange = DateRangePresets.getNextWeekendRange()
    // Verify it's a valid range
    Assert.assertTrue(weekendRange.start <= weekendRange.endInclusive)
    // The range should be 2 days (Saturday to Sunday)
    val duration = weekendRange.endInclusive - weekendRange.start
    val twoDaysInMillis = 2 * 24 * 60 * 60 * 1000L - 1 // 2 days minus 1 millisecond
    Assert.assertTrue(duration <= twoDaysInMillis)
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
    Assert.assertEquals(expectedStart, next7DaysRange.start)
    Assert.assertEquals(expectedEnd, next7DaysRange.endInclusive)
    // Verify the range spans exactly 7 days minus 1 millisecond
    val duration = next7DaysRange.endInclusive - next7DaysRange.start
    val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L - 1
    Assert.assertEquals(sevenDaysInMillis, duration)
  }

  @Test
  fun eventFiltersCopyWithNewValues() {
    val original = EventFilters(region = "Zurich", dateRange = 1000L..2000L, hideSoldOut = false)
    val updated = original.copy(region = "Geneva", dateRange = 3000L..4000L, hideSoldOut = true)
    Assert.assertEquals("Geneva", updated.region)
    Assert.assertEquals(3000L..4000L, updated.dateRange)
    Assert.assertTrue(updated.hideSoldOut)
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
    Assert.assertEquals("EventFilters with same values should be equal", filters1, filters2)
    Assert.assertNotEquals(
        "EventFilters with different regions should not be equal", filters1, filters3)
  }

  @Test
  fun eventFiltersToStringIncludesImportantFields() {
    val filters = EventFilters(region = "Zurich", dateRange = 1000L..2000L, hideSoldOut = true)
    val stringRepresentation = filters.toString()
    Assert.assertTrue("Should include region", stringRepresentation.contains("Zurich"))
    Assert.assertTrue("Should include date range", stringRepresentation.contains("1000..2000"))
    Assert.assertTrue(
        "Should include hideSoldOut", stringRepresentation.contains("hideSoldOut=true"))
  }

  @Test
  fun eventFiltersWithNullRegion() {
    val filters = EventFilters(region = null)

    Assert.assertNull(filters.region)
    Assert.assertFalse(filters.hasActiveFilters)
  }

  @Test
  fun eventFiltersWithNullDateRange() {
    val filters = EventFilters(dateRange = null)

    Assert.assertNull(filters.dateRange)
    Assert.assertFalse(filters.hasActiveFilters)
  }

  @Test
  fun eventFiltersWithEmptySwissRegions() {
    // Verify that SwissRegions object is properly initialized
    Assert.assertNotNull(SwissRegions.REGIONS)
    Assert.assertTrue(SwissRegions.REGIONS.isNotEmpty())
    Assert.assertTrue(SwissRegions.REGIONS.contains("Zurich"))
    Assert.assertTrue(SwissRegions.REGIONS.contains("Geneva"))
  }

  @Test
  fun dateRangePresetsProduceValidRanges() {
    val todayRange = DateRangePresets.getTodayRange()
    val weekendRange = DateRangePresets.getNextWeekendRange()
    val next7DaysRange = DateRangePresets.getNext7DaysRange()
    // All ranges should have start <= end
    Assert.assertTrue(todayRange.start <= todayRange.endInclusive)
    Assert.assertTrue(weekendRange.start <= weekendRange.endInclusive)
    Assert.assertTrue(next7DaysRange.start <= next7DaysRange.endInclusive)
    // Ranges should not be negative
    Assert.assertTrue(todayRange.start >= 0)
    Assert.assertTrue(weekendRange.start >= 0)
    Assert.assertTrue(next7DaysRange.start >= 0)
  }

  @Test
  fun eventFiltersHashCodeConsistency() {
    val filters1 = EventFilters(region = "Zurich", hideSoldOut = true)
    val filters2 = EventFilters(region = "Zurich", hideSoldOut = true)
    Assert.assertEquals(filters1.hashCode(), filters2.hashCode())
  }

  @Test
  fun eventFiltersComponentFunctions() {
    val filters = EventFilters(region = "Bern", dateRange = 1000L..2000L, hideSoldOut = true)
    Assert.assertEquals("Bern", filters.component1())
    Assert.assertEquals(1000L..2000L, filters.component2())
    Assert.assertTrue(filters.component3())
  }

  @Test
  fun swissRegionsConstantsAreAccessible() {
    // Test that the constants can be accessed without issues
    Assert.assertNotNull(SwissRegions.ALL_REGIONS)
    Assert.assertNotNull(SwissRegions.REGIONS)
    // Test that ALL_REGIONS is not included in the REGIONS list
    Assert.assertFalse(SwissRegions.REGIONS.contains(SwissRegions.ALL_REGIONS))
  }
}
