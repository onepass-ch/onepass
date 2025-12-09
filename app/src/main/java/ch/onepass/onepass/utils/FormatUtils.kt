package ch.onepass.onepass.utils

import java.util.Locale

object FormatUtils {

  /**
   * Formats large numbers into a compact string with suffixes (K, M). Useful for follower counts,
   * likes, etc.
   *
   * Examples: 500 -> "500" 1200 -> "1.2K" 1000000 -> "1M"
   */
  fun formatCompactNumber(count: Int): String {
    return when {
      count < 1000 -> count.toString()
      count < 1_000_000 -> {
        val thousands = count / 1000.0
        if (thousands % 1 == 0.0) {
          "${thousands.toInt()}K"
        } else {
          // Rounds to 1 decimal place, handles edge cases like 999.95 rounding to 1000.0
          val capped = thousands.coerceAtMost(999.9)
          String.format(Locale.US, "%.1fK", capped)
        }
      }
      else -> {
        val millions = count / 1_000_000.0
        if (millions % 1 == 0.0) {
          "${millions.toInt()}M"
        } else {
          String.format(Locale.US, "%.1fM", millions)
        }
      }
    }
  }

  /** Formats a price value with currency. Returns "FREE" if price is 0. */
  fun formatPrice(price: Number, currency: String = "CHF"): String {
    val priceDouble = price.toDouble()
    return if (priceDouble <= 0.0) {
      "FREE"
    } else {
      // Automatically formats decimal places (e.g., 25.0 -> 25, 25.5 -> 25.5)
      val formattedPrice =
          if (priceDouble % 1 == 0.0) {
            priceDouble.toInt().toString()
          } else {
            String.format(Locale.US, "%.2f", priceDouble)
          }
      "$currency $formattedPrice"
    }
  }

  /**
   * Formats a price value with compact notation (K/M suffix) for values greater than 1000.
   * Rounds to 2 decimal places for large numbers.
   * Returns "FREE" if price is 0 or negative.
   *
   * Examples: 0 -> "FREE" 500 -> "CHF 500" 1500 -> "CHF 1.50K" 1234.56 -> "CHF 1.23K"
   * 2500000 -> "CHF 2.50M"
   */
  fun formatPriceCompact(price: Number, currency: String = "CHF"): String {
    val priceDouble = price.toDouble()
    return if (priceDouble <= 0.0) {
      "FREE"
    } else {
      val formattedPrice = when {
        priceDouble < 1000 -> {
          // For values less than 1000, format normally with up to 2 decimals
          if (priceDouble % 1 == 0.0) {
            priceDouble.toInt().toString()
          } else {
            String.format(Locale.US, "%.2f", priceDouble)
          }
        }
        priceDouble < 1_000_000 -> {
          // For values >= 1000 and < 1,000,000, format with K suffix and 2 decimal places
          val thousands = priceDouble / 1000.0
          String.format(Locale.US, "%.2fK", thousands)
        }
        else -> {
          // For values >= 1,000,000, format with M suffix and 2 decimal places
          val millions = priceDouble / 1_000_000.0
          String.format(Locale.US, "%.2fM", millions)
        }
      }
      "$currency $formattedPrice"
    }
  }
}
