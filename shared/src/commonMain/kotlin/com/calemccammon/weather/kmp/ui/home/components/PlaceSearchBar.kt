package com.calemccammon.weather.kmp.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.calemccammon.weather.kmp.ui.core.Spacing
import com.calemccammon.weather.kmp.ui.core.asString
import com.calemccammon.weather.kmp.ui.home.models.PlaceUiModel
import com.calemccammon.weather.kmp.ui.home.models.SearchState
import kmp_weather.shared.generated.resources.Res
import kmp_weather.shared.generated.resources.clear_place
import kmp_weather.shared.generated.resources.search_hint
import kmp_weather.shared.generated.resources.search_nothing_found
import kmp_weather.shared.generated.resources.search_prompt
import kmp_weather.shared.generated.resources.search_searching
import kmp_weather.shared.generated.resources.try_again
import org.jetbrains.compose.resources.stringResource

/**
 * Material 3 search bar that opens into a list of matching places.
 *
 * Closed, it shows the selected place's name ([placeTitle]). Open, it holds
 * what is being typed, and draws [search] under itself. It does no searching
 * of its own: it reports what was typed and which row was chosen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceSearchBar(
    placeTitle: String?,
    search: SearchState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onChoose: (index: Int) -> Unit,
    onRetry: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var typed by rememberSaveable { mutableStateOf("") }
    // Opening starts from an empty search: the place's name is a label, not
    // something to search for.
    LaunchedEffect(expanded) { typed = "" }

    DockedSearchBar(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        inputField = {
            SearchBarDefaults.InputField(
                query = if (expanded) typed else placeTitle.orEmpty(),
                onQueryChange = {
                    typed = it
                    onQueryChange(it)
                },
                onSearch = {},
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                placeholder = { Text(stringResource(Res.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon =
                    if (placeTitle != null && !expanded) {
                        {
                            IconButton(onClick = onClear) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.clear_place))
                            }
                        }
                    } else {
                        null
                    },
            )
        },
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            when (search) {
                is SearchState.Results ->
                    search.places.forEachIndexed { index, place -> PlaceRow(place, onClick = { onChoose(index) }) }
                else -> SearchStatus(search, onRetry)
            }
        }
    }
}

@Composable
private fun PlaceRow(
    place: PlaceUiModel,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(Icons.Outlined.Place, contentDescription = null) },
        headlineContent = { Text(place.title.asString()) },
        supportingContent = if (place.subtitle.isEmpty()) null else ({ Text(place.subtitle) }),
    )
}

/**
 * What is happening when there are no results to show. A screen reader reads
 * it out as it changes: searching, then what came of it.
 */
@Composable
private fun SearchStatus(
    search: SearchState,
    onRetry: () -> Unit,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val padding = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)

    Column(Modifier.fillMaxWidth().announced()) {
        when (search) {
            SearchState.Idle ->
                Message(
                    Icons.Filled.TravelExplore,
                    stringResource(Res.string.search_prompt),
                    padding,
                    textColor = muted,
                )
            is SearchState.Loading -> {
                val searching = stringResource(Res.string.search_searching)
                LinearProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = searching })
            }
            is SearchState.Empty ->
                Message(
                    Icons.Filled.SearchOff,
                    stringResource(Res.string.search_nothing_found, search.query),
                    padding,
                    textColor = muted,
                )
            is SearchState.Failed ->
                Message(Icons.Filled.CloudOff, search.message.asString(), padding, textColor = muted) {
                    TextButton(onClick = onRetry) { Text(stringResource(Res.string.try_again)) }
                }
            is SearchState.Results -> Unit
        }
    }
}
