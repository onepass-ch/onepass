package ch.onepass.onepass.ui.myevents

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.R
import ch.onepass.onepass.model.payment.StripePaymentHelper
import ch.onepass.onepass.ui.components.common.EmptyState
import ch.onepass.onepass.ui.payment.LocalPaymentSheet
import ch.onepass.onepass.ui.theme.MarcFontFamily
import com.google.firebase.auth.FirebaseAuth

/** Test tags for MyEvents screen components */
object MyEventsTestTags {
  // Main tabs
  const val MAIN_TABS_ROW = "MainTabsRow"
  const val MAIN_TAB_YOUR_TICKETS = "MainTabYourTickets"
  const val MAIN_TAB_MARKET = "MainTabMarket"

  // Your Tickets section
  const val YOUR_TICKETS_TITLE = "YourTicketsTitle"
  const val TABS_ROW = "TabsRow"
  const val TAB_CURRENT = "TabCurrent"
  const val TAB_EXPIRED = "TabExpired"
  const val TAB_LISTED = "TabListed"
  const val QR_CODE_ICON = "QrCodeIcon"
  const val QR_CODE_CARD = "QrCodeCard"
  const val TICKET_CARD = "TicketCard"
  const val TICKET_TITLE = "TicketTitle"
  const val TICKET_STATUS = "TicketStatus"
  const val TICKET_DATE = "TicketDate"
  const val TICKET_LOCATION = "TicketLocation"
  const val TICKET_DIALOG_TITLE = "TicketDialogTitle"
  const val TICKET_DIALOG_STATUS = "TicketDialogStatus"
  const val TICKET_DIALOG_DATE = "TicketDialogDate"
  const val TICKET_DIALOG_LOCATION = "TicketDialogLocation"
  const val EMPTY_STATE = "EmptyState"

  // Market section
  const val MARKET_TITLE = "MarketTitle"
  const val MARKET_SEARCH_BAR = "MarketSearchBar"
  const val HOT_EVENTS_TITLE = "HotEventsTitle"
  const val HOT_EVENTS_LIST = "HotEventsList"
  const val HOT_EVENTS_EMPTY = "HotEventsEmpty"
  const val HOT_EVENT_CARD = "HotEventCard"
  const val MARKET_TICKETS_TITLE = "MarketTicketsTitle"
  const val MARKET_TICKET_LIST = "MarketTicketList"
  const val MARKET_TICKET_CARD = "MarketTicketCard"
  const val MARKET_TICKET_TITLE = "MarketTicketTitle"
  const val MARKET_TICKET_DATE = "MarketTicketDate"
  const val MARKET_TICKET_LOCATION = "MarketTicketLocation"
  const val MARKET_TICKET_SELLER_PRICE = "MarketTicketSellerPrice"
  const val MARKET_TICKET_ORIGINAL_PRICE = "MarketTicketOriginalPrice"
  const val MARKET_TICKET_BUY_BUTTON = "MarketTicketBuyButton"
  const val MARKET_EMPTY_STATE = "MarketEmptyState"
  const val MARKET_LOADING = "MarketLoading"
  const val SELL_TICKET_BUTTON = "SellTicketButton"

  // Sell dialog
  const val SELL_DIALOG = "SellDialog"
  const val SELL_DIALOG_TITLE = "SellDialogTitle"
  const val SELL_DIALOG_TICKET_LIST = "SellDialogTicketList"
  const val SELL_DIALOG_PRICE_INPUT = "SellDialogPriceInput"
  const val SELL_DIALOG_CONFIRM_BUTTON = "SellDialogConfirmButton"
  const val SELL_DIALOG_CANCEL_BUTTON = "SellDialogCancelButton"
  const val SELL_DIALOG_ERROR = "SellDialogError"
}

/**
 * Composable screen displaying user's events with main tabs for Your Tickets and Market.
 *
 * @param viewModel ViewModel providing ticket and market data
 */
@Composable
fun MyEventsScreen(viewModel: MyEventsViewModel) {
  // Collect QR data from ViewModel
  val qrData by viewModel.userQrData.collectAsState()

  // Load user pass on first composition
  LaunchedEffect(Unit) { viewModel.loadUserPass() }

  MyEventsContent(userQrData = qrData ?: "LOADING", viewModel = viewModel)
}

