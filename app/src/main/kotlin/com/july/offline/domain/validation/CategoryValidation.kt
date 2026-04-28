package com.july.offline.domain.validation

import com.july.offline.domain.model.SurvivalCategory
import com.july.offline.domain.model.ValidationAnswer
import com.july.offline.domain.model.ValidationQuestion
import com.july.offline.domain.model.ValidationResult

object CategoryValidation {

    data class Config(
        val questions: List<ValidationQuestion>,
        val evaluate: (answers: Map<String, ValidationAnswer>) -> ValidationResult
    )

    fun configFor(category: SurvivalCategory): Config = when (category) {
        SurvivalCategory.WATER    -> waterConfig()
        SurvivalCategory.FIRE     -> fireConfig()
        SurvivalCategory.FOOD     -> foodConfig()
        SurvivalCategory.SHELTER  -> shelterConfig()
        SurvivalCategory.FIRST_AID -> firstAidConfig()
        SurvivalCategory.SECURITY -> securityConfig()
    }

    // ── AGUA ─────────────────────────────────────────────────────────────────

    private fun waterConfig() = Config(
        questions = listOf(
            ValidationQuestion(
                id = "water_source",
                textEs = "¿Tienes alguna fuente de agua disponible (río, lluvia, nieve)?",
                textEn = "Do you have any water source available (river, rain, snow)?"
            ),
            ValidationQuestion(
                id = "water_tools",
                textEs = "¿Tienes recipientes o medios para tratar el agua?",
                textEn = "Do you have containers or means to treat water?"
            )
        ),
        evaluate = { answers ->
            val hasSource = answers["water_source"] == ValidationAnswer.YES
            val hasTools  = answers["water_tools"]  == ValidationAnswer.YES
            val anySkip   = answers.values.any { it == ValidationAnswer.SKIP }
            when {
                anySkip -> ValidationResult(
                    type     = ValidationResult.Type.INCOMPLETE,
                    bannerEs = "Datos incompletos. Se muestran todos los pasos.",
                    bannerEn = "Incomplete data. All steps shown."
                )
                hasSource && hasTools -> ValidationResult(ValidationResult.Type.POSITIVE)
                hasSource && !hasTools -> ValidationResult(
                    type     = ValidationResult.Type.PARTIAL,
                    bannerEs = "⚠ Sin recipientes: improvisa con hojas grandes, tela o calzado.",
                    bannerEn = "⚠ No containers: improvise with large leaves, cloth, or footwear."
                )
                !hasSource && hasTools -> ValidationResult(
                    type     = ValidationResult.Type.PARTIAL,
                    bannerEs = "⚠ Sin fuente visible. Prioridad: encontrar agua antes de tratarla.",
                    bannerEn = "⚠ No visible source. Priority: find water before treating it."
                )
                else -> ValidationResult(
                    type     = ValidationResult.Type.NEGATIVE,
                    bannerEs = "⛔ Sin agua ni herramientas. Acciones inmediatas:",
                    bannerEn = "⛔ No water or tools. Immediate actions:",
                    fallbackActionsEs = listOf(
                        "Busca rocío en plantas al amanecer o anochecer.",
                        "Excava en lechos de ríos secos — puede haber agua a 30 cm.",
                        "Recoge lluvia con cualquier superficie: tela, hoja grande, tronco hueco.",
                        "Sin agua: deshidratación grave en 3 h en calor. No te muevas innecesariamente.",
                        "Señales de alarma: orina oscura, mareo intenso, labios muy secos."
                    ),
                    fallbackActionsEn = listOf(
                        "Look for dew on plants at dawn or dusk.",
                        "Dig in dry riverbeds — water may be 30 cm down.",
                        "Collect rain with any surface: cloth, large leaf, hollow log.",
                        "Without water: severe dehydration in 3h in heat. Minimize movement.",
                        "Warning signs: dark urine, intense dizziness, very dry lips."
                    )
                )
            }
        }
    )

    // ── FUEGO ────────────────────────────────────────────────────────────────

