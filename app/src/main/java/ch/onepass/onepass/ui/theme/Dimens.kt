package ch.onepass.onepass.ui.theme

import androidx.compose.ui.unit.dp

// Event Card dimensions
object EventCardDimens {
  // Card sizing
  val maxWidth = 450.dp // Maximum width for larger screens
  val widthFraction = 0.9f // 90% of parent width on smaller screens

  val cornerRadius = 10.dp
  val shadowElevation1 = 4.dp
  val shadowElevation2 = 6.dp
  val horizontalPadding = 1.dp
  val verticalSpacing = 1.dp

  // Image dimensions
  val imageHeightRatio = 255.98468f / 392f // Maintain aspect ratio
  val imageCornerRadius = 10.dp

  // Content padding
  val contentHorizontalPadding = 12.dp
  val likeButtonPadding = 12.dp
  val eventCardPadding = 10.dp

  // Text section dimensions
  val titleSectionHeight = 60.dp
  val titleTopPadding = 9.dp
  val sectionSpacing = 15.dp
  val dateLocationSpacing = 10.dp
  val locationPriceSpacing = 8.dp
}
