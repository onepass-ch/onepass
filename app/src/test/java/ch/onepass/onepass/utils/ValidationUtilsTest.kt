package ch.onepass.onepass.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ValidationUtilsTest {

  @Test
  fun isValidEmail() {
    assertTrue(ValidationUtils.isValidEmail("test@example.com"))
    assertTrue(ValidationUtils.isValidEmail("user.name+tag@domain.co.uk"))

    assertFalse(ValidationUtils.isValidEmail(""))
    assertFalse(ValidationUtils.isValidEmail("plainaddress"))
    assertFalse(ValidationUtils.isValidEmail("@missingusername.com"))
    assertFalse(ValidationUtils.isValidEmail("user@.com.my"))
  }

  @Test
  fun isValidUrl() {
    assertTrue(ValidationUtils.isValidUrl("https://www.google.com"))
    assertTrue(ValidationUtils.isValidUrl("http://example.com"))
    assertTrue(ValidationUtils.isValidUrl("www.test.com")) // Logic adds https://

    assertFalse(ValidationUtils.isValidUrl(""))
    assertFalse(ValidationUtils.isValidUrl("invalid-url"))
  }

  @Test
  fun isValidPhone() {
    // Expecting E.164 format (+ followed by 1-4 digit country code and digits)
    assertTrue(ValidationUtils.isValidPhone("+41791234567"))
    assertTrue(ValidationUtils.isValidPhone("+15551234567"))

    assertFalse(ValidationUtils.isValidPhone(""))
    assertFalse(ValidationUtils.isValidPhone("0791234567")) // Missing +
    assertFalse(ValidationUtils.isValidPhone("+41 79 123 45 67")) // Contains spaces
    assertFalse(ValidationUtils.isValidPhone("invalid"))
  }

  @Test
  fun isPositiveNumber() {
    assertTrue(ValidationUtils.isPositiveNumber("10"))
    assertTrue(ValidationUtils.isPositiveNumber("10.5"))
    assertTrue(ValidationUtils.isPositiveNumber("0"))

    assertFalse(ValidationUtils.isPositiveNumber("-5"))
    assertFalse(ValidationUtils.isPositiveNumber("abc"))
    assertFalse(ValidationUtils.isPositiveNumber(""))
  }

  @Test
  fun isPositiveInteger() {
    assertTrue(ValidationUtils.isPositiveInteger("10"))
    assertTrue(ValidationUtils.isPositiveInteger("1"))

    assertFalse(ValidationUtils.isPositiveInteger("0")) // Strictly positive
    assertFalse(ValidationUtils.isPositiveInteger("-5"))
    assertFalse(ValidationUtils.isPositiveInteger("10.5"))
    assertFalse(ValidationUtils.isPositiveInteger("abc"))
  }
}
