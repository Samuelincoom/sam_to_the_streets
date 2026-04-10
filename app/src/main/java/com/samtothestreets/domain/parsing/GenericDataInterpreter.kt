package com.samtothestreets.domain.parsing

import com.samtothestreets.domain.schema.ColumnSchema
import com.samtothestreets.domain.schema.ColumnType
import com.samtothestreets.domain.schema.DatasetSchema

object GenericDataInterpreter {

    /**
     * Takes raw rows (where first row is headers) and builds a strongly typed DatasetSchema.
     */
    fun parseRawRows(rows: List<List<String>>): DatasetSchema {
        if (rows.isEmpty()) return DatasetSchema(emptyList(), 0)

        val headers = rows.first()
        val dataRows = rows.drop(1)
        val rowCount = dataRows.size

        val columnSchemas = mutableListOf<ColumnSchema>()

        for (colIndex in headers.indices) {
            val colName = headers[colIndex]
            
            // Extract raw values for this column
            val rawValues = mutableListOf<String>()
            for (row in dataRows) {
                if (colIndex < row.size) {
                    rawValues.add(row[colIndex].trim())
                } else {
                    rawValues.add("")
                }
            }

            // Determine Type
            val isNumeric = rawValues.filter { it.isNotEmpty() }.all { 
                it.toFloatOrNull() != null || it == "-" 
            }

            if (isNumeric && rawValues.isNotEmpty()) {
                val numericVals = rawValues.map { it.replace(",","").toFloatOrNull() ?: 0f }
                columnSchemas.add(ColumnSchema(colName, ColumnType.NUMERIC, numericValues = numericVals))
            } else {
                // If it looks like a time string, we categorize it as DATETIME (simple heuristic)
                val isTime = rawValues.filter { it.isNotEmpty() }.any { 
                    it.contains(":") || (it.contains("-") && it.length > 8) || it.contains("/")
                }
                val type = if (isTime) ColumnType.DATETIME else ColumnType.STRING
                columnSchemas.add(ColumnSchema(colName, type, stringValues = rawValues))
            }
        }

        return DatasetSchema(columnSchemas, rowCount)
    }
}
