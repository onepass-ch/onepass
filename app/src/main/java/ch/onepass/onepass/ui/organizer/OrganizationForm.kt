package ch.onepass.onepass.ui.organizer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.ui.components.buttons.UploadImageButton
import ch.onepass.onepass.ui.components.forms.FieldLabelWithCounter

/** Test tags for Organization form components. */
object OrganizationFormTestTags {
  const val SCREEN = "organization_form_screen"
  const val SCROLL_COLUMN = "organization_form_scroll"

  // Form Fields
  const val NAME_FIELD = "organization_name_field"
  const val NAME_CHAR_COUNT = "organization_name_char_count"
  const val NAME_ERROR = "organization_name_error"

  const val DESCRIPTION_FIELD = "organization_description_field"
  const val DESCRIPTION_CHAR_COUNT = "organization_description_char_count"
  const val DESCRIPTION_ERROR = "organization_description_error"

  const val EMAIL_FIELD = "organization_email_field"
  const val EMAIL_CHAR_COUNT = "organization_email_char_count"
  const val EMAIL_ERROR = "organization_email_error"

  const val PHONE_PREFIX = "organization_phone_prefix"
  const val PHONE_DROPDOWN = "organization_phone_dropdown"
  const val PHONE_FIELD = "organization_phone_field"
  const val PHONE_CHAR_COUNT = "organization_phone_char_count"
  const val PHONE_ERROR = "organization_phone_error"

  const val WEBSITE_FIELD = "organization_website_field"
  const val WEBSITE_CHAR_COUNT = "organization_website_char_count"
  const val WEBSITE_ERROR = "organization_website_error"

  const val INSTAGRAM_FIELD = "organization_instagram_field"
  const val INSTAGRAM_CHAR_COUNT = "organization_instagram_char_count"

  const val FACEBOOK_FIELD = "organization_facebook_field"
  const val FACEBOOK_CHAR_COUNT = "organization_facebook_char_count"

  const val TIKTOK_FIELD = "organization_tiktok_field"
  const val TIKTOK_CHAR_COUNT = "organization_tiktok_char_count"

  const val ADDRESS_FIELD = "organization_address_field"
  const val ADDRESS_CHAR_COUNT = "organization_address_char_count"

  // Images
  const val PROFILE_IMAGE_BUTTON = "organization_profile_image_button"
  const val PROFILE_IMAGE_STATUS = "organization_profile_image_status"
  const val COVER_IMAGE_BUTTON = "organization_cover_image_button"
  const val COVER_IMAGE_STATUS = "organization_cover_image_status"

  // Submit
  const val SUBMIT_BUTTON = "organization_submit_button"
}

/**
 * Main organization form composable with scrollable content.
 *
 * Displays form fields for organization details including name, description, contact info, social
 * media, and image uploads.
 *
 * @param formState Current form state containing all field values and errors
 * @param countryList List of available countries with their phone codes as pairs
 * @param prefixDisplayText Display text for the selected phone country prefix
 * @param prefixError Error message for phone prefix validation
 * @param dropdownExpanded Whether the country dropdown is expanded
 * @param onCountrySelected Callback when a country is selected in the dropdown
 * @param onPrefixClick Callback when the prefix button is clicked
 * @param onDropdownDismiss Callback when the dropdown is dismissed
 * @param onSubmit Callback when the submit button is pressed
 * @param submitText Text displayed on the submit button
 * @param modifier Optional modifier for the root composable
 * @param viewModel ViewModel handling form state and validation logic
 */
