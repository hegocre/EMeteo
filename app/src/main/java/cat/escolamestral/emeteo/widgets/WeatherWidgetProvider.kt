package cat.escolamestral.emeteo.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import cat.escolamestral.emeteo.services.UpdateWeatherWidgetService

class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        if (appWidgetIds != null && context != null) {
            val thisWidget = ComponentName(context, WeatherWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager?.getAppWidgetIds(thisWidget)

            val intent = Intent(context.applicationContext, UpdateWeatherWidgetService::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds)
            UpdateWeatherWidgetService.enqueueWork(context, intent)
        }
    }
}