/**
 * Composable screen displaying user's tickets and the ticket marketplace.
 *
 * @param viewModel ViewModel providing ticket and market data
 * @param userQrData String data to be encoded in the user's QR code
 */
@Composable
fun MyEventsContent(userQrData: String, viewModel: MyEventsViewModel) {
  // Collect UI state from ViewModel
  val uiState by viewModel.uiState.collectAsState()

  // Get current user ID for determining own listings
  val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid }

  // Snackbar state for error messages
  val snackbarHostState = remember { SnackbarHostState() }

  // Payment sheet integration
  val paymentSheet = LocalPaymentSheet.current
  val stripeHelper = remember(paymentSheet) { StripePaymentHelper(paymentSheet) }

  // Show error message in snackbar
  LaunchedEffect(uiState.marketError) {
    uiState.marketError?.let { error ->
      snackbarHostState.showSnackbar(error)
      viewModel.clearMarketError()
    }
  }

  // Present payment sheet when client secret is available
  LaunchedEffect(uiState.showPaymentSheet, uiState.purchaseClientSecret) {
    val clientSecret = uiState.purchaseClientSecret
    if (uiState.showPaymentSheet && clientSecret != null) {
      if (stripeHelper.isInitialized) {
        viewModel.onPaymentSheetPresented()
        stripeHelper.presentPaymentSheet(
            clientSecret = clientSecret,
            onSuccess = { viewModel.onPaymentSuccess() },
            onCancelled = { viewModel.onPaymentCancelled() },
            onError = { errorMessage -> viewModel.onPaymentFailed(errorMessage) })
      } else {
        viewModel.onPaymentFailed("Payment system not available. Please try again later.")
      }
    }
  }

  Surface(modifier = Modifier.fillMaxSize(), color = colorResource(id = R.color.background)) {
    Box(modifier = Modifier.fillMaxSize()) {
      Column(modifier = Modifier.fillMaxSize().background(colorResource(id = R.color.background))) {
        // Header with gradient and modern segmented control
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    colorResource(id = R.color.accent_purple).copy(alpha = 0.15f),
                                    colorResource(id = R.color.background))))
                    .padding(top = 16.dp, bottom = 24.dp)) {
              Column(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    // Modern Segmented Control
                    ModernSegmentedControl(
                        selectedTab = uiState.mainTab,
                        onTabSelected = { viewModel.selectMainTab(it) },
                        modifier =
                            Modifier.padding(horizontal = 24.dp)
                                .testTag(MyEventsTestTags.MAIN_TABS_ROW))
                  }
            }

        // Content based on selected main tab
        when (uiState.mainTab) {
          MyEventsMainTab.YOUR_TICKETS -> {
            YourTicketsSectionModern(
                currentTickets = uiState.currentTickets,
                expiredTickets = uiState.expiredTickets,
                listedTickets = uiState.listedTickets,
                selectedTab = uiState.selectedTab,
                onTabSelected = { viewModel.selectTab(it) },
                onCancelListing = { viewModel.cancelTicketListing(it) },
                userQrData = userQrData,
                isQrExpanded = uiState.isQrExpanded,
                onToggleQrExpanded = { viewModel.toggleQrExpansion() },
                modifier = Modifier.weight(1f))
          }
          MyEventsMainTab.MARKET -> {
            MarketSection(
                uiState = uiState,
                viewModel = viewModel,
                currentUserId = currentUserId,
                modifier = Modifier.weight(1f))
          }
        }
      }

      // Snackbar host for error messages
      SnackbarHost(
          hostState = snackbarHostState,
          modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = colorResource(id = R.color.surface_container),
                contentColor = colorResource(id = R.color.error_red),
                shape = RoundedCornerShape(12.dp))
          }
    }
  }

  // Sell ticket dialog
  SellTicketDialog(
      showDialog = uiState.showSellDialog,
      onDismiss = { viewModel.closeSellDialog() },
      sellableTickets = uiState.sellableTickets,
      selectedTicket = uiState.selectedTicketForSale,
      onTicketSelected = { viewModel.selectTicketForSale(it) },
      sellingPrice = uiState.sellingPrice,
      onPriceChange = { viewModel.updateSellingPrice(it) },
      onListForSale = {
        uiState.selectedTicketForSale?.let { ticket ->
          val price = uiState.sellingPrice.toDoubleOrNull() ?: 0.0
          viewModel.listTicketForSale(ticket.ticketId, price)
        }
      },
      isLoading = uiState.isLoadingMarket,
      errorMessage = uiState.marketError)
}

