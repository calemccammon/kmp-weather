package com.calemccammon.weather.kmp.ui.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Wording the view models choose and the screen looks up.
 *
 * A view model has no access to the device's language, so it says which
 * string it means and what goes in the gaps; the words are found when the
 * screen is drawn, and found again if the language changes.
 */
@Immutable
sealed interface UiText {
    /** Text that is the same in every language, such as a place's name. */
    data class Verbatim(
        val text: String,
    ) : UiText

    /**
     * A string from `composeResources/values/strings.xml`, with [arguments]
     * for its gaps: text, whole numbers, or more [UiText].
     */
    data class Resource(
        val resource: StringResource,
        val arguments: List<Any> = emptyList(),
    ) : UiText
}

fun uiText(
    resource: StringResource,
    vararg arguments: Any,
): UiText = UiText.Resource(resource, arguments.toList())

/** The words, in the device's language. */
@Suppress("SpreadOperator") // The lookup takes its arguments one by one.
@Composable
fun UiText.asString(): String =
    when (this) {
        is UiText.Verbatim -> text
        is UiText.Resource ->
            stringResource(resource, *arguments.map { if (it is UiText) it.asString() else it }.toTypedArray())
    }
