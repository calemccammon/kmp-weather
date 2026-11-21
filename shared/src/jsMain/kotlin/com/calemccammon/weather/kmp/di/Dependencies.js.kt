package com.calemccammon.weather.kmp.di

import kotlinx.browser.window

/** A browser says its language as "en-US": the country, when there is one, is the last part. */
internal actual fun deviceCountry(): String? =
    window.navigator.language
        .split('-')
        .drop(1)
        .lastOrNull()
        ?.uppercase()
        ?.takeIf { it.length == 2 }
