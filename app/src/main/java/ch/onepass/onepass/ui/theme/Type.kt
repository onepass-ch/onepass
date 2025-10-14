package ch.onepass.onepass.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ch.onepass.onepass.R

// Primary font - MARC
val MarcFontFamily =
    FontFamily(
        Font(R.font.marc_light, FontWeight.Light),
        Font(R.font.marc_light_italic, FontWeight.Light, FontStyle.Italic),
        Font(R.font.marc_regular, FontWeight.Normal),
        Font(R.font.marc_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.marc_bold, FontWeight.Bold),
        Font(R.font.marc_bold_italic, FontWeight.Bold, FontStyle.Italic))

// Secondary font - Inter Variable
val InterFontFamily =
    FontFamily(
        Font(R.font.inter_variable_font, FontWeight.Light),
        Font(R.font.inter_variable_font, FontWeight.Normal),
        Font(R.font.inter_variable_font, FontWeight.Medium),
        Font(R.font.inter_variable_font, FontWeight.SemiBold),
        Font(R.font.inter_variable_font, FontWeight.Bold))

val Typography =
    Typography(
        // Display styles - Primary font (MARC)
        displayLarge =
            TextStyle(
                fontFamily = MarcFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 57.sp,
                lineHeight = 64.sp,
                letterSpacing = (-0.25).sp),
        displayMedium =
            TextStyle(
                fontFamily = MarcFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 45.sp,
                lineHeight = 52.sp,
                letterSpacing = 0.sp),
        displaySmall =
            TextStyle(
                fontFamily = MarcFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                lineHeight = 44.sp,
                letterSpacing = 0.sp),
        // Headline styles - Primary font (MARC)
        headlineLarge =
            TextStyle(
                fontFamily = MarcFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = 0.sp),
        headlineMedium =
            TextStyle(
                fontFamily = MarcFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.sp),
        headlineSmall =
            TextStyle(
                fontFamily = MarcFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp),
        // Title styles - Primary font (MARC)
        titleLarge =
            TextStyle(
                fontFamily = MarcFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp),
        titleMedium =
            TextStyle(
                fontFamily = MarcFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp),
        titleSmall =
            TextStyle(
                fontFamily = MarcFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp),
        // Body styles - Secondary font (Inter)
        bodyLarge =
            TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp),
        bodyMedium =
            TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp),
        bodySmall =
            TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp),
        // Label styles - Secondary font (Inter)
        labelLarge =
            TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp),
        labelMedium =
            TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp),
        labelSmall =
            TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp))
