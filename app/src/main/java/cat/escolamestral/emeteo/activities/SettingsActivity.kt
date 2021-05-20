package cat.escolamestral.emeteo.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import cat.escolamestral.emeteo.R
import cat.escolamestral.emeteo.databinding.ActivitySettingsBinding
import cat.escolamestral.emeteo.utils.PreferencesManager

class SettingsActivity : BaseActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var binding: ActivitySettingsBinding
    private var languageChanged = false
    private var unitsChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        key?.let {
            when (it) {
                "app_theme" -> {
                    val prefs = PreferencesManager.getPreferencesInstance(this)
                    AppCompatDelegate.setDefaultNightMode(prefs.getAppTheme())
                }
                "app_language" -> {
                    languageChanged = true
                    this.recreate()
                }
                "wind_units", "temperature_units" -> unitsChanged = true
                else -> {
                }
            }
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        languageChanged = savedInstanceState.getBoolean("language_changed")
        unitsChanged = savedInstanceState.getBoolean("units_changed")
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("language_changed", languageChanged)
        outState.putBoolean("units_changed", unitsChanged)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finishWithData()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        finishWithData()
        super.onBackPressed()
    }

    private fun finishWithData() {
        val intent = Intent()
        intent.putExtra("language_changed", languageChanged)
        intent.putExtra("units_changed", unitsChanged)
        setResult(RESULT_OK, intent)
        finish()
    }
}