package ch.onepass.onepass.resources

import org.junit.Assert.assertEquals
import org.junit.Test

class CTest {
  // THIS IS A VIRTUAL TEST FOR AN EMPTY CLASS. REMOVE DURING DEVELOPMENT.
  @Test
  fun accessConstant() {
    // Ensure object access and constant read is executed for coverage
    assertEquals("main_screen_greeting", C.Tag.greeting)
  }
}
