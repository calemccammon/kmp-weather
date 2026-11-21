package com.calemccammon.weather.kmp.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.calemccammon.weather.kmp.ui.core.Spacing
import kmp_weather.shared.generated.resources.Res
import kmp_weather.shared.generated.resources.map_credit
import kmp_weather.shared.generated.resources.map_label
import kmp_weather.shared.generated.resources.selected_place
import org.jetbrains.compose.resources.stringResource
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.map.CameraConstraints
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.overlay.LocalViewportInsets
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import kotlin.time.Duration.Companion.milliseconds

/**
 * The map behind everything else. A press-and-hold, or a click of the
 * mouse's second button, picks the point under it.
 *
 * [covered] is how much of each edge the search bar and the forecast cover.
 * The pin is brought to the middle of what is left, and the map's credit is
 * drawn inside it.
 */
@Composable
fun WeatherMap(
    pin: MapPin?,
    covered: PaddingValues,
    dark: Boolean,
    onLongPress: (latitude: Double, longitude: Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(Res.string.map_label)
    val state =
        rememberMapState(
            baseStyle = BaseStyle.Uri(if (dark) DARK_STYLE else LIGHT_STYLE),
            initialCameraPosition = CameraPosition(center = initialCenter, zoom = INITIAL_ZOOM),
        )
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val interactions =
        remember {
            MapInteractions {
                camera {
                    rotate { enabled = false }
                    pitch { enabled = false }
                }
                callbacks {
                    longClick {
                        onEvent { event ->
                            event.position?.let { currentOnLongPress(it.latitude, it.longitude) }
                            ClickResult.Consume
                        }
                    }
                }
            }
        }

    // Selecting a place, or a change in what covers the map, moves the map so
    // the pin sits in the clear.
    LaunchedEffect(pin, covered) {
        if (pin == null) return@LaunchedEffect
        state.animateCamera(
            CameraUpdate(
                center = Position(longitude = pin.longitude, latitude = pin.latitude),
                zoom = maxOf(state.cameraPosition.zoom, PLACE_ZOOM),
            ),
            moveAnimation,
        )
    }

    MaplibreMap(
        modifier = modifier.semantics { contentDescription = label },
        state = state,
        viewportInsets = covered,
        cameraConstraints = zoomLimits,
        interactions = interactions,
        overlay = {
            // The credit the map's makers ask for, in the corner of the part
            // of the map nothing covers.
            Text(
                stringResource(Res.string.map_credit),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(LocalViewportInsets.current)
                        .padding(Spacing.sm)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = CREDIT_OPACITY),
                            MaterialTheme.shapes.extraSmall,
                        ).padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
            )
            if (pin != null) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = stringResource(Res.string.selected_place),
                    tint = MaterialTheme.colorScheme.primary,
                    // The pin's tip, not its middle, sits on the point.
                    modifier =
                        Modifier
                            .placedAt(
                                Position(longitude = pin.longitude, latitude = pin.latitude),
                                Alignment.BottomCenter,
                            ).size(pinSize),
                )
            }
        },
    )
}

// OpenFreeMap's hosted styles: free, no key, and no request limit.
private const val LIGHT_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val DARK_STYLE = "https://tiles.openfreemap.org/styles/dark"

private val initialCenter = Position(longitude = 0.0, latitude = 25.0)
private const val INITIAL_ZOOM = 1.5

/** How far in the map goes when a place is chosen from further out. */
private const val PLACE_ZOOM = 10.0

private val zoomLimits =
    CameraConstraints {
        minZoom = 1.0
        maxZoom = 18.0
    }
private val moveAnimation = CameraAnimation.Ease { duration = 600.milliseconds }
private val pinSize = 48.dp
private const val CREDIT_OPACITY = 0.8f
