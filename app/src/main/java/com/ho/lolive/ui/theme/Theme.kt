package com.ho.lolive.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme

val LightColors = lightColorScheme(
    primary = OceanBlue,
    secondary = SoftBlue,
    background = Sand,
    surface = ColorTokens.surfaceLight,
)

val DarkColors = darkColorScheme(
    primary = SoftBlue,
    secondary = OceanBlue,
    background = Night,
    surface = ColorTokens.surfaceDark,
)

object ColorTokens {
    val surfaceLight = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
    val surfaceDark = androidx.compose.ui.graphics.Color(0xFF1C2541)
}

@Composable
fun LoliveTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
