package ch.onepass.onepass.model.auth

import org.junit.Assert.assertNotNull
import org.junit.Test

class AuthRepositoryTest {
  // THIS IS A VIRTUAL TEST FOR AN EMPTY CLASS. REMOVE DURING DEVELOPMENT.
  @Test
  fun instantiateAuthRepository() {
    val repo: AuthRepository = object : AuthRepository {}
    assertNotNull(repo)
  }
}
