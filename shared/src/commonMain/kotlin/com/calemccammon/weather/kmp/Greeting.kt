package com.calemccammon.weather.kmp

class Greeting {
    private val platform = getPlatform()

    fun greet(): String = sayHello(platform.name)
}
