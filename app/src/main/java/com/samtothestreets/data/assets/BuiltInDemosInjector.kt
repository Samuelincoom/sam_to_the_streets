package com.samtothestreets.data.assets

import android.content.Context
import android.net.Uri
import android.util.Log
import com.samtothestreets.data.entity.ProjectCase
import com.samtothestreets.data.importing.XlsxParser
import java.io.File
import java.io.FileOutputStream

object BuiltInDemosInjector {
    private const val DEMO_DIR = "demo_data/heinen"

    suspend fun parseAndInjectDemos(context: Context): List<ProjectCase> {
        val extractedFiles = copyAssetsToCache(context)
        if (extractedFiles.isEmpty()) {
            Log.e("BuiltInDemosInjector", "Required demo files not found in assets. Make sure Excel files are bundled.")
            return generateMissingAssetNotes()
        }

        val demos = mutableListOf<ProjectCase>()

        val priceFile = extractedFiles.find { it.name.contains("EPEX_SPOT", ignoreCase = true) }
        if (priceFile != null) {
            try {
                val case = XlsxParser.parseXlsx(context, Uri.fromFile(priceFile))
                demos.add(case.copy(
                    id = "generic_demo_price",
                    title = "Energy Prices Demo",
                    description = "Built-in generic dataset mapping EPEX pricing.",
                    tags = "Demo, Price, Energy"
                ))
            } catch (e: Exception) { Log.e("Builder", "Error parsing price demo", e) }
        }

        val loadFile = extractedFiles.find { it.name.contains("Load_Germany", ignoreCase = true) }
        if (loadFile != null) {
            try {
                 val case = XlsxParser.parseXlsx(context, Uri.fromFile(loadFile))
                 demos.add(case.copy(
                    id = "generic_demo_load",
                    title = "Load and Battery Demo",
                    description = "Built-in generic dataset mapping electric load curves.",
                    tags = "Demo, Load, Grid"
                ))
            } catch (e: Exception) { Log.e("Builder", "Error parsing load demo", e) }
        }
        
        return demos
    }

    private fun copyAssetsToCache(context: Context): List<File> {
        val extracted = mutableListOf<File>()
        val assetManager = context.assets
        try {
            val fileNames = assetManager.list(DEMO_DIR) ?: return emptyList()
            for (fileName in fileNames) {
                if (!fileName.endsWith(".xlsx")) continue
                val inFile = assetManager.open("${DEMO_DIR}/$fileName")
                val outFile = File(context.cacheDir, fileName)
                inFile.use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }
                extracted.add(outFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return extracted
    }

    private fun generateMissingAssetNotes(): List<ProjectCase> {
        val missing = ProjectCase(
            id = "generic_missing",
            title = "Missing Demo Resources",
            description = "No Excel files were found bundled in the app for demos.",
            importDate = System.currentTimeMillis(),
            tags = "Error, Missing Data",
            notes = "Place XLSX files in assets/demo_data/heinen."
        )
        return listOf(missing)
    }
}
