package com.july.offline.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.july.offline.data.db.dao.DiagnosticsDao
import com.july.offline.data.db.dao.MessageDao
import com.july.offline.data.db.dao.SessionDao
import com.july.offline.data.db.dao.SurvivalContentDao
import com.july.offline.data.db.entity.DiagnosticsDbEntity
import com.july.offline.data.db.entity.MessageDbEntity
import com.july.offline.data.db.entity.SessionDbEntity
import com.july.offline.data.db.entity.SurvivalContentDbEntity
import com.july.offline.data.db.entity.SurvivalStepDbEntity

@Database(
    entities = [
        SessionDbEntity::class,
        MessageDbEntity::class,
        DiagnosticsDbEntity::class,
        SurvivalContentDbEntity::class,
        SurvivalStepDbEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class JulyDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
    abstract fun diagnosticsDao(): DiagnosticsDao
    abstract fun survivalContentDao(): SurvivalContentDao
}
