package ch.onepass.onepass.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ch.onepass.onepass.R

val MarcFontFamily =
    FontFamily(
        Font(R.font.marc_light, FontWeight.Light),
        Font(R.font.marc_light_italic, FontWeight.Light, FontStyle.Italic),
        Font(R.font.marc_regular, FontWeight.Normal),
        Font(R.font.marc_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.marc_bold, FontWeight.Bold),
        Font(R.font.marc_bold_italic, FontWeight.Bold, FontStyle.Italic))

val Typography =
    Typography(
        titleLarge =
            TextStyle(
                fontFamily = MarcFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp),
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp),
        bodyMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp),
        labelSmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp),
        labelMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp))