/**
 * Modern segmented control with pill-style tabs and smooth animations.
 *
 * @param selectedTab Currently selected main tab.
 * @param onTabSelected Callback when a tab is selected.
 * @param modifier Modifier for styling.
 */
@Composable
private fun ModernSegmentedControl(
    selectedTab: MyEventsMainTab,
    onTabSelected: (MyEventsMainTab) -> Unit,
    modifier: Modifier = Modifier
) {
  val tabs =
      listOf(
          TabItem(
              tab = MyEventsMainTab.YOUR_TICKETS,
              title = "My Tickets",
              icon = Icons.Rounded.ConfirmationNumber,
              testTag = MyEventsTestTags.MAIN_TAB_YOUR_TICKETS),
          TabItem(
              tab = MyEventsMainTab.MARKET,
              title = "Market",
              icon = Icons.Rounded.Storefront,
              testTag = MyEventsTestTags.MAIN_TAB_MARKET))

  val screenWidth = LocalConfiguration.current.screenWidthDp.dp
  val tabWidth = (screenWidth - 48.dp) / 2 // Account for padding

  // Animated indicator offset
  val indicatorOffset by
      animateDpAsState(
          targetValue = if (selectedTab == MyEventsMainTab.YOUR_TICKETS) 0.dp else tabWidth,
          animationSpec =
              spring(
                  dampingRatio = Spring.DampingRatioMediumBouncy,
                  stiffness = Spring.StiffnessMedium),
          label = "indicatorOffset")

  Box(
      modifier =
          modifier
              .fillMaxWidth()
              .height(56.dp)
              .clip(RoundedCornerShape(28.dp))
              .background(colorResource(id = R.color.surface_container))) {
        // Animated pill indicator
        Box(
            modifier =
                Modifier.offset(x = indicatorOffset + 4.dp)
                    .padding(vertical = 4.dp)
                    .width(tabWidth - 8.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors =
                                listOf(
                                    colorResource(id = R.color.accent_purple),
                                    colorResource(id = R.color.accent_purple).copy(alpha = 0.8f)))))

        // Tab items
        Row(modifier = Modifier.fillMaxSize()) {
          tabs.forEach { tabItem ->
            val isSelected = selectedTab == tabItem.tab

            val textColor by
                animateColorAsState(
                    targetValue = if (isSelected) Color.White else colorResource(id = R.color.gray),
                    label = "textColor")

            Box(
                modifier =
                    Modifier.weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null) {
                              onTabSelected(tabItem.tab)
                            }
                        .testTag(tabItem.testTag),
                contentAlignment = Alignment.Center) {
                  Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.Center) {
                        Icon(
                            imageVector = tabItem.icon,
                            contentDescription = tabItem.title,
                            tint = textColor,
                            modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tabItem.title,
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight =
                                        if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                            color = textColor)
                      }
                }
          }
        }
      }
}

