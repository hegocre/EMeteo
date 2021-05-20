package cat.escolamestral.emeteo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cat.escolamestral.emeteo.databinding.RowChartBinding
import cat.escolamestral.emeteo.utils.ChartData
import cat.escolamestral.emeteo.utils.LineChartUtils
import com.github.mikephil.charting.data.Entry

class ChartRecyclerAdapter(
    private val context: Context,
    private val chartData: Array<ChartData>
) : RecyclerView.Adapter<ChartRecyclerAdapter.ViewHolder>() {

    override fun getItemCount(): Int = chartData.size

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getItemViewType(position: Int): Int = position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            RowChartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(chartData[position]) {
                val entries = ArrayList<Entry>()
                pointsList.forEach {
                    entries.add(Entry(it.first, it.second))
                }
                LineChartUtils.create(
                    context,
                    binding.chart,
                    name,
                    entries,
                    xLabels,
                    yLabels,
                    forceLabelCount = entries.size < 7
                )
                binding.titleText.text = name
            }
        }
    }

    inner class ViewHolder(val binding: RowChartBinding) :
        RecyclerView.ViewHolder(binding.root)
}