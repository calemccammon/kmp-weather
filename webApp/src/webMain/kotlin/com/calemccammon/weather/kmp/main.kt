package com.calemccammon.weather.kmp

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.calemccammon.weather.kmp.di.startDependencies
import org.jetbrains.skiko.wasm.onWasmReady
import org.maplibre.compose.browser.installMaplibreCompose

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startDependencies()
    // Compose draws with Skia, which arrives as WebAssembly; the map shares
    // its graphics context, so both wait for it.
    onWasmReady {
        installMaplibreCompose()
        ComposeViewport {
            App()
        }
    }
}
