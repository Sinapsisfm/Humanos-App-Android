/**
 * core-maps / MapSerialization.kt
 *
 * Json canónico compartido (mismo patrón que OutdoorSnapshotCodec en core-outdoor):
 * un único formato estable para serializar packs/rutas y para calcular el hash de
 * integridad. Determinístico: claves ordenadas implícitamente por el orden de declaración
 * de las data class y `encodeDefaults=true` para que el hash no dependa de defaults omitidos.
 *
 * INVARIANTE: el contenido hasheado/serializado NO debe contener colecciones sin orden
 * (Map/Set). Hoy solo hay List, cuyo orden es estable → hash reproducible entre corridas.
 * Agregar un Map/Set a PackContent/MapSnapshot rompería la estabilidad del hash.
 */
package eco.humanos.android.core.maps

import kotlinx.serialization.json.Json

/** Formato canónico para persistencia y hashing. No cambiar sin subir MAPS_SCHEMA_VERSION. */
val MapJson: Json = Json {
    encodeDefaults = true
    prettyPrint = false
    ignoreUnknownKeys = true
}
