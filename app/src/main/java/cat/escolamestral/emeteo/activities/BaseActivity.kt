package cat.escolamestral.emeteo.activities

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import cat.escolamestral.emeteo.utils.ContextUtils
import cat.escolamestral.emeteo.utils.PreferencesManager

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null) {
            val prefs = PreferencesManager.getPreferencesInstance(newBase.applicationContext)
            val contextWrapper = ContextUtils.updateLocale(newBase, prefs.getSelectedLocale())
            super.attachBaseContext(contextWrapper)

            //Fix language chaning bug on API L to N_MR1
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1)
                applyOverrideConfiguration(contextWrapper.resources.configuration)
        } else super.attachBaseContext(newBase)
    }
}