package ch.onepass.onepass.utils

import ch.onepass.onepass.model.event.EventTag
import org.junit.Assert.*
import org.junit.Test

class TagNormalizerTest {

  @Test
  fun matchesTag_withExactDisplayValueMatch_returnsTrue() {
    val eventTag = EventTag.TECH.displayValue // "Technology"
    val targetTags = setOf("Technology")

    assertTrue(TagNormalizer.matchesTag(eventTag, targetTags))
  }

  @Test
  fun matchesTag_withCaseInsensitiveDisplayValueMatch_returnsTrue() {
    val eventTag = "technology" // lowercase
    val targetTags = setOf("Technology")

    assertTrue(TagNormalizer.matchesTag(eventTag, targetTags))
  }

  @Test
  fun matchesTag_withEnumNameMatch_returnsTrue() {
    val eventTag = "TECH" // enum name
    val targetTags = setOf("TECH")

    assertTrue(TagNormalizer.matchesTag(eventTag, targetTags))
  }

  @Test
  fun matchesTag_withEnumNameCaseInsensitive_returnsTrue() {
    val eventTag = "tech" // lowercase enum name
    val targetTags = setOf("TECH")

    assertTrue(TagNormalizer.matchesTag(eventTag, targetTags))
  }

  @Test
  fun matchesTag_withMixedTargetTags_matchesEither() {
    val eventTag = EventTag.TECH.displayValue // "Technology"

    // Test with display value in target
    assertTrue(TagNormalizer.matchesTag(eventTag, setOf("Technology")))
    // Test with enum name in target
    assertTrue(TagNormalizer.matchesTag(eventTag, setOf("TECH")))
    // Test with both in target
    assertTrue(TagNormalizer.matchesTag(eventTag, setOf("Technology", "TECH")))
  }

  @Test
  fun matchesTag_withNonEventTag_directComparisonWorks() {
    val eventTag = "CustomTag" // not in EventTag enum
    val targetTags = setOf("CustomTag")

    assertTrue(TagNormalizer.matchesTag(eventTag, targetTags))
  }

  @Test
  fun matchesTag_withNonEventTagCaseInsensitive_returnsTrue() {
    val eventTag = "customtag" // not in EventTag enum, lowercase
    val targetTags = setOf("CUSTOMTAG")

    assertTrue(TagNormalizer.matchesTag(eventTag, targetTags))
  }

  @Test
  fun matchesTag_withNoMatch_returnsFalse() {
    val eventTag = "Technology"
    val targetTags = setOf("Music", "Arts & Culture")

    assertFalse(TagNormalizer.matchesTag(eventTag, targetTags))
  }

  @Test
  fun matchesTag_withEmptyTargetTags_returnsFalse() {
    val eventTag = "Technology"
    val targetTags = emptySet<String>()

    assertFalse(TagNormalizer.matchesTag(eventTag, targetTags))
  }

  @Test
  fun matchesTag_withWhitespaceInEventTag_trimsAndMatches() {
    val eventTag = "  Technology  "
    val targetTags = setOf("Technology")

    assertTrue(TagNormalizer.matchesTag(eventTag, targetTags))
  }

  @Test
  fun matchesTag_withMultipleTargetTags_matchesAny() {
    val eventTag = "Technology"
    val targetTags = setOf("Music", "Arts & Culture", "Technology", "Sports & Fitness")

    assertTrue(TagNormalizer.matchesTag(eventTag, targetTags))
  }

  @Test
  fun matchesTag_comprehensiveEventTagEnumTest() {
    // Test all EventTag entries to ensure comprehensive coverage
    EventTag.entries.forEach { eventTag ->
      // Test matching by display value
      assertTrue(
          "EventTag ${eventTag.name} should match its display value '${eventTag.displayValue}'",
          TagNormalizer.matchesTag(eventTag.displayValue, setOf(eventTag.displayValue)))

      // Test matching by enum name
      assertTrue(
          "EventTag ${eventTag.name} should match its enum name '${eventTag.name}'",
          TagNormalizer.matchesTag(eventTag.displayValue, setOf(eventTag.name)))

      // Test case-insensitive matching for display value
      assertTrue(
          "EventTag ${eventTag.name} should match case-insensitive display value",
          TagNormalizer.matchesTag(eventTag.displayValue.lowercase(), setOf(eventTag.displayValue)))

      // Test case-insensitive matching for enum name
      assertTrue(
          "EventTag ${eventTag.name} should match case-insensitive enum name",
          TagNormalizer.matchesTag(eventTag.displayValue, setOf(eventTag.name.lowercase())))
    }
  }

  @Test
  fun matchesTag_withEventTagDisplayValueMatchesEnumNameInTarget() {
    // When event has "Technology" (display value) and target filter has "TECH" (enum name)
    val eventTag = "Technology"
    val targetTags = setOf("TECH")

    assertTrue(TagNormalizer.matchesTag(eventTag, targetTags))
  }

  @Test
  fun matchesTag_withEventTagEnumNameMatchesDisplayValueInTarget() {
    // When event has "TECH" (enum name) and target filter has "Technology" (display value)
    val eventTag = "TECH"
    val targetTags = setOf("Technology")

    assertTrue(TagNormalizer.matchesTag(eventTag, targetTags))
  }

  @Test
  fun matchesTag_handlesSpecialCharactersInDisplayValues() {
    // Test tags with special characters like "&" and "/"
    val eventTag = "Business & Finance"
    val targetTags = setOf("Business & Finance")

    assertTrue(TagNormalizer.matchesTag(eventTag, targetTags))

    // Test Workshop / Class
    val eventTag2 = "Workshop / Class"
    val targetTags2 = setOf("Workshop / Class")

    assertTrue(TagNormalizer.matchesTag(eventTag2, targetTags2))
  }

  @Test
  fun matchesTag_withAmpersandVariations() {
    // Test that "Business & Finance" matches "Business & Finance" exactly
    val eventTag = "Business & Finance"
    val targetTags = setOf("Business & Finance")

    assertTrue(TagNormalizer.matchesTag(eventTag, targetTags))
  }

  @Test
  fun matchesTag_returnsFalseForPartialDisplayValueMatch() {
    // Should not match partial strings
    val eventTag = "Technology Conference" // Not an exact EventTag
    val targetTags = setOf("Technology")

    // Since "Technology Conference" is not a known EventTag, it falls back to direct comparison
    // Direct comparison with "Technology" should fail because they're not equal
    assertFalse(TagNormalizer.matchesTag(eventTag, targetTags))
  }

  @Test
  fun matchesTag_handlesEmptyOrWhitespaceEventTag() {
    // This test ensures the function doesn't crash on null/empty strings
    assertFalse(TagNormalizer.matchesTag("", setOf("Technology")))
    assertFalse(TagNormalizer.matchesTag("   ", setOf("Technology")))
  }

  @Test
  fun matchesTag_withEventTagEnumEntryMatchesItself() {
    // Direct test using the EventTag enum constant
    EventTag.entries.forEach { eventTag ->
      // Test that the enum constant name matches itself
      assertTrue(
          "EventTag ${eventTag.name} should match itself by name",
          TagNormalizer.matchesTag(eventTag.name, setOf(eventTag.name)))
    }
  }
}
