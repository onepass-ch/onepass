package ch.onepass.onepass.utils

import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.eventfilters.EventFilters

/** Shared filtering utility used by Feed and Map screens. */
object EventFilteringUtils {

  /**
   * Apply filters locally to the given events.
   *
   * Filters events based on region, date range, tags and availability (hide sold out).
   *
   * @param events The list of [Event]s to be filtered.
   * @param filters The [EventFilters] to apply.
   * @return The filtered list of [Event]s.
   */
  fun applyFiltersLocally(events: List<Event>, filters: EventFilters): List<Event> {
    return events.filter { event ->
      val regionMatch =
          filters.region?.let { region ->
            event.location?.region.equals(region, ignoreCase = true) ||
                event.location?.name?.contains(region, ignoreCase = true) == true
          } ?: true
      val dateMatch =
          filters.dateRange?.let { range ->
            event.startTime?.toDate()?.time?.let { eventTime -> eventTime in range } ?: false
          } ?: true
      val tagMatch =
          if (filters.selectedTags.isNotEmpty()) {
            event.tags.any { eventTag -> TagNormalizer.matchesTag(eventTag, filters.selectedTags) }
          } else {
            true
          }

      val availabilityMatch = !filters.hideSoldOut || !event.isSoldOut

      regionMatch && dateMatch && tagMatch && availabilityMatch
    }
  }
}