    private fun fireConfig() = Config(
        questions = listOf(
            ValidationQuestion(
                id = "fire_ignition",
                textEs = "¿Tienes material de ignición (fósforos, encendedor, chispa)?",
                textEn = "Do you have ignition material (matches, lighter, spark)?"
            ),
            ValidationQuestion(
                id = "fire_fuel",
                textEs = "¿Tienes madera seca o combustible disponible?",
                textEn = "Do you have dry wood or fuel available?"
            )
        ),
        evaluate = { answers ->
            val hasIgnition = answers["fire_ignition"] == ValidationAnswer.YES
            val hasFuel     = answers["fire_fuel"]     == ValidationAnswer.YES
            val anySkip     = answers.values.any { it == ValidationAnswer.SKIP }
            when {
                anySkip -> ValidationResult(
                    type     = ValidationResult.Type.INCOMPLETE,
                    bannerEs = "Datos incompletos. Se incluyen técnicas alternativas.",
                    bannerEn = "Incomplete data. Alternative techniques included."
                )
                hasIgnition && hasFuel -> ValidationResult(ValidationResult.Type.POSITIVE)
                hasIgnition && !hasFuel -> ValidationResult(
                    type     = ValidationResult.Type.PARTIAL,
                    bannerEs = "⚠ Sin combustible seco: usa corteza muerta, hierba seca o resina.",
                    bannerEn = "⚠ No dry fuel: use dead bark, dry grass, or resin."
                )
                !hasIgnition && hasFuel -> ValidationResult(
                    type     = ValidationResult.Type.PARTIAL,
                    bannerEs = "⚠ Sin ignición: el protocolo incluye técnicas de fricción y lente solar.",
                    bannerEn = "⚠ No ignition: protocol includes friction and solar lens techniques."
                )
                else -> ValidationResult(
                    type     = ValidationResult.Type.NEGATIVE,
                    bannerEs = "⛔ Sin ignición ni combustible. Acciones inmediatas:",
                    bannerEn = "⛔ No ignition or fuel. Immediate actions:",
                    fallbackActionsEs = listOf(
                        "Usa luz solar + lente (lupa, botella llena, bolsa de agua) para encender yesca.",
                        "Técnica de fricción: arco y taladro con madera seca blanda (sauce, cedro).",
                        "Sin fuego: prioriza refugio y calor corporal sobre todo lo demás.",
                        "Agrupa ropa seca, hojas y hierba alrededor del cuerpo como aislante.",
                        "Evita el suelo frío — el contacto directo drena calor más rápido que el viento."
                    ),
                    fallbackActionsEn = listOf(
                        "Use sunlight + lens (magnifier, full bottle, water bag) to ignite tinder.",
                        "Friction technique: bow drill with dry soft wood (willow, cedar).",
                        "Without fire: prioritize shelter and body heat above everything else.",
                        "Layer dry clothes, leaves, and grass around your body as insulation.",
                        "Avoid cold ground — direct contact drains heat faster than wind."
                    )
                )
            }
        }
    )

    // ── ALIMENTACIÓN ─────────────────────────────────────────────────────────

    private fun foodConfig() = Config(
        questions = listOf(
            ValidationQuestion(
                id = "food_plants",
                textEs = "¿Hay vegetación o plantas comestibles en tu entorno?",
                textEn = "Is there vegetation or edible plants in your environment?"
            ),
            ValidationQuestion(
                id = "food_tools",
                textEs = "¿Tienes herramientas para cazar, pescar o recolectar?",
                textEn = "Do you have tools for hunting, fishing, or gathering?"
            )
        ),
        evaluate = { answers ->
            val hasPlants = answers["food_plants"] == ValidationAnswer.YES
            val hasTools  = answers["food_tools"]  == ValidationAnswer.YES
            val anySkip   = answers.values.any { it == ValidationAnswer.SKIP }
            when {
                anySkip -> ValidationResult(
                    type     = ValidationResult.Type.INCOMPLETE,
                    bannerEs = "Sin datos del entorno. Se muestran técnicas generales.",
                    bannerEn = "No environment data. General techniques shown."
                )
                hasPlants && hasTools -> ValidationResult(ValidationResult.Type.POSITIVE)
                hasPlants && !hasTools -> ValidationResult(
                    type     = ValidationResult.Type.PARTIAL,
                    bannerEs = "⚠ Sin herramientas: recolección manual. Verifica plantas antes de comer.",
                    bannerEn = "⚠ No tools: manual gathering. Verify plants before eating."
                )
                !hasPlants && hasTools -> ValidationResult(
                    type     = ValidationResult.Type.PARTIAL,
                    bannerEs = "⚠ Sin vegetación visible: prioriza caza o pesca con tus herramientas.",
                    bannerEn = "⚠ No visible vegetation: prioritize hunting or fishing with your tools."
                )
                else -> ValidationResult(
                    type     = ValidationResult.Type.NEGATIVE,
                    bannerEs = "⛔ Sin comida ni herramientas. Contexto crítico:",
                    bannerEn = "⛔ No food or tools. Critical context:",
                    fallbackActionsEs = listOf(
                        "El cuerpo aguanta hasta 3 semanas sin comida. Prioriza AGUA y REFUGIO primero.",
                        "Insectos: grillos, orugas sin pelos, gusanos blancos son proteína de emergencia.",
                        "Caza improvisada: trampa lazo con cordón o cable en caminos de animales.",
                        "Evita plantas desconocidas — no arriesgues un envenenamiento por hambre.",
                        "Reduce actividad física para conservar energía al máximo."
                    ),
                    fallbackActionsEn = listOf(
                        "Body can last up to 3 weeks without food. Prioritize WATER and SHELTER first.",
                        "Insects: crickets, hairless caterpillars, white grubs are emergency protein.",
                        "Improvised trap: snare with cord or wire on animal paths.",
                        "Avoid unknown plants — don't risk poisoning from hunger.",
                        "Reduce physical activity to conserve maximum energy."
                    )
                )
            }
        }
    )

