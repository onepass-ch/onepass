package ch.onepass.onepass.model.user

import java.util.Date
import org.junit.Assert
import org.junit.Test

class UserTest {
  private fun createTestUser(
      uid: String = "uid-123",
      email: String = "user@example.com",
      createdAt: Date? = Date(1734480000000), // 2024-12-18T00:00:00Z
      lastLoginAt: Date? = Date(1734566400000), // 2024-12-19T00:00:00Z
      status: Status = Status.ACTIVE,
      displayName: String = "Test User",
      bio: String? = "Bio",
      avatarUrl: String? = "https://example.com/avatar.png",
      coverUrl: String? = "https://example.com/cover.png",
      phoneE164: String? = "+41998887766",
      country: String? = "CH"
  ): User {
    return User(
        uid = uid,
        email = email,
        createdAt = createdAt,
        lastLoginAt = lastLoginAt,
        status = status,
        displayName = displayName,
        bio = bio,
        avatarUrl = avatarUrl,
        coverUrl = coverUrl,
        phoneE164 = phoneE164,
        country = country)
  }

  @Test
  fun userHasCorrectDefaults() {
    val user = User()
    Assert.assertEquals("", user.uid)
    Assert.assertEquals("", user.email)
    Assert.assertNull(user.createdAt)
    Assert.assertNull(user.lastLoginAt)
    Assert.assertEquals(Status.ACTIVE, user.status)
    Assert.assertEquals("", user.displayName)
    Assert.assertNull(user.bio)
    Assert.assertNull(user.avatarUrl)
    Assert.assertNull(user.coverUrl)
    Assert.assertNull(user.phoneE164)
    Assert.assertNull(user.country)
  }

  @Test
  fun statusEnumHasAllExpectedValues() {
    val statuses = Status.values()
    Assert.assertEquals(3, statuses.size)
    Assert.assertTrue(statuses.contains(Status.ACTIVE))
    Assert.assertTrue(statuses.contains(Status.BANNED))
    Assert.assertTrue(statuses.contains(Status.DELETED))
  }

  @Test
  fun canCreateUserWithCustomValues() {
    val createdAt = Date(1700000000000)
    val lastLoginAt = Date(1700003600000)
    val user =
        createTestUser(
            uid = "custom-uid",
            email = "custom@example.com",
            createdAt = createdAt,
            lastLoginAt = lastLoginAt,
            status = Status.BANNED,
            displayName = "Custom Name",
            bio = "About me",
            avatarUrl = "https://cdn.example.com/a.png",
            coverUrl = "https://cdn.example.com/c.png",
            phoneE164 = "+12025550123",
            country = "US")

    Assert.assertEquals("custom-uid", user.uid)
    Assert.assertEquals("custom@example.com", user.email)
    Assert.assertEquals(createdAt, user.createdAt)
    Assert.assertEquals(lastLoginAt, user.lastLoginAt)
    Assert.assertEquals(Status.BANNED, user.status)
    Assert.assertEquals("Custom Name", user.displayName)
    Assert.assertEquals("About me", user.bio)
    Assert.assertEquals("https://cdn.example.com/a.png", user.avatarUrl)
    Assert.assertEquals("https://cdn.example.com/c.png", user.coverUrl)
    Assert.assertEquals("+12025550123", user.phoneE164)
    Assert.assertEquals("US", user.country)
  }

  @Test
  fun optionalFieldsHandleNulls() {
    val user =
        createTestUser(
            bio = null,
            avatarUrl = null,
            coverUrl = null,
            phoneE164 = null,
            country = null,
            createdAt = null,
            lastLoginAt = null)

    Assert.assertNull(user.bio)
    Assert.assertNull(user.avatarUrl)
    Assert.assertNull(user.coverUrl)
    Assert.assertNull(user.phoneE164)
    Assert.assertNull(user.country)
    Assert.assertNull(user.createdAt)
    Assert.assertNull(user.lastLoginAt)
  }

  @Test
  fun equalityMatchesSameContentAndDiffersOtherwise() {
    val createdAt = Date(1600000000000)
    val lastLoginAt = Date(1600003600000)

    val a =
        createTestUser(
            uid = "u1",
            email = "a@example.com",
            createdAt = createdAt,
            lastLoginAt = lastLoginAt,
            status = Status.ACTIVE,
            displayName = "Alice",
            bio = "Bio",
            avatarUrl = "https://a.png",
            coverUrl = "https://c.png",
            phoneE164 = "+41000000000",
            country = "CH")

    val b =
        createTestUser(
            uid = "u1",
            email = "a@example.com",
            createdAt = createdAt,
            lastLoginAt = lastLoginAt,
            status = Status.ACTIVE,
            displayName = "Alice",
            bio = "Bio",
            avatarUrl = "https://a.png",
            coverUrl = "https://c.png",
            phoneE164 = "+41000000000",
            country = "CH")

    val c = a.copy(uid = "u2")

    Assert.assertEquals(a, b)
    Assert.assertNotEquals(a, c)
  }
}
