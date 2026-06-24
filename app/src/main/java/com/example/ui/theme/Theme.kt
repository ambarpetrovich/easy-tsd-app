package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = IndAccent, 
    onPrimary = IndTextDark,
    secondary = IndDarkLighter,
    onSecondary = IndTextLight,
    background = IndDark,
    surface = IndDark,
    onBackground = IndTextLight,
    onSurface = IndTextLight
  )

private val LightColorScheme =
  lightColorScheme(
    primary = IndAccent,
    onPrimary = IndTextDark,
    secondary = IndDarkLighter,
    onSecondary = IndTextLight,
    background = IndBg,
    surface = IndBg,
    onBackground = IndTextDark,
    onSurface = IndTextDark
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Disable dynamic colors to enforce the industrial look
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
