package cat.escolamestral.emeteo.utils

import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter

class StringAxisFormatter(private val stringValues: Array<String>) : ValueFormatter() {
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        return stringValues[value.toInt()]
    }
}