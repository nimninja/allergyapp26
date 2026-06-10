package com.ingredientchecker.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = AuraForest,
    onPrimary = AuraCream,
    primaryContainer = AuraSuccessContainer,
    onPrimaryContainer = AuraForest,
    secondary = AuraSage,
    onSecondary = AuraCream,
    secondaryContainer = AuraSurfaceVariant,
    onSecondaryContainer = AuraOnSurface,
    tertiary = AuraEmerald,
    onTertiary = AuraCream,
    background = AuraCream,
    onBackground = AuraOnSurface,
    surface = AuraSurface,
    onSurface = AuraOnSurface,
    surfaceVariant = AuraSurfaceVariant,
    onSurfaceVariant = AuraOnSurfaceMuted,
    error = AuraError,
    onError = AuraCream,
    errorContainer = AuraErrorContainer,
    onErrorContainer = AuraError,
    outline = AuraSage.copy(alpha = 0.4f),
    outlineVariant = AuraSurfaceVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = AuraMint,
    onPrimary = AuraDarkBg,
    primaryContainer = AuraDarkSurfaceVariant,
    onPrimaryContainer = AuraMint,
    secondary = AuraSage,
    onSecondary = AuraDarkBg,
    secondaryContainer = AuraDarkSurfaceVariant,
    onSecondaryContainer = AuraDarkOnSurface,
    tertiary = AuraEmerald,
    onTertiary = AuraDarkBg,
    background = AuraDarkBg,
    onBackground = AuraDarkOnSurface,
    surface = AuraDarkSurface,
    onSurface = AuraDarkOnSurface,
    surfaceVariant = AuraDarkSurfaceVariant,
    onSurfaceVariant = AuraDarkOnSurfaceMuted,
    error = AuraError,
    onError = AuraCream,
    errorContainer = AuraError.copy(alpha = 0.2f),
    onErrorContainer = AuraErrorContainer,
    outline = AuraSage.copy(alpha = 0.3f),
    outlineVariant = AuraDarkSurfaceVariant,
)

@Composable
fun IngredientCheckerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
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
        typography = AuraTypography,
        shapes = AuraShapes,
        content = content,
    )
}
