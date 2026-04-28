package com.july.offline.data.repository

import com.july.offline.data.db.dao.SurvivalContentDao
import com.july.offline.data.db.entity.SurvivalContentDbEntity
import com.july.offline.data.db.entity.SurvivalStepDbEntity
import com.july.offline.domain.model.SurvivalCategory
import com.july.offline.domain.model.SurvivalContent
import com.july.offline.domain.model.SurvivalStep
import com.july.offline.domain.port.SurvivalRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SurvivalRepositoryImpl @Inject constructor(
    private val dao: SurvivalContentDao
) : SurvivalRepository {

    override suspend fun getContent(category: SurvivalCategory, language: String): SurvivalContent? {
        val entity = dao.getContent(category.name, language) ?: return null
        val steps = dao.getSteps(entity.id)
        return entity.toDomain(steps)
    }

    override suspend fun listCategories(language: String): List<SurvivalContent> {
        return dao.listAll(language).map { entity ->
            val steps = dao.getSteps(entity.id)
            entity.toDomain(steps)
        }
    }

    override suspend fun searchSteps(query: String, language: String): List<SurvivalContent> {
        return dao.searchContent(query, language).map { entity ->
            val steps = dao.getSteps(entity.id)
            entity.toDomain(steps)
        }
    }

    private fun SurvivalContentDbEntity.toDomain(steps: List<SurvivalStepDbEntity>): SurvivalContent {
        val category = SurvivalCategory.entries.firstOrNull { it.name == this.category }
            ?: SurvivalCategory.WATER
        return SurvivalContent(
            category = category,
            language = language,
            title = title,
            summary = summary,
            steps = steps.map { it.toDomain() }
        )
    }

    private fun SurvivalStepDbEntity.toDomain() = SurvivalStep(
        index = stepIndex,
        title = title,
        description = description,
        warningNote = warningNote,
        svgDiagram = svgDiagram,
        ttsText = ttsText
    )
}
