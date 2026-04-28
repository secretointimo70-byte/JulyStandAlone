package com.july.offline.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "survival_steps",
    foreignKeys = [ForeignKey(
        entity = SurvivalContentDbEntity::class,
        parentColumns = ["id"],
        childColumns = ["contentId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("contentId")]
)
data class SurvivalStepDbEntity(
    @PrimaryKey val id: String,
    val contentId: String,
    val stepIndex: Int,
    val title: String,
    val description: String,
    val warningNote: String?,
    val svgDiagram: String?,
    val ttsText: String
)
