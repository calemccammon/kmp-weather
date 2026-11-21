package com.calemccammon.weather.kmp

import androidx.compose.ui.window.ComposeUIViewController
import com.calemccammon.weather.kmp.di.startDependencies
import platform.UIKit.UIViewController

/** The app as a view controller, for the Swift entry point in `iosApp`. Called once per launch. */
fun MainViewController(): UIViewController {
    startDependencies()
    return ComposeUIViewController { App() }
}
