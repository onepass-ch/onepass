package ch.onepass.onepass.model.staff

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Test

class StaffSearchResultTest {

    @Test
    fun defaultAvatarUrlIsNull() {
        val result =
            StaffSearchResult(
                id = "123",
                email = "staff.member@example.com",
                displayName = "Staff Member"
            )

        assertEquals("123", result.id)
        assertEquals("staff.member@example.com", result.email)
        assertEquals("Staff Member", result.displayName)
        assertNull(result.avatarUrl)
    }

    @Test
    fun copyCreatesModifiedInstance() {
        val original =
            StaffSearchResult(
                id = "123",
                email = "staff.member@example.com",
                displayName = "Staff Member",
                avatarUrl = "https://example.com/avatar.png"
            )

        val updated = original.copy(displayName = "Updated Name")

        assertNotSame(original, updated)
        assertEquals(original.id, updated.id)
        assertEquals(original.email, updated.email)
        assertEquals("Updated Name", updated.displayName)
        assertEquals(original.avatarUrl, updated.avatarUrl)
    }

    @Test
    fun equalityDependsOnAllProperties() {
        val first =
            StaffSearchResult(
                id = "123",
                email = "staff.member@example.com",
                displayName = "Staff Member",
                avatarUrl = "https://example.com/avatar.png"
            )

        val second =
            StaffSearchResult(
                id = "123",
                email = "staff.member@example.com",
                displayName = "Staff Member",
                avatarUrl = "https://example.com/avatar.png"
            )

        assertEquals(first, second)
    }
}

