package com.july.offline.domain.model

data class SurvivalStep(
    val index: Int,
    val title: String,
    val description: String,
    val warningNote: String? = null,
    val svgDiagram: String? = null,
    val ttsText: String = description
)
