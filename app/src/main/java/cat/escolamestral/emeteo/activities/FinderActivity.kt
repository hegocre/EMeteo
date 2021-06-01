package cat.escolamestral.emeteo.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import cat.escolamestral.emeteo.R
import cat.escolamestral.emeteo.databinding.ActivityFinderBinding
import cat.escolamestral.emeteo.utils.PreferencesManager
import cat.escolamestral.emeteo.utils.Weather
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.text.DecimalFormat

class FinderActivity : BaseActivity() {

    private lateinit var binding: ActivityFinderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinderBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        val year = intent.getIntExtra("year", 2007)
        val month = intent.getIntExtra("month", 0)
        val dayOfMonth = intent.getIntExtra("dayOfMonth", 0)

        title = String.format(
            "%s/%s/%s", DecimalFormat("00").format(dayOfMonth).toString(),
            DecimalFormat("00").format(month).toString(), year.toString()
        )

        binding.weatherLayout.visibility = View.GONE

        fetchData(year, month, dayOfMonth)
    }

    private fun fetchData(year: Int, month: Int, dayOfMonth: Int) {
        Thread {
            val url = String.format(
                "http://infomet.am.ub.es/clima/sfl2/sf2%s%s.htm",
                DecimalFormat("00").format(year - 2000).toString(),
                DecimalFormat("00").format(month).toString()
            )

            val data: Elements = try {
                Jsoup.connect(url).get()
                    .select("table")[0].select("tr")[dayOfMonth + 3].select("td")
            } catch (e: HttpStatusException) {
                Elements()
            }

            runOnUiThread {
                binding.weatherLoading.visibility = View.GONE
                if (data.isEmpty() || data[2].text() == "-") {
                    //No data was returned, or it is not filled yet
                    showNoDataDialog()
                } else {
                    setData(Weather.parseElements(data))
                }
            }
        }.start()
    }

    private fun setData(weather: Weather) {
        binding.weatherLoading.visibility = View.GONE
        val prefs = PreferencesManager.getPreferencesInstance(this)

        with(weather) {
            binding.temperatureText.text = getString(R.string.weather_temperature, temperature)
            binding.temperatureMinMaxText.text =
                getString(R.string.weather_temperature_min_max, minTemperature, maxTemperature)

            binding.humidityText.text = getString(R.string.weather_humidity, humidity)
            binding.humidityMinMaxText.text =
                getString(R.string.weather_humidity_min_max, minHumidity, maxHumidity)

            binding.rainText.text = getString(R.string.weather_rain, rain)

            val windPlaceholder = when (prefs.getWindUnits()) {
                PreferencesManager.WIND_MS -> R.string.weather_wind_speed_ms
                PreferencesManager.WIND_MPH -> R.string.weather_wind_speed_mph
                else -> R.string.weather_wind_speed_kmh
            }
            binding.windSpeedText.text = getString(windPlaceholder, windSpeed)
            binding.windSpeedMaxText.text = getString(windPlaceholder, maxWindSpeed)

            binding.windDirectionText.text = Weather.degreesToCompass(resources, windDirection)
            binding.windDirectionImage.rotation = (windDirection - 45).toFloat()
        }

        binding.weatherLayout.visibility = View.VISIBLE
    }

    private fun showNoDataDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.finder_no_data_available)
            .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home)
            finish()
        return super.onOptionsItemSelected(item)
    }
}
