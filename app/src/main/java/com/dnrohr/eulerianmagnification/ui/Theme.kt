package com.dnrohr.eulerianmagnification.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF00BFA5),
    secondary = Color(0xFFFFC857),
    background = Color(0xFF101418),
    surface = Color(0xFF161B20),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        content = content,
    )
}
