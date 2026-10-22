package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val StudioDarkColorScheme = darkColorScheme(
    primary = HandBrakeAmber,
    onPrimary = Color(0xFF1A0F00),
    primaryContainer = Color(0xFF3E2200),
    onPrimaryContainer = HandBrakeAmberLight,
    secondary = HardwareCyan,
    onSecondary = Color(0xFF002026),
    secondaryContainer = Color(0xFF003B46),
    onSecondaryContainer = HardwareCyan,
    tertiary = SuccessEmerald,
    onTertiary = Color(0xFF002114),
    background = StudioDarkBackground,
    onBackground = TextPrimaryDark,
    surface = StudioDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = StudioDarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    error = ErrorCoral
)

private val StudioLightColorScheme = lightColorScheme(
    primary = HandBrakeAmberDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFF3E1900),
    secondary = HardwareCyanDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2EBF2),
    onSecondaryContainer = Color(0xFF002930),
    tertiary = SuccessEmerald,
    background = StudioLightBackground,
    onBackground = Color(0xFF0F172A),
    surface = StudioLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = StudioLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    error = ErrorCoral
)

@Composable
fun HandBrakeMobileTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else StudioLightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    HandBrakeMobileTheme(darkTheme = darkTheme, content = content)
}
