package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// Skema warna terang utama (White #FFFFFF + Sky Blue #38BDF8 + Deep Sky Blue #0284C7)
private val BrightWhiteSkyColorScheme = lightColorScheme(
    primary = DeepSkyBlue,
    onPrimary = PureWhite,
    primaryContainer = LightSkyBlue,
    onPrimaryContainer = DarkSlateText,
    secondary = SkyBluePrimary,
    onSecondary = DarkSlateText,
    secondaryContainer = LightSkyBlue,
    onSecondaryContainer = DarkSlateText,
    tertiary = DeepSkyBlue,
    onTertiary = PureWhite,
    tertiaryContainer = SoftSkySurface,
    onTertiaryContainer = DarkSlateText,
    background = PureWhite,
    onBackground = DarkSlateText,
    surface = PureWhite,
    onSurface = DarkSlateText,
    surfaceVariant = LightSkyBlue,
    onSurfaceVariant = SlateSecondaryText,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = SoftSkySurface,
    surfaceContainer = LightSkyBlue,
    surfaceContainerHigh = LightSkyBlue,
    surfaceContainerHighest = SkyBorder,
    outline = SkyBorder,
    outlineVariant = LightSkyBlue,
    error = StockRedText,
    onError = PureWhite,
    errorContainer = StockRedBg,
    onErrorContainer = StockRedText
)

// Variasi tema terang lembut bernuansa biru langit (#F0F9FF)
private val SoftSkyLightColorScheme = lightColorScheme(
    primary = DeepSkyBlue,
    onPrimary = PureWhite,
    primaryContainer = LightSkyBlue,
    onPrimaryContainer = DarkSlateText,
    secondary = SkyBluePrimary,
    onSecondary = DarkSlateText,
    secondaryContainer = LightSkyBlue,
    onSecondaryContainer = DarkSlateText,
    tertiary = DeepSkyBlue,
    onTertiary = PureWhite,
    tertiaryContainer = PureWhite,
    onTertiaryContainer = DarkSlateText,
    background = SoftSkySurface,
    onBackground = DarkSlateText,
    surface = SoftSkySurface,
    onSurface = DarkSlateText,
    surfaceVariant = LightSkyBlue,
    onSurfaceVariant = SlateSecondaryText,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = PureWhite,
    surfaceContainer = LightSkyBlue,
    surfaceContainerHigh = LightSkyBlue,
    surfaceContainerHighest = SkyBorder,
    outline = SkyBorder,
    outlineVariant = LightSkyBlue,
    error = StockRedText,
    onError = PureWhite,
    errorContainer = StockRedBg,
    onErrorContainer = StockRedText
)

// Bentuk sudut membulat 16dp untuk Card dan 12dp untuk komponen kecil/tombol
val EduPinjamShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

/**
 * Tema utama aplikasi EDU-PINJAM.
 * Menggunakan skema warna Sky Blue konsisten tanpa warna ungu, pink, atau hitam pekat.
 */
@Composable
fun EduPinjamTheme(
    useSoftSkyTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (useSoftSkyTheme) {
        SoftSkyLightColorScheme
    } else {
        BrightWhiteSkyColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EduPinjamTypography,
        shapes = EduPinjamShapes,
        content = content
    )
}
