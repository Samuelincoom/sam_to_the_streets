package com.samtothestreets.data.importing

import android.content.Context
import android.net.Uri
import com.samtothestreets.data.entity.ProjectCase
import com.samtothestreets.domain.parsing.GenericDataInterpreter
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.util.UUID

object XlsxParser {
    fun parseXlsx(context: Context, uri: Uri): ProjectCase {
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Cannot open file")
        
        val workbook = WorkbookFactory.create(inputStream)
        val sheet = workbook.getSheetAt(0) ?: throw Exception("No sheets in workbook")
        val formatter = DataFormatter()

        val rows = mutableListOf<List<String>>()

        for (r in sheet.firstRowNum..sheet.lastRowNum) {
            val row = sheet.getRow(r)
            if (row != null) {
                val rowData = mutableListOf<String>()
                val maxCell = row.lastCellNum.toInt().coerceAtLeast(0)
                for (cn in 0 until maxCell) {
                    val cell = row.getCell(cn, org.apache.poi.ss.usermodel.Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
                    rowData.add(formatter.formatCellValue(cell))
                }
                if (rowData.any { it.isNotBlank() }) {
                    rows.add(rowData)
                }
            }
        }
        
        workbook.close()

        val filename = uri.lastPathSegment ?: "Imported Excel Dataset"
        val schema = GenericDataInterpreter.parseRawRows(rows)

        return ProjectCase(
            id = UUID.randomUUID().toString(),
            title = filename,
            description = "Excel file parsed locally. Contains ${schema.columns.size} columns.",
            importDate = System.currentTimeMillis(),
            tags = "Import, Excel",
            notes = "",
            serializedDataset = schema.toJson()
        )
    }
}
