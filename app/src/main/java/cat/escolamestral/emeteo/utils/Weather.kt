package cat.escolamestral.emeteo.utils

import android.content.res.Resources
import cat.escolamestral.emeteo.R
import org.jsoup.select.Elements

class Weather {

    private val prefs = PreferencesManager.getPreferencesInstanceNoContext()

    var temperature = -274.0        // *TMP - Set in celsius, get in user's prefered units
        get() = if (prefs == null) field else when (prefs.getTemperatureUnits()) {
            PreferencesManager.TEMPERATURE_FAHRENHEITS -> celsiusToFahrenheits(field)
            else -> field
        }
    var maxTemperature = -1.0        // *DHTM
        get() = if (prefs == null) field else when (prefs.getTemperatureUnits()) {
            PreferencesManager.TEMPERATURE_FAHRENHEITS -> celsiusToFahrenheits(field)
            else -> field
        }
    var minTemperature = -1.0        // *DLTM
        get() = if (prefs == null) field else when (prefs.getTemperatureUnits()) {
            PreferencesManager.TEMPERATURE_FAHRENHEITS -> celsiusToFahrenheits(field)
            else -> field
        }
    var insideTemperature = -1.0     // *ITMP
        get() = if (prefs == null) field else when (prefs.getTemperatureUnits()) {
            PreferencesManager.TEMPERATURE_FAHRENHEITS -> celsiusToFahrenheits(field)
            else -> field
        }
    var windSpeed = -1.0             // *WND - Set in Km/h, get in user's prefered units
        get() = if (prefs == null) field else when (prefs.getWindUnits()) {
            PreferencesManager.WIND_MS -> kmhToMs(field)
            PreferencesManager.WIND_MPH -> kmhToMph(field)
            else -> field
        }
    var maxWindSpeed = -1.0          // *DGST
        get() = if (prefs == null) field else when (prefs.getWindUnits()) {
            PreferencesManager.WIND_MS -> kmhToMs(field)
            PreferencesManager.WIND_MPH -> kmhToMph(field)
            else -> field
        }
    var humidity = -1                // *HUM     -   In %
    var maxHumidity = -1             // *DHHM
    var minHumidity = -1             // *DLHM
    var windDirection = -1           // *AZI     -   In degrees
    var rain = -1.0                  // *DPCP    -   In mm
    var lastUpdate = ""

    companion object {
        private const val FINDER_TEMPERATURE = 2
        private const val FINDER_MAX_TEMPERATURE = 7
        private const val FINDER_MIN_TEMPERATURE = 8
        private const val FINDER_WIND_SPEED = 5
        private const val FINDER_MAX_WIND_SPEED = 13
        private const val FINDER_HUMIDITY = 3
        private const val FINDER_MAX_HUMIDITY = 9
        private const val FINDER_MIN_HUMIDITY = 10
        private const val FINDER_WIND_DIRECTION = 6
        private const val FINDER_RAIN = 1

        fun parseString(data: String): Weather {
            val weatherData = Weather()
            data.split("*").forEach {
                when {
                    it.startsWith("TMP") and !it.contains("[-]+".toRegex()) ->
                        weatherData.temperature = it.replace("TMP=", "").trim().toDouble()

                    it.startsWith("ITMP") and !it.contains("[-]+".toRegex()) ->
                        weatherData.insideTemperature = it.replace("ITMP=", "").trim().toDouble()

                    it.startsWith("DHTM") and !it.contains("[-]+".toRegex()) ->
                        weatherData.maxTemperature = it.replace("DHTM=", "").trim().toDouble()

                    it.startsWith("DLTM") and !it.contains("[-]+".toRegex()) ->
                        weatherData.minTemperature = it.replace("DLTM=", "").trim().toDouble()

                    it.startsWith("WND") and !it.contains("[-]+".toRegex()) ->
                        weatherData.windSpeed = it.replace("WND=", "").trim().toDouble()

                    it.startsWith("DGST") and !it.contains("[-]+".toRegex()) ->
                        weatherData.maxWindSpeed = it.replace("DGST=", "").trim().toDouble()

                    it.startsWith("HUM") and !it.contains("[-]+".toRegex()) ->
                        weatherData.humidity = it.replace("HUM=", "").trim().toInt()

                    it.startsWith("DHHM") and !it.contains("[-]+".toRegex()) ->
                        weatherData.maxHumidity = it.replace("DHHM=", "").trim().toInt()

                    it.startsWith("DLHM") and !it.contains("[-]+".toRegex()) ->
                        weatherData.minHumidity = it.replace("DLHM=", "").trim().toInt()

                    it.startsWith("AZI") and !it.contains("[-]+".toRegex()) ->
                        weatherData.windDirection = it.replace("AZI=", "").trim().toInt()

                    it.startsWith("DPCP") and !it.contains("[-]+".toRegex()) ->
                        weatherData.rain = it.replace("DPCP=", "").trim().toDouble()

                    it.startsWith("UPD") and !it.contains("[-]+".toRegex()) ->
                        weatherData.lastUpdate = it.replace("UPD=", "").trim()
                }
            }
            return weatherData
        }

        fun parseElements(row: Elements): Weather {
            val weather = Weather()
            weather.temperature = row[FINDER_TEMPERATURE].text().toDouble()
            weather.maxTemperature = row[FINDER_MAX_TEMPERATURE].text().toDouble()
            weather.minTemperature = row[FINDER_MIN_TEMPERATURE].text().toDouble()
            weather.windSpeed = row[FINDER_WIND_SPEED].text().toDouble()
            weather.maxWindSpeed = row[FINDER_MAX_WIND_SPEED].text().toDouble()
            weather.humidity = row[FINDER_HUMIDITY].text().toInt()
            weather.maxHumidity = row[FINDER_MAX_HUMIDITY].text().toInt()
            weather.minHumidity = row[FINDER_MIN_HUMIDITY].text().toInt()
            weather.windDirection = row[FINDER_WIND_DIRECTION].text().toInt()
            weather.rain = row[FINDER_RAIN].text().toDouble()
            return weather
        }

        fun degreesToCompass(resources: Resources, degree: Int): String {
            val directionsArray = resources.getStringArray(R.array.directions)
            val index = (0.5 + (degree % 360) / 45.0).toInt() % 8
            return directionsArray[index]
        }

        fun celsiusToUnits(temperature: Double, units: Int): Double {
            return when (units) {
                PreferencesManager.TEMPERATURE_FAHRENHEITS -> celsiusToFahrenheits(temperature)
                else -> temperature
            }
        }

        fun kmhToUnits(speed: Double, units: Int): Double {
            return when (units) {
                PreferencesManager.WIND_MS -> kmhToMs(speed)
                PreferencesManager.WIND_MPH -> kmhToMph(speed)
                else -> speed
            }
        }

        private fun celsiusToFahrenheits(temperature: Double): Double {
            return temperature * 1.8 + 32
        }

        private fun kmhToMs(speed: Double): Double {
            return speed * (1 / 3.6)
        }

        private fun kmhToMph(speed: Double): Double {
            return speed * 0.62137119223733
        }

    }

}