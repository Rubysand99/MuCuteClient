package com.mucheng.mucute.client.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0A8FF),
    onPrimary = Color(0xFF3A0A6B),
    primaryContainer = Color(0xFF5A2A94),
    onPrimaryContainer = Color(0xFFEBDDFF),
    secondary = Color(0xFFCDC0E0),
    onSecondary = Color(0xFF342B47),
    secondaryContainer = Color(0xFF4B4260),
    onSecondaryContainer = Color(0xFFE9DDFD),
    tertiary = Color(0xFFF2B7DB),
    onTertiary = Color(0xFF4A1F3D)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6A1B9A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEBDDFF),
    onPrimaryContainer = Color(0xFF2A0050),
    secondary = Color(0xFF665A7A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDDDFF),
    onSecondaryContainer = Color(0xFF211735),
    tertiary = Color(0xFF8E4A78),
    onTertiary = Color(0xFFFFFFFF)
)

@Composable
fun MuCuteClientTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
        content = content
    )
}