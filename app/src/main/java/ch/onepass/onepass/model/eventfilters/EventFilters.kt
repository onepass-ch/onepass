package ch.onepass.onepass.model.eventfilters

import ch.onepass.onepass.model.event.EventTag
import java.util.Calendar

/**
 * Represents filter criteria for events.
 *
 * @property region Selected Swiss region/canton, null means all regions
 * @property dateRange Selected date range as timestamps, null means all dates
 * @property hideSoldOut Whether to hide sold out events
 * @property selectedTags Set of tags to filter events by, empty set means no tag filter
 * @property hasActiveFilters Indicates whether any filter criteria is currently active
 */
data class EventFilters(
    val region: String? = null,
    val dateRange: ClosedRange<Long>? = null,
    val hideSoldOut: Boolean = false,
    val selectedTags: Set<String> = emptySet()
) {
  /**
   * Indicates whether any filter criteria is currently active.
   *
   * Returns true if any of the following conditions are met:
   * - [region] is not null
   * - [dateRange] is not null
   * - [hideSoldOut] is true
   * - [selectedTags] is not empty
   */
  val hasActiveFilters: Boolean
    get() = region != null || dateRange != null || hideSoldOut || selectedTags.isNotEmpty()
}

object TagCategories {
  const val THEMES = "Theme"
  const val FORMATS = "Format"
  const val SETTINGS = "Setting & Cost"

  val ALL_CATEGORIES = listOf(THEMES, FORMATS, SETTINGS)

  fun getTagsByCategory(category: String): List<String> {
    return EventTag.categories[category]?.map { it.displayValue } ?: emptyList()
  }
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
