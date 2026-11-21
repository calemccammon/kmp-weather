package com.calemccammon.weather.kmp.di

import java.util.Locale

internal actual fun deviceCountry(): String? = Locale.getDefault().country.ifEmpty { null }
