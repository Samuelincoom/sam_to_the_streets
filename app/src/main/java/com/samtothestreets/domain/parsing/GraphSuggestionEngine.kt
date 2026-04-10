package com.samtothestreets.domain.parsing

import com.samtothestreets.domain.schema.ColumnType
import com.samtothestreets.domain.schema.DatasetSchema

object GraphSuggestionEngine {

    /**
     * Determines default X and Y columns and the ideal ChartType (assuming Vico Line or Bar).
     */
    fun suggestDefaultGraph(schema: DatasetSchema): GraphSuggestion {
        if (schema.columns.isEmpty()) return GraphSuggestion("No Data", null, null)

        val timeColumn = schema.columns.find { it.type == ColumnType.DATETIME }
        val stringColumn = schema.columns.find { it.type == ColumnType.STRING && it != timeColumn }
        val numericColumns = schema.columns.filter { it.type == ColumnType.NUMERIC }

        if (numericColumns.isEmpty()) return GraphSuggestion("No Numeric Data", null, null)

        val yAxis = numericColumns.first()

        // 1. Time series -> Line
        if (timeColumn != null) {
            return GraphSuggestion("Line", timeColumn.name, yAxis.name, suggestionReason = "Detected time series.")
        }
        
        // 2. Categories -> Bar
        if (stringColumn != null) {
            return GraphSuggestion("Bar", stringColumn.name, yAxis.name, suggestionReason = "Detected categories vs values.")
        }

        // 3. Just Numerics -> Line or Scatter natively
        return GraphSuggestion(
            "Line", 
            null, // Index based X
            yAxis.name, 
            suggestionReason = "Numeric trends detected."
        )
    }
}

data class GraphSuggestion(
    val recommendedType: String,
    val xAxisName: String?,
    val yAxisName: String?,
    val suggestionReason: String = ""
)
