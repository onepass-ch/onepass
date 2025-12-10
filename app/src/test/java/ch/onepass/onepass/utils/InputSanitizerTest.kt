package ch.onepass.onepass.utils

import org.junit.Assert.*
import org.junit.Test

class InputSanitizerTest {

  // ============ sanitizeTitle() Tests ============

  @Test
  fun sanitizeTitle_withValidInput_returnsCleanedText() {
    val result = InputSanitizer.sanitizeTitle("Amazing Event")
    assertEquals("Amazing Event", result)
  }

  @Test
  fun sanitizeTitle_withNewlines_convertsToSpaces() {
    val result = InputSanitizer.sanitizeTitle("Amazing\nEvent")
    assertEquals("Amazing Event", result)
  }

  @Test
  fun sanitizeTitle_withMultipleSpaces_collapsesToSingle() {
    val result = InputSanitizer.sanitizeTitle("Amazing    Event")
    assertEquals("Amazing Event", result)
  }

  @Test
  fun sanitizeTitle_withTabs_convertsToSpaces() {
    val result = InputSanitizer.sanitizeTitle("Amazing\t\tEvent")
    assertEquals("Amazing Event", result)
  }

  @Test
  fun sanitizeTitle_withCarriageReturn_removesIt() {
    val result = InputSanitizer.sanitizeTitle("Amazing\rEvent")
    assertEquals("AmazingEvent", result)
  }

  @Test
  fun sanitizeTitle_withZeroWidthSpace_removesIt() {
    val result = InputSanitizer.sanitizeTitle("Amazing\u200BEvent")
    assertEquals("AmazingEvent", result)
  }

  @Test
  fun sanitizeTitle_withHtmlScript_throwsException() {
    assertThrows(IllegalArgumentException::class.java) {
      InputSanitizer.sanitizeTitle("Event<script>alert(1)</script>")
    }
  }

  @Test
  fun sanitizeTitle_withImgTag_throwsException() {
    assertThrows(IllegalArgumentException::class.java) {
      InputSanitizer.sanitizeTitle("Event<img src=x>")
    }
  }

  @Test
  fun sanitizeTitle_withOnErrorHandler_throwsException() {
    assertThrows(IllegalArgumentException::class.java) {
      InputSanitizer.sanitizeTitle("Event onerror=alert(1)")
    }
  }

  @Test
  fun sanitizeTitle_withSqlDropTable_throwsException() {
    assertThrows(IllegalArgumentException::class.java) {
      InputSanitizer.sanitizeTitle("Event'; DROP TABLE users;")
    }
  }

  @Test
  fun sanitizeTitle_withSqlComment_throwsException() {
    assertThrows(IllegalArgumentException::class.java) { InputSanitizer.sanitizeTitle("Event --") }
  }

  @Test
  fun sanitizeTitle_withJavascriptProtocol_throwsException() {
    assertThrows(IllegalArgumentException::class.java) {
      InputSanitizer.sanitizeTitle("javascript:alert(1)")
    }
  }

  @Test
  fun sanitizeTitle_withDataProtocol_throwsException() {
    assertThrows(IllegalArgumentException::class.java) {
      InputSanitizer.sanitizeTitle("data:text/html,<script>alert(1)</script>")
    }
  }

  @Test
  fun sanitizeTitle_withVbscriptProtocol_throwsException() {
    assertThrows(IllegalArgumentException::class.java) {
      InputSanitizer.sanitizeTitle("vbscript:msgbox")
    }
  }

  @Test
  fun sanitizeTitle_withFileProtocol_throwsException() {
    assertThrows(IllegalArgumentException::class.java) {
      InputSanitizer.sanitizeTitle("file:///etc/passwd")
    }
  }

  // ============ sanitizeDescription() Tests ============

  @Test
  fun sanitizeDescription_withValidMultilineText_returnsCleanedText() {
    val input = "Line 1\nLine 2\nLine 3"
    val result = InputSanitizer.sanitizeDescription(input)
    assertEquals("Line 1\nLine 2\nLine 3", result)
  }

  @Test
  fun sanitizeDescription_withMultipleConsecutiveNewlines_limitsToTwo() {
    val input = "Line 1\n\n\n\nLine 2"
    val result = InputSanitizer.sanitizeDescription(input)
    assertEquals("Line 1\n\nLine 2", result)
  }

