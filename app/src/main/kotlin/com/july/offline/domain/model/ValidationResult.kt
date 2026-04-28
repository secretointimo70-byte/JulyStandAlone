package com.july.offline.domain.model

data class ValidationResult(
    val type: Type,
    val bannerEs: String? = null,
    val bannerEn: String? = null,
    val fallbackActionsEs: List<String> = emptyList(),
    val fallbackActionsEn: List<String> = emptyList()
) {
    enum class Type { POSITIVE, PARTIAL, NEGATIVE, INCOMPLETE }
}
