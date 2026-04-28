package com.july.offline.data.db.dao

import androidx.room.*
import com.july.offline.data.db.entity.SessionDbEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionDbEntity)

    @Update
    suspend fun update(session: SessionDbEntity)

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SessionDbEntity?

    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<SessionDbEntity>>

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteById(id: String)
}
