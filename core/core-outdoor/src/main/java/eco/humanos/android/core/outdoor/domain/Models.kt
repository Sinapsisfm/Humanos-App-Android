/**
 * core-outdoor / domain / Models.kt
 *
 * Modelo de dominio Outdoor (R1: camping familiar offline), portado del núcleo de
 * referencia TypeScript. Kotlin/JVM puro, SIN dependencias de Android.
 *
 * Invariantes: trazabilidad (sourceRuleId), privacidad (edad por clase, no fecha de
 * nacimiento; restricciones marcadas sensibles), determinismo (estructuras de datos,
 * sin reloj ni azar). Sin contenido clínico.
 */
package eco.humanos.android.core.outdoor.domain

import kotlinx.serialization.Serializable

@Serializable
enum class ScenarioKind { CAMPING, DAY_TRIP, TREKKING, SNOW_MOUNTAIN, WATER, MINORS_GROUP, FIELDWORK }

@Serializable
enum class Intent {
    CAMPING_VACACIONES, PASEO_DIA, TREKKING, NIEVE_MONTANA, ACTIVIDAD_AGUA,
    GRUPO_MENORES, TRABAJO_TERRENO, APRENDER, AYUDA_AHORA,
}

@Serializable
enum class OutingStatus { DRAFT, PREPARED, ACTIVE, PAUSED, INCIDENT, COMPLETED, CANCELLED, ARCHIVED }

@Serializable
enum class Season { VERANO, OTONO, INVIERNO, PRIMAVERA }

@Serializable
enum class AgeClass { INFANT, CHILD, TEEN, ADULT }

@Serializable
enum class ParticipantRole { LEAD, ADULT, MINOR, GUIDE, DRIVER, GUARDIAN_EXTERNAL, OBSERVER }

@Serializable
enum class Accommodation { TENT, CABIN, CAMPER, MIXED }

@Serializable
enum class AccessMode { VEHICLE, SHORT_WALK, ON_FOOT }

@Serializable
enum class Availability { NONE, LIMITED, AVAILABLE }

@Serializable
enum class PackingCategory {
    SHELTER, SLEEP, CLOTHING, CHILD, COOKING, FOOD, WATER, LIGHTING, HYGIENE,
    SUN_RAIN, SAFETY, TOOLS, ENTERTAINMENT, DOCUMENTS, OTHER,
}

@Serializable
enum class ItemClassification { MANDATORY, RECOMMENDED, OPTIONAL }

@Serializable
enum class ItemOrigin { RULE, MANUAL, QBOT, INVENTORY, INSTITUTIONAL }

@Serializable
data class OutdoorParticipant(
    val id: String,
    val displayName: String,
    val role: ParticipantRole,
    val ageClass: AgeClass,
)

/** Restricción alimentaria voluntaria. DATO SENSIBLE (no en logs/export por defecto). */
@Serializable
data class DietaryRestriction(
    val id: String,
    val label: String,
    val sensitive: Boolean = true,
)

@Serializable
data class FacilityProfile(
    val accommodation: Accommodation,
    val access: AccessMode,
    val water: Availability,
    val potableWater: Boolean,
    val electricity: Availability,
    val toilets: Boolean,
    val showers: Boolean,
    val refrigeration: Boolean,
    val nearbyCommerce: Availability,
)

@Serializable
data class PackingInput(
    val scenario: ScenarioKind,
    val season: Season,
    val territory: String,
    val nights: Int,
    val participants: List<OutdoorParticipant>,
    val facility: FacilityProfile,
    val dietary: List<DietaryRestriction> = emptyList(),
    val activities: List<String> = emptyList(),
)

@Serializable
data class OutdoorOuting(
    val id: String,
    val title: String,
    val intent: Intent,
    val scenario: ScenarioKind,
    val status: OutingStatus,
    val rulesetVersion: String,
    val input: PackingInput,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class GeneratedItem(
    val key: String,
    val name: String,
    val category: PackingCategory,
    val classification: ItemClassification,
    val quantity: Int,
    val unit: String,
    val sourceRuleId: String,
    val explanation: String,
)

@Serializable
data class OutingPackingItem(
    val key: String,
    val name: String,
    val category: PackingCategory,
    val classification: ItemClassification,
    val quantity: Int,
    val unit: String,
    val sourceRuleId: String,
    val explanation: String,
    val origin: ItemOrigin,
    val packed: Boolean = false,
    val bought: Boolean = false,
    val responsibleId: String? = null,
    val containerId: String? = null,
    val removed: Boolean = false,
    val removalReason: String? = null,
)

@Serializable
data class PreparationTask(
    val id: String,
    val title: String,
    val sourceRuleId: String,
    val done: Boolean = false,
    val responsibleId: String? = null,
)

@Serializable
data class CampingPlan(
    val rulesetVersion: String,
    val items: List<OutingPackingItem>,
    val tasks: List<PreparationTask>,
)

data class PackingDiff(
    val added: List<GeneratedItem>,
    val removed: List<GeneratedItem>,
    val changed: List<Changed>,
) {
    data class Changed(val key: String, val before: GeneratedItem, val after: GeneratedItem)
}
