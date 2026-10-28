package com.etsviewer.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object AppTokens {
    val micro = 4.dp; val small = 8.dp; val compact = 12.dp; val medium = 16.dp
    val large = 24.dp; val xlarge = 32.dp; val touch = 48.dp; val control = 52.dp
    val icon = 24.dp; val statusIcon = 18.dp; val progress = 32.dp
    val pageWidth = 840.dp; val formWidth = 480.dp
    val mediaHeight = 220.dp; val imageMin = 80.dp; val imageMax = 360.dp; val browserMin = 320.dp
    val border = 1.dp; val flat = 0.dp; val toolbar = 56.dp
}
val AppLightColors = lightColorScheme(
    primary = Color(0xFF6750A4), onPrimary = Color.White, primaryContainer = Color(0xFFEADDFF), onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71), onSecondary = Color.White, secondaryContainer = Color(0xFFE8DEF8), onSecondaryContainer = Color(0xFF1D192B),
    background = Color(0xFFF8F7FC), onBackground = Color(0xFF1D1B20), surface = Color(0xFFFFFBFE), onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC), onSurfaceVariant = Color(0xFF49454F), outline = Color(0xFF79747E), outlineVariant = Color(0xFFCAC4D0),
    error = Color(0xFFBA1A1A), onError = Color.White, errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF322F38), inverseOnSurface = Color(0xFFF5EFF7), inversePrimary = Color(0xFFD0BCFF),
    surfaceContainer = Color(0xFFF2ECF4), surfaceContainerLow = Color(0xFFF7F2FA), surfaceContainerHigh = Color(0xFFECE6F0),
)
val AppDarkColors = darkColorScheme(
    primary = Color(0xFFC8B4F7), onPrimary = Color(0xFF302046), primaryContainer = Color(0xFF4C3E66), onPrimaryContainer = Color(0xFFF0E6FF),
    secondary = Color(0xFFCCC2DC), onSecondary = Color(0xFF332D41), secondaryContainer = Color(0xFF4A4458), onSecondaryContainer = Color(0xFFE8DEF8),
    background = Color(0xFF18151E), onBackground = Color(0xFFF4F0F8), surface = Color(0xFF292430), onSurface = Color(0xFFF4F0F8),
    surfaceVariant = Color(0xFF38323F), onSurfaceVariant = Color(0xFFB3ACBF), outline = Color(0xFF93899E), outlineVariant = Color(0xFF504758),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005), errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFE6E0E9), inverseOnSurface = Color(0xFF322F38), inversePrimary = Color(0xFF6750A4),
    surfaceContainer = Color(0xFF292430), surfaceContainerLow = Color(0xFF221D29), surfaceContainerHigh = Color(0xFF38323F),
)
data class StatusColors(val success: Color, val onSuccess: Color, val warning: Color, val onWarning: Color,
    val answer: Color, val onAnswer: Color, val answerAccent: Color, val media: Color, val onMedia: Color)
val AppLightStatusColors = StatusColors(Color(0xFFD7F1E0), Color(0xFF173D29), Color(0xFFFFF2DB), Color(0xFF683F0B),
    Color(0xFFDCF6E7), Color(0xFF12392A), Color(0xFF286544), Color(0xFFF0E9EF), Color(0xFF5A4E5B))
val AppDarkStatusColors = StatusColors(Color(0xFF17392F), Color(0xFFD4FFEA), Color(0xFF352919), Color(0xFFF3D59F),
    Color(0xFF17392F), Color(0xFFD4FFEA), Color(0xFF91E9C1), Color(0xFF38323F), Color(0xFFD9C1B1))
val LocalStatusColors = staticCompositionLocalOf { AppLightStatusColors }
val AppShapes = Shapes(extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(16.dp), extraLarge = RoundedCornerShape(24.dp))
val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 42.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 38.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 30.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 26.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 24.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 18.sp),
)
@Composable fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalStatusColors provides if (darkTheme) AppDarkStatusColors else AppLightStatusColors) {
        MaterialTheme(if (darkTheme) AppDarkColors else AppLightColors, AppShapes, AppTypography, content)
    }
}
