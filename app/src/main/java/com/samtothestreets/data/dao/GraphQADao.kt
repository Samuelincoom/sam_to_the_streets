package com.samtothestreets.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.samtothestreets.data.entity.GraphQA
import kotlinx.coroutines.flow.Flow

@Dao
interface GraphQADao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGraphQA(qa: GraphQA)

    @Update
    suspend fun updateGraphQA(qa: GraphQA)

    @Query("SELECT * FROM graph_qa WHERE caseId = :caseId ORDER BY timestamp DESC")
    fun getQAForCase(caseId: String): Flow<List<GraphQA>>
}
