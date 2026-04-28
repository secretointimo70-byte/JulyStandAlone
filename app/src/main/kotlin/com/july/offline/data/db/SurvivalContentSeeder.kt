package com.july.offline.data.db

import com.july.offline.data.db.dao.SurvivalContentDao
import com.july.offline.data.db.entity.SurvivalContentDbEntity
import com.july.offline.data.db.entity.SurvivalStepDbEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SurvivalContentSeeder @Inject constructor(private val dao: SurvivalContentDao) {

    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.countSeeded() > 0) return@withContext
        seedWater(); seedFire(); seedFood(); seedShelter(); seedFirstAid(); seedSecurity()
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private data class RawStep(
        val index: Int, val title: String, val description: String,
        val warning: String? = null, val svg: String? = null, val tts: String? = null
    )

    private suspend fun insert(category: String, lang: String, title: String, summary: String, rawSteps: List<RawStep>) {
        val id = "${category}_${lang}"
        dao.insertContent(SurvivalContentDbEntity(id = id, category = category, language = lang,
            title = title, summary = summary, stepCount = rawSteps.size))
        dao.insertSteps(rawSteps.map { s ->
            SurvivalStepDbEntity(id = "${id}_step_${s.index}", contentId = id, stepIndex = s.index,
                title = s.title, description = s.description, warningNote = s.warning,
                svgDiagram = s.svg, ttsText = s.tts ?: s.description)
        })
    }

    private fun s(index: Int, title: String, description: String, warning: String? = null, tts: String? = null) =
        RawStep(index, title, description, warning, null, tts)

    // ── WATER ──────────────────────────────────────────────────────────────

    private suspend fun seedWater() {
        insert("WATER", "es", "Obtención y purificación de agua",
            "Cómo encontrar, recolectar y purificar agua para beber", listOf(
            s(1, "Encuentra fuentes de agua",
                "Sigue el terreno hacia abajo para encontrar arroyos o manantiales. El musgo verde en rocas indica humedad cercana. Los animales convergen hacia el agua al amanecer y anochecer.",
                tts = "Sigue el terreno hacia abajo. El musgo en rocas indica agua cercana."),
            s(2, "Recolecta agua de lluvia",
                "Extiende una lona o tela impermeable en forma de embudo dirigido a un recipiente. El agua de lluvia es generalmente segura sin purificación adicional.",
                tts = "Extiende una tela como embudo para recolectar lluvia en un recipiente."),
            s(3, "Purifica por ebullición",
                "Hierve el agua al menos 1 minuto (3 minutos sobre 2000 metros). La ebullición destruye bacterias, virus y parásitos. Deja enfriar antes de beber.",
                warning = "No omitas la ebullición aunque el agua parezca limpia.",
                tts = "Hierve el agua mínimo 1 minuto para destruir bacterias y parásitos."),
            s(4, "Filtrado con arena y carbón",
                "Sin fuego: llena un envase con capas de grava gruesa, arena fina y carbón vegetal. Vierte el agua y recoge lo que filtra. Elimina partículas pero no todos los patógenos.",
                warning = "El filtrado no reemplaza la ebullición.",
                tts = "Sin fuego, filtra con capas de grava, arena y carbón vegetal."),
            s(5, "Destilación solar",
                "Cava un hoyo de 1 metro. Coloca vegetación verde dentro. Cubre con plástico transparente con una piedra en el centro sobre un recipiente. El sol evapora la humedad que condensa y gotea.",
                tts = "Hoyo con vegetación cubierto con plástico: la humedad condensa y gotea al recipiente."),
            s(6, "Señales de agua en el entorno",
                "Insectos volando en espiral suelen estar sobre agua. Vegetación más verde y densa, concavidades en rocas y raíces de árboles apuntando hacia abajo también son indicadores.",
                tts = "Insectos en espiral, vegetación más verde y huecos en rocas indican agua cercana.")
        ))
        insert("WATER", "en", "Water sourcing and purification",
            "How to find, collect and make water safe to drink", listOf(
            s(1, "Find water sources", "Follow terrain downhill to find streams. Green moss on rocks signals moisture. Animal trails converge at water at dawn and dusk."),
            s(2, "Collect rainwater", "Spread a tarp as a funnel into a container. Rainwater is generally safe without purification."),
            s(3, "Purify by boiling", "Boil water at least 1 minute (3 min above 6500 ft). Kills bacteria, viruses and parasites.", warning = "Don't skip boiling even if water looks clean."),
            s(4, "Sand and charcoal filter", "Layer gravel, sand and charcoal in a container. Pour water through. Removes particles but not all pathogens.", warning = "Filtering does not replace boiling."),
            s(5, "Solar still", "Dig a 3-foot hole, add green vegetation, cover with clear plastic weighted in the center over a container."),
            s(6, "Environmental water signs", "Spiraling insects, denser greener vegetation and rock hollows indicate nearby water.")
        ))
    }

    // ── FIRE ───────────────────────────────────────────────────────────────

    private suspend fun seedFire() {
        insert("FIRE", "es", "Encender y mantener fuego",
            "Técnicas de encendido sin encendedor ni fósforos", listOf(
            s(1, "Recolecta materiales combustibles",
                "Tres tipos: yesca (fibras secas, corteza molida), leña pequeña (ramas de lápiz) y madera gruesa. Todos deben estar completamente secos.",
                tts = "Recolecta yesca fina, ramas pequeñas y madera gruesa, todos completamente secos."),
            s(2, "Construye el arco de fuego",
                "Rama curvada como arco con cuerda tensa. Husillo: vara recta y seca. Tablilla con muesca donde el husillo genera fricción y brasas.",
                tts = "Arco con cuerda, husillo seco y tablilla con muesca para generar brasas por fricción."),
            s(3, "Genera brasas por fricción",
                "Presiona el husillo en la muesca y frota rápido con el arco. Las brasas caen en corteza bajo la tablilla. Transfiere al nido de yesca.",
                tts = "Frota rápido. Las brasas caen en la corteza y se transfieren al nido de yesca."),
            s(4, "Enciende el nido de yesca",
                "Envuelve las brasas en el nido formando un pajarito. Sopla suave y continuo hasta obtener llama. Colócalo sobre la leña pequeña.",
                tts = "Envuelve las brasas en yesca y sopla suavemente hasta obtener llama."),
            s(5, "Construye el fuego progresivamente",
                "Alimenta primero con ramas del grosor de un lápiz, luego dedos, luego muñeca. Protege del viento con rocas o terraplén.",
                tts = "Alimenta con ramas de menor a mayor grosor. Protege del viento con rocas."),
            s(6, "Señalización con fuego y humo",
                "Tres fogatas en triángulo equilátero de 30 metros: señal universal de socorro. Hojas verdes dan humo blanco (día), goma o plástico humo negro (noche).",
                tts = "Tres fogatas en triángulo son señal universal de socorro.")
        ))
        insert("FIRE", "en", "Starting and maintaining fire",
            "Fire-starting techniques without lighter or matches", listOf(
            s(1, "Gather combustible materials", "Collect tinder (dry fibers), kindling (pencil-sized sticks) and fuel wood. Everything must be completely dry."),
            s(2, "Build a bow drill", "Curved branch as bow with taut cordage. Straight dry spindle. Fireboard with notch for friction embers."),
            s(3, "Generate embers", "Press spindle into notch and saw quickly with bow. Embers fall onto bark. Transfer to tinder bundle."),
            s(4, "Light the tinder bundle", "Fold embers into tinder bundle. Blow gently and steadily until flame appears."),
            s(5, "Build fire progressively", "Feed pencil-sized sticks first, then finger-sized, then wrist-sized. Shield from wind with rocks."),
            s(6, "Signaling", "Three fires in equilateral triangle is universal distress signal. Green leaves create white smoke; rubber creates black smoke.")
        ))
    }

    // ── FOOD ───────────────────────────────────────────────────────────────

    private suspend fun seedFood() {
        insert("FOOD", "es", "Alimentación en campo",
            "Plantas comestibles, proteínas silvestres y conservación", listOf(
            s(1, "Plantas comestibles universales",
                "Seguras en la mayoría de regiones: diente de león (toda la planta), ortiga cocida, corteza interior de pino, bellotas cocidas. Evita plantas con olor almendrado o látex lechoso.",
                warning = "Nunca comas si no identificas la planta con certeza.",
                tts = "Diente de león, ortiga cocida, corteza interior de pino y bellotas cocidas son seguras."),
            s(2, "Prueba de comestibilidad universal",
                "Frota la planta en muñeca interior 15 min. Sin reacción: frota en labio 5 min. Sin ardor: mastica sin tragar, espera 8 horas. Solo entonces come en pequeña cantidad.",
                warning = "Una sola planta por prueba. Nunca mezcles plantas desconocidas.",
                tts = "Frota en muñeca, luego labio, luego mastica sin tragar. Espera 8 horas."),
            s(3, "Trampas para pequeños animales",
                "Lazo simple: cuerda en bucle sobre rama en Y plantada en sendero animal, atada a árbol. El lazo queda a la altura del cuello del animal. Revisa cada 4-6 horas.",
                tts = "Lazo con cuerda en rama en Y sobre sendero animal. Revisa cada 6 horas."),
            s(4, "Insectos como proteína",
                "Grillos, hormigas y gusanos de madera son nutritivos. Evita insectos de colores vivos o con olor fuerte. Cocinados siempre: asa o hierve 5 minutos.",
                warning = "Evita insectos de colores llamativos: señal de veneno.",
                tts = "Grillos, hormigas y gusanos son nutritivos. Cocínalos siempre al menos 5 minutos."),
            s(5, "Conservación de alimentos",
                "Secado: corta carne en tiras de 3 mm, sécala 2 días al sol o junto al fuego. Ahumado: cuelga sobre el humo 12 horas. Frota con sal si tienes disponible.",
                tts = "Seca carne en tiras al sol 2 días o ahúmala 12 horas sobre el fuego.")
        ))
        insert("FOOD", "en", "Field nutrition",
            "Edible plants, wild proteins and food preservation", listOf(
            s(1, "Universal edible plants", "Safe in most regions: dandelion (whole plant), cooked nettles, pine inner bark, cooked acorns. Avoid almond smell or milky latex.", warning = "Never eat if you can't identify the plant with certainty."),
            s(2, "Universal edibility test", "Rub on inner wrist 15 min. No reaction: rub on lip 5 min. No burning: chew without swallowing, wait 8 hours.", warning = "Test one plant at a time."),
            s(3, "Small animal traps", "Simple snare: rope loop on Y-branch stuck in animal trail, tied to fixed tree. Loop at neck height. Check every 4-6 hours."),
            s(4, "Insects as protein", "Crickets, ants and wood grubs are nutritious. Avoid brightly colored or strong-smelling insects. Always cook: roast or boil 5 minutes.", warning = "Avoid brightly colored insects."),
            s(5, "Food preservation", "Drying: slice meat 3mm thin, dry 2 days in sun or near fire. Smoking: hang over fire smoke 12 hours.")
        ))
    }

    // ── SHELTER ────────────────────────────────────────────────────────────

    private suspend fun seedShelter() {
        insert("SHELTER", "es", "Refugio y orientación",
            "Construcción de refugio y navegación sin GPS", listOf(
            s(1, "Elige el sitio del refugio",
                "Terreno elevado, seco y protegido del viento. Evita cauces de ríos (crecida), árboles muertos (caída) y depresiones (humedad). La pendiente suave drena el agua.",
                tts = "Elige terreno alto, seco y protegido del viento. Evita cauces y zonas bajas."),
            s(2, "Construye un lean-to",
                "Dos postes verticales de 2 metros. Viga horizontal entre ellos. Ramas inclinadas desde la viga al suelo. Capas densas de hojas de abajo hacia arriba como techo.",
                tts = "Dos postes, viga horizontal, ramas inclinadas y hojas densas forman el lean-to."),
            s(3, "Aísla el suelo",
                "15 cm de hojas secas, agujas de pino o pasto seco bajo tu cuerpo. El suelo roba el calor corporal más rápido que el aire frío.",
                tts = "15 centímetros de hojas secas bajo tu cuerpo. El suelo roba más calor que el aire."),
            s(4, "Orientación sin GPS",
                "El sol sale al este y se pone al oeste. Al mediodía la sombra más corta apunta al norte. De noche: la Estrella Polar está siempre al norte.",
                tts = "El sol señala este y oeste. La sombra más corta al mediodía apunta al norte."),
            s(5, "Señalización para rescate",
                "Tres fogatas en triángulo de 30 metros: señal internacional. X en terreno despejado visible desde el aire. Espejo o papel aluminio para reflejar luz solar hacia aviones.",
                tts = "Tres fogatas en triángulo o X en terreno abierto son señales de socorro desde el aire.")
        ))
        insert("SHELTER", "en", "Shelter and navigation",
            "Building shelter and navigating without GPS", listOf(
            s(1, "Choose shelter site", "Pick elevated dry ground sheltered from wind. Avoid riverbeds, dead trees and depressions."),
            s(2, "Build a lean-to", "Two vertical poles, a horizontal beam, branches leaning to ground. Layer leaves from bottom to top."),
            s(3, "Insulate the ground", "Place 15 cm of dry leaves or pine needles under your body. Ground steals heat faster than cold air."),
            s(4, "Navigation without GPS", "Sun rises east, sets west. At noon, shortest shadow points north. At night: Polaris is always north."),
            s(5, "Signaling for rescue", "Three fires in triangle (30m side) is international distress. X on open ground visible from air. Mirror or foil to reflect sunlight.")
        ))
    }

    // ── FIRST AID ──────────────────────────────────────────────────────────

    private suspend fun seedFirstAid() {
        insert("FIRST_AID", "es", "Primeros auxilios de campo",
            "Hemorragias, fracturas, hipotermia y emergencias básicas", listOf(
            s(1, "Controla hemorragias",
                "Presión directa firme con tela limpia durante 10 minutos sin retirar. Eleva la extremidad. No retires la tela: si se empapa, agrega más encima.",
                tts = "Presión directa 10 minutos sin retirar la tela. Eleva la extremidad."),
            s(2, "Torniquete improvisado",
                "Solo en hemorragia severa de extremidad que no cede. Cinta de 5 cm a 5 cm sobre la herida. Aprieta hasta detener el sangrado. Anota la hora.",
                warning = "Solo en hemorragia que amenaza la vida. Nunca en cuello o torso.",
                tts = "Torniquete a 5 centímetros sobre la herida. Aprieta hasta parar el sangrado. Anota la hora."),
            s(3, "Limpieza de heridas",
                "Lava con agua hervida a presión. No uses alcohol en heridas abiertas. Cubre con tela limpia. Cambia el apósito cada 24 horas.",
                tts = "Lava con agua hervida a presión. Cubre con tela limpia. Cambia cada 24 horas."),
            s(4, "Inmovilización de fracturas",
                "Inmoviliza la articulación superior e inferior a la fractura con ramas y vendaje. No intentes alinear el hueso.",
                warning = "No muevas a paciente con posible fractura de columna.",
                tts = "Inmoviliza articulaciones arriba y abajo con ramas y vendaje. No alinear el hueso."),
            s(5, "Hipotermia",
                "Seca y abriga en capas. Calienta el torso primero, no las extremidades. Líquidos calientes y azúcar si está consciente. Sin calor directo intenso.",
                tts = "Seca, abriga y calienta el torso primero. Líquidos calientes y azúcar si consciente."),
            s(6, "Picaduras y venenos",
                "Mantén la zona bajo el nivel del corazón. Inmoviliza la extremidad. No succionas ni cortes. Retira el aguijón raspando, no pellizcando.",
                warning = "Nunca succionas el veneno ni hagas incisiones.",
                tts = "Zona bajo el corazón, inmoviliza, no succionas ni cortes. Raspa el aguijón.")
        ))
        insert("FIRST_AID", "en", "Field first aid",
            "Bleeding, fractures, hypothermia and basic emergencies", listOf(
            s(1, "Control bleeding", "Firm direct pressure with clean cloth for 10 minutes without removing. Elevate limb. Don't remove cloth — add more if soaked."),
            s(2, "Improvised tourniquet", "Only for life-threatening limb bleeding. 5cm-wide band 5cm above wound. Tighten until bleeding stops. Note time.", warning = "Only for life-threatening bleeding. Never on neck or torso."),
            s(3, "Wound cleaning", "Flush with boiled water under pressure. Don't pour alcohol on open wound. Cover with clean cloth. Change every 24 hours."),
            s(4, "Fracture immobilization", "Immobilize joint above and below with sticks and bandage. Don't try to align bone.", warning = "Don't move patient with possible spinal injury."),
            s(5, "Hypothermia", "Dry and layer insulation. Warm torso first, not extremities. Give warm liquids and sugar if conscious."),
            s(6, "Bites and venom", "Keep area below heart level. Immobilize limb. Don't suck or cut. Scrape stinger — don't pinch.", warning = "Never suck venom or make incisions.")
        ))
    }

    // ── SECURITY ───────────────────────────────────────────────────────────

    private suspend fun seedSecurity() {
        insert("SECURITY", "es", "Seguridad y defensa del perímetro",
            "Alertas, señales y protección del campamento", listOf(
            s(1, "Perímetro de alarma",
                "Cordel a 20 cm del suelo alrededor del campamento. Cuelga latas con piedras, campanillas o botellas. Cualquier intrusión hará ruido audible desde dentro.",
                tts = "Cordel a 20 centímetros del suelo con latas o campanillas como alarma perimetral."),
            s(2, "Señal SOS internacional",
                "Morse: tres cortos, tres largos, tres cortos. Con luz o sonido. Repite con 1 minuto de pausa. Señal universal de socorro reconocida globalmente.",
                tts = "SOS en morse: tres cortos, tres largos, tres cortos. Repite cada minuto."),
            s(3, "Señal visual de rescate",
                "SOS con rocas o troncos en campo abierto, visible desde el aire. Letras de mínimo 3 metros. Usa materiales que contrasten con el suelo.",
                tts = "Forma SOS con rocas en campo abierto, letras de 3 metros mínimo."),
            s(4, "Camuflaje del campamento",
                "Evita reflejos metálicos. Humo mínimo con madera seca. No dejes rastros en senderos de entrada. Oculta residuos y restos de comida bajo tierra.",
                tts = "Evita reflejos y humo visible. Oculta todos los rastros de tu presencia."),
            s(5, "Rutas de escape",
                "Dos rutas de salida en direcciones opuestas. Un punto de reunión a 500 metros del campamento. Todos deben conocerlo antes de necesitarlo.",
                tts = "Dos rutas de escape y un punto de reunión a 500 metros. Todos deben conocerlo.")
        ))
        insert("SECURITY", "en", "Security and perimeter defense",
            "Alerts, signals and camp protection", listOf(
            s(1, "Perimeter alarm", "String cordage 20cm off ground around camp. Hang cans with pebbles or bells. Any intrusion will make audible noise."),
            s(2, "International SOS", "Morse: three short, three long, three short. With light or sound. Repeat with 1-minute pause."),
            s(3, "Visual rescue signal", "Spell SOS with rocks in open field, visible from air. Letters at least 3 meters tall."),
            s(4, "Camp camouflage", "Avoid metal reflections. Minimize smoke using dry wood. Don't leave tracks on approach paths. Bury waste."),
            s(5, "Escape routes", "Two exit routes in opposite directions. Rally point 500m away. Everyone must know it before it's needed.")
        ))
    }
}