@Composable
fun OrganizerForm(
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
    modifier: Modifier = Modifier,
    viewModel: OrganizationFormViewModel
) {
  val scrollState = rememberScrollState()

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .verticalScroll(scrollState)
              .background(color = colorScheme.background)
              .padding(16.dp)
              .testTag(OrganizationFormTestTags.SCROLL_COLUMN)) {

        // Organization Name Field
        OrganizationNameField(
            value = formState.name.value,
            onValueChange = viewModel::updateName,
            onFocusChanged = viewModel::onFocusChangeName,
            error = formState.name.error,
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Description Field
        OrganizationDescriptionField(
            value = formState.description.value,
            onValueChange = viewModel::updateDescription,
            onFocusChanged = viewModel::onFocusChangeDescription,
            error = formState.description.error,
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Contact Email Field
        OrganizationEmailField(
            value = formState.contactEmail.value,
            onValueChange = viewModel::updateContactEmail,
            onFocusChanged = viewModel::onFocusChangeEmail,
            error = formState.contactEmail.error,
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Contact Phone Field with Prefix Dropdown
        OrganizationPhoneField(
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
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Website Field
        OrganizationWebsiteField(
            value = formState.website.value,
            onValueChange = viewModel::updateWebsite,
            onFocusChanged = viewModel::onFocusChangeWebsite,
            error = formState.website.error,
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Social Media Section
        OrganizationSocialField(
            label = "Instagram",
            value = formState.instagram.value,
            onValueChange = viewModel::updateInstagram,
            testTag = OrganizationFormTestTags.INSTAGRAM_FIELD,
            charCountTestTag = OrganizationFormTestTags.INSTAGRAM_CHAR_COUNT)
        Spacer(modifier = Modifier.height(12.dp))

        OrganizationSocialField(
            label = "Facebook",
            value = formState.facebook.value,
            onValueChange = viewModel::updateFacebook,
            testTag = OrganizationFormTestTags.FACEBOOK_FIELD,
            charCountTestTag = OrganizationFormTestTags.FACEBOOK_CHAR_COUNT)
        Spacer(modifier = Modifier.height(12.dp))

        OrganizationSocialField(
            label = "TikTok",
            value = formState.tiktok.value,
            onValueChange = viewModel::updateTiktok,
            testTag = OrganizationFormTestTags.TIKTOK_FIELD,
            charCountTestTag = OrganizationFormTestTags.TIKTOK_CHAR_COUNT)
        Spacer(modifier = Modifier.height(16.dp))

        // Address Field
        OrganizationAddressField(
            value = formState.address.value,
            onValueChange = viewModel::updateAddress,
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Upload Profile Image
        UploadImageButton(
            imageDescription = "Profile image",
            onImageSelected = { uri -> viewModel.selectProfileImage(uri) },
            testTag = OrganizationFormTestTags.PROFILE_IMAGE_BUTTON)

        if (formState.profileImageUri != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
              text = "✓ Profile image selected",
              style = MaterialTheme.typography.bodyMedium,
              color = colorScheme.onBackground,
              modifier =
                  Modifier.padding(start = 8.dp)
                      .testTag(OrganizationFormTestTags.PROFILE_IMAGE_STATUS))
        }

        Spacer(Modifier.height(16.dp))

        // Upload banner image
        UploadImageButton(
            imageDescription = "Banner image",
            onImageSelected = { uri -> viewModel.selectCoverImage(uri) },
            testTag = OrganizationFormTestTags.COVER_IMAGE_BUTTON)

        if (formState.coverImageUri != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
              text = "✓ Banner image selected",
              style = MaterialTheme.typography.bodyMedium,
              color = colorScheme.onBackground,
              modifier =
                  Modifier.padding(start = 8.dp)
                      .testTag(OrganizationFormTestTags.COVER_IMAGE_STATUS))
        }

        Spacer(Modifier.height(24.dp))

        // Submit Button
        Button(
            onClick = onSubmit,
            modifier =
                Modifier.fillMaxWidth()
                    .height(50.dp)
                    .testTag(OrganizationFormTestTags.SUBMIT_BUTTON),
            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
            shape = RoundedCornerShape(10.dp)) {
              Text(
                  text = submitText,
                  style = MaterialTheme.typography.bodyMedium,
                  color = colorScheme.onPrimary)
            }

        Spacer(modifier = Modifier.height(16.dp))
      }
}

/**
 * Organization name input field with character counter.
 *
 * @param value Current name value
 * @param onValueChange Callback when name changes
 * @param onFocusChanged Callback when focus state changes
 * @param error Error message to display
 */
@Composable
private fun OrganizationNameField(
    value: String,
    onValueChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    error: String?,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    FieldLabelWithCounter(
        label = "Organization Name*",
        currentLength = value.length,
        maxLength = OrganizationFormViewModel.MAX_NAME_LENGTH,
        isError = error != null,
        testTag = OrganizationFormTestTags.NAME_CHAR_COUNT)
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
          Text(
              "Amazing Organization",
              style = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface))
        },
        modifier =
            Modifier.fillMaxWidth()
                .border(1.dp, colorScheme.outline, RoundedCornerShape(10.dp))
                .heightIn(min = 50.dp)
                .onFocusChanged { onFocusChanged(it.isFocused) }
                .testTag(OrganizationFormTestTags.NAME_FIELD),
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = colorScheme.surface,
                unfocusedContainerColor = colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = colorScheme.onBackground,
                unfocusedTextColor = colorScheme.onBackground,
            ),
        shape = RoundedCornerShape(10.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        singleLine = true,
        isError = error != null)
    error?.let {
      Text(
          text = it,
          color = colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
          modifier =
              Modifier.padding(start = 8.dp, top = 4.dp)
                  .testTag(OrganizationFormTestTags.NAME_ERROR))
    }
  }
}

