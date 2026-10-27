package com.mucheng.mucute.client.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box

val NeonViolet = Color(0xFFB466FF)
val NeonPink = Color(0xFFFF4FD8)
val NeonCyan = Color(0xFF22E5FF)

/** Gradient viền phát sáng dùng cho thẻ và đường kẻ. */
val NeonBrush: Brush
    get() = Brush.linearGradient(listOf(NeonViolet, NeonPink, NeonCyan))

private val NeonVerticalBrush = Brush.verticalGradient(listOf(NeonViolet, NeonPink, NeonCyan))

private val DarkColorScheme = darkColorScheme(
    primary = NeonViolet,
    onPrimary = Color(0xFF1A0036),
    primaryContainer = Color(0xFF3B1478),
    onPrimaryContainer = Color(0xFFEBD9FF),
    secondary = NeonPink,
    onSecondary = Color(0xFF3A002F),
    secondaryContainer = Color(0xFF5A0F4A),
    onSecondaryContainer = Color(0xFFFFD6F5),
    tertiary = NeonCyan,
    onTertiary = Color(0xFF00363F),
    tertiaryContainer = Color(0xFF004E5B),
    onTertiaryContainer = Color(0xFFB8F3FF),
    background = Color(0xFF07040F),
    onBackground = Color(0xFFEDE7FF),
    surface = Color(0xFF0B0716),
    onSurface = Color(0xFFEDE7FF),
    surfaceVariant = Color(0xFF1B1230),
    onSurfaceVariant = Color(0xFFC9BCE6),
    surfaceContainerLowest = Color(0xFF05030B),
    surfaceContainerLow = Color(0xFF0E0919),
    surfaceContainer = Color(0xFF120C22),
    surfaceContainerHigh = Color(0xFF18102C),
    surfaceContainerHighest = Color(0xFF1E1536),
    outline = Color(0xFF8A5CD6),
    outlineVariant = Color(0xFF5A2FB0)
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

private val NeonShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val NeonTypography = Typography().let { base ->
    base.copy(
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
        bodyLarge = base.bodyLarge.copy(fontWeight = FontWeight.Medium)
    )
}

/** Mặc định luôn dùng nền tối neon, không phụ thuộc chế độ sáng/tối của hệ thống. */
@Composable
fun MuCuteClientTheme(
    darkTheme: Boolean = true,
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
        shapes = NeonShapes,
        typography = NeonTypography,
        content = content
    )
}

@Composable
fun neonRailItemColors(): NavigationRailItemColors =
    NavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

/** Đường kẻ dọc phát sáng gradient tím – hồng – xanh. */
@Composable
fun NeonVerticalDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxHeight()
            .width(1.5.dp)
            .background(NeonVerticalBrush)
    )
}
