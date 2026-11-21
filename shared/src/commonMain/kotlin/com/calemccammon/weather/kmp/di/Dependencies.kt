package com.calemccammon.weather.kmp.di

import com.calemccammon.weather.kmp.data.repositories.OpenMeteoWeatherRepository
import com.calemccammon.weather.kmp.data.repositories.StoredSettingsRepository
import com.calemccammon.weather.kmp.domain.repositories.SettingsRepository
import com.calemccammon.weather.kmp.domain.repositories.WeatherRepository
import com.calemccammon.weather.kmp.domain.usecases.GetForecast
import com.calemccammon.weather.kmp.domain.usecases.GetUnits
import com.calemccammon.weather.kmp.domain.usecases.SearchPlaces
import com.calemccammon.weather.kmp.domain.usecases.SetUnits
import com.calemccammon.weather.kmp.ui.home.viewmodels.HomeViewModel
import com.calemccammon.weather.kmp.ui.home.viewmodels.SearchViewModel
import com.russhwolf.settings.Settings
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/*
 * The one place that knows which implementation stands behind each domain
 * interface, and how each use case and view model is built. Everything else
 * asks for the interface.
 */

/** The real repositories: Open-Meteo, and the device's own key-value store. */
val repositoriesModule =
    module {
        single { OpenMeteoWeatherRepository.httpClient() }
        single<WeatherRepository> { OpenMeteoWeatherRepository(get()) }
        single<SettingsRepository> { StoredSettingsRepository(Settings()) }
    }

/**
 * The use cases and the view models over them, built on whichever
 * repositories are supplied beside this module: the real ones, or a test's.
 */
val appModule =
    module {
        factoryOf(::SearchPlaces)
        factoryOf(::GetForecast)
        factoryOf(::GetUnits)
        factoryOf(::SetUnits)
        viewModel { HomeViewModel(get(), get(), get(), deviceCountry()) }
        viewModelOf(::SearchViewModel)
    }

/** Wires the app up. Each platform's entry point calls this once, before drawing anything. */
fun startDependencies() {
    startKoin { modules(repositoriesModule, appModule) }
}

/** The country the device is set to, such as "US", if it says. */
internal expect fun deviceCountry(): String?
