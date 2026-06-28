/**
 * core-outdoor / gate / TripGate.kt
 *
 * Trip Gate + Complexity Classifier (R2). Motor determinístico, sin Compose/Android/red.
 *
 * Principios (ADR §10, GATE-*):
 *  - Reglas EXPLÍCITAS y composables, no un score opaco. Cada resultado trae su factor.
 *  - La complejidad surge de ACUMULACIÓN de factores, no de un umbral único.
 *  - Superar 2.000 m NO activa "alta montaña" por sí solo.
 *  - El gate ADVIERTE o BLOQUEA, pero NUNCA afirma que una salida es "segura".
 *  - Blockers configurables y versionados. Misma entrada ⇒ mismo resultado.
 */
package eco.humanos.android.core.outdoor.gate

import eco.humanos.android.core.outdoor.domain.OutdoorOuting
import eco.humanos.android.core.outdoor.domain.OutingPackingItem
import eco.humanos.android.core.outdoor.domain.PackingCategory
import eco.humanos.android.core.outdoor.domain.ScenarioKind
import kotlinx.serialization.Serializable

const val TRIP_GATE_VERSION = "tripgate-cl.v0.1.0"

@Serializable
enum class ComplexityClass { SIMPLE, MODERATE, ADVANCED, HIGH_MOUNTAIN }

/** verde / amarillo / rojo / gris (gris = información insuficiente). */
@Serializable
enum class ReadinessState { GREEN, YELLOW, RED, GRAY }

@Serializable
enum class FindingSeverity { INFO, WARNING, BLOCKER }

enum class IsolationLevel { LOW, MODERATE, HIGH }
enum class GroupExperience { NOVICE, INTERMEDIATE, EXPERIENCED, UNKNOWN }
enum class CommsLevel { NONE, INTERMITTENT, RELIABLE, UNKNOWN }

data class TripConditions(
    val cold: Boolean = false,
    val rain: Boolean = false,
    val wind: Boolean = false,
    val snow: Boolean = false,
    /** Altitud máxima estimada en metros; null = desconocida. */
    val altitudeMeters: Int? = null,
    val isolation: IsolationLevel = IsolationLevel.LOW,
)

/** Contexto R2 que no está en PackingInput (clima, experiencia, comms, tiempos). */
data class TripContext(
    val conditions: TripConditions = TripConditions(),
    val experience: GroupExperience = GroupExperience.UNKNOWN,
    /** Antigüedad de los datos meteorológicos en horas; null = sin datos. */
    val weatherDataAgeHours: Int? = null,
    /** Batería esperada al inicio (0..100); null = desconocida. */
    val batteryPercentExpected: Int? = null,
    val comms: CommsLevel = CommsLevel.UNKNOWN,
    /** ISO8601; null = no definido. */
    val startTime: String? = null,
    val hardTurnaround: String? = null,
)

/** Umbrales y requisitos configurables (versionados por TRIP_GATE_VERSION). */
data class TripGateConfig(
    val staleWeatherHours: Int = 24,
    val lowBatteryPercent: Int = 30,
    val tooManyWarnings: Int = 4,
    /** Altitud (m) que, combinada con nieve/aislamiento, fuerza alta montaña. */
    val highMountainAltitude: Int = 2500,
)

val DEFAULT_TRIP_GATE_CONFIG = TripGateConfig()

@Serializable
data class Finding(
    val code: String,
    val severity: FindingSeverity,
    val title: String,
    val detail: String,
    /** Factor/origen que disparó el hallazgo (trazabilidad). */
    val factor: String,
    val recommendation: String? = null,
)

@Serializable
data class TripGateResult(
    val rulesetVersion: String,
    val complexityClass: ComplexityClass,
    val complexityFactors: List<String>,
    val readinessState: ReadinessState,
    val findings: List<Finding>,
    val blockingFindings: List<Finding>,
    val warnings: List<Finding>,
    val recommendations: List<String>,
    val pendingRequirements: List<String>,
)

object TripGate {

    private val ORDER = listOf(
        ComplexityClass.SIMPLE, ComplexityClass.MODERATE,
        ComplexityClass.ADVANCED, ComplexityClass.HIGH_MOUNTAIN,
    )

    private fun classOf(ordinal: Int): ComplexityClass =
        ORDER[ordinal.coerceIn(0, ORDER.size - 1)]

    private fun baseOrdinal(scenario: ScenarioKind): Int = when (scenario) {
        ScenarioKind.CAMPING, ScenarioKind.DAY_TRIP, ScenarioKind.MINORS_GROUP, ScenarioKind.FIELDWORK -> 0
        ScenarioKind.TREKKING, ScenarioKind.WATER -> 1
        ScenarioKind.SNOW_MOUNTAIN -> 2
    }

    data class Complexity(val klass: ComplexityClass, val factors: List<String>)