  @Test
  fun sanitizeDescription_withMultipleSpaces_collapsesToSingle() {
    val input = "Amazing    Event"
    val result = InputSanitizer.sanitizeDescription(input)
    assertEquals("Amazing Event", result)
  }

  @Test
  fun sanitizeDescription_withTabs_convertsToSpaces() {
    val input = "Amazing\t\tEvent"
    val result = InputSanitizer.sanitizeDescription(input)
    assertEquals("Amazing Event", result)
  }

  @Test
  fun sanitizeDescription_withHtmlScript_throwsException() {
    assertThrows(IllegalArgumentException::class.java) {
      InputSanitizer.sanitizeDescription("Description<script>alert(1)</script>")
    }
  }

  @Test
  fun sanitizeDescription_withSqlInjection_throwsException() {
    assertThrows(IllegalArgumentException::class.java) {
      InputSanitizer.sanitizeDescription("Description; DROP TABLE users")
    }
  }

  @Test
  fun sanitizeDescription_withZeroWidthCharacters_removesIt() {
    val input = "Amazing\u200B\u200C\u200DEvent"
    val result = InputSanitizer.sanitizeDescription(input)
    assertEquals("AmazingEvent", result)
  }

  @Test
  fun sanitizeDescription_withRtlOverride_removesIt() {
    val input = "Amazing\u202EEvent"
    val result = InputSanitizer.sanitizeDescription(input)
    assertEquals("AmazingEvent", result)
  }

  @Test
  fun sanitizeDescription_withLtlOverride_removesIt() {
    val input = "Amazing\u202DEvent"
    val result = InputSanitizer.sanitizeDescription(input)
    assertEquals("AmazingEvent", result)
  }

  @Test
  fun sanitizeDescription_withSoftHyphen_removesIt() {
    val input = "Amazing\u00ADEvent"
    val result = InputSanitizer.sanitizeDescription(input)
    assertEquals("AmazingEvent", result)
  }

  @Test
  fun sanitizeDescription_withControlCharacters_removesIt() {
    val input = "Amazing\u0001\u0002Event"
    val result = InputSanitizer.sanitizeDescription(input)
    assertEquals("AmazingEvent", result)
  }

  // ============ sanitizePrice() Tests ============

  @Test
  fun sanitizePrice_withValidPrice_returnsAsIs() {
    val result = InputSanitizer.sanitizePrice("12.99")
    assertEquals("12.99", result)
  }

  @Test
  fun sanitizePrice_withOnlyInteger_returnsAsIs() {
    val result = InputSanitizer.sanitizePrice("50")
    assertEquals("50", result)
  }

  @Test
  fun sanitizePrice_withSpecialCharacters_removesIt() {
    val result = InputSanitizer.sanitizePrice("$12.99")
    assertEquals("12.99", result)
  }

  @Test
  fun sanitizePrice_withMultipleDecimalPoints_keepsFirst() {
    val result = InputSanitizer.sanitizePrice("12.99.50")
    assertEquals("12.99", result)
  }

  @Test
  fun sanitizePrice_withExcessiveDecimalPlaces_limitsToTwo() {
    val result = InputSanitizer.sanitizePrice("12.999")
    assertEquals("12.99", result)
  }

  @Test
  fun sanitizePrice_withPlusSign_removesIt() {
    val result = InputSanitizer.sanitizePrice("+12.99")
    assertEquals("12.99", result)
  }

  @Test
  fun sanitizePrice_withMinusSign_removesIt() {
    val result = InputSanitizer.sanitizePrice("-12.99")
    assertEquals("12.99", result)
  }

  @Test
  fun sanitizePrice_withLetters_removesIt() {
    val result = InputSanitizer.sanitizePrice("12.99abc")
    assertEquals("12.99", result)
  }

  @Test
  fun sanitizePrice_withZero_returnsZero() {
    val result = InputSanitizer.sanitizePrice("0")
    assertEquals("0", result)
  }

  @Test
  fun sanitizePrice_withEmpty_returnsEmpty() {
    val result = InputSanitizer.sanitizePrice("")
    assertEquals("", result)
  }

  @Test
  fun sanitizePrice_withOneDecimalPlace_keepsAsIs() {
    val result = InputSanitizer.sanitizePrice("12.5")
    assertEquals("12.5", result)
  }

