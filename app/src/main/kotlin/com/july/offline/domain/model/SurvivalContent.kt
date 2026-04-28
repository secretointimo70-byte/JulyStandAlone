package com.july.offline.domain.model

data class SurvivalContent(
    val category: SurvivalCategory,
    val language: String,
    val title: String,
    val summary: String,
    val steps: List<SurvivalStep>,
    val source: String = "local"
) {
    val stepCount: Int get() = steps.size
    fun stepAt(index: Int): SurvivalStep? = steps.getOrNull(index)
}
