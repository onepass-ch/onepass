package ch.onepass.onepass.ui.staff

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.onepass.onepass.R
import ch.onepass.onepass.model.staff.StaffSearchResult
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StaffListItemTest {

  @get:Rule val composeRule = createComposeRule()

  private val baseUser =
      StaffSearchResult(
          id = "user-123",
          email = "jane.doe@example.com",
          displayName = "Jane Doe",
          avatarUrl = null)

  @Test
  fun staffListItem_whenRendered_showsDisplayNameAndEmail() {
    composeRule.setContent { StaffListItem(user = baseUser, onClick = {}) }

    composeRule
        .onNodeWithTag(StaffTestTags.Item.LIST_ITEM, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertHasClickAction()

    composeRule
        .onNodeWithTag(StaffTestTags.Item.HEADLINE, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextEquals(baseUser.displayName)

    composeRule
        .onNodeWithTag(StaffTestTags.Item.SUPPORTING, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextEquals(baseUser.email)
  }

  @Test
  fun staffListItem_whenClicked_invokesCallbackWithUser() {
    var clicked: Boolean = false

    composeRule.setContent { StaffListItem(user = baseUser, onClick = { clicked = true }) }

    composeRule.onNodeWithTag(StaffTestTags.Item.LIST_ITEM, useUnmergedTree = true).performClick()

    org.junit.Assert.assertTrue(clicked)
  }

  @Test
  fun staffListItem_withoutAvatar_showsComputedInitials() {
    val userWithoutAvatar =
        baseUser.copy(displayName = "  élodie  ", avatarUrl = null, email = "elodie@example.com")

    composeRule.setContent { StaffListItem(user = userWithoutAvatar, onClick = {}) }

    composeRule
        .onNodeWithTag(StaffTestTags.Avatar.Initials.TEXT, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextEquals("É")
  }

  @Test
  fun staffListItem_withAvatarUrl_rendersImageContent() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val userWithAvatar =
        baseUser.copy(
            avatarUrl = resourceUri(context, R.drawable.ic_launcher_foreground),
            email = "jane.with.avatar@example.com")

    composeRule.setContent { StaffListItem(user = userWithAvatar, onClick = {}) }

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule
          .onAllNodesWithTag(StaffTestTags.Avatar.IMAGE, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeRule
        .onNodeWithTag(StaffTestTags.Avatar.IMAGE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun staffListItem_whenAvatarLoadFails_showsErrorMonogram() {
    val failingUser =
        baseUser.copy(avatarUrl = "file:///does/not/exist.png", email = "broken.avatar@example.com")

    composeRule.setContent { StaffListItem(user = failingUser, onClick = {}) }

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule
          .onAllNodesWithTag(StaffTestTags.Avatar.Error.CONTAINER, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeRule
        .onNodeWithTag(StaffTestTags.Avatar.Error.CONTAINER, useUnmergedTree = true)
        .assertIsDisplayed()

    composeRule
        .onNodeWithTag(StaffTestTags.Avatar.Error.TEXT, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextEquals("J")
  }

  @Test
  fun staffListItem_whenDisabled_hasNoClickAction() {
    composeRule.setContent { StaffListItem(user = baseUser, onClick = null, enabled = false) }

    composeRule
        .onNodeWithTag(StaffTestTags.Item.LIST_ITEM, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertHasNoClickAction()
  }

  @Test
  fun staffListItem_whenOnClickIsNull_hasNoClickAction() {
    composeRule.setContent { StaffListItem(user = baseUser, onClick = null) }

    composeRule
        .onNodeWithTag(StaffTestTags.Item.LIST_ITEM, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertHasNoClickAction()
  }

  @Test
  fun staffListItem_whenDisabled_doesNotInvokeCallback() {
    var clicked: Boolean = false

    composeRule.setContent {
      StaffListItem(user = baseUser, onClick = { clicked = true }, enabled = false)
    }

    composeRule.onNodeWithTag(StaffTestTags.Item.LIST_ITEM, useUnmergedTree = true).performClick()

    org.junit.Assert.assertFalse(clicked)
  }

  private fun resourceUri(context: Context, @DrawableRes resId: Int): String {
    return "android.resource://${context.packageName}/$resId"
  }
}
