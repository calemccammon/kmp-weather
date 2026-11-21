package com.calemccammon.weather.kmp.di

import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale

internal actual fun deviceCountry(): String? = NSLocale.currentLocale.countryCode
