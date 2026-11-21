package com.calemccammon.weather.kmp.ui.home

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calemccammon.weather.kmp.ui.core.Breakpoints
import com.calemccammon.weather.kmp.ui.core.Spacing
import com.calemccammon.weather.kmp.ui.core.asString
import com.calemccammon.weather.kmp.ui.home.components.ForecastPanel
import com.calemccammon.weather.kmp.ui.home.components.MapPin
import com.calemccammon.weather.kmp.ui.home.components.PlaceSearchBar
import com.calemccammon.weather.kmp.ui.home.components.WeatherMap
import com.calemccammon.weather.kmp.ui.home.viewmodels.HomeViewModel
import com.calemccammon.weather.kmp.ui.home.viewmodels.SearchViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Draws the map. The screen says where the pin goes, how much of each edge
 * of the map something else covers, and what to do with a press-and-hold.
 */
typealias MapSlot = @Composable (
    pin: MapPin?,
    covered: PaddingValues,
    onLongPress: (latitude: Double, longitude: Double) -> Unit,
) -> Unit

/**
 * The app's one screen: a map, a search bar over it, and the forecast for
 * the selected place.
 *
 * On a phone the forecast is a bottom sheet that drags up over the map. From
 * Material's "expanded" window width up, it is a panel at the side.
 *
 * [map] is the real map unless a test draws its own.
 */
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = koinViewModel(),
    searchViewModel: SearchViewModel = koinViewModel(),
    map: MapSlot = { pin, covered, onLongPress ->
        WeatherMap(pin, covered, dark = isSystemInDarkTheme(), onLongPress = onLongPress)
    },
) {
    val home by homeViewModel.state.collectAsStateWithLifecycle()
    val search by searchViewModel.state.collectAsStateWithLifecycle()
    var searchOpen by rememberSaveable { mutableStateOf(false) }

    val pin = home.place?.let { MapPin(it.latitude, it.longitude) }
    val searchBar: @Composable (Modifier) -> Unit = { modifier ->
        PlaceSearchBar(
            placeTitle = home.place?.title?.asString(),
            search = search,
            expanded = searchOpen,
            onExpandedChange = { open ->
                if (open) searchViewModel.reset()
                searchOpen = open
            },
            onQueryChange = searchViewModel::queryChanged,
            onChoose = { index ->
                // The two view models meet here: the row chosen in one is the
                // place selected in the other.
                searchViewModel.resultAt(index)?.let(homeViewModel::select)
                searchOpen = false
            },
            onRetry = searchViewModel::retry,
            onClear = homeViewModel::clear,
            modifier = modifier,
        )
    }
    val forecast: @Composable (Modifier, PaddingValues) -> Unit = { modifier, padding ->
        ForecastPanel(home, homeViewModel::setUnits, homeViewModel::retryForecast, modifier, padding)
    }
    // A press anywhere else closes the search results.
    val scrim: @Composable () -> Unit = {
        if (searchOpen) {
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { searchOpen = false } })
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= Breakpoints.expanded) {
            WideLayout(pin, homeViewModel::selectPoint, map, searchBar, forecast, scrim)
        } else {
            PhoneLayout(maxHeight, pin, homeViewModel::selectPoint, map, searchBar, forecast, scrim)
        }
    }
}

/** The forecast in a panel floating over the map's left side, under the search bar. */
@Composable
private fun WideLayout(
    pin: MapPin?,
    onLongPress: (Double, Double) -> Unit,
    map: MapSlot,
    searchBar: @Composable (Modifier) -> Unit,
    forecast: @Composable (Modifier, PaddingValues) -> Unit,
    scrim: @Composable () -> Unit,
) {
    map(pin, PaddingValues(start = sidePanelWidth + Spacing.lg * 2), onLongPress)
    scrim()
    Box(
        Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(Spacing.lg)
            .width(sidePanelWidth),
    ) {
        // The sheet's look, rounded on every corner since it floats clear of
        // the window's edges.
        Surface(
            modifier = Modifier.padding(top = searchBarHeight + Spacing.md).fillMaxSize(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shadowElevation = panelElevation,
        ) {
            forecast(Modifier, PaddingValues(Spacing.xl))
        }
        // Drawn after the panel so its results lie over it.
        searchBar(Modifier.fillMaxWidth())
    }
}

/** The forecast in a sheet that rests over the bottom of the map and drags up. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneLayout(
    screenHeight: Dp,
    pin: MapPin?,
    onLongPress: (Double, Double) -> Unit,
    map: MapSlot,
    searchBar: @Composable (Modifier) -> Unit,
    forecast: @Composable (Modifier, PaddingValues) -> Unit,
    scrim: @Composable () -> Unit,
) {
    val sheetResting = screenHeight * SHEET_RESTING
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val underSearchBar = statusBar + Spacing.lg + searchBarHeight + Spacing.md

    BottomSheetScaffold(
        sheetPeekHeight = sheetResting,
        sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        sheetShadowElevation = panelElevation,
        sheetContent = {
            forecast(
                // Pulled right up, the sheet stops just under the search bar.
                Modifier.fillMaxWidth().height(screenHeight - underSearchBar - dragHandleHeight),
                // Clear of the system's gesture bar at the bottom of the screen.
                PaddingValues(start = Spacing.xl, end = Spacing.xl, bottom = Spacing.xl + navigationBar),
            )
        },
    ) {
        map(pin, PaddingValues(top = underSearchBar, bottom = sheetResting), onLongPress)
    }
    scrim()
    // Drawn after the sheet so its results lie over it.
    searchBar(
        Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(Spacing.lg)
            .fillMaxWidth(),
    )
}

private val sidePanelWidth = 400.dp

/** Material 3's search bar height. */
private val searchBarHeight = 56.dp

private val panelElevation = 1.dp

/** Material 3's bottom sheet handle, with the space around it. */
private val dragHandleHeight = 48.dp

/** How much of the screen's height the bottom sheet takes at rest. */
private const val SHEET_RESTING = 0.3f
