package com.calemccammon.weather.kmp.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calemccammon.weather.kmp.ui.core.IconSizes
import com.calemccammon.weather.kmp.ui.core.Spacing
import com.calemccammon.weather.kmp.ui.core.asString
import com.calemccammon.weather.kmp.ui.home.models.CurrentUiModel
import com.calemccammon.weather.kmp.ui.home.models.DayUiModel
import com.calemccammon.weather.kmp.ui.home.models.ForecastState
import com.calemccammon.weather.kmp.ui.home.models.ForecastUiModel
import com.calemccammon.weather.kmp.ui.home.models.HomeState
import com.calemccammon.weather.kmp.ui.home.models.UnitsUiModel
import kmp_weather.shared.generated.resources.Res
import kmp_weather.shared.generated.resources.forecast_credit
import kmp_weather.shared.generated.resources.forecast_hint
import kmp_weather.shared.generated.resources.forecast_loading
import kmp_weather.shared.generated.resources.forecast_no_days
import kmp_weather.shared.generated.resources.forecast_this_week
import kmp_weather.shared.generated.resources.try_again
import kmp_weather.shared.generated.resources.units_celsius
import kmp_weather.shared.generated.resources.units_celsius_name
import kmp_weather.shared.generated.resources.units_fahrenheit
import kmp_weather.shared.generated.resources.units_fahrenheit_name
import org.jetbrains.compose.resources.stringResource

/**
 * Current conditions and the week ahead for the selected place. It scrolls,
 * so it can live inside the bottom sheet or the side panel.
 */
@Composable
fun ForecastPanel(
    state: HomeState,
    onUnitsChange: (UnitsUiModel) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Spacing.xl),
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        state.place?.let { place ->
            Column {
                Text(
                    place.title.asString(),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                if (place.subtitle.isNotEmpty()) {
                    Text(
                        place.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        when (val forecast = state.forecast) {
            is ForecastState.Loaded -> Loaded(forecast.forecast, state.units, onUnitsChange)
            else -> ForecastStatus(forecast, onRetry)
        }
    }
}

/**
 * What is happening when there is no forecast to show. A screen reader reads
 * it out as it changes: loading, then failed.
 */
@Composable
private fun ForecastStatus(
    forecast: ForecastState,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().announced(), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        when (forecast) {
            ForecastState.Idle ->
                Message(
                    Icons.Outlined.TouchApp,
                    stringResource(Res.string.forecast_hint),
                    iconTint = MaterialTheme.colorScheme.primary,
                )
            ForecastState.Loading -> {
                val loading = stringResource(Res.string.forecast_loading)
                LinearProgressIndicator(
                    Modifier.fillMaxWidth().padding(vertical = Spacing.xl).semantics { contentDescription = loading },
                )
            }
            is ForecastState.Failed -> {
                Message(
                    Icons.Filled.CloudOff,
                    forecast.message.asString(),
                    iconTint = MaterialTheme.colorScheme.error,
                )
                FilledTonalButton(onClick = onRetry, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(Res.string.try_again))
                }
            }
            is ForecastState.Loaded -> Unit
        }
    }
}

@Composable
private fun Loaded(
    forecast: ForecastUiModel,
    units: UnitsUiModel,
    onUnitsChange: (UnitsUiModel) -> Unit,
) {
    Current(forecast.current)

    // The units choice drops below the heading when the text is too large for
    // both on one line.
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.forecast_this_week),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        UnitsChoice(units, onUnitsChange)
    }

    Week(forecast.days)

    Text(
        stringResource(Res.string.forecast_credit),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Current(current: CurrentUiModel) {
    val description = current.description.asString()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    // Read out as one sentence. Wraps onto a second line when the text is too
    // large for one.
    FlowRow(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            Icon(
                current.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(IconSizes.current),
            )
            Text(current.temperature.asString(), style = MaterialTheme.typography.displayMedium)
        }
        Column {
            Text(current.condition.asString(), style = MaterialTheme.typography.titleMedium)
            Text(current.feelsLike.asString(), style = MaterialTheme.typography.bodyMedium, color = muted)
            Text(current.wind.asString(), style = MaterialTheme.typography.bodyMedium, color = muted)
        }
    }
}

