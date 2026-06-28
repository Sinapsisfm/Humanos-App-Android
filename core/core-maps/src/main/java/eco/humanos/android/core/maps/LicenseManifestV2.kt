/**
 * core-maps / LicenseManifestV2.kt
 *
 * License Manifest V2: un pack puede combinar varios datasets con licencias distintas. La
 * política agregada adopta la restricción MÁS SEVERA (fail-closed). NO selecciona proveedor.
 *
 * Determinístico: `now` y fechas se inyectan (ISO-8601 UTC, comparación lexicográfica).
 */
package eco.humanos.android.core.maps

import kotlinx.serialization.Serializable

/** Licencia de un dataset individual dentro de un pack. */
@Serializable
data class DatasetLicense(
    val datasetId: String,
    val providerId: String,
    val title: String,
    val licenseId: String,
    val licenseName: String,
    val licenseVersion: String,
    val attributionText: String,
    val attributionUrl: String? = null,
    val sourceUrl: String? = null,
    val territory: String,
    val minZoom: Int,
    val maxZoom: Int,
    val issuedAt: String,
    val expiresAt: String? = null,
    val offlineAllowed: Boolean,
    val cachingAllowed: Boolean,
    val redistributionAllowed: Boolean,
    val modificationAllowed: Boolean,
    val derivativeWorksAllowed: Boolean,
    val commercialUseAllowed: Boolean,
    val institutionalUseAllowed: Boolean,
    val schoolUseAllowed: Boolean,
    val deviceLimit: Int? = null,
    val userLimit: Int? = null,
    val retentionLimit: String? = null,
    val updateRequirement: String? = null,
    val telemetryRequirement: String? = null,
    val displayRequirements: String? = null,
    val sourceHash: String,
    val lineage: String? = null,
    val derivedFrom: List<String> = emptyList(),
    val notes: String? = null,
) {
    companion object { const val UNSPECIFIED = "UNSPECIFIED" }
}

/** Resultado de la política agregada del pack (más restrictiva, fail-closed). */
@Serializable
data class LicensePolicyResult(
    val releaseReady: Boolean,
    val releaseBlockers: List<String>,
    val offlineEligible: Boolean,
    val redistributable: Boolean,
    val commercialUseEligible: Boolean,
    val schoolUseEligible: Boolean,
    val attributionBundle: List<String>,
    val nearestExpiration: String?,
    val effectiveMinZoom: Int?,
    val effectiveMaxZoom: Int?,
    val effectiveTerritories: List<String>,
    val effectiveDeviceLimit: Int?,
    val effectiveUserLimit: Int?,
    val incompatibilities: List<String>,
)

object LicensePolicyEngine {

    private val HEX64 = Regex("^[0-9a-f]{64}$")

    /**
     * Agrega las licencias de los datasets en una política de pack (más restrictiva).
     * @param requestedTerritory si se indica, debe estar cubierto por TODOS los datasets.
     */
    fun aggregate(datasets: List<DatasetLicense>, now: String, requestedTerritory: String? = null): LicensePolicyResult {
        if (datasets.isEmpty()) {
            return LicensePolicyResult(
                releaseReady = false, releaseBlockers = listOf("sin datasets"),
                offlineEligible = false, redistributable = false, commercialUseEligible = false,
                schoolUseEligible = false, attributionBundle = emptyList(), nearestExpiration = null,
                effectiveMinZoom = null, effectiveMaxZoom = null, effectiveTerritories = emptyList(),
                effectiveDeviceLimit = null, effectiveUserLimit = null,
                incompatibilities = listOf("pack sin datasets"),
            )
        }

        val blockers = mutableListOf<String>()
        val incompat = mutableListOf<String>()

        // fail-closed por dataset
        for (d in datasets) {
            fun unset(v: String) = v.isBlank() || v == DatasetLicense.UNSPECIFIED
            if (unset(d.providerId)) blockers += "${d.datasetId}: provider UNSPECIFIED"
            if (unset(d.licenseId)) blockers += "${d.datasetId}: license ausente"
            if (unset(d.attributionText)) blockers += "${d.datasetId}: atribución obligatoria ausente"
            if (d.sourceHash.isBlank()) blockers += "${d.datasetId}: sourceHash ausente"
            else if (!HEX64.matches(d.sourceHash)) blockers += "${d.datasetId}: sourceHash inválido"
            if (d.expiresAt != null && d.expiresAt < now) blockers += "${d.datasetId}: dataset expirado"
            if (d.derivedFrom.isNotEmpty() && (d.lineage == null || d.lineage.isBlank())) {
                blockers += "${d.datasetId}: derivación sin lineage"
            }
            if (d.minZoom > d.maxZoom) incompat += "${d.datasetId}: zoom inválido (${d.minZoom}..${d.maxZoom})"
            if (requestedTerritory != null && d.territory != requestedTerritory) {
                blockers += "${d.datasetId}: no licenciado para territorio $requestedTerritory (es ${d.territory})"
            }
        }

        // agregaciones más-restrictivas
        val offlineEligible = datasets.all { it.offlineAllowed }
        val redistributable = datasets.all { it.redistributionAllowed }
        val commercialUseEligible = datasets.all { it.commercialUseAllowed }
        val schoolUseEligible = datasets.all { it.schoolUseAllowed }
        val attributionBundle = datasets.map { it.attributionText }.filter { it.isNotBlank() }.distinct().sorted()
        val nearestExpiration = datasets.mapNotNull { it.expiresAt }.minOrNull()

        // zoom efectivo = intersección [max(min), min(max)]
        val effMinZoom = datasets.maxOf { it.minZoom }
        val effMaxZoom = datasets.minOf { it.maxZoom }
        val zoomOk = effMinZoom <= effMaxZoom
        if (!zoomOk) incompat += "zoom sin solapamiento (efectivo $effMinZoom..$effMaxZoom)"

        // territorio efectivo = intersección de territorios (igualdad exacta)
        val territories = datasets.map { it.territory }.toSet()
        val effectiveTerritories = if (territories.size == 1) territories.toList() else emptyList()
        if (territories.size > 1) incompat += "territorios distintos no combinables: ${territories.sorted()}"

        val effDeviceLimit = datasets.mapNotNull { it.deviceLimit }.minOrNull()
        val effUserLimit = datasets.mapNotNull { it.userLimit }.minOrNull()

        val releaseReady = blockers.isEmpty() && incompat.isEmpty()
        return LicensePolicyResult(
            releaseReady = releaseReady,
            releaseBlockers = (blockers + incompat).distinct(),
            offlineEligible = offlineEligible,
            redistributable = redistributable,
            commercialUseEligible = commercialUseEligible,
            schoolUseEligible = schoolUseEligible,
            attributionBundle = attributionBundle,
            nearestExpiration = nearestExpiration,
            effectiveMinZoom = if (zoomOk) effMinZoom else null,
            effectiveMaxZoom = if (zoomOk) effMaxZoom else null,
            effectiveTerritories = effectiveTerritories,
            effectiveDeviceLimit = effDeviceLimit,
            effectiveUserLimit = effUserLimit,
            incompatibilities = incompat,
        )
    }
}
