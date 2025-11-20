package ch.onepass.onepass.utils

import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class DateTimeUtilsTest {

  // Helper to create a fixed date: Oct 14, 2025 14:30:00
  private fun getFixedDate(): Date {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.OCTOBER, 14, 14, 30, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.time
  }

  @Test
  fun formatDisplayDate_Timestamp_returnsFormattedString() {
    val date = getFixedDate()
    val timestamp = Timestamp(date)
    // Note: Exact output depends on the test environment's Locale, assuming Locale.US/Default
    // similar structure
    // We verify the format structure generally or mocking Locale if strictness is needed.
    // For standard unit tests, SimpleDateFormat uses system default.
    val result = DateTimeUtils.formatDisplayDate(timestamp)

    // Just checking it's not the fallback
    assert(result != "Date not set")
    assert(result.contains("2025"))
  }

  @Test
  fun formatDisplayDate_Timestamp_null_returnsFallback() {
    val timestamp: Timestamp? = null
    assertEquals("Date not set", DateTimeUtils.formatDisplayDate(timestamp))
  }

  @Test
  fun formatDisplayDate_Date_returnsFormattedString() {
    val date = getFixedDate()
    val result = DateTimeUtils.formatDisplayDate(date)
    assert(result.contains("2025"))
  }

  @Test
  fun formatDisplayDate_Date_null_returnsFallback() {
    val date: Date? = null
    assertEquals("Date not set", DateTimeUtils.formatDisplayDate(date))
  }

  @Test
  fun formatMemberSince_returnsMonthYear() {
    val date = getFixedDate() // Oct 2025
    val timestamp = Timestamp(date)
    val result = DateTimeUtils.formatMemberSince(timestamp)
    // Pattern MMM.yyyy -> Oct.2025 or Oct 2025 depending on Locale
    assert(result.contains("2025"))
  }

  @Test
  fun formatMemberSince_null_returnsEmpty() {
    assertEquals("", DateTimeUtils.formatMemberSince(null))
  }

  @Test
  fun parseDateAndTime_validInput_returnsTimestamp() {
    val dateStr = "14/10/2025"
    val timeStr = "14:30"

    val result = DateTimeUtils.parseDateAndTime(dateStr, timeStr)

    assert(result != null)
    val cal = Calendar.getInstance()
    cal.time = result!!.toDate()

    assertEquals(2025, cal.get(Calendar.YEAR))
    assertEquals(Calendar.OCTOBER, cal.get(Calendar.MONTH))
    assertEquals(14, cal.get(Calendar.DAY_OF_MONTH))
    assertEquals(14, cal.get(Calendar.HOUR_OF_DAY))
    assertEquals(30, cal.get(Calendar.MINUTE))
  }

  @Test
  fun parseDateAndTime_emptyInput_returnsNull() {
    assertEquals(null, DateTimeUtils.parseDateAndTime("", "14:30"))
    assertEquals(null, DateTimeUtils.parseDateAndTime("14/10/2025", ""))
  }

  @Test
  fun parseDateAndTime_invalidFormat_returnsNull() {
    assertEquals(null, DateTimeUtils.parseDateAndTime("invalid", "date"))
  }
}
