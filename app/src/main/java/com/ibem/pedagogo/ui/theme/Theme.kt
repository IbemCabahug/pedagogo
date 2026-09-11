package com.ibem.pedagogo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SagePrimaryDark,
    onPrimary = SageOnPrimaryDark,
    primaryContainer = SageContainerDark,
    onPrimaryContainer = SageOnContainerDark,
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
    outline = ChalkboardDarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = SagePrimary,
    onPrimary = SageOnPrimary,
    primaryContainer = SageContainer,
    onPrimaryContainer = SageOnContainer,
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
    outline = ParchmentOutline
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
        content = content
    )
}

