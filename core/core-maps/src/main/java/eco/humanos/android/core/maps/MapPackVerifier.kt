/**
 * core-maps / MapPackVerifier.kt
 *
 * Construcción y verificación de integridad de packs offline. Mismo principio que los
 * Knowledge Packs del ecosistema: el manifiesto lleva sha256 + tamaño del contenido
 * canónico; al cargar un pack se RECALCULA y se compara (fail-closed ante corrupción).
 *
 * Determinístico: sha256 sobre la serialización canónica (MapJson). Sin reloj/azar.
 * El `createdAt` se INYECTA (no se lee de System.now()).
 */
package eco.humanos.android.core.maps

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.security.MessageDigest

/** Contenido canónico hasheado: rutas + waypoints (sin el manifiesto, para evitar ciclo). */
@Serializable
private data class PackContent(val routes: List<RouteGeometry>, val waypoints: List<Waypoint>)

sealed interface PackVerification {
    data object Valid : PackVerification
    data class Corrupt(val expectedHash: String, val actualHash: String) : PackVerification
    data class SchemaMismatch(val expected: Int, val actual: Int) : PackVerification
    data class SizeMismatch(val expected: Long, val actual: Long) : PackVerification
    data class Invalid(val reason: String) : PackVerification
}

class MapPackException(message: String) : Exception(message)

object MapPackVerifier {

    /** sha256 hex (minúsculas) del contenido canónico. */
    fun contentHash(routes: List<RouteGeometry>, waypoints: List<Waypoint>): String {
        val bytes = canonicalBytes(routes, waypoints)
        return MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }

    /**
     * Construye un pack íntegro: calcula hash + tamaño. `createdAt` se inyecta.
     * Fail-closed: rechaza límites excedidos o geometría inválida con [MapPackException].
     */
    fun buildPack(
        packId: String,
        version: String,
        region: MapRegion,
        routes: List<RouteGeometry>,
        waypoints: List<Waypoint>,
        createdAt: String,
        datum: Datum = Datum.WGS84,
    ): OfflineMapPack {
        if (routes.size > MAX_PACK_ROUTES) throw MapPackException("pack excede MAX_PACK_ROUTES (${routes.size})")
        routes.firstOrNull { !it.isValid() }?.let { throw MapPackException("ruta inválida en pack: ${it.id}") }
        waypoints.firstOrNull { !GeoPoint(it.lat, it.lon).isValid() }?.let { throw MapPackException("waypoint inválido: ${it.id}") }
        val bytes = canonicalBytes(routes, waypoints)
        val manifest = MapPackManifest(
            packId = packId,
            version = version,
            schemaVersion = MAPS_SCHEMA_VERSION,
            createdAt = createdAt,
            region = region,
            datum = datum,
            contentHash = contentHash(routes, waypoints),
            sizeBytes = bytes.size.toLong(),
        )
        return OfflineMapPack(manifest = manifest, routes = routes, waypoints = waypoints)
    }

    /** Verifica esquema, geometría, tamaño y hash del contenido contra el manifiesto (fail-closed). */
    fun verify(pack: OfflineMapPack): PackVerification {
        if (pack.manifest.schemaVersion != MAPS_SCHEMA_VERSION) {
            return PackVerification.SchemaMismatch(MAPS_SCHEMA_VERSION, pack.manifest.schemaVersion)
        }
        if (pack.routes.size > MAX_PACK_ROUTES) {
            return PackVerification.Invalid("pack excede MAX_PACK_ROUTES (${pack.routes.size})")
        }
        pack.routes.firstOrNull { !it.isValid() }?.let {
            return PackVerification.Invalid("ruta inválida: ${it.id}")
        }
        val bytes = canonicalBytes(pack.routes, pack.waypoints)
        if (bytes.size.toLong() != pack.manifest.sizeBytes) {
            return PackVerification.SizeMismatch(pack.manifest.sizeBytes, bytes.size.toLong())
        }
        val actual = contentHash(pack.routes, pack.waypoints)
        if (actual != pack.manifest.contentHash) {
            return PackVerification.Corrupt(pack.manifest.contentHash, actual)
        }
        return PackVerification.Valid
    }

    private fun canonicalBytes(routes: List<RouteGeometry>, waypoints: List<Waypoint>): ByteArray =
        MapJson.encodeToString(PackContent(routes, waypoints)).toByteArray(Charsets.UTF_8)
}
