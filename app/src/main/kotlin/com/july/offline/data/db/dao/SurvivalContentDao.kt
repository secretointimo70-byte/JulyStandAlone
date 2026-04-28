package com.july.offline.data.db.dao

import androidx.room.*
import com.july.offline.data.db.entity.SurvivalContentDbEntity
import com.july.offline.data.db.entity.SurvivalStepDbEntity

@Dao
interface SurvivalContentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContent(content: SurvivalContentDbEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<SurvivalStepDbEntity>)

    @Query("SELECT * FROM survival_content WHERE category = :category AND language = :language LIMIT 1")
    suspend fun getContent(category: String, language: String): SurvivalContentDbEntity?

    @Query("SELECT * FROM survival_steps WHERE contentId = :contentId ORDER BY stepIndex ASC")
    suspend fun getSteps(contentId: String): List<SurvivalStepDbEntity>

    @Query("SELECT * FROM survival_content WHERE language = :language")
    suspend fun listAll(language: String): List<SurvivalContentDbEntity>

    @Query("""
        SELECT DISTINCT sc.* FROM survival_content sc
        INNER JOIN survival_steps ss ON sc.id = ss.contentId
        WHERE sc.language = :language
        AND (ss.title LIKE '%' || :query || '%' OR ss.description LIKE '%' || :query || '%')
    """)
    suspend fun searchContent(query: String, language: String): List<SurvivalContentDbEntity>

    @Query("SELECT COUNT(*) FROM survival_content WHERE language = 'es'")
    suspend fun countSeeded(): Int
}
