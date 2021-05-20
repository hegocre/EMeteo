package cat.escolamestral.emeteo.utils

import java.io.Serializable

data class ChartData(
    val name: String,
    val pointsList: ArrayList<Pair<Float, Float>>,
    val xLabels: Array<String>? = null,
    val yLabels: Array<String>? = null
) :
    Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChartData

        if (name != other.name) return false
        if (pointsList != other.pointsList) return false
        if (xLabels != null) {
            if (other.xLabels == null) return false
            if (!xLabels.contentEquals(other.xLabels)) return false
        } else if (other.xLabels != null) return false
        if (yLabels != null) {
            if (other.yLabels == null) return false
            if (!yLabels.contentEquals(other.yLabels)) return false
        } else if (other.yLabels != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + pointsList.hashCode()
        result = 31 * result + (xLabels?.contentHashCode() ?: 0)
        result = 31 * result + (yLabels?.contentHashCode() ?: 0)
        return result
    }

}