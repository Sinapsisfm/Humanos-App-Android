/**
 * core-outdoor / domain / Events.kt
 *
 * Eventos de dominio con distinción obligatoria de FUENTE (declaración/sensor/externo/
 * inferencia determinística/inferencia IA/acción de sistema) e `eventId` idempotente,
 * base de la persistencia restaurable e idempotente.
 */
package eco.humanos.android.core.outdoor.domain

import kotlinx.serialization.Serializable

@Serializable
enum class SourceType {
    USER_DECLARATION, SENSOR_MEASUREMENT, EXTERNAL_DATA,
    DETERMINISTIC_INFERENCE, AI_INFERENCE, SYSTEM_ACTION,
}

@Serializable
enum class OutingEventType {
    OUTING_CREATED, SCENARIO_SELECTED, PACKING_GENERATED, PACKING_ITEM_EDITED,
    PREP_TASK_TOGGLED, OUTING_STATUS_CHANGED, AWARENESS_SIGNAL_RAISED,
    AWARENESS_SIGNAL_DISMISSED, SESSION_RESTORED,
}

@Serializable
data class OutingEvent(
    /** Clave de idempotencia: reaplicar es no-op. */
    val eventId: String,
    val outingId: String,
    val type: OutingEventType,
    val sourceType: SourceType,
    /** ISO8601, inyectado (sin reloj interno). */
    val at: String,
    /** Payload serializable. Nunca datos sensibles en texto plano. */
    val payload: Map<String, String> = emptyMap(),
)
