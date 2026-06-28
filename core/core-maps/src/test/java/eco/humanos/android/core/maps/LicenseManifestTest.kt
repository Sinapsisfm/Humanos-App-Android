package eco.humanos.android.core.maps

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Validador de License Manifest: estructura → placeholder → vigencia → listo (fail-closed). */
class LicenseManifestTest {

    private val sha = "a".repeat(64)

    private fun ready(expiresAt: String? = "2030-01-01T00:00:00Z") = LicenseManifest(
        provider = "ProveedorEjemplo", license = "CC-BY-4.0", attribution = "© Ejemplo",
        territory = "CL", zoom = ZoomRange(0, 14), issuedAt = "2026-01-01T00:00:00Z",
        expiresAt = expiresAt, offlineRestrictions = "uso offline permitido",
        redistribution = RedistributionPolicy.ATTRIBUTION_REQUIRED, sourceHash = sha, source = "fuente-ejemplo",
    )

    @Test fun unspecified_placeholder_is_not_ready_for_release() {
        val v = LicenseManifestValidator.validate(LicenseManifest.unspecified(), "2026-06-28T00:00:00Z")
        assertThat(v).isInstanceOf(LicenseValidation.NotReadyForRelease::class.java)
        val reasons = (v as LicenseValidation.NotReadyForRelease).reasons
        assertThat(reasons.any { it.contains("provider UNSPECIFIED") }).isTrue()
    }

    @Test fun complete_manifest_is_ready_for_release() {
        assertThat(LicenseManifestValidator.validate(ready(), "2026-06-28T00:00:00Z"))
            .isEqualTo(LicenseValidation.ReadyForRelease)
    }

    @Test fun bad_hash_is_invalid() {
        val v = LicenseManifestValidator.validate(ready().copy(sourceHash = "xyz"), "2026-06-28T00:00:00Z")
        assertThat(v).isInstanceOf(LicenseValidation.Invalid::class.java)
        assertThat((v as LicenseValidation.Invalid).reasons.any { it.contains("sourceHash") }).isTrue()
    }

    @Test fun bad_zoom_is_invalid() {
        val v = LicenseManifestValidator.validate(ready().copy(zoom = ZoomRange(10, 5)), "2026-06-28T00:00:00Z")
        assertThat(v).isInstanceOf(LicenseValidation.Invalid::class.java)
    }

    @Test fun expiry_before_issued_is_invalid() {
        val v = LicenseManifestValidator.validate(
            ready(expiresAt = "2020-01-01T00:00:00Z"), "2026-06-28T00:00:00Z",
        )
        assertThat(v).isInstanceOf(LicenseValidation.Invalid::class.java)
    }

    @Test fun expired_manifest_is_expired() {
        // expira 2030 pero now=2031 → expirado
        val v = LicenseManifestValidator.validate(ready(), "2031-01-01T00:00:00Z")
        assertThat(v).isInstanceOf(LicenseValidation.Expired::class.java)
    }

    @Test fun no_expiry_is_allowed() {
        assertThat(LicenseManifestValidator.validate(ready(expiresAt = null), "2026-06-28T00:00:00Z"))
            .isEqualTo(LicenseValidation.ReadyForRelease)
    }
}