    /** Clasificación por ACUMULACIÓN explícita de factores. */
    fun classifyComplexity(
        outing: OutdoorOuting,
        ctx: TripContext,
        secondary: Set<ScenarioKind> = emptySet(),
        config: TripGateConfig = DEFAULT_TRIP_GATE_CONFIG,
    ): Complexity {
        val input = outing.input
        val c = ctx.conditions
        val factors = mutableListOf<String>()

        // Base = el escenario más exigente entre primario y secundarios (composición).
        var ordinal = (secondary + input.scenario).maxOf { baseOrdinal(it) }
        factors += if (secondary.isEmpty()) {
            "escenario base: ${input.scenario}"
        } else {
            "escenario base (primario+anexos): ${input.scenario}+$secondary"
        }

        val minors = input.participants.any { it.ageClass.name != "ADULT" }

        if (input.nights >= 5) { ordinal++; factors += "duración prolongada (>=5 noches)" }
        if (c.isolation == IsolationLevel.HIGH) { ordinal++; factors += "aislamiento alto" }
        if (minors && (c.cold || c.snow || c.isolation != IsolationLevel.LOW)) {
            ordinal++; factors += "menores + condiciones exigentes (frío/nieve/aislamiento)"
        }
        if (c.cold && c.rain && c.wind) { ordinal++; factors += "frío + lluvia + viento combinados" }
        if (c.snow) { ordinal = maxOf(ordinal, 2); factors += "nieve presente" }

        // Altitud: sola NO activa alta montaña; combinada con nieve/aislamiento sí.
        val alt = c.altitudeMeters
        if (alt != null) {
            if (alt >= config.highMountainAltitude && (c.snow || c.isolation == IsolationLevel.HIGH)) {
                ordinal = maxOf(ordinal, 3)
                factors += "altitud >=${config.highMountainAltitude} m + nieve/aislamiento → alta montaña"
            } else if (alt >= 2000) {
                factors += "altitud sobre 2.000 m (no activa alta montaña por sí sola)"
            }
        }

        return Complexity(classOf(ordinal), factors)
    }

    private fun presentCapabilities(items: List<OutingPackingItem>): Set<String> {
        val active = items.filter { !it.removed && it.quantity > 0 }
        val caps = mutableSetOf<String>()
        if (active.any { it.category == PackingCategory.SHELTER }) caps += "shelter"
        if (active.any { it.category == PackingCategory.WATER }) caps += "water"
        if (active.any { it.key == "safety.first_aid" }) caps += "first_aid"
        if (active.any { it.category == PackingCategory.LIGHTING }) caps += "lighting"
        if (active.any { it.key == "trekking.navigation" }) caps += "navigation"
        if (active.any { it.key == "winter.insulation" || it.key == "clothing.warm_layer" }) caps += "warm_insulation"
        return caps
    }

    private fun requiredCapabilities(
        klass: ComplexityClass,
        outing: OutdoorOuting,
        secondary: Set<ScenarioKind>,
    ): Set<String> {
        val req = sortedSetOf("water", "first_aid")
        if (outing.input.nights > 0) { req += "shelter"; req += "lighting" }
        val ordinal = ORDER.indexOf(klass)
        if (ordinal >= ORDER.indexOf(ComplexityClass.ADVANCED) || secondary.contains(ScenarioKind.TREKKING)) {
            req += "navigation"
        }
        if (klass == ComplexityClass.HIGH_MOUNTAIN) req += "warm_insulation"
        return req
    }

