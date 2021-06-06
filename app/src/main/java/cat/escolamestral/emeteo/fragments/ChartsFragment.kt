package cat.escolamestral.emeteo.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import cat.escolamestral.emeteo.R
import cat.escolamestral.emeteo.adapters.ChartRecyclerAdapter
import cat.escolamestral.emeteo.adapters.ViewPagerAdapter
import cat.escolamestral.emeteo.databinding.FragmentChartsBinding
import cat.escolamestral.emeteo.utils.ChartData
import cat.escolamestral.emeteo.utils.PreferencesManager
import cat.escolamestral.emeteo.utils.Weather
import cat.escolamestral.emeteo.utils.isDarkThemeOn
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import org.jsoup.Jsoup
import java.text.DateFormatSymbols
import java.util.*
import kotlin.collections.ArrayList

class ChartsFragment : Fragment() {

    private var _binding: FragmentChartsBinding? = null
    private val binding get() = _binding!!

    private val prefs = PreferencesManager.getPreferencesInstanceNoContext()
    private var temperatureUnits = PreferencesManager.TEMPERATURE_CELSIUS
    private var windUnits = PreferencesManager.WIND_KMH

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartsBinding.inflate(inflater, container, false)

        downloadData(HISTORIC_YEAR_URL)
        downloadData(HISTORIC_MONTH_URL)

        setupTabs()

        if (prefs != null) {
            temperatureUnits = prefs.getTemperatureUnits()
            windUnits = prefs.getWindUnits()
        }

        return binding.root
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_monthly))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_yearly))

        binding.tabLayout.setSelectedTabIndicatorColor(
            ContextCompat.getColor(
                requireContext(),
                if (requireContext().isDarkThemeOn()) R.color.colorSecondaryDark else R.color.white
            )
        )

        binding.chartsPager.adapter =
            ViewPagerAdapter(binding.root, arrayOf(R.id.month_charts, R.id.year_charts))
        binding.chartsPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(binding.tabLayout))
        binding.tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.chartsPager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun downloadData(url: String) {
        Thread {
            val data: String = try {
                Jsoup.connect(url)
                    .get().wholeText()
            } catch (e: Exception) {
                "null"
            }

            runOnUiThread {
                if (data != "null") {
                    when (url) {
                        HISTORIC_MONTH_URL -> readHistoricMonthData(data)
                        HISTORIC_YEAR_URL -> readHistoricYearData(data)
                    }
                }
            }
        }.start()
    }

    private fun readHistoricYearData(data: String) {
        if (data.isEmpty()) return
        val temperaturePointsList = ArrayList<Pair<Float, Float>>()
        val rainPointsList = ArrayList<Pair<Float, Float>>()
        val windPointsList = ArrayList<Pair<Float, Float>>()

        //Temperature lines
        for (i in 10..21) {
            val line = data.split("\n")[i].trim()
            val values = line.split(" +".toRegex())
            if (values.size > 4)
                temperaturePointsList.add(
                    Pair(
                        values[1].toFloat() - 1,
                        Weather.celsiusToUnits(values[4].toDouble(), temperatureUnits).toFloat()
                    )
                )
        }

        //Precipitation values
        for (i in 31..42) {
            val line = data.split("\n")[i].trim()
            val values = line.split(" +".toRegex())
            if (values.size > 2)
                rainPointsList.add(Pair(values[1].toFloat() - 1, values[2].toFloat()))
        }

        //Wind values
        for (i in 50..61) {
            val line = data.split("\n")[i].trim()
            val values = line.split(" +".toRegex())
            if (values.size > 2)
                windPointsList.add(
                    Pair(
                        values[1].toFloat() - 1,
                        Weather.kmhToUnits(values[2].toDouble(), windUnits).toFloat()
                    )
                )
        }

        val temperatureData = ChartData(
            "${getString(R.string.temperature)} (${prefs?.getUserUnits()?.get(temperatureUnits)})",
            temperaturePointsList,
            DateFormatSymbols(Locale.getDefault()).shortMonths
        )
        val rainData = ChartData(
            "${getString(R.string.precipitation)} (${getString(R.string.mm)})",
            rainPointsList,
            DateFormatSymbols(Locale.getDefault()).shortMonths
        )
        val windData = ChartData(
            "${getString(R.string.wind)} (${prefs?.getUserUnits()?.get(windUnits)})",
            windPointsList,
            DateFormatSymbols(Locale.getDefault()).shortMonths
        )

        val chartData = arrayOf(temperatureData, rainData, windData)
        binding.yearCharts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ChartRecyclerAdapter(requireContext(), chartData)
        }
    }

    private fun readHistoricMonthData(data: String) {
        if (data.isEmpty()) return
        val temperaturePointsList = ArrayList<Pair<Float, Float>>()
        val rainPointsList = ArrayList<Pair<Float, Float>>()
        val windPointsList = ArrayList<Pair<Float, Float>>()

        //Temperature, precipitation and lines
        for (i in 11..41) {
            val line = data.split("\n")[i].trim()
            val values = line.split(" +".toRegex())
            if (values.size > 4) {
                temperaturePointsList.add(
                    Pair(
                        values[0].toFloat(),
                        Weather.celsiusToUnits(values[1].toDouble(), temperatureUnits).toFloat()
                    )
                )
                rainPointsList.add(Pair(values[0].toFloat(), values[8].toFloat()))
                windPointsList.add(
                    Pair(
                        values[0].toFloat(),
                        Weather.kmhToUnits(values[9].toDouble(), windUnits).toFloat()
                    )
                )
            }
        }

        val temperatureData = ChartData(
            "${getString(R.string.temperature)} (${prefs?.getUserUnits()?.get(temperatureUnits)})",
            temperaturePointsList
        )
        val rainData = ChartData(
            "${getString(R.string.precipitation)} (${getString(R.string.mm)})",
            rainPointsList
        )
        val windData = ChartData(
            "${getString(R.string.wind)} (${prefs?.getUserUnits()?.get(windUnits)})",
            windPointsList
        )

        val chartData = arrayOf(temperatureData, rainData, windData)
        binding.monthCharts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ChartRecyclerAdapter(requireContext(), chartData)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun Fragment?.runOnUiThread(action: () -> Unit) {
        this ?: return
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }

    companion object {
        private const val HISTORIC_MONTH_URL = "https://www.escolamestral.cat/meteo/NOAAMO.TXT"
        private const val HISTORIC_YEAR_URL = "https://www.escolamestral.cat/meteo/NOAAYR.TXT"
    }
}
