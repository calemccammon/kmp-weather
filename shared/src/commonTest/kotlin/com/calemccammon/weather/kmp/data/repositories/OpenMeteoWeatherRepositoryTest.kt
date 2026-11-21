package com.calemccammon.weather.kmp.data.repositories

import com.calemccammon.weather.kmp.domain.models.CurrentConditions
import com.calemccammon.weather.kmp.domain.models.DailyForecast
import com.calemccammon.weather.kmp.domain.models.Place
import com.calemccammon.weather.kmp.domain.models.Units
import com.calemccammon.weather.kmp.domain.models.WeatherCondition
import com.calemccammon.weather.kmp.domain.repositories.WeatherException
import com.calemccammon.weather.kmp.domain.repositories.WeatherFailure
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds

class OpenMeteoWeatherRepositoryTest {
    private val urls = mutableListOf<Url>()

    /** A repository whose every request is answered with [body], and whose URLs are recorded. */
    private fun answering(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = OpenMeteoWeatherRepository(
        OpenMeteoWeatherRepository.httpClient(
            MockEngine { request ->
                urls += request.url
                respond(body, status)
            },
        ),
    )

    private suspend fun OpenMeteoWeatherRepository.forecastSomewhere(units: Units = Units.Imperial) =
        forecast(latitude = 35.6, longitude = -82.55, units = units)

    private suspend fun failureOf(request: suspend () -> Unit): WeatherException =
        assertFailsWith<WeatherException> {
            request()
        }

    // Searching for places.

    @Test
    fun `a search asks the geocoding service and reads its results`() =
        runTest {
            val repository =
                answering(
                    """
                    {"results": [
                      {"id": 1, "name": "Asheville", "latitude": 35.6, "longitude": -82.55,
                       "admin1": "North Carolina", "country": "United States"},
                      {"name": "Ashford", "latitude": 51.15, "longitude": 0.87}
                    ], "generationtime_ms": 0.4}
                    """,
                )

            val places = repository.searchPlaces("ash ford")

            assertEquals(
                listOf(
                    Place(
                        name = "Asheville",
                        latitude = 35.6,
                        longitude = -82.55,
                        region = "North Carolina",
                        country = "United States",
                    ),
                    Place(name = "Ashford", latitude = 51.15, longitude = 0.87),
                ),
                places,
            )
            val url = urls.single()
            assertEquals("geocoding-api.open-meteo.com", url.host)
            assertEquals("/v1/search", url.encodedPath)
            assertEquals("ash ford", url.parameters["name"])
        }

    @Test
    fun `a search response with no results key is no matches`() =
        runTest {
            assertEquals(emptyList(), answering("""{"generationtime_ms": 0.4}""").searchPlaces("zzzz"))
        }

    @Test
    fun `search results of the wrong shape are a bad response`() =
        runTest {
            val bodies =
                listOf(
                    """{"results": "none"}""",
                    """{"results": ["Asheville"]}""",
                    """{"results": [{"latitude": 1, "longitude": 2}]}""",
                    """{"results": [{"name": "A", "latitude": "north", "longitude": 2}]}""",
                )

            for (body in bodies) {
                assertEquals(
                    WeatherFailure.BadResponse,
                    failureOf { answering(body).searchPlaces("ash") }.failure,
                    body,
                )
            }
        }

    // Getting a forecast.

    @Test
    fun `a forecast has codes as conditions and columns as days and leaves out a day with no temperatures`() =
        runTest {
            val forecast = answering(FORECAST_BODY).forecastSomewhere()

            assertEquals(
                CurrentConditions(
                    observedAt = LocalDateTime(2026, 11, 18, 9, 0),
                    temperature = 35.8,
                    feelsLike = 31.3,
                    windSpeed = 4.4,
                    condition = WeatherCondition.Overcast,
                    isDay = true,
                ),
                forecast.current,
            )
            assertEquals(
                listOf(
                    DailyForecast(LocalDate(2026, 11, 18), 30.2, 64.3, WeatherCondition.Clear, 0),
                    DailyForecast(LocalDate(2026, 11, 19), 41.9, 58.1, WeatherCondition.Rain, 47),
                ),
                forecast.days,
            )
        }

    @Test
    fun `a forecast is asked for in the units wanted on the place's own clock`() =
        runTest {
            answering(FORECAST_BODY).forecastSomewhere(Units.Imperial)
            answering(FORECAST_BODY).forecastSomewhere(Units.Metric)

            val (imperial, metric) = urls
            assertEquals("api.open-meteo.com", imperial.host)
            assertEquals("/v1/forecast", imperial.encodedPath)
            assertEquals("35.6", imperial.parameters["latitude"])
            assertEquals("-82.55", imperial.parameters["longitude"])
            assertEquals("auto", imperial.parameters["timezone"])
            assertEquals("fahrenheit", imperial.parameters["temperature_unit"])
            assertEquals("mph", imperial.parameters["wind_speed_unit"])
            assertEquals("celsius", metric.parameters["temperature_unit"])
            assertEquals("kmh", metric.parameters["wind_speed_unit"])
        }

    @Test
    fun `a forecast copes without the column of precipitation chances`() =
        runTest {
            val body = FORECAST_BODY.replace(""", "precipitation_probability_max": [0, 46.6, null]""", "")

            assertEquals(
                DailyForecast(LocalDate(2026, 11, 18), 30.2, 64.3, WeatherCondition.Clear),
                answering(body).forecastSomewhere().days.first(),
            )
        }

    @Test
    fun `a forecast of the wrong shape is a bad response`() =
        runTest {
            val bodies =
                mapOf(
                    "no current conditions" to FORECAST_BODY.replace("\"current\"", "\"now\""),
                    "no days" to FORECAST_BODY.replace("\"daily\"", "\"weekly\""),
                    "a temperature that is text" to FORECAST_BODY.replace("35.8", "\"warm\""),
                    "a time that is not a date" to FORECAST_BODY.replace("2026-11-18T09:00", "this morning"),
                    "a day that is not a date" to FORECAST_BODY.replace("\"2026-11-19\"", "\"Thu\""),
                    "columns of different lengths" to FORECAST_BODY.replace("[0, 61, null]", "[0, 61]"),
                    "a column that is not a list" to FORECAST_BODY.replace("[64.3, 58.1, null]", "64.3"),
                )

            for ((what, body) in bodies) {
                assertEquals(
                    WeatherFailure.BadResponse,
                    failureOf { answering(body).forecastSomewhere() }.failure,
                    what,
                )
            }
        }

    // When the service does not answer properly.

    @Test
    fun `the service's own refusal is reported with its reason kept for the logs`() =
        runTest {
            val repository =
                answering("""{"error": true, "reason": "Latitude must be in range."}""", HttpStatusCode.BadRequest)

            val error = failureOf { repository.forecastSomewhere() }

            assertEquals(WeatherFailure.Rejected, error.failure)
            assertEquals("Latitude must be in range.", error.detail)
        }

    @Test
    fun `an error status with no reason is a refusal`() =
        runTest {
            for (body in listOf("{}", "<html>Not found</html>")) {
                val error = failureOf { answering(body, HttpStatusCode.NotFound).searchPlaces("ash") }

                assertEquals(WeatherFailure.Rejected to "HTTP 404", error.failure to error.detail, body)
            }
        }

    @Test
    fun `a server error or too many requests is the service's own trouble`() =
        runTest {
            val answers =
                listOf(
                    HttpStatusCode.ServiceUnavailable to "<html>Service unavailable</html>",
                    HttpStatusCode.BadGateway to """{"error": true, "reason": "Upstream failed."}""",
                    HttpStatusCode.TooManyRequests to """{"error": true, "reason": "Minutely limit exceeded."}""",
                )

            for ((status, body) in answers) {
                val error = failureOf { answering(body, status).forecastSomewhere() }

                assertEquals(WeatherFailure.Unavailable to "HTTP ${status.value}", error.failure to error.detail)
            }
        }

    @Test
    fun `an answer that is not a JSON object is a bad response`() =
        runTest {
            for (body in listOf("<html>Bad gateway</html>", "[1, 2, 3]")) {
                assertEquals(
                    WeatherFailure.BadResponse,
                    failureOf { answering(body).searchPlaces("ash") }.failure,
                    body,
                )
            }
        }

    @Test
    fun `a request that cannot be made is no connection`() =
        runTest {
            val repository =
                OpenMeteoWeatherRepository(
                    OpenMeteoWeatherRepository.httpClient(MockEngine { throw IOException("Network is unreachable") }),
                )

            assertEquals(WeatherFailure.NoConnection, failureOf { repository.searchPlaces("ash") }.failure)
        }

    @Test
    fun `an answer that takes too long is a timeout`() =
        runTest {
            val repository =
                OpenMeteoWeatherRepository(
                    OpenMeteoWeatherRepository.httpClient(
                        MockEngine { awaitCancellation() },
                        timeout = 50.milliseconds,
                    ),
                )

            assertEquals(WeatherFailure.Timeout, failureOf { repository.searchPlaces("ash") }.failure)
        }

    private companion object {
        const val FORECAST_BODY = """
            {
              "latitude": 35.6, "timezone": "America/New_York",
              "current": {
                "time": "2026-11-18T09:00", "interval": 900, "temperature_2m": 35.8,
                "apparent_temperature": 31.3, "weather_code": 3, "wind_speed_10m": 4.4, "is_day": 1
              },
              "daily": {
                "time": ["2026-11-18", "2026-11-19", "2026-11-20"],
                "weather_code": [0, 61, null],
                "temperature_2m_max": [64.3, 58.1, null],
                "temperature_2m_min": [30.2, 41.9, null], "precipitation_probability_max": [0, 46.6, null]
              }
            }
        """
    }
}
