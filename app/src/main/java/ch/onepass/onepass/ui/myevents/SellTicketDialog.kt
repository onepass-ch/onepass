package ch.onepass.onepass.ui.myevents

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.onepass.onepass.R
import ch.onepass.onepass.ui.theme.MarcFontFamily
import ch.onepass.onepass.utils.FormatUtils.formatPriceCompact

/**
 * Bottom sheet dialog for selling a ticket with dropdown ticket selection.
 *
 * @param showDialog Whether to show the dialog.
 * @param onDismiss Callback when the dialog is dismissed.
 * @param sellableTickets List of tickets the user can sell.
 * @param selectedTicket Currently selected ticket for sale (null if none).
 * @param onTicketSelected Callback when a ticket is selected.
 * @param sellingPrice Current selling price input.
 * @param onPriceChange Callback when the price changes.
 * @param onListForSale Callback when list for sale is clicked.
 * @param isLoading Whether a listing operation is in progress.
 * @param errorMessage Error message to display (null if no error).
 * @param modifier Modifier for styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
// This sonar warning won't be fixed.
fun SellTicketDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    sellableTickets: List<SellableTicket>,
    selectedTicket: SellableTicket?,
    onTicketSelected: (SellableTicket) -> Unit,
    sellingPrice: String,
    onPriceChange: (String) -> Unit,
    onListForSale: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
  if (!showDialog) return

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var dropdownExpanded by remember { mutableStateOf(false) }

  ModalBottomSheet(
      onDismissRequest = onDismiss,
      sheetState = sheetState,
      containerColor = colorScheme.surface,
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
      modifier = modifier.testTag(MyEventsTestTags.SELL_DIALOG)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
              // Header
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.sell_dialog_title),
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontFamily = MarcFontFamily, fontWeight = FontWeight.Bold),
                        color = colorScheme.onBackground,
                        modifier = Modifier.testTag(MyEventsTestTags.SELL_DIALOG_TITLE))

                    IconButton(onClick = onDismiss) {
                      Icon(
                          imageVector = Icons.Default.Close,
                          contentDescription =
                              stringResource(R.string.sell_dialog_close_description),
                          tint = colorScheme.onSurface)
                    }
                  }

              // Error message
              if (!errorMessage.isNullOrEmpty()) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorScheme.error)
                            .padding(12.dp)) {
                      Text(
                          text = errorMessage,
                          style = MaterialTheme.typography.bodySmall,
                          color = colorScheme.error,
                          modifier = Modifier.testTag(MyEventsTestTags.SELL_DIALOG_ERROR))
                    }
              }

              // Ticket Selection Dropdown
              Text(
                  text = stringResource(R.string.sell_dialog_select_ticket),
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                  color = colorScheme.onBackground)

              if (sellableTickets.isEmpty()) {
                // No tickets available
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorScheme.surface),
                    contentAlignment = Alignment.Center) {
                      Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.ConfirmationNumber,
                            contentDescription = null,
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.sell_dialog_no_tickets),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurface)
                      }
                    }
              } else {
                // Dropdown selector
                Box(modifier = Modifier.fillMaxWidth()) {
                  TicketDropdownSelector(
                      selectedTicket = selectedTicket,
                      isExpanded = dropdownExpanded,
                      onClick = { dropdownExpanded = !dropdownExpanded },
                      modifier =
                          Modifier.fillMaxWidth().testTag(MyEventsTestTags.SELL_DIALOG_TICKET_LIST))

                  DropdownMenu(
                      expanded = dropdownExpanded,
                      onDismissRequest = { dropdownExpanded = false },
                      modifier = Modifier.fillMaxWidth(0.9f).background(colorScheme.surface)) {
                        sellableTickets.forEach { ticket ->
                          DropdownMenuItem(
                              text = {
                                TicketDropdownItem(
                                    ticket = ticket, isSelected = ticket == selectedTicket)
                              },
                              onClick = {
                                onTicketSelected(ticket)
                                dropdownExpanded = false
                              })
                        }
                      }
                }
              }

              // Price input section (only show when ticket is selected)
              if (selectedTicket != null) {
                Spacer(modifier = Modifier.height(8.dp))

                // Selected ticket preview
                SelectedTicketPreview(ticket = selectedTicket)

                Spacer(modifier = Modifier.height(8.dp))

                // Price input
                Text(
                    text = stringResource(R.string.sell_dialog_set_price),
                    style =
                        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = colorScheme.onBackground)

                OutlinedTextField(
                    value = sellingPrice,
                    onValueChange = { value ->
                      // Only allow numeric input with optional decimal
                      if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        onPriceChange(value)
                      }
                    },
                    modifier =
                        Modifier.fillMaxWidth().testTag(MyEventsTestTags.SELL_DIALOG_PRICE_INPUT),
                    placeholder = { Text(stringResource(R.string.sell_dialog_price_placeholder)) },
                    leadingIcon = {
                      Text(
                          text = stringResource(R.string.sell_dialog_currency),
                          style =
                              MaterialTheme.typography.bodyLarge.copy(
                                  fontWeight = FontWeight.SemiBold),
                          color = colorScheme.primary,
                          modifier = Modifier.padding(start = 16.dp))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = colorScheme.surface,
                            unfocusedContainerColor = colorScheme.surface,
                            focusedIndicatorColor = colorScheme.primary,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = colorScheme.onBackground,
                            unfocusedTextColor = colorScheme.onBackground),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle =
                        MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))

                // Original price hint
                Text(
                    text =
                        stringResource(
                            R.string.sell_dialog_original_price,
                            formatPriceCompact(selectedTicket.originalPrice)),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurface)
              }

              Spacer(modifier = Modifier.height(8.dp))

              // List for sale button
              Button(
                  onClick = onListForSale,
                  enabled =
                      !isLoading &&
                          selectedTicket != null &&
                          sellingPrice.isNotEmpty() &&
                          isValidPrice(sellingPrice),
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(56.dp)
                          .testTag(MyEventsTestTags.SELL_DIALOG_CONFIRM_BUTTON),
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = colorScheme.primary,
                          contentColor = colorScheme.onBackground,
                          disabledContainerColor = colorScheme.primary.copy(alpha = 0.4f),
                          disabledContentColor = colorScheme.onBackground.copy(alpha = 0.6f)),
                  shape = RoundedCornerShape(16.dp)) {
                    if (isLoading) {
                      CircularProgressIndicator(
                          modifier = Modifier.size(24.dp),
                          color = colorScheme.onBackground,
                          strokeWidth = 2.dp)
                    } else {
                      Text(
                          text = stringResource(R.string.sell_dialog_list_button),
                          style =
                              MaterialTheme.typography.bodyLarge.copy(
                                  fontWeight = FontWeight.SemiBold))
                    }
                  }

              // Cancel button
              Button(
                  onClick = onDismiss,
                  enabled = !isLoading,
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(50.dp)
                          .testTag(MyEventsTestTags.SELL_DIALOG_CANCEL_BUTTON),
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = Color.Transparent, contentColor = colorScheme.onSurface),
                  shape = RoundedCornerShape(12.dp)) {
                    Text(
                        text = stringResource(R.string.sell_dialog_cancel_button),
                        style =
                            MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                  }
            }
      }
}

/** Dropdown selector showing the currently selected ticket or placeholder. */
@Composable
private fun TicketDropdownSelector(
    selectedTicket: SellableTicket?,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  val borderColor by
      animateColorAsState(
          targetValue =
              if (isExpanded) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.3f),
          label = "borderColor")

  Card(
      modifier =
          modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .border(1.dp, borderColor, RoundedCornerShape(12.dp))
              .clickable(onClick = onClick),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              if (selectedTicket != null) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically) {
                      // Ticket icon
                      Box(
                          modifier =
                              Modifier.size(40.dp)
                                  .clip(RoundedCornerShape(10.dp))
                                  .background(colorScheme.primary.copy(alpha = 0.15f)),
                          contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.ConfirmationNumber,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(24.dp))
                          }

                      Spacer(modifier = Modifier.width(12.dp))

                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedTicket.eventTitle,
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold),
                            color = colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Text(
                            text = selectedTicket.eventDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                      }
                    }
              } else {
                Text(
                    text = stringResource(R.string.sell_dialog_choose_ticket),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface,
                    modifier = Modifier.weight(1f))
              }

              Icon(
                  imageVector =
                      if (isExpanded) Icons.Default.KeyboardArrowUp
                      else Icons.Default.KeyboardArrowDown,
                  contentDescription =
                      if (isExpanded) stringResource(R.string.sell_dialog_collapse_description)
                      else stringResource(R.string.sell_dialog_expand_description),
                  tint = colorScheme.onSurface)
            }
      }
}

