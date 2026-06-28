package eco.humanos.android.core.maps

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** License Manifest V2: agregación multi-dataset más-restrictiva, fail-closed. Sin proveedor real. */
class LicenseManifestV2Test {

    private val NOW = "2026-06-28T00:00:00Z"
    private fun ds(
        id: String, territory: String = "CL", minZoom: Int = 0, maxZoom: Int = 14,
        offline: Boolean = true, redistribution: Boolean = true, school: Boolean = true,
        commercial: Boolean = true, attribution: String = "© $id", expiresAt: String? = "2030-01-01T00:00:00Z",
        derivedFrom: List<String> = emptyList(), lineage: String? = null, sourceHash: String = "a".repeat(64),
        providerId: String = "prov-$id", licenseId: String = "CC-BY-4.0",
    ) = DatasetLicense(
        datasetId = id, providerId = providerId, title = id, licenseId = licenseId,
        licenseName = "lic", licenseVersion = "1.0", attributionText = attribution,
        territory = territory, minZoom = minZoom, maxZoom = maxZoom, issuedAt = "2026-01-01T00:00:00Z",
        expiresAt = expiresAt, offlineAllowed = offline, cachingAllowed = true,
        redistributionAllowed = redistribution, modificationAllowed = true, derivativeWorksAllowed = true,
        commercialUseAllowed = commercial, institutionalUseAllowed = true, schoolUseAllowed = school,
        sourceHash = sourceHash, derivedFrom = derivedFrom, lineage = lineage,
    )

    @Test fun single_license_release_ready() {
        val r = LicensePolicyEngine.aggregate(listOf(ds("a")), NOW)
        assertThat(r.releaseReady).isTrue()
        assertThat(r.offlineEligible).isTrue()
    }

    @Test fun two_compatible_release_ready_zoom_intersection() {
        val r = LicensePolicyEngine.aggregate(listOf(ds("a", minZoom = 0, maxZoom = 14), ds("b", minZoom = 5, maxZoom = 18)), NOW)
        assertThat(r.releaseReady).isTrue()
        assertThat(r.effectiveMinZoom).isEqualTo(5)
        assertThat(r.effectiveMaxZoom).isEqualTo(14)
    }

    @Test fun distinct_territories_incompatible() {
        val r = LicensePolicyEngine.aggregate(listOf(ds("a", territory = "CL"), ds("b", territory = "AR")), NOW)
        assertThat(r.releaseReady).isFalse()
        assertThat(r.incompatibilities.any { it.contains("territorios distintos") }).isTrue()
    }

    @Test fun most_restrictive_offline_wins() {
        val r = LicensePolicyEngine.aggregate(listOf(ds("a", offline = true), ds("b", offline = false)), NOW)
        assertThat(r.offlineEligible).isFalse()
    }

    @Test fun nearest_expiration_is_min() {
        val r = LicensePolicyEngine.aggregate(
            listOf(ds("a", expiresAt = "2030-01-01T00:00:00Z"), ds("b", expiresAt = "2027-06-01T00:00:00Z")), NOW,
        )
        assertThat(r.nearestExpiration).isEqualTo("2027-06-01T00:00:00Z")
    }

    @Test fun zoom_no_overlap_incompatible() {
        val r = LicensePolicyEngine.aggregate(listOf(ds("a", minZoom = 0, maxZoom = 5), ds("b", minZoom = 10, maxZoom = 14)), NOW)
        assertThat(r.releaseReady).isFalse()
        assertThat(r.effectiveMaxZoom).isNull()
        assertThat(r.incompatibilities.any { it.contains("zoom sin solapamiento") }).isTrue()
    }

    @Test fun multi_attribution_bundle_sorted_deduped() {
        val r = LicensePolicyEngine.aggregate(listOf(ds("a", attribution = "© Z"), ds("b", attribution = "© A"), ds("c", attribution = "© A")), NOW)
        assertThat(r.attributionBundle).isEqualTo(listOf("© A", "© Z"))
    }

    @Test fun derivation_without_lineage_is_blocked() {
        val r = LicensePolicyEngine.aggregate(listOf(ds("a", derivedFrom = listOf("base-x"), lineage = null)), NOW)
        assertThat(r.releaseReady).isFalse()
        assertThat(r.releaseBlockers.any { it.contains("derivación sin lineage") }).isTrue()
    }

    @Test fun derivation_with_lineage_ok() {
        val r = LicensePolicyEngine.aggregate(listOf(ds("a", derivedFrom = listOf("base-x"), lineage = "derivado de base-x v1")), NOW)
        assertThat(r.releaseReady).isTrue()
    }

    @Test fun placeholder_provider_blocks_release() {
        val r = LicensePolicyEngine.aggregate(listOf(ds("a", providerId = DatasetLicense.UNSPECIFIED)), NOW)
        assertThat(r.releaseReady).isFalse()
        assertThat(r.releaseBlockers.any { it.contains("provider UNSPECIFIED") }).isTrue()
    }

    @Test fun school_use_prohibited_not_eligible() {
        val r = LicensePolicyEngine.aggregate(listOf(ds("a", school = true), ds("b", school = false)), NOW)
        assertThat(r.schoolUseEligible).isFalse()
    }

    @Test fun redistribution_prohibited_not_redistributable() {
        val r = LicensePolicyEngine.aggregate(listOf(ds("a", redistribution = false)), NOW)
        assertThat(r.redistributable).isFalse()
    }

    @Test fun expired_dataset_blocks_release() {
        val r = LicensePolicyEngine.aggregate(listOf(ds("a", expiresAt = "2020-01-01T00:00:00Z")), NOW)
        assertThat(r.releaseReady).isFalse()
        assertThat(r.releaseBlockers.any { it.contains("expirado") }).isTrue()
    }

    @Test fun requested_territory_outside_blocks() {
        val r = LicensePolicyEngine.aggregate(listOf(ds("a", territory = "CL")), NOW, requestedTerritory = "AR")
        assertThat(r.releaseReady).isFalse()
        assertThat(r.releaseBlockers.any { it.contains("territorio AR") }).isTrue()
    }

    @Test fun missing_source_hash_blocks() {
        val r = LicensePolicyEngine.aggregate(listOf(ds("a", sourceHash = "")), NOW)
        assertThat(r.releaseReady).isFalse()
        assertThat(r.releaseBlockers.any { it.contains("sourceHash ausente") }).isTrue()
    }
}
