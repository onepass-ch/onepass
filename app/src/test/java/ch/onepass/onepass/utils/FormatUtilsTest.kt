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
  fun formatPriceCompact_zeroOrNegative_returnsFree() {
    assertEquals("FREE", FormatUtils.formatPriceCompact(0))
    assertEquals("FREE", FormatUtils.formatPriceCompact(-5.0))
  }

  @Test
  fun formatPriceCompact_formatsCorrectly() {
    // Values less than 1000
    assertEquals("CHF 500", FormatUtils.formatPriceCompact(500))
    assertEquals("CHF 999.99", FormatUtils.formatPriceCompact(999.99))
    assertEquals("CHF 25.50", FormatUtils.formatPriceCompact(25.5))

    // Values >= 1000 and < 1,000,000 with K suffix and 2 decimals
    assertEquals("CHF 1.50K", FormatUtils.formatPriceCompact(1500))
    assertEquals("CHF 1.23K", FormatUtils.formatPriceCompact(1234.56))
    assertEquals("CHF 10.00K", FormatUtils.formatPriceCompact(10000))
    assertEquals("CHF 12.35K", FormatUtils.formatPriceCompact(12345.67))
    assertEquals("CHF 999.99K", FormatUtils.formatPriceCompact(999990))

    // Values >= 1,000,000 with M suffix and 2 decimals
    assertEquals("CHF 1.00M", FormatUtils.formatPriceCompact(1_000_000))
    assertEquals("CHF 2.50M", FormatUtils.formatPriceCompact(2_500_000))
    assertEquals("CHF 10.00M", FormatUtils.formatPriceCompact(10_000_000))
    assertEquals("CHF 12.35M", FormatUtils.formatPriceCompact(12_350_000))
  }

  @Test
  fun formatPriceCompact_customCurrency() {
    assertEquals("USD 50", FormatUtils.formatPriceCompact(50, "USD"))
    assertEquals("EUR 1.50K", FormatUtils.formatPriceCompact(1500, "EUR"))
    assertEquals("GBP 2.50M", FormatUtils.formatPriceCompact(2_500_000, "GBP"))
  }
}
