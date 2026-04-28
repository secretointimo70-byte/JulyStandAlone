package com.july.offline.data.db.dao

import androidx.room.*
import com.july.offline.data.db.entity.DiagnosticsDbEntity

@Dao
interface DiagnosticsDao {

    @Insert
    suspend fun insert(entry: DiagnosticsDbEntity)

    @Query("DELETE FROM diagnostics WHERE timestamp < :olderThanMs")
    suspend fun deleteOlderThan(olderThanMs: Long)

    @Query("SELECT COUNT(*) FROM diagnostics")
    suspend fun count(): Int
}
