package ch.onepass.onepass.utils

import ch.onepass.onepass.model.event.EventTag

/**
 * Utility object for normalizing and comparing tags with case-insensitive matching.
 *
 * Provides functionality to match event tags against target tags, handling both enum names and
 * display values with normalization.
 */
object TagNormalizer {

  // Lazy-initialized lookup maps for O(1) tag resolution
  private val displayValueMap: Map<String, EventTag> by lazy {
    EventTag.entries.associateBy { it.displayValue.lowercase() }
  }

  private val nameMap: Map<String, EventTag> by lazy {
    EventTag.entries.associateBy { it.name.lowercase() }
  }

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

    // Early return for empty tags
    if (normalizedEventTag.isEmpty()) return false

    val normalizedLowercase = normalizedEventTag.lowercase()

    // O(1) lookup
    val matchingEventTag = displayValueMap[normalizedLowercase] ?: nameMap[normalizedLowercase]

    return if (matchingEventTag != null) {
      // Check if any target tag matches either the enum name or display value
      targetTags.any { targetTag ->
        val targetLowercase = targetTag.lowercase()
        targetLowercase == matchingEventTag.displayValue.lowercase() ||
            targetLowercase == matchingEventTag.name.lowercase()
      }
    } else {
      // Direct comparison for non-EventTag tags
      targetTags.any { targetTag -> targetTag.equals(normalizedEventTag, ignoreCase = true) }
    }
  }
}
