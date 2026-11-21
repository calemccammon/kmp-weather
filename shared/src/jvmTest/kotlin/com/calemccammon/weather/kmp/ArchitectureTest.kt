package com.calemccammon.weather.kmp

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Holds the layers to their rule: dependencies point inward only.
 *
 * It reads the import lines of every source file of the app, in every
 * platform's source set, and fails on one that reaches across a boundary.
 */
class ArchitectureTest {
    private data class Rule(
        /** The files it covers: those whose path under the app's package starts with this. */
        val path: String,
        val because: String,
        /** Imports no file there may have, by prefix. */
        val forbidden: List<String> = emptyList(),
        /** When set, the only imports a file there may have, by prefix. */
        val allowedOnly: List<String>? = null,
    )

    private val rules =
        listOf(
            Rule(
                path = "domain/",
                because = "the domain knows nothing of the other layers, and no library but the language's own",
                allowedOnly = listOf("$APP.domain.", "kotlin.", "kotlinx.datetime."),
            ),
            Rule(
                path = "data/",
                because = "data implements the domain and knows nothing of the screen or the wiring",
                forbidden = listOf("$APP.ui.", "$APP.di.", "androidx.compose.", "androidx.lifecycle.", "org.koin."),
            ),
            Rule(
                path = "ui/",
                because = "the screen reaches data only through the domain, and is handed what it needs",
                forbidden = listOf("$APP.data.", "$APP.di.", "io.ktor.", "com.russhwolf.", "kotlinx.serialization."),
            ),
            Rule(
                path = "ui/home/components/",
                because = "components draw UI models and know nothing of the domain",
                forbidden = listOf("$APP.domain."),
            ),
            Rule(
                path = "ui/home/HomeScreen.kt",
                because = "the screen draws UI models and knows nothing of the domain",
                forbidden = listOf("$APP.domain."),
            ),
        )

    /** Every main source file of the shared module, by its path under the app's package. */
    private val sources: Map<String, File> =
        File("src")
            .listFiles { file -> file.isDirectory && file.name.endsWith("Main") }
            .orEmpty()
            .flatMap { sourceSet ->
                val root = File(sourceSet, "kotlin/${APP.replace('.', '/')}")
                root.walkTopDown().filter { it.extension == "kt" }.map {
                    "${it.relativeTo(root).invariantSeparatorsPath}" to
                        it
                }
            }.groupBy({ it.first }, { it.second })
            .flatMap { (path, files) ->
                files.map {
                    "$path (${it.path.substringAfter("src/").substringBefore('/')})" to
                        it
                }
            }.toMap()

    private fun importsOf(file: File): List<String> =
        file
            .readLines()
            .filter {
                it.startsWith(
                    "import ",
                )
            }.map { it.removePrefix("import ").substringBefore(" as ").trim() }

    private fun violations(): List<String> =
        sources.flatMap { (path, file) ->
            rules.filter { path.startsWith(it.path) }.flatMap { rule ->
                importsOf(file)
                    .filter { import ->
                        rule.forbidden.any(import::startsWith) || rule.allowedOnly?.none(import::startsWith) == true
                    }.map { "$path imports $it, but ${rule.because}." }
            }
        }

    @Test
    fun `no file imports across a layer boundary`() {
        assertEquals(emptyList(), violations())
    }

    @Test
    fun `the check reads every layer`() {
        for (layer in listOf("domain/", "data/", "ui/", "di/")) {
            assertTrue(sources.keys.any { it.startsWith(layer) }, "no source files found under $layer")
        }
    }

    private companion object {
        const val APP = "com.calemccammon.weather.kmp"
    }
}
