package com.calemccammon.weather.kmp

import androidx.compose.runtime.Composable
import com.calemccammon.weather.kmp.ui.core.WeatherTheme
import com.calemccammon.weather.kmp.ui.home.HomeScreen
import kmp_weather.shared.generated.resources.Res
import kmp_weather.shared.generated.resources.app_title
import org.jetbrains.compose.resources.stringResource

/** The whole app, the same on every platform. */
@Composable
fun App() {
    WeatherTheme {
        HomeScreen()
    }
}

/** The app's name, for a window's title bar. */
@Composable
fun appTitle(): String = stringResource(Res.string.app_title)
