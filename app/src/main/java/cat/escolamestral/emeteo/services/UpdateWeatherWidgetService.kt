package cat.escolamestral.emeteo.services

import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.RemoteViews
import cat.escolamestral.emeteo.R
import cat.escolamestral.emeteo.utils.PreferencesManager
import cat.escolamestral.emeteo.utils.Weather
import cat.escolamestral.emeteo.widgets.WeatherWidgetProvider
import org.jsoup.Jsoup


class UpdateWeatherWidgetService : Service() {

    private var i: Intent? = null
    private var started = false
    private val h = Handler(Looper.getMainLooper())
    lateinit var r: Runnable

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (started) h.removeCallbacks(r)

        started = true
        i = intent
        r = Runnable {
            downloadData()
        }
        h.post(r)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        h.removeCallbacks(r)
    }

    private fun downloadData() {
        val appWidgetManager = AppWidgetManager.getInstance(this.applicationContext)

        val allWidgetIds = i?.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)

        Thread(kotlinx.coroutines.Runnable {
            val data = try {
                Jsoup.connect(WEATHER_URL)
                    .get().body().html()
            } catch (e: Exception) {
                "null"
            }

            if (allWidgetIds != null) {
                for (widgetId in allWidgetIds) {
                    val remoteViews =
                        RemoteViews(this.applicationContext.packageName, R.layout.widget_weather)

                    if (data != "null") {
                        val weather = Weather.parseString(data)

                        with(remoteViews) {
                            with(weather) {
                                setTextViewText(
                                    R.id.temperature_text,
                                    applicationContext.getString(
                                        R.string.widget_temperature,
                                        temperature
                                    )
                                )
                                setTextViewText(
                                    R.id.humidity_text,
                                    applicationContext.getString(R.string.widget_humidity, humidity)
                                )
                                /*setTextViewText(
                                    R.id.rain_text,
                                    applicationContext.getString(R.string.widget_rain, rain)
                                )*/
                                val windPlaceholder =
                                    when (PreferencesManager.getPreferencesInstance(
                                        applicationContext
                                    )
                                        .getWindUnits()) {
                                        PreferencesManager.WIND_MS -> R.string.widget_wind_speed_ms
                                        PreferencesManager.WIND_MPH -> R.string.widget_wind_speed_mph
                                        else -> R.string.widget_wind_speed_kmh
                                    }
                                setTextViewText(
                                    R.id.wind_speed_text,
                                    applicationContext.getString(windPlaceholder, windSpeed)
                                )
                                setTextViewText(
                                    R.id.wind_direction_text,
                                    Weather.degreesToCompass(
                                        applicationContext.resources,
                                        windDirection
                                    )
                                )

                            }
                        }

                    }

                    val clickIntent =
                        Intent(this.applicationContext, WeatherWidgetProvider::class.java)

                    clickIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds)

                    val pendingIntent = PendingIntent.getBroadcast(
                        applicationContext,
                        0,
                        clickIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    remoteViews.setOnClickPendingIntent(R.id.refresh_button, pendingIntent)
                    appWidgetManager.updateAppWidget(widgetId, remoteViews)

                }
            }
        }).start()

        if (allWidgetIds != null) {
            for (widgetId in allWidgetIds) {
                val remoteViews =
                    RemoteViews(this.applicationContext.packageName, R.layout.widget_weather)

                val clickIntent = Intent(this.applicationContext, WeatherWidgetProvider::class.java)

                clickIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds)

                val pendingIntent = PendingIntent.getBroadcast(
                    applicationContext,
                    0,
                    clickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
                remoteViews.setOnClickPendingIntent(R.id.refresh_button, pendingIntent)
                appWidgetManager.updateAppWidget(widgetId, remoteViews)

            }
        }

    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val WEATHER_URL = "https://www.escolamestral.cat/meteo/meteoclimatic.htm"
    }

}