/**
 * Organization description input field with character counter.
 *
 * @param value Current description value
 * @param onValueChange Callback when description changes
 * @param onFocusChanged Callback when focus state changes
 * @param error Error message to display
 */
@Composable
private fun OrganizationDescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    error: String?,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    FieldLabelWithCounter(
        label = "Description*",
        currentLength = value.length,
        maxLength = OrganizationFormViewModel.MAX_DESCRIPTION_LENGTH,
        isError = error != null,
        testTag = OrganizationFormTestTags.DESCRIPTION_CHAR_COUNT)
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .height(122.dp)
                .border(1.dp, colorScheme.outline, RoundedCornerShape(10.dp))) {
          TextField(
              value = value,
              onValueChange = onValueChange,
              placeholder = {
                Text(
                    "Tell us about your organization...",
                    style = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface))
              },
              modifier =
                  Modifier.fillMaxSize()
                      .onFocusChanged { onFocusChanged(it.isFocused) }
                      .testTag(OrganizationFormTestTags.DESCRIPTION_FIELD),
              colors =
                  TextFieldDefaults.colors(
                      focusedContainerColor = colorScheme.surface,
                      unfocusedContainerColor = colorScheme.surface,
                      focusedIndicatorColor = Color.Transparent,
                      unfocusedIndicatorColor = Color.Transparent,
                      focusedTextColor = colorScheme.onBackground,
                      unfocusedTextColor = colorScheme.onBackground,
                  ),
              shape = RoundedCornerShape(10.dp),
              textStyle = MaterialTheme.typography.bodyMedium,
              maxLines = 5,
              isError = error != null)
        }
    error?.let {
      Text(
          text = it,
          color = colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
          modifier =
              Modifier.padding(start = 8.dp, top = 4.dp)
                  .testTag(OrganizationFormTestTags.DESCRIPTION_ERROR))
    }
  }
}

/**
 * Contact email input field with character counter and email keyboard.
 *
 * @param value Current email value
 * @param onValueChange Callback when email changes
 * @param onFocusChanged Callback when focus state changes
 * @param error Error message to display
 */
