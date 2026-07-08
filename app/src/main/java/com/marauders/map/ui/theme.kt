package com.marauders.map.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 羊皮纸底色 */
val Parchment = Color(0xFFE9D8B0)
/** 墨水色（深棕） */
val Ink = Color(0xFF3B2A1A)
/** 扫描线绿色 */
val SweepColor = Color(0xFF4A7C3F)

@Composable
fun MaraudersTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = lightColors(
            primary = Ink,
            background = Parchment,
            surface = Parchment,
            onBackground = Ink,
            onSurface = Ink
        ),
        content = content
    )
}