    // ── REFUGIO ───────────────────────────────────────────────────────────────

    private fun shelterConfig() = Config(
        questions = listOf(
            ValidationQuestion(
                id = "shelter_materials",
                textEs = "¿Tienes materiales disponibles (ramas, hojas, lona, tela)?",
                textEn = "Do you have materials available (branches, leaves, tarp, fabric)?"
            ),
            ValidationQuestion(
                id = "shelter_location",
                textEs = "¿Tienes una ubicación protegida del viento y la lluvia?",
                textEn = "Do you have a location protected from wind and rain?"
            )
        ),
        evaluate = { answers ->
            val hasMaterials = answers["shelter_materials"] == ValidationAnswer.YES
            val hasLocation  = answers["shelter_location"]  == ValidationAnswer.YES
            val anySkip      = answers.values.any { it == ValidationAnswer.SKIP }
            when {
                anySkip -> ValidationResult(
                    type     = ValidationResult.Type.INCOMPLETE,
                    bannerEs = "Sin datos. Se muestran los pasos básicos de refugio.",
                    bannerEn = "No data. Basic shelter steps shown."
                )
                hasMaterials && hasLocation -> ValidationResult(ValidationResult.Type.POSITIVE)
                hasMaterials && !hasLocation -> ValidationResult(
                    type     = ValidationResult.Type.PARTIAL,
                    bannerEs = "⚠ Sin ubicación ideal: elige terreno elevado, alejado de agua y viento.",
                    bannerEn = "⚠ No ideal location: choose elevated ground, away from water and wind."
                )
                !hasMaterials && hasLocation -> ValidationResult(
                    type     = ValidationResult.Type.PARTIAL,
                    bannerEs = "⚠ Sin materiales: usa cuerpo como aislante, excava hoyo si es posible.",
                    bannerEn = "⚠ No materials: use body as insulation, dig a hole if possible."
                )
                else -> ValidationResult(
                    type     = ValidationResult.Type.NEGATIVE,
                    bannerEs = "⛔ Sin materiales ni ubicación. Acciones inmediatas:",
                    bannerEn = "⛔ No materials or location. Immediate actions:",
                    fallbackActionsEs = listOf(
                        "Busca refugio natural inmediato: roca saliente, cueva, depresión del terreno.",
                        "La hipotermia mata en horas. Prioridad absoluta: salir del viento y la lluvia.",
                        "Si hay sol: aprovechar ahora para construir antes de que anochezca.",
                        "Aísla el suelo primero — el frío del suelo es más peligroso que el del aire.",
                        "Agrupa hojas secas, hierba o musgo alrededor del cuerpo si no hay otra opción."
                    ),
                    fallbackActionsEn = listOf(
                        "Find immediate natural shelter: rock overhang, cave, terrain depression.",
                        "Hypothermia kills in hours. Absolute priority: get out of wind and rain.",
                        "If sunny: use the light now to build before nightfall.",
                        "Insulate the ground first — ground cold is more dangerous than air cold.",
                        "Pile dry leaves, grass, or moss around your body if no other option."
                    )
                )
            }
        }
    )

    // ── PRIMEROS AUXILIOS ─────────────────────────────────────────────────────

