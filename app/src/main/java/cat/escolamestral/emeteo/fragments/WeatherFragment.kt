package cat.escolamestral.emeteo.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import cat.escolamestral.emeteo.R
import cat.escolamestral.emeteo.databinding.FragmentWeatherBinding
import cat.escolamestral.emeteo.utils.ContextUtils
import cat.escolamestral.emeteo.utils.LineChartUtils
import cat.escolamestral.emeteo.utils.PreferencesManager
import cat.escolamestral.emeteo.utils.Weather
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import kotlinx.coroutines.Runnable
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class WeatherFragment : Fragment() {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!
    private lateinit var weatherData: Weather

    private lateinit var chartsData: Document

    private val prefs = PreferencesManager.getPreferencesInstanceNoContext()
    private var temperatureUnits = PreferencesManager.TEMPERATURE_CELSIUS
    private var windUnits = PreferencesManager.WIND_KMH

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)
        binding.weatherLayout.visibility = View.GONE

        binding.swipeRefreshLayout.setOnRefreshListener { downloadData(); downloadCharts(null) }
        if (ContextUtils.isDarkThemeOn(requireContext())) {
            binding.swipeRefreshLayout.setColorSchemeResources(R.color.colorSecondaryDark)
            binding.swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(requireContext(), R.color.swipeRefreshBackgroundDark)
            )
        } else
            binding.swipeRefreshLayout.setColorSchemeResources(R.color.colorSecondary)

        binding.temperatureLayout.setOnClickListener { getChart(TEMPERATURE) }
        binding.humidityLayout.setOnClickListener { getChart(HUMIDITY) }
        binding.precipitationLayout.setOnClickListener { getChart(RAIN) }
        binding.windSpeedLayout.setOnClickListener { getChart(WIND) }

        if (prefs != null) {
            temperatureUnits = prefs.getTemperatureUnits()
            windUnits = prefs.getWindUnits()
        }

        downloadData()
        downloadCharts(null)
        return binding.root
    }

    private fun downloadData() {
        Thread(Runnable {
            val data: String = try {
                Jsoup.connect(WEATHER_URL)
                    .get().body().html()
            } catch (e: Exception) {
                "null"
            }

            runOnUiThread {
                if (data == "null") {
                    showNoDataDialog()
                } else {
                    setData(Weather.parseString(data))
                }
            }
        }).start()
    }

    private fun downloadCharts(type: Int?) {
        if (context == null) return

        val dialog = AlertDialog.Builder(requireContext()).create()

        val thread = Thread(Runnable {
            val data: Document = try {
                Jsoup.connect(CHARTS_URL)
                    .get()
            } catch (e: Exception) {
                Document(null)
            }

            runOnUiThread {
                chartsData = data
                if (type != null) {
                    dialog.dismiss()
                    openChart(type)
                }
            }
        })
        thread.start()

        if (type != null) {
            dialog.setMessage(getString(R.string.loading))
            dialog.setCancelable(true)
            dialog.setOnCancelListener { thread.interrupt() }
            dialog.show()
        }
    }

    private fun setData(weather: Weather) {
        weatherData = weather
        if (_binding == null) return

        binding.weatherLoading.visibility = View.GONE
        binding.swipeRefreshLayout.isRefreshing = false

        val prefs = PreferencesManager.getPreferencesInstance(requireContext())

        with(weatherData) {
            binding.temperatureText.text = if (prefs.showIndoorTemperature()) String.format(
                "%s | %s",
                getString(R.string.weather_temperature, temperature),
                getString(R.string.weather_temperature, insideTemperature)
            ) else getString(R.string.weather_temperature, temperature)


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

            binding.windDirectionText.text = Weather.degreesToCompass(resources, windDirection)
            binding.windDirectionImage.rotation = (windDirection - 45).toFloat()

            binding.updatedText.text = getString(R.string.weather_updated, lastUpdate)
        }

        binding.weatherLayout.visibility = View.VISIBLE
    }

    private fun getChart(type: Int) {
        if (!this::chartsData.isInitialized)
            downloadCharts(type)
        else
            openChart(type)
    }

    private fun openChart(type: Int) {
        if (context == null) return
        val names = mapOf(
            TEMPERATURE to getString(R.string.temperature),
            HUMIDITY to getString(R.string.humidity),
            RAIN to getString(R.string.precipitation),
            WIND to getString(R.string.wind)
        )

        val units = mapOf(
            TEMPERATURE to if (prefs != null) prefs.getUserUnits()[temperatureUnits] else getString(
                R.string.celsius
            ),
            HUMIDITY to getString(R.string.percent),
            RAIN to getString(R.string.mm),
            WIND to if (prefs != null) prefs.getUserUnits()[windUnits] else getString(R.string.km_h)
        )

        val builder = AlertDialog.Builder(requireContext())
        val customLayout = View.inflate(activity, R.layout.dialog_charts, null)

        val chart = customLayout.findViewById<LineChart>(R.id.data_chart)
        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()

        chartsData.body().html().split("[0-9]{2}/[0-9]{2}/[0-9]{2}".toRegex())
            .forEachIndexed { index, s ->
                if (index > 0) {
                    val value = s.trim().split(" +".toRegex())[type - 1]
                    entries.add(
                        Entry(
                            (index - 1).toFloat(),
                            when {
                                value == "---" -> 0f
                                type == TEMPERATURE -> Weather.celsiusToUnits(
                                    value.toDouble(),
                                    temperatureUnits
                                ).toFloat()
                                type == WIND -> Weather.kmhToUnits(value.toDouble(), windUnits)
                                    .toFloat()
                                else -> value.toFloat()
                            }
                        )
                    )
                    labels.add(s.trim().split(" +".toRegex())[0])
                }
            }

        LineChartUtils.create(
            requireContext(),
            chart,
            names[type].toString(),
            entries,
            labels.toTypedArray(),
            animate = true
        )

        builder.setView(customLayout)
        builder.setCancelable(true)
        builder.setTitle("${names[type]} (${units[type]})")
        builder.create().show()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun showNoDataDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.weather_no_data_available)
            .setPositiveButton(R.string.retry) { _, _ -> downloadData(); downloadCharts(null) }
            .show()
    }

    private fun Fragment?.runOnUiThread(action: () -> Unit) {
        this ?: return
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }

    companion object {
        private const val WEATHER_URL = "https://www.escolamestral.cat/meteo/meteoclimatic.htm"
        private const val CHARTS_URL = "https://www.escolamestral.cat/meteo/downld02.txt"
        private const val TEMPERATURE = 2
        private const val HUMIDITY = 5
        private const val RAIN = 17
        private const val WIND = 7
    }

}
