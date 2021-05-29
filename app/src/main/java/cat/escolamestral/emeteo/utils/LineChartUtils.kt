package cat.escolamestral.emeteo.utils

import android.content.Context
import androidx.core.content.ContextCompat
import cat.escolamestral.emeteo.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class LineChartUtils {
    companion object {
        fun create(
            context: Context,
            chart: LineChart,
            label: String,
            entries: ArrayList<Entry>,
            xLabels: Array<String>? = null,
            yLabels: Array<String>? = null,
            forceLabelCount: Boolean = false
        ) {

            val dataSet = LineDataSet(entries, label)
            dataSet.color = ContextCompat.getColor(
                context,
                if (ContextUtils.isDarkThemeOn(context)) R.color.colorSecondaryDark else R.color.colorSecondary
            )
            dataSet.setDrawCircleHole(false)
            dataSet.setCircleColor(
                ContextCompat.getColor(
                    context,
                    if (ContextUtils.isDarkThemeOn(context)) R.color.colorSecondaryDark else R.color.colorSecondary
                )
            )
            dataSet.circleRadius = 1f
            dataSet.setDrawValues(false)

            chart.data = LineData(dataSet)
            chart.axisRight.isEnabled = false
            chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            chart.xAxis.textColor = ContextCompat.getColor(
                context,
                if (ContextUtils.isDarkThemeOn(context)) R.color.textColorPrimaryDark else R.color.textColorPrimary
            )
            chart.axisLeft.textColor = ContextCompat.getColor(
                context,
                if (ContextUtils.isDarkThemeOn(context)) R.color.textColorPrimaryDark else R.color.textColorPrimary
            )
            chart.legend.isEnabled = false
            chart.description.text = ""
            if (xLabels != null)
                chart.xAxis.valueFormatter = StringAxisFormatter(xLabels)
            if (yLabels != null)
                chart.axisLeft.valueFormatter = StringAxisFormatter(yLabels)
            chart.isDoubleTapToZoomEnabled = false

            if (forceLabelCount)
                chart.xAxis.setLabelCount(entries.size, true)
        }
    }
}