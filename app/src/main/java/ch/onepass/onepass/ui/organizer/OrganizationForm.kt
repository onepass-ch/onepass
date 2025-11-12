package ch.onepass.onepass.ui.organizer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.R

/**
 * Composable for the organization form used in both creation and editing.
 *
 * @param title Title of the form
 * @param formState Current state of the form fields
 * @param countryList List of countries for phone prefix selection
 * @param prefixDisplayText Currently selected phone prefix display text
 * @param prefixError Optional error message for the prefix field
 * @param dropdownExpanded Whether the country dropdown is expanded
 * @param onCountrySelected Callback when a country is selected
 * @param onPrefixClick Callback when the prefix field is clicked
 * @param onDropdownDismiss Callback when the dropdown is dismissed
 * @param onSubmit Callback when the form is submitted
 * @param submitText Text for the submit button
 * @param testTags Object containing test tag strings for UI testing
 * @param modifier Optional modifier for the form container
 * @param viewModel ViewModel managing the form state and logic
 */
@Composable
fun OrganizerForm(
    title: String,
    formState: OrganizationFormState,
    countryList: List<Pair<String, Int>>,
    prefixDisplayText: String,
    prefixError: String?,
    dropdownExpanded: Boolean,
    onCountrySelected: (Int) -> Unit,
    onPrefixClick: () -> Unit,
    onDropdownDismiss: () -> Unit,
    onSubmit: () -> Unit,
    submitText: String,
    testTags: Any,
    modifier: Modifier = Modifier,
    viewModel: OrganizationFormViewModel
) {
  val scrollState = rememberScrollState()

  Column(modifier = modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp)) {
    // Title
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = colorResource(id = R.color.on_background),
        modifier = Modifier.padding(vertical = 24.dp))

    // Organization Name Field
    FormTextField(
        value = formState.name.value,
        onValueChange = viewModel::updateName,
        label = "Organization Name*",
        isError = formState.name.error != null,
        onFocusChanged = viewModel::onFocusChangeName,
        errorMessage = formState.name.error,
        testTag = testTagsField(testTags, "NAME_FIELD"))

    // Description Field
    FormTextField(
        value = formState.description.value,
        onValueChange = viewModel::updateDescription,
        label = "Description*",
        isError = formState.description.error != null,
        onFocusChanged = viewModel::onFocusChangeDescription,
        maxLines = 5,
        errorMessage = formState.description.error,
        testTag = testTagsField(testTags, "DESCRIPTION_FIELD"))

    // Contact Email Field
    FormTextField(
        value = formState.contactEmail.value,
        onValueChange = viewModel::updateContactEmail,
        label = "Contact Email",
        isError = formState.contactEmail.error != null,
        onFocusChanged = viewModel::onFocusChangeEmail,
        keyboardType = KeyboardType.Email,
        errorMessage = formState.contactEmail.error,
        testTag = testTagsField(testTags, "EMAIL_FIELD"))

    // Contact Phone Field with Prefix Dropdown
    PrefixPhoneRow(
        prefixDisplayText = prefixDisplayText,
        prefixError = prefixError,
        countryList = countryList,
        dropdownExpanded = dropdownExpanded,
        onDropdownDismiss = onDropdownDismiss,
        onCountrySelected = onCountrySelected,
        phoneValue = formState.contactPhone.value,
        onPhoneChange = viewModel::updateContactPhone,
        onPhoneFocusChanged = viewModel::onFocusChangePhone,
        onPrefixClick = onPrefixClick,
        phoneTestTag = testTagsField(testTags, "PHONE_FIELD"),
        prefixTestTag = testTagsField(testTags, "PREFIX_DROPDOWN"))

    // Social Media
    FormTextField(
        value = formState.website.value,
        onValueChange = viewModel::updateWebsite,
        label = "Website",
        testTag = testTagsField(testTags, "WEBSITE_FIELD"))
    FormTextField(
        value = formState.instagram.value,
        onValueChange = viewModel::updateInstagram,
        label = "Instagram",
        testTag = testTagsField(testTags, "INSTAGRAM_FIELD"))
    FormTextField(
        value = formState.facebook.value,
        onValueChange = viewModel::updateFacebook,
        label = "Facebook",
        testTag = testTagsField(testTags, "FACEBOOK_FIELD"))
    FormTextField(
        value = formState.tiktok.value,
        onValueChange = viewModel::updateTiktok,
        label = "TikTok",
        testTag = testTagsField(testTags, "TIKTOK_FIELD"))

    // Address Field
    FormTextField(
        value = formState.address.value,
        onValueChange = viewModel::updateAddress,
        label = "Address",
        testTag = testTagsField(testTags, "ADDRESS_FIELD"))

    Spacer(Modifier.height(32.dp))

    // Submit Button
    SubmitButton(
        onClick = onSubmit,
        text = submitText,
        modifier = Modifier.testTag(testTagsField(testTags, "SUBMIT_BUTTON")))
  }
}

/**
 * Helper to access test tag fields dynamically.
 *
 * @param obj The object containing the test tag fields.
 * @param fieldName The name of the test tag field to access.
 * @return The test tag string if found, otherwise an empty string.
 */
@Suppress("UNCHECKED_CAST")
private fun testTagsField(obj: Any, fieldName: String): String {
  return try {
    val field = obj::class.java.getDeclaredField(fieldName)
    field.get(null) as String
  } catch (e: Exception) {
    ""
  }
}