@Composable
private fun OrganizationEmailField(
    value: String,
    onValueChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    error: String?,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    FieldLabelWithCounter(
        label = "Contact Email*",
        currentLength = value.length,
        maxLength = OrganizationFormViewModel.MAX_EMAIL_LENGTH,
        isError = error != null,
        testTag = OrganizationFormTestTags.EMAIL_CHAR_COUNT)
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
          Text(
              "contact@organization.com",
              style = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface))
        },
        modifier =
            Modifier.fillMaxWidth()
                .border(1.dp, colorScheme.outline, RoundedCornerShape(10.dp))
                .heightIn(min = 50.dp)
                .onFocusChanged { onFocusChanged(it.isFocused) }
                .testTag(OrganizationFormTestTags.EMAIL_FIELD),
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = colorScheme.surface,
                unfocusedContainerColor = colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = colorScheme.onBackground,
                unfocusedTextColor = colorScheme.onBackground,
            ),
        shape = RoundedCornerShape(10.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        isError = error != null)
    error?.let {
      Text(
          text = it,
          color = colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
          modifier =
              Modifier.padding(start = 8.dp, top = 4.dp)
                  .testTag(OrganizationFormTestTags.EMAIL_ERROR))
    }
  }
}

/**
 * Contact phone input field with country code prefix dropdown and character counter.
 *
 * Displays a dropdown for country selection and an aligned text field for phone number entry.
 *
 * @param prefixDisplayText Display text for the selected country prefix
 * @param prefixError Error message for phone validation
 * @param countryList List of countries with phone codes
 * @param dropdownExpanded Whether the country dropdown is currently expanded
 * @param onDropdownDismiss Callback when dropdown is dismissed
 * @param onCountrySelected Callback when a country is selected
 * @param phoneValue Current phone number value
 * @param onPhoneChange Callback when phone number changes
 * @param onPhoneFocusChanged Callback when phone field focus changes
 * @param onPrefixClick Callback when prefix button is clicked
 */
@Composable
private fun OrganizationPhoneField(
    prefixDisplayText: String,
    prefixError: String?,
    countryList: List<Pair<String, Int>>,
    dropdownExpanded: Boolean,
    onDropdownDismiss: () -> Unit,
    onCountrySelected: (Int) -> Unit,
    phoneValue: String,
    onPhoneChange: (String) -> Unit,
    onPhoneFocusChanged: (Boolean) -> Unit,
    onPrefixClick: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    FieldLabelWithCounter(
        label = "Contact Phone*",
        currentLength = phoneValue.length,
        maxLength = OrganizationFormViewModel.MAX_PHONE_LENGTH,
        isError = prefixError != null,
        testTag = OrganizationFormTestTags.PHONE_CHAR_COUNT)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
          // Prefix Dropdown
          Box(
              modifier =
                  Modifier.widthIn(90.dp)
                      .height(50.dp)
                      .border(1.dp, colorScheme.outline, RoundedCornerShape(10.dp))) {
                OutlinedButton(
                    onClick = onPrefixClick,
                    modifier =
                        Modifier.fillMaxSize().testTag(OrganizationFormTestTags.PHONE_PREFIX),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = colorScheme.surface,
                            contentColor = colorScheme.onSurface),
                    border = BorderStroke(0.dp, Color.Transparent),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)) {
                      Text(
                          text = prefixDisplayText,
                          style = MaterialTheme.typography.bodyMedium,
                          color = colorScheme.onSurface)
                    }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = onDropdownDismiss,
                    modifier = Modifier.testTag(OrganizationFormTestTags.PHONE_DROPDOWN)) {
                      countryList.forEachIndexed { index, (country, code) ->
                        DropdownMenuItem(
                            text = { Text("$country +$code") },
                            onClick = { onCountrySelected(index) })
                      }
                    }
              }

          // Phone Number TextField
          Box(
              modifier =
                  Modifier.weight(1f)
                      .height(50.dp)
                      .border(1.dp, colorScheme.outline, RoundedCornerShape(10.dp))) {
                TextField(
                    value = phoneValue,
                    onValueChange = onPhoneChange,
                    placeholder = {
                      Text(
                          "123456789",
                          style =
                              MaterialTheme.typography.bodyMedium.copy(
                                  color = colorScheme.onSurface))
                    },
                    modifier =
                        Modifier.fillMaxSize()
                            .onFocusChanged { onPhoneFocusChanged(it.isFocused) }
                            .testTag(OrganizationFormTestTags.PHONE_FIELD),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = colorScheme.surface,
                            unfocusedContainerColor = colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = colorScheme.onBackground,
                            unfocusedTextColor = colorScheme.onBackground,
                        ),
                    shape = RoundedCornerShape(10.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = prefixError != null)
              }
        }

    prefixError?.let {
      Text(
          text = it,
          color = colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
          modifier =
              Modifier.padding(start = 8.dp, top = 4.dp)
                  .testTag(OrganizationFormTestTags.PHONE_ERROR))
    }
  }
}

