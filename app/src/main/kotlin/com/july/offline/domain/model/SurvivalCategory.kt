package com.july.offline.domain.model

enum class SurvivalCategory(
    val displayNameEs: String,
    val displayNameEn: String
) {
    WATER("Agua", "Water"),
    FIRE("Fuego", "Fire"),
    FOOD("Alimentación", "Food"),
    SHELTER("Refugio", "Shelter"),
    FIRST_AID("Primeros auxilios", "First aid"),
    SECURITY("Seguridad", "Security"),
    MENTAL_RESILIENCE("Fortaleza mental", "Mental resilience")
}
