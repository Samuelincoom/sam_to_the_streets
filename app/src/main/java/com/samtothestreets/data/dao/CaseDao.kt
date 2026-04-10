package com.samtothestreets.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.samtothestreets.data.entity.ProjectCase
import kotlinx.coroutines.flow.Flow

@Dao
interface CaseDao {
    @Query("SELECT * FROM project_cases")
    fun getAllCases(): Flow<List<ProjectCase>>

    @Query("SELECT * FROM project_cases WHERE id = :caseId")
    suspend fun getCaseById(caseId: String): ProjectCase?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCase(projectCase: ProjectCase)

    @Query("SELECT COUNT(*) FROM project_cases")
    suspend fun countCases(): Int
}
