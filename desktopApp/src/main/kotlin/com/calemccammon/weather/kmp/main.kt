package com.calemccammon.weather.kmp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.calemccammon.weather.kmp.di.startDependencies
import org.maplibre.compose.desktop.ProvideMapPresentationHost
import org.maplibre.compose.desktop.rememberAwtComposeMapPresentationHost

fun main() {
    startDependencies()
    application {
        Window(onCloseRequest = ::exitApplication, title = appTitle()) {
            // The map draws straight into the window, so it is told which one.
            ProvideMapPresentationHost(rememberAwtComposeMapPresentationHost(window)) {
                App()
            }
        }
    }
}
