package com.july.offline.domain.model

data class ValidationQuestion(
    val id: String,
    val textEs: String,
    val textEn: String
)

enum class ValidationAnswer { YES, NO, SKIP }
