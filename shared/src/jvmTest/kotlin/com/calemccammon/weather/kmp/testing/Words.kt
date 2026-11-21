package com.calemccammon.weather.kmp.testing

import com.calemccammon.weather.kmp.ui.core.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.jetbrains.compose.resources.getString
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/** What the user would read: the string looked up from the app's resources, gaps filled. */
val UiText.words: String
    get() =
        when (this) {
            is UiText.Verbatim -> text
            is UiText.Resource -> {
                val filled = arguments.map { if (it is UiText) it.words else it }.toTypedArray()
                runBlocking { getString(resource, *filled) }
            }
        }

/**
 * For tests of view models, which do their work on the main dispatcher. It
 * stands a test dispatcher in for it, so `runTest` controls the clock.
 */
abstract class MainDispatcherTest {
    protected val mainDispatcher: TestDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setMainDispatcher() = Dispatchers.setMain(mainDispatcher)

    @AfterTest
    fun resetMainDispatcher() = Dispatchers.resetMain()
}
