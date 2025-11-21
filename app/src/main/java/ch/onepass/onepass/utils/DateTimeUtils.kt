package ch.onepass.onepass.utils

import com.google.firebase.Timestamp
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Centralized utilities for Date and Time formatting to ensure consistency across the app. */
object DateTimeUtils {

  // Patterns used throughout the app
  const val PATTERN_DISPLAY_FULL = "MMMM dd, yyyy • h:mm a" // Used in Event cards/details
  const val PATTERN_DATE_INPUT = "dd/MM/yyyy" // Used in forms
  const val PATTERN_TIME_INPUT = "HH:mm" // Used in forms
  const val PATTERN_MONTH_YEAR = "MMM.yyyy" // Used in Organization profile

  /**
   * Formats a Firestore Timestamp to the standard display format (e.g., "October 14, 2025 • 2:30
   * PM"). Returns "Date not set" if the timestamp is null.
   */
  fun formatDisplayDate(timestamp: Timestamp?): String {
    return timestamp?.toDate()?.let { format(it, PATTERN_DISPLAY_FULL) } ?: "Date not set"
  }

  /** Formats a Date object to the standard display format. */
  fun formatDisplayDate(date: Date?): String {
    return date?.let { format(it, PATTERN_DISPLAY_FULL) } ?: "Date not set"
  }

  /** Formats a Date to a specific pattern string. */
  fun format(date: Date, pattern: String): String {
    return getFormatter(pattern).format(date)
  }

  /** Formats a Timestamp to the "MMM.yyyy" format used in organization cards. */
  fun formatMemberSince(timestamp: Timestamp?): String {
    return timestamp?.toDate()?.let { format(it, PATTERN_MONTH_YEAR) } ?: ""
  }

  /**
   * Parses a date string and a time string (from input forms) into a single Firestore Timestamp.
   * Returns null if parsing fails.
   * * @param dateStr Date in "dd/MM/yyyy"
   *
   * @param timeStr Time in "HH:mm"
   */
  fun parseDateAndTime(dateStr: String, timeStr: String): Timestamp? {
    if (dateStr.isBlank() || timeStr.isBlank()) return null
    return try {
      val combinedPattern = "$PATTERN_DATE_INPUT $PATTERN_TIME_INPUT"
      val formatter = getFormatter(combinedPattern)
      val date = formatter.parse("$dateStr $timeStr")
      date?.let { Timestamp(it) }
    } catch (e: ParseException) {
      null
    }
  }

  /** Helper to get a SimpleDateFormat instance with default locale. */
  private fun getFormatter(pattern: String): SimpleDateFormat {
    return SimpleDateFormat(pattern, Locale.getDefault())
  }
}
