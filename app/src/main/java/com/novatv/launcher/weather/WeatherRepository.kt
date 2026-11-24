package com.novatv.launcher.weather

import android.content.Context
import android.content.SharedPreferences
import com.novatv.launcher.R
import com.novatv.launcher.ui.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

data class City(val id: String, val name: String, val lat: Double, val lon: Double)

object Cities {
    val ALL = listOf(
        City("nsk", "Новосибирск", 55.03, 82.92),
        City("msk", "Москва", 55.75, 37.62),
        City("spb", "Санкт-Петербург", 59.94, 30.31),
        City("ekb", "Екатеринбург", 56.84, 60.65),
        City("krasnoyarsk", "Красноярск", 56.01, 92.85),
        City("vladivostok", "Владивосток", 43.12, 131.89)
    )
    val DEFAULT = ALL.first()

    fun byId(id: String?): City = ALL.firstOrNull { it.id == id } ?: DEFAULT
}

/**
 * Current weather via the free Open-Meteo API (no API key required).
 * Successful responses are cached in SharedPreferences and shown when offline.
 */
class WeatherRepository(context: Context) {

    data class WeatherData(
        val temperatureC: Int,
        val descriptionRes: Int,
        val emoji: String,
        val windMs: Int
    )

    private val prefs: SharedPreferences = Prefs.get(context)

    fun currentCity(): City = Cities.byId(prefs.getString(Prefs.KEY_CITY, null))

    fun cached(): WeatherData? {
        val temp = prefs.getInt(KEY_CACHE_TEMP, Int.MIN_VALUE)
        if (temp == Int.MIN_VALUE) return null
        return WeatherData(
            temperatureC = temp,
            descriptionRes = prefs.getInt(KEY_CACHE_DESC, R.string.wd_clear),
            emoji = prefs.getString(KEY_CACHE_EMOJI, "☀️") ?: "☀️",
            windMs = prefs.getInt(KEY_CACHE_WIND, 0)
        )
    }

    suspend fun fetchCurrent(): WeatherData? = withContext(Dispatchers.IO) {
        runCatching {
            val city = currentCity()
            val url = URL(
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=${city.lat}&longitude=${city.lon}&current_weather=true"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            try {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val cw = JSONObject(text).getJSONObject("current_weather")
                val code = cw.optInt("weathercode", 0)
                val data = WeatherData(
                    temperatureC = cw.getDouble("temperature").roundToInt(),
                    descriptionRes = describeRes(code).first,
                    emoji = describeRes(code).second,
                    windMs = cw.getDouble("windspeed").roundToInt()
                )
                cache(data)
                data
            } finally {
                conn.disconnect()
            }
        }.getOrNull() ?: cached()
    }

    private fun cache(data: WeatherData) {
        prefs.edit()
            .putInt(KEY_CACHE_TEMP, data.temperatureC)
            .putInt(KEY_CACHE_DESC, data.descriptionRes)
            .putString(KEY_CACHE_EMOJI, data.emoji)
            .putInt(KEY_CACHE_WIND, data.windMs)
            .apply()
    }

    companion object {
        private const val KEY_CACHE_TEMP = "cache_weather_temp"
        private const val KEY_CACHE_DESC = "cache_weather_desc"
        private const val KEY_CACHE_EMOJI = "cache_weather_emoji"
        private const val KEY_CACHE_WIND = "cache_weather_wind"

        /** WMO weather codes -> (description string res, emoji). */
        fun describeRes(code: Int): Pair<Int, String> = when (code) {
            0 -> R.string.wd_clear to "☀️"
            1, 2 -> R.string.wd_partly to "🌤️"
            3 -> R.string.wd_cloudy to "☁️"
            45, 48 -> R.string.wd_fog to "🌫️"
            in 51..57 -> R.string.wd_drizzle to "🌦️"
            in 61..67 -> R.string.wd_rain to "🌧️"
            in 71..77 -> R.string.wd_snow to "❄️"
            in 80..82 -> R.string.wd_shower to "🌧️"
            85, 86 -> R.string.wd_snow to "🌨️"
            in 95..99 -> R.string.wd_thunder to "⛈️"
            else -> R.string.wd_overcast to "🌥️"
        }
    }
}
