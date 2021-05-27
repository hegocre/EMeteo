package cat.escolamestral.emeteo.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import androidx.preference.PreferenceManager
import cat.escolamestral.emeteo.R
import java.util.*

class PreferencesManager private constructor(context: Context) {

    private val _sharedPrefs: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    private val userUnits = mapOf(
        TEMPERATURE_CELSIUS to context.getString(R.string.celsius),
        TEMPERATURE_FAHRENHEITS to context.getString(R.string.fahrenheits),

        WIND_KMH to context.getString(R.string.km_h),
        WIND_MS to context.getString(R.string.m_s),
        WIND_MPH to context.getString(R.string.mph)
    )

    fun getUserUnits(): Map<Int, String> {
        return userUnits
    }

    fun getLiveViewUrl(): String {
        return when (_sharedPrefs.getString("live_view_quality", "standard")) {
            "high" -> STREAM_LIVE_URL_HQ
            else -> STREAM_LIVE_URL
        }
    }

    fun getSelectedLocale(): Locale {
        val language = _sharedPrefs.getString("app_language", "follow_system")!!
        ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0]
        return if (language == "follow_system" || language.isEmpty()) ConfigurationCompat.getLocales(
            Resources.getSystem().configuration
        )[0]
        else Locale(language)
    }

    fun getAppTheme(): Int {
        return when (_sharedPrefs.getString("app_theme", "follow_system")) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }

    fun getTemperatureUnits(): Int {
        val units = _sharedPrefs.getString("temperature_units", "celsius")
        return if (units == "celsius") TEMPERATURE_CELSIUS else TEMPERATURE_FAHRENHEITS
    }

    fun getWindUnits(): Int {
        val units = _sharedPrefs.getString("wind_units", "km_h")
        return if (units == "km_h") WIND_KMH else if (units == "m_s") WIND_MS else WIND_MPH
    }

    fun showIndoorTemperature(): Boolean {
        return _sharedPrefs.getBoolean("show_inside_temperature", false)
    }

    companion object {
        private var instance: PreferencesManager? = null

        fun getPreferencesInstance(context: Context): PreferencesManager {
            if (instance == null) {
                instance = PreferencesManager(context)
            }
            return instance as PreferencesManager
        }

        fun getPreferencesInstanceNoContext(): PreferencesManager? {
            return instance
        }

        const val TEMPERATURE_CELSIUS = 3
        const val TEMPERATURE_FAHRENHEITS = 4

        const val WIND_KMH = 0
        const val WIND_MS = 1
        const val WIND_MPH = 2

        private const val STREAM_LIVE_URL =
            "http://exterior.escolamestral.cat:8083/stream/emeteo/channel/1/hls/live/index.m3u8"
        private const val STREAM_LIVE_URL_HQ =
            "http://exterior.escolamestral.cat:8083/stream/emeteo/channel/0/hls/live/index.m3u8"
    }
}