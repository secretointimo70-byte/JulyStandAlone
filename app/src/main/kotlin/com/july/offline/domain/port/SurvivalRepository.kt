package com.july.offline.domain.port

import com.july.offline.domain.model.SurvivalCategory
import com.july.offline.domain.model.SurvivalContent

interface SurvivalRepository {
    suspend fun getContent(category: SurvivalCategory, language: String): SurvivalContent?
    suspend fun listCategories(language: String): List<SurvivalContent>
    suspend fun searchSteps(query: String, language: String): List<SurvivalContent>
}
