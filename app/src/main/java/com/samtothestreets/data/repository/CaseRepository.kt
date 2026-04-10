package com.samtothestreets.data.repository

import com.samtothestreets.data.dao.CaseDao
import com.samtothestreets.data.dao.GraphQADao
import com.samtothestreets.data.entity.ProjectCase
import com.samtothestreets.data.entity.GraphQA
import kotlinx.coroutines.flow.Flow

class CaseRepository(
    private val caseDao: CaseDao,
    private val graphQADao: GraphQADao
) {

    val allCases: Flow<List<ProjectCase>> = caseDao.getAllCases()

    suspend fun getCaseById(caseId: String): ProjectCase? {
        return caseDao.getCaseById(caseId)
    }

    suspend fun insertCaseConfig(c: ProjectCase) {
        caseDao.insertCase(c)
    }

    // Graph QA Methods
    suspend fun insertGraphQA(qa: GraphQA) {
        graphQADao.insertGraphQA(qa)
    }

    suspend fun updateGraphQA(qa: GraphQA) {
        graphQADao.updateGraphQA(qa)
    }

    fun getQAForCase(caseId: String): Flow<List<GraphQA>> {
        return graphQADao.getQAForCase(caseId)
    }
}
