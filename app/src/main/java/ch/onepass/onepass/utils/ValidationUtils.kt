package ch.onepass.onepass.utils

import android.util.Patterns
import java.util.regex.Pattern

object ValidationUtils {

  private val REGEX_WEBSITE_URL = Pattern.compile("""^https?://[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}.*$""")

  private val REGEX_PHONE_E164 = Pattern.compile("""^\+\d{1,4}\d{4,14}$""")

  fun isValidEmail(email: String): Boolean {
    return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
  }

  fun isValidUrl(url: String): Boolean {
    if (url.isBlank()) return false
    // Ensure scheme is present for stricter validation if needed,
    // or rely on Patterns.WEB_URL for broader matching.
    val withScheme =
        if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
    return REGEX_WEBSITE_URL.matcher(withScheme).matches()
  }

  fun isValidPhone(phone: String): Boolean {
    // Expects E.164 format (e.g. +41...)
    return phone.isNotBlank() && REGEX_PHONE_E164.matcher(phone).matches()
  }

  fun isPositiveNumber(value: String): Boolean {
    return value.toDoubleOrNull()?.let { it >= 0 } == true
  }

  fun isPositiveInteger(value: String): Boolean {
    return value.toIntOrNull()?.let { it > 0 } == true
  }
}
