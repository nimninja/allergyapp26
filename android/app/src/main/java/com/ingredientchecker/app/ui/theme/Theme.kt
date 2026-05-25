package com.ingredientchecker.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF1A5F4A)
private val LightColors = lightColorScheme(primary = Green)
private val DarkColors = darkColorScheme(primary = Color(0xFF4CAF88))

@Composable
fun IngredientCheckerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
