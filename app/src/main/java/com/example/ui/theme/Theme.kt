package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ThemePrimary,
    primaryContainer = ThemePrimaryContainer,
    onPrimaryContainer = ThemeOnPrimaryContainer,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = ThemeBackground,
    surface = ThemeBackground,
    onBackground = ThemeTextDark,
    onSurface = ThemeTextDark,
    surfaceVariant = ThemeSurfaceVariant,
    onSurfaceVariant = ThemeOnSurfaceVariant,
    outline = ThemeBorder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Disable dynamic colors to ensure Bold Typography theme is perfectly applied
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
