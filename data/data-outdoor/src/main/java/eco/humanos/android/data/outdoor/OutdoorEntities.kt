/**
 * data-outdoor / OutdoorEntities.kt
 *
 * Entidades Room para la persistencia offline de Outdoor (CORE-002 en dispositivo).
 * Base de datos SEPARADA (`outdoor.db`, v1) → estrictamente ADITIVA, sin tocar la
 * `HumanosDatabase` compartida ni migrar datos existentes.
 *
 * Estrategia de serialización: los objetos anidados (PackingInput, CampingPlan, payload
 * de evento) se guardan como JSON (kotlinx.serialization) en columnas TEXT. Índices en
 * `outingId` y `at` para consultas de eventos.
 */
package eco.humanos.android.data.outdoor

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "outdoor_outings")
data class OutdoorOutingEntity(
    @PrimaryKey val id: String,
    val title: String,
    val intent: String,
    val scenario: String,
    val status: String,
    val rulesetVersion: String,
    /** PackingInput serializado a JSON. */
    val inputJson: String,
    val createdAt: String,
    val updatedAt: String,
)

@Entity(tableName = "outdoor_plans")
data class OutdoorPlanEntity(
    @PrimaryKey val outingId: String,
    /** CampingPlan serializado a JSON. */
    val planJson: String,
)

@Entity(
    tableName = "outdoor_events",
    indices = [Index("outingId"), Index("at")],
)
data class OutdoorEventEntity(
    @PrimaryKey val eventId: String,
    val outingId: String,
    val type: String,
    val sourceType: String,
    val at: String,
    /** payload (Map<String,String>) serializado a JSON. */
    val payloadJson: String,
)