    fun evaluate(
        outing: OutdoorOuting,
        items: List<OutingPackingItem>,
        ctx: TripContext = TripContext(),
        secondary: Set<ScenarioKind> = emptySet(),
        config: TripGateConfig = DEFAULT_TRIP_GATE_CONFIG,
    ): TripGateResult {
        val complexity = classifyComplexity(outing, ctx, secondary, config)
        val caps = presentCapabilities(items)
        val required = requiredCapabilities(complexity.klass, outing, secondary)
        val findings = mutableListOf<Finding>()
        val pending = mutableListOf<String>()

        // Blockers: equipo crítico requerido ausente.
        for (missing in required - caps) {
            findings += Finding(
                code = "gate.missing.$missing",
                severity = FindingSeverity.BLOCKER,
                title = "Falta equipo crítico: $missing",
                detail = "El nivel ${complexity.klass} requiere '$missing' y no está en la lista activa.",
                factor = "required_for=${complexity.klass}; missing=$missing",
                recommendation = "Agregar '$missing' a la lista antes de salir.",
            )
            pending += "equipo:$missing"
        }

        // Coherencia de tiempos.
        val start = ctx.startTime
        val turn = ctx.hardTurnaround
        if (start != null && turn != null && turn <= start) {
            findings += Finding(
                code = "gate.time.incoherent",
                severity = FindingSeverity.BLOCKER,
                title = "Hora de retorno incoherente",
                detail = "La hora dura de retorno ($turn) no es posterior al inicio ($start).",
                factor = "start=$start; turnaround=$turn",
                recommendation = "Corregir la hora de retorno.",
            )
        }

        // Warnings.
        val ordinal = ORDER.indexOf(complexity.klass)
        val needsWeather = ordinal >= ORDER.indexOf(ComplexityClass.MODERATE)
        when {
            ctx.weatherDataAgeHours == null && needsWeather ->
                findings += warn("gate.weather.none", "Sin datos meteorológicos", "No hay pronóstico cargado para una salida ${complexity.klass}.", "weather=none", "Cargar pronóstico antes de salir.").also { pending += "dato:meteorología" }
            ctx.weatherDataAgeHours != null && ctx.weatherDataAgeHours > config.staleWeatherHours ->
                findings += warn("gate.weather.stale", "Datos meteorológicos vencidos", "El pronóstico tiene ${ctx.weatherDataAgeHours} h (> ${config.staleWeatherHours} h).", "weather_age=${ctx.weatherDataAgeHours}", "Actualizar el pronóstico.")
        }
        ctx.batteryPercentExpected?.let { b ->
            if (b < config.lowBatteryPercent) {
                findings += warn("gate.battery.low", "Batería esperada baja", "Batería prevista $b% (< ${config.lowBatteryPercent}%).", "battery=$b", "Cargar dispositivos / llevar batería externa.")
            }
        }
        if (ctx.comms == CommsLevel.NONE && ctx.conditions.isolation == IsolationLevel.HIGH) {
            findings += warn("gate.comms.none", "Sin comunicaciones en zona aislada", "Aislamiento alto sin comunicaciones previstas.", "comms=none; isolation=high", "Definir plan de contacto / guardián externo.")
        }
        if (ctx.experience == GroupExperience.NOVICE && ordinal >= ORDER.indexOf(ComplexityClass.ADVANCED)) {
            findings += warn("gate.experience.gap", "Experiencia del grupo vs complejidad", "Grupo novato para una salida ${complexity.klass}.", "experience=novice; class=${complexity.klass}", "Considerar guía, ruta más simple o margen extra.")
        }

        // Acumulación de advertencias.
        val warnings0 = findings.filter { it.severity == FindingSeverity.WARNING }
        if (warnings0.size >= config.tooManyWarnings) {
            findings += warn("gate.too_many_warnings", "Acumulación de advertencias", "${warnings0.size} advertencias simultáneas: reconsiderar el plan.", "warning_count=${warnings0.size}", "Reevaluar alcance, fecha o equipo.")
        }

        // Info insuficiente.
        if (outing.input.participants.isEmpty()) {
            findings += Finding("gate.info.no_participants", FindingSeverity.INFO, "Sin participantes", "No hay participantes para evaluar el grupo.", "participants=0", "Agregar participantes.")
            pending += "dato:participantes"
        }
        if (start == null) pending += "dato:hora_inicio"
        if (turn == null) pending += "dato:hora_retorno"

        val blockers = findings.filter { it.severity == FindingSeverity.BLOCKER }
        val warnings = findings.filter { it.severity == FindingSeverity.WARNING }

        // Estado: rojo si hay blocker; gris si falta info crítica; amarillo si hay warnings; verde si nada.
        val insufficientInfo = outing.input.participants.isEmpty() ||
            (ordinal >= ORDER.indexOf(ComplexityClass.ADVANCED) && ctx.weatherDataAgeHours == null && ctx.experience == GroupExperience.UNKNOWN)
        val state = when {
            blockers.isNotEmpty() -> ReadinessState.RED
            insufficientInfo -> ReadinessState.GRAY
            warnings.isNotEmpty() -> ReadinessState.YELLOW
            else -> ReadinessState.GREEN
        }

        val recommendations = findings.mapNotNull { it.recommendation }.distinct()

        return TripGateResult(
            rulesetVersion = TRIP_GATE_VERSION,
            complexityClass = complexity.klass,
            complexityFactors = complexity.factors,
            readinessState = state,
            findings = findings.sortedWith(compareByDescending<Finding> { sev(it.severity) }.thenBy { it.code }),
            blockingFindings = blockers.sortedBy { it.code },
            warnings = warnings.sortedBy { it.code },
            recommendations = recommendations,
            pendingRequirements = pending.distinct().sorted(),
        )
    }

    private fun warn(code: String, title: String, detail: String, factor: String, reco: String) =
        Finding(code, FindingSeverity.WARNING, title, detail, factor, reco)

    private fun sev(s: FindingSeverity) = when (s) {
        FindingSeverity.BLOCKER -> 3; FindingSeverity.WARNING -> 2; FindingSeverity.INFO -> 1
    }
}
