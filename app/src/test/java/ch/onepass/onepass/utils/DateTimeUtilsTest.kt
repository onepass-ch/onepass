package ch.onepass.onepass.utils

import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class DateTimeUtilsTest {

  // Helper to create a fixed date: Oct 14, 2025 14:30:00
  private fun getFixedDate(
      year: Int = 2025,
      month: Int = Calendar.OCTOBER,
      day: Int = 14,
      hour: Int = 14,
      minute: Int = 30
  ): Date {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, day, hour, minute, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.time
  }

  @Test
  fun formatDisplayDate_Timestamp_returnsFormattedString() {
    val date = getFixedDate()
    val timestamp = Timestamp(date)
    val result = DateTimeUtils.formatDisplayDate(timestamp)
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
    assert(result.contains("2025"))
  }

  @Test
  fun formatMemberSince_null_returnsEmpty() {
    assertEquals("", DateTimeUtils.formatMemberSince(null))
  }

  @Test
  fun formatNotificationDate_isToday_returnsTime() {
    val now = getFixedDate(hour = 18, minute = 0)
    val notificationTime = getFixedDate(hour = 14, minute = 30)

    val result = DateTimeUtils.formatNotificationDate(notificationTime, now)
    assertEquals("14:30", result)
  }

  @Test
  fun formatNotificationDate_isDifferentDay_returnsDate() {
    val now = getFixedDate(day = 15)
    val notificationTime = getFixedDate(day = 14)

    val result = DateTimeUtils.formatNotificationDate(notificationTime, now)
    assert(result.contains("14"))
    assert(!result.contains(":"))
  }

  @Test
  fun formatNotificationDate_isDifferentYear_returnsDate() {
    val now = getFixedDate(year = 2026)
    val notificationTime = getFixedDate(year = 2025)

    val result = DateTimeUtils.formatNotificationDate(notificationTime, now)
    assert(result.contains("14"))
    assert(!result.contains(":"))
  }

  @Test
  fun formatNotificationDate_TimestampOverload_worksCorrectly() {
    val now = getFixedDate(hour = 18, minute = 0)
    val notificationTime = getFixedDate(hour = 14, minute = 30)
    val timestamp = Timestamp(notificationTime)

    val result = DateTimeUtils.formatNotificationDate(timestamp, now)
    assertEquals("14:30", result)
  }

  @Test
  fun formatNotificationDate_TimestampOverload_null_returnsEmpty() {
    val timestamp: Timestamp? = null
    val result = DateTimeUtils.formatNotificationDate(timestamp)
    assertEquals("", result)
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
