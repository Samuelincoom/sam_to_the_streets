package com.samtothestreets.domain.schema

import com.google.gson.Gson

data class DatasetSchema(
    val columns: List<ColumnSchema> = emptyList(),
    val rowCount: Int = 0
) {
    fun toJson(): String = Gson().toJson(this)
    
    companion object {
        fun fromJson(json: String): DatasetSchema {
            return try {
                Gson().fromJson(json, DatasetSchema::class.java)
            } catch (e: Exception) {
                DatasetSchema()
            }
        }
    }
}

data class ColumnSchema(
    val name: String,
    val type: ColumnType,
    val stringValues: List<String> = emptyList(),
    val numericValues: List<Float> = emptyList()
) {
    fun min(): Float = numericValues.minOrNull() ?: 0f
    fun max(): Float = numericValues.maxOrNull() ?: 0f
    fun avg(): Float = if (numericValues.isNotEmpty()) numericValues.average().toFloat() else 0f
}

enum class ColumnType {
    NUMERIC,
    STRING,
    DATETIME
}
