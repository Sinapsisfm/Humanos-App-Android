/**
 * core-maps / LicenseManifest.kt
 *
 * Estructura LOCAL para registrar la procedencia y licencia de datos cartográficos. NO se
 * selecciona ni se incorpora ningún proveedor real: el POC solo define el contrato y el
 * validador fail-closed que un release gate usaría (MAP-002 / DAT-004 en ADR-OUT-003).
 *
 * Mientras `provider`/`license` sean UNSPECIFIED, la validación devuelve NotReadyForRelease
 * → el gate de release bloquea cualquier mapa real sin manifiesto firmado. Determinístico:
 * `now`/fechas se inyectan (sin reloj).
 */
package eco.humanos.android.core.maps

import kotlinx.serialization.Serializable

/** Política de redistribución del dato cartográfico. */
@Serializable
enum class RedistributionPolicy { UNSPECIFIED, PROHIBITED, ATTRIBUTION_REQUIRED, PERMISSIVE }

@Serializable
data class ZoomRange(val min: Int, val max: Int) {
    fun isValid(): Boolean = min in 0..30 && max in 0..30 && min <= max
}

/**
 * Manifiesto de licencia cartográfica (estructura, sin proveedor real).
 * Fechas en ISO-8601 (inyectadas). `sourceHash` = sha256 hex (64 chars) del dato fuente.
 */
@Serializable
data class LicenseManifest(
    val provider: String,
    val license: String,
    val attribution: String,
    val territory: String,
    val zoom: ZoomRange,
    val issuedAt: String,
    val expiresAt: String? = null,
    val offlineRestrictions: String,
    val redistribution: RedistributionPolicy,
    val sourceHash: String,
    val source: String,
) {
    companion object {
        const val UNSPECIFIED = "UNSPECIFIED"

        /** Placeholder: NO hay proveedor seleccionado. El gate de release debe bloquearlo. */
        fun unspecified(): LicenseManifest = LicenseManifest(
            provider = UNSPECIFIED,
            license = UNSPECIFIED,
            attribution = UNSPECIFIED,
            territory = UNSPECIFIED,
            zoom = ZoomRange(0, 0),
            issuedAt = "",
            expiresAt = null,
            offlineRestrictions = UNSPECIFIED,
            redistribution = RedistributionPolicy.UNSPECIFIED,
            sourceHash = "",
            source = UNSPECIFIED,
        )
    }
}

sealed interface LicenseValidation {
    /** Estructuralmente completo Y con proveedor/licencia reales → apto para release gate. */
    data object ReadyForRelease : LicenseValidation
    /** Estructura válida pero sin proveedor/licencia real (placeholder) → gate debe BLOQUEAR. */
    data class NotReadyForRelease(val reasons: List<String>) : LicenseValidation
    /** Estructura inválida (campos faltantes/incorrectos) → fail-closed. */
    data class Invalid(val reasons: List<String>) : LicenseValidation
    /** Vigencia expirada respecto de `now`. */
    data class Expired(val expiresAt: String, val now: String) : LicenseValidation
}

object LicenseManifestValidator {

    private val HEX64 = Regex("^[0-9a-f]{64}$")

    /**
     * Valida el manifiesto. `now` ISO-8601 se inyecta (determinístico). Orden fail-closed:
     * estructura → placeholder → vigencia → listo.
     */
    fun validate(m: LicenseManifest, now: String): LicenseValidation {
        // estructura = MALFORMADO (no meros placeholders): rangos/format/coherencia
        val structural = buildList {
            if (m.provider.isBlank()) add("provider vacío")
            if (m.license.isBlank()) add("license vacío")
            if (m.territory.isBlank()) add("territory vacío")
            if (!m.zoom.isValid()) add("zoom inválido (${m.zoom.min}..${m.zoom.max})")
            // sourceHash debe ser sha256 hex (o vacío SOLO en placeholder, validado abajo)
            if (m.sourceHash.isNotEmpty() && !HEX64.matches(m.sourceHash)) add("sourceHash no es sha256 hex")
            if (m.expiresAt != null && m.expiresAt < m.issuedAt) add("expiresAt anterior a issuedAt")
        }
        if (structural.isNotEmpty()) return LicenseValidation.Invalid(structural)

        // placeholder: cualquier campo aún en UNSPECIFIED/ausente → no apto para release
        fun unset(v: String) = v.isBlank() || v == LicenseManifest.UNSPECIFIED
        val placeholderReasons = buildList {
            if (unset(m.provider)) add("provider UNSPECIFIED (no hay proveedor seleccionado)")
            if (unset(m.license)) add("license UNSPECIFIED")
            if (unset(m.territory)) add("territory UNSPECIFIED")
            if (unset(m.source)) add("source UNSPECIFIED")
            if (unset(m.offlineRestrictions)) add("offlineRestrictions UNSPECIFIED")
            if (m.redistribution == RedistributionPolicy.UNSPECIFIED) add("redistribution UNSPECIFIED")
            if (m.sourceHash.isEmpty()) add("sourceHash ausente (dato fuente no verificado)")
            if (m.issuedAt.isEmpty()) add("issuedAt ausente")
            if (unset(m.attribution)) add("attribution ausente")
        }
        if (placeholderReasons.isNotEmpty()) return LicenseValidation.NotReadyForRelease(placeholderReasons)

        if (m.expiresAt != null && m.expiresAt < now) {
            return LicenseValidation.Expired(m.expiresAt, now)
        }
        return LicenseValidation.ReadyForRelease
    }
}
