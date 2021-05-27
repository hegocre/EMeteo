package cat.escolamestral.emeteo.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.*

class ContextUtils(base: Context) : ContextWrapper(base) {

    companion object {

        //Source: https://medium.com/swlh/android-app-specific-language-change-programmatically-using-kotlin-d650a5392220
        @Suppress("DEPRECATION")
        fun updateLocale(c: Context, newLocale: Locale): ContextUtils {
            var context = c
            val resources = context.resources
            val configuration: Configuration = resources.configuration

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val localeList = LocaleList(newLocale)
                LocaleList.setDefault(localeList)
                configuration.setLocales(localeList)
            } else configuration.locale = newLocale

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
                context = context.createConfigurationContext(configuration)
            else resources.updateConfiguration(configuration, resources.displayMetrics)

            return ContextUtils(context)
        }

        fun isDarkThemeOn(c: Context): Boolean {
            return c.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
    }
}