package com.july.offline.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "survival_content")
data class SurvivalContentDbEntity(
    @PrimaryKey val id: String,
    val category: String,
    val language: String,
    val title: String,
    val summary: String,
    val stepCount: Int
)
