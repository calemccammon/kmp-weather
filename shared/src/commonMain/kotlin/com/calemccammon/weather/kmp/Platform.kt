package com.calemccammon.weather.kmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