/** Data class for tab items */
private data class TabItem(
    val tab: MyEventsMainTab,
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

/** Modern Your Tickets section with pill-style sub-tabs. */
@Composable
private fun YourTicketsSectionModern(
    currentTickets: List<Ticket>,
    expiredTickets: List<Ticket>,
    listedTickets: List<ListedTicket>,
    selectedTab: TicketTab,
    onTabSelected: (TicketTab) -> Unit,
    onCancelListing: (String) -> Unit,
    userQrData: String,
    isQrExpanded: Boolean,
    onToggleQrExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth()) {
    // Modern sub-tabs for Current, Expired, and Listed
    ModernSubTabs(
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        listedCount = listedTickets.size,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

    // QR Code component
    if (selectedTab !=
        TicketTab.LISTED) { // I put this so that it's visible only in the first two tabs
      QrCodeComponent(
          qrData = userQrData,
          isExpanded = isQrExpanded,
          onToggleExpanded = onToggleQrExpanded,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
    }

    // Content based on selected tab
    when (selectedTab) {
      TicketTab.CURRENT -> {
        if (currentTickets.isEmpty()) {
          EmptyState(
              title = "No Current Tickets",
              message = "You don't have any active tickets. Browse events to get started!",
              modifier = Modifier.padding(top = 32.dp),
              testTag = MyEventsTestTags.EMPTY_STATE)
        } else {
          TicketsList(tickets = currentTickets, modifier = Modifier.fillMaxWidth().weight(1f))
        }
      }
      TicketTab.EXPIRED -> {
        if (expiredTickets.isEmpty()) {
          EmptyState(
              title = "No Expired Tickets",
              message = "You don't have any expired tickets yet.",
              modifier = Modifier.padding(top = 32.dp),
              testTag = MyEventsTestTags.EMPTY_STATE)
        } else {
          TicketsList(tickets = expiredTickets, modifier = Modifier.fillMaxWidth().weight(1f))
        }
      }
      TicketTab.LISTED -> {
        if (listedTickets.isEmpty()) {
          EmptyState(
              title = "No Listed Tickets",
              message =
                  "You haven't listed any tickets for sale. Go to the Market tab to sell your tickets!",
              modifier = Modifier.padding(top = 32.dp),
              testTag = MyEventsTestTags.EMPTY_STATE)
        } else {
          ListedTicketsList(
              listedTickets = listedTickets,
              onCancelListing = onCancelListing,
              modifier = Modifier.fillMaxWidth().weight(1f))
        }
      }
    }
  }
}

/** Tickets list using LazyColumn. */
@Composable
private fun TicketsList(tickets: List<Ticket>, modifier: Modifier = Modifier) {
  LazyColumn(
      modifier = modifier,
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items = tickets, key = { it.ticketId }) { ticket ->
          TicketComponent(
              title = ticket.title,
              status = ticket.status,
              dateTime = ticket.dateTime,
              location = ticket.location,
              modifier = Modifier.testTag(MyEventsTestTags.TICKET_CARD))
        }
      }
}

/** Listed tickets list using LazyColumn with cancel listing option. */
@Composable
private fun ListedTicketsList(
    listedTickets: List<ListedTicket>,
    onCancelListing: (String) -> Unit,
    modifier: Modifier = Modifier
) {
  LazyColumn(
      modifier = modifier,
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items = listedTickets, key = { it.ticketId }) { ticket ->
          ListedTicketCard(
              ticket = ticket,
              onCancelListing = { onCancelListing(ticket.ticketId) },
              modifier = Modifier.testTag(MyEventsTestTags.TICKET_CARD))
        }
      }
}

