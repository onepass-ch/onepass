package ch.onepass.onepass.ui.organizer

import androidx.lifecycle.ViewModel
import ch.onepass.onepass.model.organization.*
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

/**
 * Data class representing the editable fields of an organization.
 *
 * @param id The unique identifier of the organization.
 * @param name The name of the organization.
 * @param description A brief description of the organization.
 * @param contactEmail The contact email of the organization (optional).
 * @param contactPhone The contact phone number of the organization (optional).
 * @param website The website URL of the organization (optional).
 * @param instagram The Instagram handle or URL of the organization (optional).
 * @param facebook The Facebook page URL of the organization (optional).
 * @param tiktok The TikTok handle or URL of the organization (optional).
 * @param address The physical address of the organization (optional).
 */
data class EditOrganizationData(
    val id: String,
    val name: String,
    val description: String,
    val contactEmail: String?,
    val contactPhone: String?,
    val website: String?,
    val instagram: String?,
    val facebook: String?,
    val tiktok: String?,
    val address: String?
)

/**
 * ViewModel for editing organization details.
 *
 * @property repository The [OrganizationRepository] used for data operations.
 */
class EditOrganizationViewModel(
    private val repository: OrganizationRepository = OrganizationRepositoryFirebase()
) : ViewModel() {

  /**
   * Loads the first organization associated with the given user ID.
   *
   * @param userId The user ID whose organization is to be loaded.
   * @return An [EditOrganizationData] object if an organization is found, otherwise null
   */
  suspend fun loadFirstOrganizationForUser(userId: String): EditOrganizationData? =
      withContext(Dispatchers.Default) {
        // Fetch organizations owned by the user
        val ownedOrgs = repository.getOrganizationsByOwner(userId).firstOrNull() ?: emptyList()
        // Fetch organizations where the user is a member
        val memberOrgs = repository.getOrganizationsByMember(userId).firstOrNull() ?: emptyList()

        // Combine both lists and find the most recently created organization
        val firstOrg = (ownedOrgs + memberOrgs).maxByOrNull { it.createdAt?.seconds ?: 0L }

        firstOrg?.let {
          EditOrganizationData(
              id = it.id,
              name = it.name,
              description = it.description,
              contactEmail = it.contactEmail,
              contactPhone = it.contactPhone,
              website = it.website,
              instagram = it.instagram,
              facebook = it.facebook,
              tiktok = it.tiktok,
              address = it.address)
        }
      }

  /**
   * Updates the organization with the provided data.
   *
   * @param data The [EditOrganizationData] containing updated organization details.
   * @throws Exception if the update operation fails.
   */
  suspend fun updateOrganization(data: EditOrganizationData) {
    val org =
        Organization(
            id = data.id,
            name = data.name,
            description = data.description,
            ownerId = "", // optional ownerId
            status = OrganizationStatus.ACTIVE,
            contactEmail = data.contactEmail,
            contactPhone = data.contactPhone,
            website = data.website,
            instagram = data.instagram,
            facebook = data.facebook,
            tiktok = data.tiktok,
            address = data.address,
            createdAt = Timestamp.now())

    val result = repository.updateOrganization(org)
    if (!result.isSuccess) throw result.exceptionOrNull() ?: Exception("Update failed")
  }
}
