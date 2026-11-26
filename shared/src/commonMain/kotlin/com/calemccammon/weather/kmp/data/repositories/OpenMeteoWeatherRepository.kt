package com.calemccammon.weather.kmp.data.repositories

import com.calemccammon.weather.kmp.data.models.ForecastDto
import com.calemccammon.weather.kmp.data.models.PlaceDto
import com.calemccammon.weather.kmp.data.models.PlaceSearchDto
import com.calemccammon.weather.kmp.domain.models.Forecast
import com.calemccammon.weather.kmp.domain.models.Place
import com.calemccammon.weather.kmp.domain.repositories.WeatherException
import com.calemccammon.weather.kmp.domain.repositories.WeatherFailure
import com.calemccammon.weather.kmp.domain.repositories.WeatherRepository
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Places and forecasts from Open-Meteo. No API key is needed.
 *
 * Asks the service, reads its answers into the wire-shaped models in
 * `data/models`, and hands back domain models. Every way this can go wrong
 * leaves as a [WeatherException].
 *
 * Docs: https://open-meteo.com/en/docs and
 * https://open-meteo.com/en/docs/geocoding-api
 */
class OpenMeteoWeatherRepository(
    private val client: HttpClient,
) : WeatherRepository {
    override suspend fun searchPlaces(query: String): List<Place> {
        val body =
            get(GEOCODING_URL) {
                parameter("name", query)
                parameter("count", MAX_RESULTS)
                parameter("language", "en")
                parameter("format", "json")
            }
        return read { json.decodeFromString<PlaceSearchDto>(body).results.map(PlaceDto::toDomain) }
    }

    override suspend fun forecast(
        latitude: Double,
        longitude: Double,
    ): Forecast {
        val body =
            get(FORECAST_URL) {
                parameter("latitude", latitude)
                parameter("longitude", longitude)
                parameter("current", "temperature_2m,apparent_temperature,weather_code,wind_speed_10m,is_day")
                parameter(
                    "daily",
                    "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max",
                )
                parameter("forecast_days", FORECAST_DAYS)
                parameter("timezone", "auto")
                // Always metric; the app converts on the device (see Forecast.inUnits).
                parameter("temperature_unit", "celsius")
                parameter("wind_speed_unit", "kmh")
            }
        return read { json.decodeFromString<ForecastDto>(body).toDomain() }
    }

    /** Runs [parse], reporting a response of the wrong shape as a bad response. */
    private fun <T> read(parse: () -> T): T =
        try {
            parse()
        } catch (e: IllegalArgumentException) {
            // Not JSON, not the expected shape, or a date that is not one.
            throw WeatherException(WeatherFailure.BadResponse, e.message, e)
        }

    /** The body of a successful answer from [url]. */
    private suspend fun get(
        url: String,
        parameters: HttpRequestBuilder.() -> Unit,
    ): String {
        val (status, body) = send(url, parameters)

        // The service's own trouble, or too many requests: its answer, often a
        // proxy's error page, is not read at all.
        if (status.value >= FIRST_SERVER_ERROR || status == HttpStatusCode.TooManyRequests) {
            throw WeatherException(WeatherFailure.Unavailable, "HTTP ${status.value}")
        }
        // Bad input comes back as HTTP 400 with {"error": true, "reason": "..."}.
        if (status != HttpStatusCode.OK) {
            val reason = runCatching { json.decodeFromString<ErrorDto>(body).reason }.getOrNull()
            throw WeatherException(WeatherFailure.Rejected, reason ?: "HTTP ${status.value}")
        }
        return body
    }

    // Each platform's HTTP engine reports a failed request with its own
    // exception, and the browser's with an Error, so everything is caught.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun send(
        url: String,
        parameters: HttpRequestBuilder.() -> Unit,
    ): Pair<HttpStatusCode, String> =
        try {
            val response = client.get(url, parameters)
            response.status to response.bodyAsText()
        } catch (e: CancellationException) {
            // The caller gave up waiting; that is not a failure to report.
            throw e
        } catch (e: HttpRequestTimeoutException) {
            throw WeatherException(WeatherFailure.Timeout, e.message, e)
        } catch (e: ConnectTimeoutException) {
            throw WeatherException(WeatherFailure.Timeout, e.message, e)
        } catch (e: SocketTimeoutException) {
            throw WeatherException(WeatherFailure.Timeout, e.message, e)
        } catch (e: Throwable) {
            // A dropped socket, a failed TLS handshake, a refused connection.
            throw WeatherException(WeatherFailure.NoConnection, e.message, e)
        }

    /** What the service sends with an error status. */
    @Serializable
    private data class ErrorDto(
        val reason: String? = null,
    )

    companion object {
        /** How long to wait for an answer before giving up. */
        val defaultTimeout = 15.seconds

        private const val GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search"
        private const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
        private const val MAX_RESULTS = 8
        private const val FORECAST_DAYS = 7
        private const val FIRST_SERVER_ERROR = 500

        private val json = Json { ignoreUnknownKeys = true }

        /**
         * The HTTP client the repository asks through: one that gives up after
         * [timeout]. It uses the platform's own [engine] unless a test supplies one.
         */
        fun httpClient(
            engine: HttpClientEngine? = null,
            timeout: Duration = defaultTimeout,
        ): HttpClient {
            val configure: HttpClientConfig<*>.() -> Unit = {
                install(HttpTimeout) { requestTimeoutMillis = timeout.inWholeMilliseconds }
            }
            return if (engine == null) HttpClient(configure) else HttpClient(engine, configure)
        }
    }
}
