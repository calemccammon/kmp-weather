package com.calemccammon.weather.kmp

import android.app.Application
import com.calemccammon.weather.kmp.di.startDependencies

class WeatherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startDependencies()
    }
}
