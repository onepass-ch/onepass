package ch.onepass.onepass.ui.organizer

import ch.onepass.onepass.model.organization.*
import com.google.firebase.Timestamp
import io.mockk.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditOrganizationViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var repository: OrganizationRepository
  private lateinit var viewModel: EditOrganizationViewModel

  @Before
  fun setUp() {
    repository = mockk()
    viewModel = EditOrganizationViewModel(repository)
  }

  @Test
  fun loadFirstOrganizationForUser_returnsFirstOwnedOrganization() =
      runTest(testDispatcher) {
        val org =
            Organization(
                id = "org1",
                name = "Org One",
                description = "Description One",
                ownerId = "user1",
                createdAt = Timestamp.now())
        coEvery { repository.getOrganizationsByOwner("user1") } returns flowOf(listOf(org))
        coEvery { repository.getOrganizationsByMember("user1") } returns flowOf(emptyList())

        val result = viewModel.loadFirstOrganizationForUser("user1")

        assertEquals("org1", result?.id)
        assertEquals("Org One", result?.name)
      }

  @Test
  fun loadFirstOrganizationForUser_returnsFirstMemberOrganization() =
      runTest(testDispatcher) {
        val org =
            Organization(
                id = "org1",
                name = "Org One",
                description = "Description One",
                ownerId = "other",
                createdAt = Timestamp.now())
        coEvery { repository.getOrganizationsByOwner("user1") } returns flowOf(emptyList())
        coEvery { repository.getOrganizationsByMember("user1") } returns flowOf(listOf(org))

        val result = viewModel.loadFirstOrganizationForUser("user1")

        assertEquals("org1", result?.id)
        assertEquals("Org One", result?.name)
      }

  @Test
  fun loadFirstOrganizationForUser_returnsNewestOrganizationIfMultiple() =
      runTest(testDispatcher) {
        val older =
            Organization(
                id = "org1",
                name = "Older Org",
                description = "Old",
                ownerId = "user1",
                createdAt = Timestamp(1000, 0))
        val newer =
            Organization(
                id = "org2",
                name = "Newer Org",
                description = "New",
                ownerId = "user1",
                createdAt = Timestamp(2000, 0))
        coEvery { repository.getOrganizationsByOwner("user1") } returns flowOf(listOf(older, newer))
        coEvery { repository.getOrganizationsByMember("user1") } returns flowOf(emptyList())

        val result = viewModel.loadFirstOrganizationForUser("user1")

        assertEquals("org2", result?.id)
        assertEquals("Newer Org", result?.name)
      }

  @Test
  fun loadFirstOrganizationForUser_returnsNullIfNoOrganizations() =
      runTest(testDispatcher) {
        coEvery { repository.getOrganizationsByOwner("user1") } returns flowOf(emptyList())
        coEvery { repository.getOrganizationsByMember("user1") } returns flowOf(emptyList())

        val result = viewModel.loadFirstOrganizationForUser("user1")

        assertNull(result)
      }

  @Test
  fun updateOrganization_callsRepositoryWithCorrectOrganization() =
      runTest(testDispatcher) {
        val data =
            EditOrganizationData(
                id = "org1",
                name = "Updated Org",
                description = "Updated",
                contactEmail = "test@example.com",
                contactPhone = "+4123456789",
                website = "https://example.com",
                instagram = "insta",
                facebook = "fb",
                tiktok = "tiktok",
                address = "Address")

        coEvery { repository.updateOrganization(any()) } returns Result.success(Unit)

        viewModel.updateOrganization(data)

        coVerify {
          repository.updateOrganization(
              match {
                it.id == "org1" &&
                    it.name == "Updated Org" &&
                    it.contactEmail == "test@example.com" &&
                    it.website == "https://example.com"
              })
        }
      }
}
