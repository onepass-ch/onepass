package ch.onepass.onepass.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeepLinkHandlerTest {

  private lateinit var navController: NavController

  @Before
  fun setup() {
    navController = mockk(relaxed = true)
  }

  @Test
  fun handleDeepLink_eventPath_navigatesToEventDetail() {
    // CHANGE THIS LINE - add "notification" host
    DeepLinkHandler.handleDeepLink("onepass://notification/event/event123", navController)
    verify { navController.navigate("event/event123") }
  }

  @Test
  fun handleDeepLink_organizationPath_navigatesToOrgDashboard() {
    // CHANGE THIS LINE
    DeepLinkHandler.handleDeepLink("onepass://notification/organization/org456", navController)
    verify { navController.navigate("organization/org456") }
  }

  @Test
  fun handleDeepLink_notificationsPath_navigatesToNotifications() {
    DeepLinkHandler.handleDeepLink("onepass://notifications", navController)
    verify { navController.navigate("notifications") }
  }

  @Test
  fun handleDeepLink_invitationsPath_navigatesToInvitations() {
    DeepLinkHandler.handleDeepLink("onepass://invitations", navController)
    verify { navController.navigate("my_invitations") }
  }

  @Test
  fun handleDeepLink_ticketsPath_navigatesToTickets() {
    DeepLinkHandler.handleDeepLink("onepass://tickets", navController)
    verify { navController.navigate("tickets") }
  }

  @Test
  fun handleDeepLink_unknownPath_logsWarning() {
    DeepLinkHandler.handleDeepLink("onepass://unknown", navController)
    verify(exactly = 0) { navController.navigate(any<String>()) }
  }

  @Test
  fun handleIntent_withValidUri_callsHandleDeepLink() {
    val intent =
        Intent().apply {
          // CHANGE THIS LINE
          data = Uri.parse("onepass://notification/event/test123")
        }
    DeepLinkHandler.handleIntent(intent, navController)
    verify { navController.navigate("event/test123") }
  }

  @Test
  fun handleIntent_withNullIntent_doesNotCrash() {
    DeepLinkHandler.handleIntent(null, navController)
    verify(exactly = 0) { navController.navigate(any<String>()) }
  }

  @Test
  fun handleIntent_withWrongScheme_doesNotNavigate() {
    val intent = Intent().apply { data = Uri.parse("https://example.com/event/123") }
    DeepLinkHandler.handleIntent(intent, navController)
    verify(exactly = 0) { navController.navigate(any<String>()) }
  }
}
