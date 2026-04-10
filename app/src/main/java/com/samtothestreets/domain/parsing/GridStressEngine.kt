package com.samtothestreets.domain.parsing

import com.samtothestreets.domain.schema.DatasetSchema
import com.samtothestreets.domain.schema.ColumnType

data class StressInsight(
    val title: String,
    val description: String
)

object GridStressEngine {
    
    fun analyzeFlexibility(schema: DatasetSchema, targetColumn: String): StressInsight? {
        val yCol = schema.columns.find { it.name == targetColumn && it.type == ColumnType.NUMERIC } ?: return null
        if (yCol.numericValues.size < 5) {
            return StressInsight("Insufficient Data", "Not enough data points to compute grid stress heuristics.")
        }

        val values = yCol.numericValues

        // 1. Identify Peak Window
        var peakValue = values.first()
        var peakIndex = 0
        for (i in values.indices) {
            if (values[i] > peakValue) {
                peakValue = values[i]
                peakIndex = i
            }
        }

        // 2. Identify Steep Ramp (Max Delta)
        var maxRamp = 0f
        var rampIndexStart = 0
        for (i in 0 until values.size - 1) {
            val delta = Math.abs(values[i + 1] - values[i])
            if (delta > maxRamp) {
                maxRamp = delta
                rampIndexStart = i
            }
        }

        val description = buildString {
            append("• Peak Demand Window identified around data index $peakIndex (Value: $peakValue).\n")
            append("• Steepest Ramp Period detected between index $rampIndexStart and ${rampIndexStart + 1} (Delta: $maxRamp).\n")
            append("These periods represent maximum local stress where battery dispatch or load shifting (flexibility) would be most valuable to the network.")
        }

        return StressInsight(
            title = "Flexibility & Grid Stress Profile",
            description = description
        )
    }
}
