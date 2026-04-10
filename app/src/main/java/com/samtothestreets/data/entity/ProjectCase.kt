package com.samtothestreets.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "project_cases")
data class ProjectCase(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val importDate: Long,
    val tags: String, // comma separated
    val notes: String,
    val serializedDataset: String = "{}"
)
