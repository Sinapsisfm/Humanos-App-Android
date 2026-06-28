package eco.humanos.android.core.maps

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MapPackVerifierTest {

    private fun pack() = MapPackVerifier.buildPack(
        packId = "syn-pack",
        version = "v0.1.0",
        region = MapFixtures.SYNTHETIC_REGION,
        routes = listOf(MapFixtures.ROUTE),
        waypoints = MapFixtures.WAYPOINTS,
        createdAt = "2026-01-01T00:00:00Z",
    )

    @Test fun built_pack_verifies_valid() {
        assertThat(MapPackVerifier.verify(pack())).isEqualTo(PackVerification.Valid)
    }

    @Test fun content_hash_is_deterministic() {
        val h1 = MapPackVerifier.contentHash(listOf(MapFixtures.ROUTE), MapFixtures.WAYPOINTS)
        val h2 = MapPackVerifier.contentHash(listOf(MapFixtures.ROUTE), MapFixtures.WAYPOINTS)
        assertThat(h1).isEqualTo(h2)
        assertThat(h1).hasLength(64) // sha256 hex
    }

    @Test fun corrupted_content_is_detected() {
        val p = pack()
        // mutar el contenido sin recalcular el manifiesto → corrupción
        val tampered = p.copy(routes = p.routes + RouteGeometry("intruso", listOf(GeoPoint(0.0, 0.0))))
        val v = MapPackVerifier.verify(tampered)
        assertThat(v).isInstanceOf(PackVerification.SizeMismatch::class.java) // tamaño cambia primero
    }

    @Test fun hash_mismatch_with_same_size_is_corrupt() {
        val p = pack()
        // Cambio de IGUAL largo de string ("-41.12" → "-41.13"): el contenido serializado
        // mantiene EXACTAMENTE el mismo tamaño en bytes → la verificación de tamaño pasa y
        // la que debe disparar es la de HASH. Así se ejercita la rama Corrupt de verdad.
        val tamperedRoute = p.routes.first().let { r ->
            val pts = r.points.toMutableList()
            pts[2] = pts[2].copy(lat = -41.13) // antes -41.12 (mismo nº de caracteres)
            r.copy(points = pts)
        }
        val tampered = p.copy(routes = listOf(tamperedRoute))
        // Que el resultado sea Corrupt (y NO SizeMismatch) prueba que el tamaño coincidió y
        // fue el hash el que detectó la manipulación.
        assertThat(MapPackVerifier.verify(tampered)).isInstanceOf(PackVerification.Corrupt::class.java)
    }

    @Test fun schema_mismatch_is_detected() {
        val p = pack()
        val badSchema = p.copy(manifest = p.manifest.copy(schemaVersion = 999))
        assertThat(MapPackVerifier.verify(badSchema))
            .isEqualTo(PackVerification.SchemaMismatch(MAPS_SCHEMA_VERSION, 999))
    }
}