    private fun firstAidConfig() = Config(
        questions = listOf(
            ValidationQuestion(
                id = "aid_conscious",
                textEs = "¿La persona afectada está consciente y respira?",
                textEn = "Is the affected person conscious and breathing?"
            ),
            ValidationQuestion(
                id = "aid_kit",
                textEs = "¿Tienes botiquín o materiales médicos básicos?",
                textEn = "Do you have a first aid kit or basic medical materials?"
            )
        ),
        evaluate = { answers ->
            val isConscious = answers["aid_conscious"] == ValidationAnswer.YES
            val notConscious = answers["aid_conscious"] == ValidationAnswer.NO
            val hasKit  = answers["aid_kit"] == ValidationAnswer.YES
            val anySkip = answers.values.any { it == ValidationAnswer.SKIP }
            when {
                notConscious -> ValidationResult(
                    type     = ValidationResult.Type.NEGATIVE,
                    bannerEs = "🚨 PERSONA INCONSCIENTE — RCP INMEDIATO:",
                    bannerEn = "🚨 UNCONSCIOUS PERSON — IMMEDIATE CPR:",
                    fallbackActionsEs = listOf(
                        "Verifica pulso carotídeo (cuello) y respiración durante 10 segundos.",
                        "Sin pulso: inicia RCP. 30 compresiones al centro del pecho, fuerte y rápido.",
                        "2 respiraciones de rescate cada 30 compresiones (si tienes entrenamiento).",
                        "Continúa sin parar hasta que respire, llegue ayuda o no puedas más.",
                        "NO muevas a la persona si sospechas trauma de cuello o columna."
                    ),
                    fallbackActionsEn = listOf(
                        "Check carotid pulse (neck) and breathing for 10 seconds.",
                        "No pulse: start CPR. 30 compressions at center of chest, hard and fast.",
                        "2 rescue breaths every 30 compressions (if trained).",
                        "Continue without stopping until breathing resumes, help arrives, or you can't.",
                        "Do NOT move the person if you suspect neck or spine trauma."
                    )
                )
                anySkip -> ValidationResult(
                    type     = ValidationResult.Type.INCOMPLETE,
                    bannerEs = "⚠ Aplica el protocolo de evaluación primaria primero (ABC).",
                    bannerEn = "⚠ Apply primary assessment protocol first (ABC)."
                )
                isConscious && hasKit  -> ValidationResult(ValidationResult.Type.POSITIVE)
                isConscious && !hasKit -> ValidationResult(
                    type     = ValidationResult.Type.PARTIAL,
                    bannerEs = "⚠ Sin botiquín: usa telas limpias e improvisa vendajes con lo disponible.",
                    bannerEn = "⚠ No kit: use clean cloths and improvise bandages with available materials."
                )
                else -> ValidationResult(
                    type     = ValidationResult.Type.INCOMPLETE,
                    bannerEs = "⚠ Evalúa estado de consciencia antes de continuar.",
                    bannerEn = "⚠ Assess consciousness state before proceeding."
                )
            }
        }
    )

    // ── SEGURIDAD ─────────────────────────────────────────────────────────────

    private fun securityConfig() = Config(
        questions = listOf(
            ValidationQuestion(
                id = "sec_threat",
                textEs = "¿Hay una amenaza activa o inminente en este momento?",
                textEn = "Is there an active or imminent threat right now?"
            ),
            ValidationQuestion(
                id = "sec_group",
                textEs = "¿Estás con otras personas o estás solo/a?",
                textEn = "Are you with other people or are you alone?"
            )
        ),
        evaluate = { answers ->
            val activeThreat = answers["sec_threat"] == ValidationAnswer.YES
            val hasGroup     = answers["sec_group"]  == ValidationAnswer.YES
            val anySkip      = answers.values.any { it == ValidationAnswer.SKIP }
            when {
                anySkip -> ValidationResult(
                    type     = ValidationResult.Type.INCOMPLETE,
                    bannerEs = "⚠ Evalúa tu entorno completamente antes de actuar.",
                    bannerEn = "⚠ Fully assess your surroundings before acting."
                )
                activeThreat && !hasGroup -> ValidationResult(
                    type     = ValidationResult.Type.NEGATIVE,
                    bannerEs = "🚨 AMENAZA ACTIVA — ESTÁS SOLO/A. Acciones inmediatas:",
                    bannerEn = "🚨 ACTIVE THREAT — YOU ARE ALONE. Immediate actions:",
                    fallbackActionsEs = listOf(
                        "HUIR es la prioridad absoluta. No confrontes bajo ninguna circunstancia.",
                        "Si no puedes huir: escóndete y mantén silencio total.",
                        "Cierra y atrancar puertas. Apaga luces. Silencia el teléfono.",
                        "Último recurso si amenaza directa de vida: distrae, desorienta, actúa rápido.",
                        "Memoriza y reporta: descripción física, dirección, hora, vehículo si aplica."
                    ),
                    fallbackActionsEn = listOf(
                        "FLEE is the absolute priority. Do not confront under any circumstance.",
                        "If you can't flee: hide and maintain complete silence.",
                        "Close and barricade doors. Turn off lights. Silence your phone.",
                        "Last resort if direct life threat: distract, disorient, act fast.",
                        "Memorize and report: physical description, direction, time, vehicle if applicable."
                    )
                )
                activeThreat && hasGroup -> ValidationResult(
                    type     = ValidationResult.Type.PARTIAL,
                    bannerEs = "⚠ Amenaza activa con grupo: coordinen. Una persona vigila, el resto se mueve.",
                    bannerEn = "⚠ Active threat with group: coordinate. One person watches, rest moves."
                )
                else -> ValidationResult(ValidationResult.Type.POSITIVE)
            }
        }
    )
}