  @Test
  fun sanitizePrice_withTwoDecimalPlaces_keepsAsIs() {
    val result = InputSanitizer.sanitizePrice("12.50")
    assertEquals("12.50", result)
  }

  @Test
  fun sanitizePrice_withNoDecimal_keepsAsIs() {
    val result = InputSanitizer.sanitizePrice("100")
    assertEquals("100", result)
  }

  // ============ sanitizeCapacity() Tests ============

  @Test
  fun sanitizeCapacity_withValidInteger_returnsAsIs() {
    val result = InputSanitizer.sanitizeCapacity("100")
    assertEquals("100", result)
  }

  @Test
  fun sanitizeCapacity_withLeadingZeros_removesIt() {
    val result = InputSanitizer.sanitizeCapacity("00100")
    assertEquals("100", result)
  }

  @Test
  fun sanitizeCapacity_withSpecialCharacters_removesIt() {
    val result = InputSanitizer.sanitizeCapacity("1#0@0")
    assertEquals("100", result)
  }

  @Test
  fun sanitizeCapacity_withMinusSign_removesIt() {
    val result = InputSanitizer.sanitizeCapacity("-100")
    assertEquals("100", result)
  }

  @Test
  fun sanitizeCapacity_withDecimalPoint_removesIt() {
    val result = InputSanitizer.sanitizeCapacity("100.5")
    assertEquals("1005", result)
  }

  @Test
  fun sanitizeCapacity_withLetters_removesIt() {
    val result = InputSanitizer.sanitizeCapacity("100abc")
    assertEquals("100", result)
  }

  @Test
  fun sanitizeCapacity_withOnlyZeros_returnsEmpty() {
    val result = InputSanitizer.sanitizeCapacity("000")
    assertEquals("", result)
  }

  @Test
  fun sanitizeCapacity_withEmpty_returnsEmpty() {
    val result = InputSanitizer.sanitizeCapacity("")
    assertEquals("", result)
  }

  @Test
  fun sanitizeCapacity_withZero_returnsEmpty() {
    val result = InputSanitizer.sanitizeCapacity("0")
    assertEquals("", result)
  }

  // ============ Edge Cases & Combined Attacks ============

  @Test
  fun sanitizeTitle_withCaseInsensitiveScript_throwsException() {
    assertThrows(IllegalArgumentException::class.java) {
      InputSanitizer.sanitizeTitle("Event<SCRIPT>alert(1)</SCRIPT>")
    }
  }

  @Test
  fun sanitizeTitle_withUnionSelect_throwsException() {
    assertThrows(IllegalArgumentException::class.java) {
      InputSanitizer.sanitizeTitle("Event'; UNION SELECT * FROM users")
    }
  }

  @Test
  fun sanitizeDescription_withIframeTag_throwsException() {
    assertThrows(IllegalArgumentException::class.java) {
      InputSanitizer.sanitizeDescription("Check this<iframe src='...'></iframe>")
    }
  }

  @Test
  fun sanitizeTitle_withOnClickHandler_throwsException() {
    assertThrows(IllegalArgumentException::class.java) {
      InputSanitizer.sanitizeTitle("Click onclick=alert(1)")
    }
  }

  @Test
  fun sanitizeTitle_withOnLoadHandler_throwsException() {
    assertThrows(IllegalArgumentException::class.java) {
      InputSanitizer.sanitizeTitle("Image onload=alert(1)")
    }
  }

  @Test
  fun sanitizeDescription_withFileProtocol_throwsException() {
    assertThrows(IllegalArgumentException::class.java) {
      InputSanitizer.sanitizeDescription("file:///etc/passwd")
    }
  }

  @Test
  fun sanitizeTitle_withUppercaseJavascript_throwsException() {
    assertThrows(IllegalArgumentException::class.java) {
      InputSanitizer.sanitizeTitle("JAVASCRIPT:alert(1)")
    }
  }

  @Test
  fun sanitizeTitle_withMixedCaseData_throwsException() {
    assertThrows(IllegalArgumentException::class.java) {
      InputSanitizer.sanitizeTitle("Data:text/html,test")
    }
  }

  @Test
  fun sanitizeTitle_withUppercaseFile_throwsException() {
    assertThrows(IllegalArgumentException::class.java) {
      InputSanitizer.sanitizeTitle("FILE:///etc/passwd")
    }
  }
}