/** Card displaying a listed ticket with cancel option. */
@Composable
private fun ListedTicketCard(
    ticket: ListedTicket,
    onCancelListing: () -> Unit,
    modifier: Modifier = Modifier
) {
  var showCancelDialog by remember { mutableStateOf(false) }

  Card(
      modifier = modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors =
          CardDefaults.cardColors(
              containerColor = colorResource(id = R.color.surface_card_color))) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
          // Header with event title
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                      text = ticket.eventTitle,
                      style =
                          MaterialTheme.typography.titleMedium.copy(
                              fontWeight = FontWeight.SemiBold),
                      color = colorResource(id = R.color.on_background),
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis)

                  Spacer(modifier = Modifier.height(4.dp))

                  Text(
                      text = ticket.eventDate,
                      style = MaterialTheme.typography.bodySmall,
                      color = colorResource(id = R.color.gray))

                  Text(
                      text = ticket.eventLocation,
                      style = MaterialTheme.typography.bodySmall,
                      color = colorResource(id = R.color.gray),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis)
                }

                // Listed badge
                Box(
                    modifier =
                        Modifier.clip(RoundedCornerShape(8.dp))
                            .background(
                                colorResource(id = R.color.accent_purple).copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)) {
                      Text(
                          text = "On Sale",
                          style =
                              MaterialTheme.typography.labelSmall.copy(
                                  fontWeight = FontWeight.SemiBold),
                          color = colorResource(id = R.color.accent_purple))
                    }
              }

          Spacer(modifier = Modifier.height(12.dp))

          // Price row
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                Column {
                  Text(
                      text = "Asking Price",
                      style = MaterialTheme.typography.labelSmall,
                      color = colorResource(id = R.color.gray))
                  Text(
                      text = "${ticket.currency} ${String.format("%.2f", ticket.listingPrice)}",
                      style =
                          MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                      color = colorResource(id = R.color.accent_purple))
                }

                Column(horizontalAlignment = Alignment.End) {
                  Text(
                      text = "Original Price",
                      style = MaterialTheme.typography.labelSmall,
                      color = colorResource(id = R.color.gray))
                  Text(
                      text = "${ticket.currency} ${String.format("%.2f", ticket.originalPrice)}",
                      style = MaterialTheme.typography.bodyMedium,
                      color = colorResource(id = R.color.on_background).copy(alpha = 0.7f))
                }
              }

          Spacer(modifier = Modifier.height(4.dp))

          Text(
              text = "Listed on ${ticket.listedAt}",
              style = MaterialTheme.typography.labelSmall,
              color = colorResource(id = R.color.gray))

          Spacer(modifier = Modifier.height(12.dp))

          // Cancel listing button
          Button(
              onClick = { showCancelDialog = true },
              modifier = Modifier.fillMaxWidth().height(44.dp),
              colors =
                  ButtonDefaults.buttonColors(
                      containerColor = colorResource(id = R.color.error_red).copy(alpha = 0.1f),
                      contentColor = colorResource(id = R.color.error_red)),
              shape = RoundedCornerShape(12.dp)) {
                Text(
                    text = "Cancel Listing",
                    style =
                        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
              }
        }
      }

  // Confirmation dialog
  if (showCancelDialog) {
    AlertDialog(
        onDismissRequest = { showCancelDialog = false },
        title = {
          Text(
              text = "Cancel Listing?",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
          Text(
              text =
                  "Are you sure you want to remove this ticket from the marketplace? You can list it again later.",
              style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
          TextButton(
              onClick = {
                showCancelDialog = false
                onCancelListing()
              }) {
                Text(text = "Cancel Listing", color = colorResource(id = R.color.error_red))
              }
        },
        dismissButton = {
          TextButton(onClick = { showCancelDialog = false }) { Text(text = "Keep Listed") }
        },
        containerColor = colorResource(id = R.color.surface_container))
  }
}

/** Modern sub-tabs with pill style for Current/Expired/Listed tickets. */
@Composable
private fun ModernSubTabs(
    selectedTab: TicketTab,
    onTabSelected: (TicketTab) -> Unit,
    listedCount: Int = 0,
    modifier: Modifier = Modifier
) {
  val tabs =
      listOf(
          TicketTab.CURRENT to "Current",
          TicketTab.EXPIRED to "Expired",
          TicketTab.LISTED to if (listedCount > 0) "Listed ($listedCount)" else "Listed")

  Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    tabs.forEach { (tab, title) ->
      val isSelected = selectedTab == tab

      val backgroundColor by
          animateColorAsState(
              targetValue =
                  if (isSelected) colorResource(id = R.color.accent_purple)
                  else colorResource(id = R.color.surface_container),
              label = "backgroundColor")

      val textColor by
          animateColorAsState(
              targetValue = if (isSelected) Color.White else colorResource(id = R.color.gray),
              label = "textColor")

      val tabTestTag =
          when (tab) {
            TicketTab.CURRENT -> MyEventsTestTags.TAB_CURRENT
            TicketTab.EXPIRED -> MyEventsTestTags.TAB_EXPIRED
            TicketTab.LISTED -> MyEventsTestTags.TAB_LISTED
          }

      Box(
          modifier =
              Modifier.clip(RoundedCornerShape(20.dp))
                  .background(backgroundColor)
                  .clickable { onTabSelected(tab) }
                  .padding(horizontal = 20.dp, vertical = 10.dp)
                  .testTag(tabTestTag),
          contentAlignment = Alignment.Center) {
            Text(
                text = title,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                color = textColor)
          }
    }
  }
}

