package com.ibem.pedagogo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Full M3 role mapping from the web Desk's design tokens (design-research.md section 3).
// Dynamic color stays OFF by default: the handcrafted pedagogical palette is the
// brand (user-confirmed "brand continuity" direction, 2026-09-13).
private val DarkColorScheme = darkColorScheme(
    primary = SagePrimaryDark,
    onPrimary = SageOnPrimaryDark,
    primaryContainer = SageContainerDark,
    onPrimaryContainer = SageOnContainerDark,
    inversePrimary = InversePrimaryDark,
    secondary = TerracottaDark,
    onSecondary = TerracottaOnDark,
    secondaryContainer = TerracottaContainerDark,
    onSecondaryContainer = TerracottaOnContainerDark,
    tertiary = HoneyDark,
    onTertiary = HoneyOnDark,
    tertiaryContainer = HoneyContainerDark,
    onTertiaryContainer = HoneyOnContainerDark,
    background = ChalkboardDarkBg,
    onBackground = OnChalkboardText,
    surface = ChalkboardDarkSurface,
    onSurface = OnChalkboardText,
    surfaceVariant = ChalkboardDarkCard,
    onSurfaceVariant = OnChalkboardSubtle,
    surfaceTint = SurfaceTintDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    error = CalmClayErrorDark,
    onError = CalmClayOnErrorDark,
    errorContainer = CalmClayErrorContainerDark,
    onErrorContainer = CalmClayOnErrorContainerDark,
    outline = ChalkboardDarkOutline,
    outlineVariant = OutlineVariantDark,
    scrim = ScrimColor,
    surfaceBright = SurfaceBrightDark,
    surfaceDim = SurfaceDimDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark
)

private val LightColorScheme = lightColorScheme(
    primary = SagePrimary,
    onPrimary = SageOnPrimary,
    primaryContainer = SageContainer,
    onPrimaryContainer = SageOnContainer,
    inversePrimary = InversePrimaryLight,
    secondary = TerracottaSecondary,
    onSecondary = TerracottaOnSecondary,
    secondaryContainer = TerracottaContainer,
    onSecondaryContainer = TerracottaOnContainer,
    tertiary = HoneyTertiary,
    onTertiary = HoneyOnTertiary,
    tertiaryContainer = HoneyContainer,
    onTertiaryContainer = HoneyOnContainer,
    background = ParchmentBackground,
    onBackground = OnParchmentText,
    surface = ParchmentSurface,
    onSurface = OnParchmentText,
    surfaceVariant = ParchmentCard,
    onSurfaceVariant = OnParchmentSubtle,
    surfaceTint = SurfaceTintLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    error = CalmClayError,
    onError = CalmClayOnError,
    errorContainer = CalmClayErrorContainer,
    onErrorContainer = CalmClayOnErrorContainer,
    outline = ParchmentOutline,
    outlineVariant = OutlineVariantLight,
    scrim = ScrimColor,
    surfaceBright = SurfaceBrightLight,
    surfaceDim = SurfaceDimLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
)

@Composable
fun PedagogoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We intentionally default dynamicColor to false so our soothing, handcrafted
    // pedagogical palette is experienced reliably instead of system dynamic blue/purple
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = PedagogoShapes,
        content = content
    )
}