@Composable
private fun UnitsChoice(
    selected: UnitsUiModel,
    onChange: (UnitsUiModel) -> Unit,
) {
    val choices =
        listOf(
            Triple(UnitsUiModel.Celsius, Res.string.units_celsius, Res.string.units_celsius_name),
            Triple(UnitsUiModel.Fahrenheit, Res.string.units_fahrenheit, Res.string.units_fahrenheit_name),
        )

    SingleChoiceSegmentedButtonRow {
        choices.forEachIndexed { index, (units, label, name) ->
            val spoken = stringResource(name)
            SegmentedButton(
                selected = units == selected,
                onClick = { onChange(units) },
                shape = SegmentedButtonDefaults.itemShape(index, choices.size),
                icon = {},
                modifier = Modifier.semantics { contentDescription = spoken },
            ) {
                Text(stringResource(label))
            }
        }
    }
}

@Composable
private fun Week(days: List<DayUiModel>) {
    if (days.isEmpty()) {
        Text(stringResource(Res.string.forecast_no_days), style = MaterialTheme.typography.bodyMedium)
        return
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = Spacing.sm)) {
            days.forEach { DayRow(it) }
        }
    }
}

@Composable
private fun DayRow(day: DayUiModel) {
    val description = day.description.asString()
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme
    // Column widths are in text units, so they grow with the user's text size
    // and nothing wraps.
    val (dayWidth, chanceWidth, temperatureWidth) =
        with(LocalDensity.current) {
            Triple(DAY_COLUMN.sp.toDp(), CHANCE_COLUMN.sp.toDp(), TEMPERATURE_COLUMN.sp.toDp())
        }
    val fixedWidth = dayWidth + IconSizes.row + chanceWidth + temperatureWidth * 2 + Spacing.sm + Spacing.md

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            // Read out as one sentence.
            .clearAndSetSemantics { contentDescription = description }
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        // Very large text, or a very narrow window: the numbers matter more
        // than the bar.
        val hasRoomForBar = maxWidth - fixedWidth >= minBarWidth

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(day.dayLabel.asString(), Modifier.width(dayWidth), style = typography.bodyLarge, maxLines = 1)
            Icon(day.icon, contentDescription = null, Modifier.size(IconSizes.row), tint = colors.primary)
            Text(
                day.chance?.asString().orEmpty(),
                Modifier.width(chanceWidth),
                style = typography.labelMedium,
                color = colors.tertiary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                day.low.asString(),
                Modifier.width(temperatureWidth),
                style = typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
            if (hasRoomForBar) {
                RangeBar(
                    start = day.barStart,
                    end = day.barEnd,
                    modifier = Modifier.weight(1f).padding(start = Spacing.sm, end = Spacing.md),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            Text(
                day.high.asString(),
                Modifier.width(temperatureWidth),
                style = typography.bodyLarge,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
        }
    }
}

/**
 * A track for the week's whole temperature range with one day's part of it
 * filled in, from [start] to [end] (each 0 to 1).
 */
@Composable
private fun RangeBar(
    start: Float,
    end: Float,
    modifier: Modifier = Modifier,
) {
    val track = MaterialTheme.colorScheme.surface
    val fill = MaterialTheme.colorScheme.primary

    Canvas(modifier.height(barThickness)) {
        val round = CornerRadius(size.height / 2)
        // Never thinner than the bar is tall, or a day whose low and high
        // match would vanish; and never pushed off the end of the track.
        val width = (size.width * (end - start)).coerceIn(minOf(size.height, size.width), size.width)
        val left = (size.width * start).coerceAtMost(size.width - width)

        drawRoundRect(track, cornerRadius = round)
        drawRoundRect(fill, topLeft = Offset(left, 0f), size = Size(width, size.height), cornerRadius = round)
    }
}

// Column widths at the default text size: room for "Today", "100%" and "-88°".
private const val DAY_COLUMN = 52
private const val CHANCE_COLUMN = 44
private const val TEMPERATURE_COLUMN = 36

private val barThickness: Dp = 6.dp

/** The range bar is dropped when it would be shorter than this. */
private val minBarWidth: Dp = 24.dp
