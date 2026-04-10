package com.samtothestreets.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "graph_qa")
data class GraphQA(
    @PrimaryKey val id: String,
    val caseId: String,
    val question: String,
    val answer: String,
    val wasUseful: Boolean? = null,
    val timestamp: Long = System.currentTimeMillis()
)
