package com.samtothestreets.data.importing

import android.content.Context
import android.net.Uri
import com.opencsv.CSVReaderBuilder
import com.samtothestreets.data.entity.ProjectCase
import com.samtothestreets.domain.parsing.GenericDataInterpreter
import java.io.InputStreamReader
import java.util.UUID

object CsvParser {
    fun parseCsv(context: Context, uri: Uri): ProjectCase {
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Cannot open file")
        val reader = InputStreamReader(inputStream)
        val csvReader = CSVReaderBuilder(reader).build()
        
        val rows = mutableListOf<List<String>>()
        var row = csvReader.readNext()
        while (row != null) {
            rows.add(row.toList())
            row = csvReader.readNext()
        }
        csvReader.close()

        val filename = uri.lastPathSegment ?: "Imported CSV Dataset"
        val schema = GenericDataInterpreter.parseRawRows(rows)

        return ProjectCase(
            id = UUID.randomUUID().toString(),
            title = filename,
            description = "Imported File containing ${schema.columns.size} columns and ${schema.rowCount} rows.",
            importDate = System.currentTimeMillis(),
            tags = "Import, CSV",
            notes = "",
            serializedDataset = schema.toJson()
        )
    }
}
