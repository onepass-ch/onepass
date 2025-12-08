package ch.onepass.onepass.utils

/** Utility class for sanitizing user input against injection attacks and malicious patterns. */
object InputSanitizer {

  // Dangerous patterns to detect and reject
  private val HTML_SCRIPT_PATTERN =
      Regex(
          "<script|<img|<iframe|javascript:|onerror=|onclick=|onload=|on\\w+=",
          RegexOption.IGNORE_CASE)
  private val SQL_INJECTION_PATTERN =
      Regex(
          "(;\\s*(DROP|DELETE|INSERT|UPDATE|CREATE|ALTER|UNION|SELECT)|--\\s*$|/\\*|\\*/)",
          RegexOption.IGNORE_CASE)
  private val XSS_PATTERN = Regex("javascript:|data:|vbscript:|file:", RegexOption.IGNORE_CASE)

  /** Removes control characters, null bytes, and invisible Unicode characters */
  private fun removeControlCharacters(input: String): String {
    return input
        .replace(
            Regex("[\\x00-\\x08\\x0B-\\x0C\\x0E-\\x1F\\x7F]"),
            "") // Control chars except \n (0x0A) and \t (0x09)
        .replace("\u200B", "") // Zero-width space
        .replace("\u200C", "") // Zero-width non-joiner
        .replace("\u200D", "") // Zero-width joiner
        .replace("\u202E", "") // Right-to-left override
        .replace("\u202D", "") // Left-to-right override
        .replace("\u00AD", "") // Soft hyphen
  }

  /** Detects suspicious patterns that look like injection attacks */
  private fun containsDangerousPatterns(input: String): Boolean {
    return HTML_SCRIPT_PATTERN.containsMatchIn(input) ||
        SQL_INJECTION_PATTERN.containsMatchIn(input) ||
        XSS_PATTERN.containsMatchIn(input)
  }

  /** Sanitizes and validates text against common attacks */
  private fun validateAgainstAttacks(input: String) {
    require(!containsDangerousPatterns(input)) { "Input contains potentially dangerous patterns" }
  }

  /**
   * Sanitizes title input:
   * - No newlines
   * - Removes dangerous patterns
   * - Collapses multiple spaces
   */
  fun sanitizeTitle(input: String): String {
    var sanitized =
        input
            .replace("\n", " ") // Remove newlines
            .replace("\r", "") // Remove carriage returns

    sanitized = removeControlCharacters(sanitized)
    sanitized = sanitized.replace(Regex(" {2,}"), " ") // Multiple spaces -> single
    sanitized = sanitized.replace(Regex("\t+"), " ") // Tabs -> space

    // Validate against attacks
    validateAgainstAttacks(sanitized)

    return sanitized
  }

  /**
   * Sanitizes description/multiline text:
   * - Allows newlines and spaces
   * - Removes dangerous patterns
   * - Limits consecutive newlines to max 2
   */
  fun sanitizeDescription(input: String): String {
    var sanitized = input

    sanitized = removeControlCharacters(sanitized)
    sanitized = sanitized.replace(Regex(" {2,}"), " ") // Multiple spaces -> single
    sanitized = sanitized.replace(Regex("\t+"), " ") // Tabs -> space
    sanitized = sanitized.replace(Regex("\n\n\n+"), "\n\n") // Max 2 newlines

    // Validate against attacks
    validateAgainstAttacks(sanitized)

    return sanitized
  }

  /**
   * Sanitizes price input:
   * - Only digits and decimal point
   * - No +/- signs
   * - No scientific notation (e, E)
   * - Max 2 decimal places
   */
  fun sanitizePrice(input: String): String {
    var sanitized = input.replace(Regex("[^\\d.]"), "") // Only digits and decimal

    // Keep only first decimal point
    val parts = sanitized.split(".")
    sanitized =
        if (parts.size > 1) {
          "${parts[0]}.${parts.drop(1).joinToString("")}"
        } else {
          parts[0]
        }

    // Validate decimal places (max 2)
    if (sanitized.contains(".")) {
      val decimalParts = sanitized.split(".")
      if (decimalParts[1].length > 2) {
        sanitized = "${decimalParts[0]}.${decimalParts[1].take(2)}"
      }
    }

    return sanitized
  }

  /**
   * Sanitizes capacity input:
   * - Only positive integers
   * - No leading zeros
   * - No +/- signs
   */
  fun sanitizeCapacity(input: String): String {
    val sanitized =
        input
            .replace(Regex("\\D"), "") // Only digits
            .replace(Regex("^0+"), "") // Remove leading zeros

    return sanitized.ifEmpty { "" }
  }
}
