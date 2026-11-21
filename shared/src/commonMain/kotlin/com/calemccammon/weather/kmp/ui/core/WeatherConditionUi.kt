package com.calemccammon.weather.kmp.ui.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.calemccammon.weather.kmp.domain.models.WeatherCondition
import kmp_weather.shared.generated.resources.Res
import kmp_weather.shared.generated.resources.condition_clear
import kmp_weather.shared.generated.resources.condition_drizzle
import kmp_weather.shared.generated.resources.condition_fog
import kmp_weather.shared.generated.resources.condition_freezing_drizzle
import kmp_weather.shared.generated.resources.condition_freezing_rain
import kmp_weather.shared.generated.resources.condition_mostly_clear
import kmp_weather.shared.generated.resources.condition_overcast
import kmp_weather.shared.generated.resources.condition_partly_cloudy
import kmp_weather.shared.generated.resources.condition_rain
import kmp_weather.shared.generated.resources.condition_rain_showers
import kmp_weather.shared.generated.resources.condition_snow
import kmp_weather.shared.generated.resources.condition_snow_showers
import kmp_weather.shared.generated.resources.condition_thunderstorm
import kmp_weather.shared.generated.resources.condition_unknown

/*
 * How each weather condition is worded and drawn.
 */

val WeatherCondition.label: UiText
    get() =
        uiText(
            when (this) {
                WeatherCondition.Clear -> Res.string.condition_clear
                WeatherCondition.MostlyClear -> Res.string.condition_mostly_clear
                WeatherCondition.PartlyCloudy -> Res.string.condition_partly_cloudy
                WeatherCondition.Overcast -> Res.string.condition_overcast
                WeatherCondition.Fog -> Res.string.condition_fog
                WeatherCondition.Drizzle -> Res.string.condition_drizzle
                WeatherCondition.FreezingDrizzle -> Res.string.condition_freezing_drizzle
                WeatherCondition.Rain -> Res.string.condition_rain
                WeatherCondition.FreezingRain -> Res.string.condition_freezing_rain
                WeatherCondition.Snow -> Res.string.condition_snow
                WeatherCondition.RainShowers -> Res.string.condition_rain_showers
                WeatherCondition.SnowShowers -> Res.string.condition_snow_showers
                WeatherCondition.Thunderstorm -> Res.string.condition_thunderstorm
                WeatherCondition.Unknown -> Res.string.condition_unknown
            },
        )

fun WeatherCondition.icon(isDay: Boolean = true): ImageVector =
    when (this) {
        WeatherCondition.Clear, WeatherCondition.MostlyClear ->
            if (isDay) Icons.Filled.WbSunny else Icons.Filled.NightlightRound
        WeatherCondition.PartlyCloudy -> if (isDay) Icons.Filled.WbCloudy else Icons.Filled.NightsStay
        WeatherCondition.Overcast -> Icons.Filled.Cloud
        WeatherCondition.Fog -> Icons.Filled.Dehaze
        WeatherCondition.Drizzle, WeatherCondition.FreezingDrizzle -> Icons.Filled.Grain
        WeatherCondition.Rain, WeatherCondition.FreezingRain -> Icons.Filled.WaterDrop
        WeatherCondition.RainShowers -> Icons.Filled.Umbrella
        WeatherCondition.Snow, WeatherCondition.SnowShowers -> Icons.Filled.AcUnit
        WeatherCondition.Thunderstorm -> Icons.Filled.Thunderstorm
        WeatherCondition.Unknown -> Icons.AutoMirrored.Filled.HelpOutline
    }
