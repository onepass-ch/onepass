package ch.onepass.onepass.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatUtilsTest {

  @Test
  fun formatCompactNumber_lessThanThousand() {
    assertEquals("500", FormatUtils.formatCompactNumber(500))
    assertEquals("999", FormatUtils.formatCompactNumber(999))
  }

  @Test
  fun formatCompactNumber_thousands() {
    assertEquals("1K", FormatUtils.formatCompactNumber(1000))
    assertEquals("1.5K", FormatUtils.formatCompactNumber(1500))
    assertEquals("10K", FormatUtils.formatCompactNumber(10000))
    // Rounding check: 1200 -> 1.2K
    assertEquals("1.2K", FormatUtils.formatCompactNumber(1240))
    // Rounding check: 1290 -> 1.2K (floor/format logic dependent) or 1.3K
    // Based on implementation String.format("%.1f") rounds half up.
    // 1290 / 1000.0 = 1.29 -> 1.3K
    assertEquals("1.3K", FormatUtils.formatCompactNumber(1290))
  }

  @Test
  fun formatCompactNumber_millions() {
    assertEquals("1M", FormatUtils.formatCompactNumber(1_000_000))
    assertEquals("2.5M", FormatUtils.formatCompactNumber(2_500_000))
    // Integer million
    assertEquals("2M", FormatUtils.formatCompactNumber(2_000_000))
  }

  @Test
  fun formatPrice_zeroOrNegative_returnsFree() {
    assertEquals("FREE", FormatUtils.formatPrice(0))
    assertEquals("FREE", FormatUtils.formatPrice(-5.0))
  }

  @Test
  fun formatPrice_integerValue() {
    assertEquals("CHF 25", FormatUtils.formatPrice(25))
    assertEquals("CHF 100", FormatUtils.formatPrice(100.0))
  }

  @Test
  fun formatPrice_decimalValue() {
    assertEquals("CHF 25.50", FormatUtils.formatPrice(25.5))
    assertEquals("CHF 10.99", FormatUtils.formatPrice(10.99))
  }

  @Test
  fun formatPrice_customCurrency() {
    assertEquals("USD 50", FormatUtils.formatPrice(50, "USD"))
  }
}
