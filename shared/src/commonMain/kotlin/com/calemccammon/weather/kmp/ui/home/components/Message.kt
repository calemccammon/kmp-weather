package com.calemccammon.weather.kmp.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.calemccammon.weather.kmp.ui.core.Spacing

/** An icon beside a line or two of text: a hint, or word of what went wrong. */
@Composable
fun Message(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Icon(icon, contentDescription = null, tint = iconTint)
        Text(text, style = MaterialTheme.typography.bodyLarge, color = textColor, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

/** Marks something a screen reader announces when it appears or changes. */
fun Modifier.announced(): Modifier = semantics { liveRegion = LiveRegionMode.Polite }