/** Individual ticket item in the dropdown. */
@Composable
private fun TicketDropdownItem(ticket: SellableTicket, isSelected: Boolean) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically) {
        // Ticket icon
        Box(
            modifier =
                Modifier.size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) colorScheme.primary.copy(alpha = 0.15f)
                        else colorScheme.surface),
            contentAlignment = Alignment.Center) {
              Icon(
                  imageVector = Icons.Rounded.ConfirmationNumber,
                  contentDescription = null,
                  tint = if (isSelected) colorScheme.primary else colorScheme.onSurface,
                  modifier = Modifier.size(20.dp))
            }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
              text = ticket.eventTitle,
              style =
                  MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
              color = colorScheme.onBackground,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis)
          Text(
              text =
                  stringResource(
                      R.string.sell_dialog_ticket_details, ticket.eventDate, ticket.eventLocation),
              style = MaterialTheme.typography.bodySmall,
              color = colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis)
        }

        if (isSelected) {
          Box(
              modifier =
                  Modifier.size(20.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(colorScheme.primary),
              contentAlignment = Alignment.Center) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onBackground)
              }
        }
      }
}

/** Preview card showing the selected ticket details. */
@Composable
private fun SelectedTicketPreview(ticket: SellableTicket) {
  Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = colorScheme.primary.copy(alpha = 0.1f))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
              Box(
                  modifier =
                      Modifier.size(48.dp)
                          .clip(RoundedCornerShape(12.dp))
                          .background(colorScheme.primary.copy(alpha = 0.2f)),
                  contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.ConfirmationNumber,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(28.dp))
                  }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ticket.eventTitle,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(
                    text = ticket.eventDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurface)
                Text(
                    text = ticket.eventLocation,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurface)
              }

              Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.sell_dialog_original_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurface)
                Text(
                    text =
                        stringResource(
                            R.string.sell_dialog_currency_price,
                            formatPriceCompact(ticket.originalPrice)),
                    style =
                        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colorScheme.onBackground)
              }
            }
      }
}

/**
 * Validates if a price string is valid.
 *
 * @param price The price string to validate.
 * @return True if the price is valid (positive number).
 */
private fun isValidPrice(price: String): Boolean {
  return try {
    val value = price.toDouble()
    value > 0
  } catch (e: NumberFormatException) {
    false
  }
}