/**
 * Website URL input field with character counter and URI keyboard.
 *
 * @param value Current website URL value
 * @param onValueChange Callback when website changes
 * @param onFocusChanged Callback when focus state changes
 * @param error Error message to display
 */
@Composable
private fun OrganizationWebsiteField(
    value: String,
    onValueChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    error: String?,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    FieldLabelWithCounter(
        label = "Website",
        currentLength = value.length,
        maxLength = OrganizationFormViewModel.MAX_WEBSITE_LENGTH,
        isError = error != null,
        testTag = OrganizationFormTestTags.WEBSITE_CHAR_COUNT)
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
          Text(
              "https://yourwebsite.com",
              style = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface))
        },
        modifier =
            Modifier.fillMaxWidth()
                .border(1.dp, colorScheme.outline, RoundedCornerShape(10.dp))
                .heightIn(min = 50.dp)
                .onFocusChanged { onFocusChanged(it.isFocused) }
                .testTag(OrganizationFormTestTags.WEBSITE_FIELD),
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = colorScheme.surface,
                unfocusedContainerColor = colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = colorScheme.onBackground,
                unfocusedTextColor = colorScheme.onBackground,
            ),
        shape = RoundedCornerShape(10.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        isError = error != null)
    error?.let {
      Text(
          text = it,
          color = colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
          modifier =
              Modifier.padding(start = 8.dp, top = 4.dp)
                  .testTag(OrganizationFormTestTags.WEBSITE_ERROR))
    }
  }
}

/**
 * Social media username input field with character counter.
 *
 * @param label Display label for the social platform (e.g., "Instagram", "Facebook")
 * @param value Current username value
 * @param onValueChange Callback when username changes
 * @param testTag Test tag for the input field
 * @param charCountTestTag Test tag for the character counter
 */
@Composable
private fun OrganizationSocialField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String,
    charCountTestTag: String
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    FieldLabelWithCounter(
        label = label,
        currentLength = value.length,
        maxLength = OrganizationFormViewModel.MAX_SOCIAL_LENGTH,
        testTag = charCountTestTag)
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
          Text(
              "@username",
              style = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface))
        },
        modifier =
            Modifier.fillMaxWidth()
                .border(1.dp, colorScheme.outline, RoundedCornerShape(10.dp))
                .heightIn(min = 50.dp)
                .testTag(testTag),
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = colorScheme.surface,
                unfocusedContainerColor = colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = colorScheme.onBackground,
                unfocusedTextColor = colorScheme.onBackground,
            ),
        shape = RoundedCornerShape(10.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        singleLine = true)
  }
}

/**
 * Address input field with character counter.
 *
 * @param value Current address value
 * @param onValueChange Callback when address changes
 */
@Composable
private fun OrganizationAddressField(
    value: String,
    onValueChange: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    FieldLabelWithCounter(
        label = "Address",
        currentLength = value.length,
        maxLength = OrganizationFormViewModel.MAX_ADDRESS_LENGTH,
        testTag = OrganizationFormTestTags.ADDRESS_CHAR_COUNT)
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
          Text(
              "123 Main Street, City",
              style = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface))
        },
        modifier =
            Modifier.fillMaxWidth()
                .border(1.dp, colorScheme.outline, RoundedCornerShape(10.dp))
                .heightIn(min = 50.dp)
                .testTag(OrganizationFormTestTags.ADDRESS_FIELD),
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = colorScheme.surface,
                unfocusedContainerColor = colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = colorScheme.onBackground,
                unfocusedTextColor = colorScheme.onBackground,
            ),
        shape = RoundedCornerShape(10.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        singleLine = true)
  }
}