/**
 * Market section displaying search, hot events, and available tickets.
 *
 * @param uiState Current UI state.
 * @param viewModel ViewModel for handling actions.
 * @param currentUserId Current user's ID.
 * @param modifier Modifier for styling.
 */
@Composable
private fun MarketSection(
    uiState: MyEventsUiState,
    viewModel: MyEventsViewModel,
    currentUserId: String?,
    modifier: Modifier = Modifier
) {
  Column(
      modifier =
          modifier
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .background(colorResource(id = R.color.background))) {
        Spacer(modifier = Modifier.height(8.dp))

        // Search bar
        MarketSearchBar(
            query = uiState.searchQuery,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            onClear = { viewModel.clearSearch() },
            searchResults = uiState.searchResults,
            onResultSelected = { viewModel.filterMarketBySearchResult(it) },
            isSearching = uiState.isSearching)

        Spacer(modifier = Modifier.height(16.dp))

        // Hot events section
        HotEventsSection(
            events = uiState.hotEvents,
            onEventClick = { event ->
              viewModel.filterMarketBySearchResult(SearchResult.EventResult(event))
            },
            isLoading = false)

        // Market ticket list (inline to avoid nested scrolling)
        MarketTicketListInline(
            marketTickets = uiState.marketTickets,
            onBuyTicket = { viewModel.purchaseTicket(it) },
            onSellTicket = {
              // Open sell dialog without preselecting a ticket
              viewModel.openSellDialog(null)
            },
            currentUserId = currentUserId,
            isLoading = uiState.isLoadingMarket,
            purchasingTicketId = uiState.purchasingTicketId,
            hasSellableTickets = uiState.sellableTickets.isNotEmpty())

        Spacer(modifier = Modifier.height(16.dp))
      }
}

/**
 * Inline version of MarketTicketList for use within a scroll container. Displays tickets without
 * its own lazy list to avoid nested scrolling.
 */
@Composable
private fun MarketTicketListInline(
    marketTickets: List<MarketTicket>,
    onBuyTicket: (String) -> Unit,
    onSellTicket: () -> Unit,
    currentUserId: String?,
    isLoading: Boolean,
    purchasingTicketId: String?,
    hasSellableTickets: Boolean
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    // Header row with title and sell button
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = "Available Tickets",
              style =
                  MaterialTheme.typography.titleMedium.copy(
                      fontFamily = MarcFontFamily, fontWeight = FontWeight.Bold),
              color = colorResource(id = R.color.on_background),
              modifier = Modifier.testTag(MyEventsTestTags.MARKET_TICKETS_TITLE))

          if (hasSellableTickets) {
            Button(
                onClick = onSellTicket,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.accent_purple),
                        contentColor = colorResource(id = R.color.white)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag(MyEventsTestTags.SELL_TICKET_BUTTON)) {
                  Icon(
                      imageVector = Icons.Default.Add,
                      contentDescription = "Sell",
                      modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.size(4.dp))
                  Text(
                      text = "Sell Ticket",
                      style =
                          MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                }
          }
        }

    when {
      isLoading -> {
        Box(
            modifier =
                Modifier.fillMaxWidth().height(200.dp).testTag(MyEventsTestTags.MARKET_LOADING),
            contentAlignment = Alignment.Center) {
              CircularProgressIndicator(
                  color = colorResource(id = R.color.primary), modifier = Modifier.size(40.dp))
            }
      }
      marketTickets.isEmpty() -> {
        EmptyState(
            title = "No Tickets Available",
            message = "There are no tickets listed for sale at the moment.",
            modifier = Modifier.padding(top = 32.dp, bottom = 32.dp),
            testTag = MyEventsTestTags.MARKET_EMPTY_STATE)
      }
      else -> {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
              marketTickets.forEach { ticket ->
                MarketTicketCard(
                    marketTicket = ticket,
                    onBuyClick = { onBuyTicket(ticket.ticketId) },
                    isCurrentUserSeller = ticket.sellerId == currentUserId,
                    isLoading = purchasingTicketId == ticket.ticketId,
                    modifier = Modifier.testTag(MyEventsTestTags.MARKET_TICKET_CARD))
              }
            }
      }
    }
  }
}
