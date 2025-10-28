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

// Date range presets
object DateRangePresets {
  fun getTodayRange(): ClosedRange<Long> {
    val calendar =
        Calendar.getInstance().apply {
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
    val start = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_MONTH, 1)
    val end = calendar.timeInMillis - 1
    return start..end
  }

  fun getThisWeekendRange(): ClosedRange<Long> {
    val calendar =
        Calendar.getInstance().apply {
          // Find coming Saturday
          val currentDay = get(Calendar.DAY_OF_WEEK)
          val daysUntilSaturday = Calendar.SATURDAY - currentDay
          if (daysUntilSaturday < 0) add(Calendar.DAY_OF_MONTH, daysUntilSaturday + 7)
          else add(Calendar.DAY_OF_MONTH, daysUntilSaturday)
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

  fun getNext7DaysRange(): ClosedRange<Long> {
    val calendar =
        Calendar.getInstance().apply {
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
    val start = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_MONTH, 7)
    val end = calendar.timeInMillis - 1
    return start..end
  }
}
