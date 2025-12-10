package ch.onepass.onepass.utils

import ch.onepass.onepass.model.event.EventTag

/**
 * Utility object for normalizing and comparing tags with case-insensitive matching.
 *
 * Provides functionality to match event tags against target tags, handling both enum names and
 * display values with normalization.
 */
object TagNormalizer {
  /**
   * Checks if an event tag matches any of the target tags, considering both enum names and display
   * values.
   *
   * @param eventTag The tag from an event to check for matches
   * @param targetTags The set of tags to match against (typically from filter selections)
   * @return true if the event tag matches any of the target tags, false otherwise
   */
  fun matchesTag(eventTag: String, targetTags: Set<String>): Boolean {
    val normalizedEventTag = eventTag.trim()

    // Try to find the EventTag for this event tag
    val matchingEventTag =
        EventTag.entries.find {
          it.displayValue.equals(normalizedEventTag, ignoreCase = true) ||
              it.name.equals(normalizedEventTag, ignoreCase = true)
        }

    return if (matchingEventTag != null) {
      // Check if any target tag matches either the enum name or display value
      targetTags.any { targetTag ->
        targetTag.equals(matchingEventTag.displayValue, ignoreCase = true) ||
            targetTag.equals(matchingEventTag.name, ignoreCase = true)
      }
    } else {
      // Direct comparison for non-EventTag tags
      targetTags.any { targetTag -> targetTag.equals(normalizedEventTag, ignoreCase = true) }
    }
  }
}
