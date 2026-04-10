package com.samtothestreets.data.importing

import android.content.Context
import android.net.Uri
import com.samtothestreets.data.entity.ProjectCase
import com.samtothestreets.domain.schema.DatasetSchema
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

object SamParser {
    fun parseSam(context: Context, uri: Uri): ProjectCase {
        val metadataMap = mutableMapOf<String, String>()
        var fileContent = ""
        
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val reader = BufferedReader(InputStreamReader(stream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                fileContent = sb.toString()
            }
        } catch (e: Exception) {
            throw Exception("Could not read .sam file stream: ${e.message}")
        }

        try {
            // Best effort JSON parse for metadata extraction
            val json = JSONObject(fileContent)
            
            // Common SAM JSON keys to look for
            val scanKeys = listOf("project_name", "system_capacity", "module_type", "inverter_type", "scenario", "location", "analysis_period")
            
            for (key in scanKeys) {
                if (json.has(key)) {
                    metadataMap[key] = json.getString(key)
                }
            }
            
            // Sometimes it's nested deep in a "system_design" or "active_case" object
            val keys = json.keys()
            while (keys.hasNext()) {
                val topLevelKey = keys.next()
                val maybeObj = json.optJSONObject(topLevelKey)
                if (maybeObj != null) {
                    for (k in scanKeys) {
                        if (maybeObj.has(k)) {
                            metadataMap[k] = maybeObj.getString(k)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Not a flat JSON or unparsable structure. We will gracefully fallback.
            metadataMap["Parsing Error"] = "File could not be deeply parsed as JSON. It may be compressed or use an unknown schema."
        }

        val extractedNotes = if (metadataMap.isEmpty()) {
            "Partial Import: Could not extract specific system metrics from this .sam file."
        } else {
            "Extracted Metadata:\n" + metadataMap.entries.joinToString("\n") { "• ${it.key}: ${it.value}" }
        }

        val emptySchema = DatasetSchema(columns = emptyList())
        val name = uri.lastPathSegment ?: "Imported SAM Project"

        return ProjectCase(
            id = UUID.randomUUID().toString(),
            title = name,
            description = extractedNotes,
            importDate = System.currentTimeMillis(),
            tags = "SAM-Project",
            notes = "",
            serializedDataset = emptySchema.toJson()
        )
    }
}
