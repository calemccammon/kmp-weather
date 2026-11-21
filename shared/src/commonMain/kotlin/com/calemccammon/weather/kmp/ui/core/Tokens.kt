package com.calemccammon.weather.kmp.ui.core

import androidx.compose.ui.unit.dp

/*
 * The app's own design tokens: the numbers Material 3 leaves to the app.
 * Colours and type come from the theme in `Theme.kt`. A number that only one
 * composable cares about is a named constant beside it instead.
 */

/** The steps every gap and padding is taken from. */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
}

/** Window widths at which the layout changes, after Material 3's window size classes. */
object Breakpoints {
    /** From this width up there is room for a panel beside the map. */
    val expanded = 840.dp
}

/** Icon sizes: beside a line of text, and beside the current temperature. */
object IconSizes {
    val row = 24.dp
    val current = 56.dp
}
