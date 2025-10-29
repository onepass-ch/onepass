package ch.onepass.onepass.model.eventFilters

import java.util.Calendar

/**
 * Represents filter criteria for events.
 *
 * @property region Selected Swiss region/canton, null means all regions
 * @property dateRange Selected date range as timestamps, null means all dates
 * @property hideSoldOut Whether to hide sold out events
 */
data class EventFilters(
    val region: String? = null,
    val dateRange: ClosedRange<Long>? = null,
    val hideSoldOut: Boolean = false,
) {
  val hasActiveFilters: Boolean
    get() = region != null || dateRange != null || hideSoldOut
}

// Swiss regions constants
object SwissRegions {
  const val ALL_REGIONS = "All Regions"
  val REGIONS =
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
}

typealias DateRange = ClosedRange<Long>

// Date range presets
object DateRangePresets {
  private fun dayRange(startOffsetDays: Int, lengthDays: Int): DateRange {
    val calendar =
        Calendar.getInstance().apply {
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
          add(Calendar.DAY_OF_MONTH, startOffsetDays)
        }
    val start = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_MONTH, lengthDays)
    val end = calendar.timeInMillis - 1
    return start..end
  }

  fun getTodayRange() = dayRange(0, 1)

  fun getNext7DaysRange() = dayRange(0, 7)

  fun getNextWeekendRange(): ClosedRange<Long> {
    val calendar =
        Calendar.getInstance().apply {
          val currentDay = get(Calendar.DAY_OF_WEEK)
          val daysUntilNextSaturday = (Calendar.SATURDAY - currentDay) % 7
          add(Calendar.DAY_OF_MONTH, if (daysUntilNextSaturday == 0) 7 else daysUntilNextSaturday)
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
    val saturdayStart = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_MONTH, 2) // Sunday end
    val sundayEnd = calendar.timeInMillis - 1
    return saturdayStart..sundayEnd
  }
}
