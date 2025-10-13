package ch.onepass.onepass.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ch.onepass.onepass.R

// Marc Font Family
val MarcFontFamily =
    FontFamily(
        Font(R.font.marc_light, FontWeight.Light),
        Font(R.font.marc_light_italic, FontWeight.Light, FontStyle.Italic),
        Font(R.font.marc_regular, FontWeight.Normal),
        Font(R.font.marc_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.marc_bold, FontWeight.Bold),
        Font(R.font.marc_bold_italic, FontWeight.Bold, FontStyle.Italic))

// Set of Material typography styles to start with
val Typography =
    Typography(
        // Event card title style
        headlineMedium =
            TextStyle(
                fontFamily = MarcFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 27.6.sp),
        // Event card organizer style
        bodyLarge =
            TextStyle(
                fontFamily = MarcFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 20.8.sp),
        // Event card date and location style
        bodyMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 20.8.sp),
        // Event card price style
        headlineSmall =
            TextStyle(
                fontFamily = MarcFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 28.sp))
