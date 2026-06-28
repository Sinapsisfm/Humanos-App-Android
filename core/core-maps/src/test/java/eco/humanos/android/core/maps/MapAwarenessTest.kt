package eco.humanos.android.core.maps

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Evaluador AwarenessOS de mapas (determinístico, dedup, severidad). */
class MapAwarenessTest {

    @Test fun no_state_no_signals() {
        assertThat(MapAwarenessEvaluator.evaluate(MapGovernanceState())).isEmpty()
    }

    @Test fun all_green_no_signals() {
        val lic = LicensePolicyResult(true, emptyList(), true, true, true, true, listOf("© x"), null, 0, 14, listOf("CL"), null, null, emptyList())
        val s = MapGovernanceState(signed = SignedVerification.Valid, license = lic, load = MapLoadState.Idle)
        assertThat(MapAwarenessEvaluator.evaluate(s)).isEmpty()
    }

    @Test fun unsigned_pack_is_blocker() {
        val sig = MapAwarenessEvaluator.evaluate(MapGovernanceState(signed = SignedVerification.Unsigned))
        assertThat(sig).hasSize(1)
        assertThat(sig.first().severity).isEqualTo(MapSignalSeverity.BLOCKER)
        assertThat(sig.first().id).isEqualTo("pack-unsigned")
    }

    @Test fun license_not_ready_blocks_with_reasons() {
        val lic = LicensePolicyResult(false, listOf("provider UNSPECIFIED"), true, true, true, true, emptyList(), null, 0, 14, listOf("CL"), null, null, emptyList())
        val sig = MapAwarenessEvaluator.evaluate(MapGovernanceState(license = lic))
        assertThat(sig.any { it.id == "license-not-ready" && it.severity == MapSignalSeverity.BLOCKER }).isTrue()
    }

    @Test fun license_expiring_is_caution() {
        val lic = LicensePolicyResult(true, emptyList(), true, true, true, true, emptyList(), "2026-07-01T00:00:00Z", 0, 14, listOf("CL"), null, null, emptyList())
        val sig = MapAwarenessEvaluator.evaluate(MapGovernanceState(license = lic, licenseExpiryWarnByIso = "2026-08-01T00:00:00Z"))
        assertThat(sig.any { it.id == "license-expiring" && it.severity == MapSignalSeverity.CAUTION }).isTrue()
    }

    @Test fun corrupt_load_is_blocker() {
        val sig = MapAwarenessEvaluator.evaluate(MapGovernanceState(load = MapLoadState.Corrupt("hash")))
        assertThat(sig.any { it.id == "pack-corrupt" && it.severity == MapSignalSeverity.BLOCKER }).isTrue()
    }

    @Test fun signals_ordered_blocker_first_and_deduped() {
        val lic = LicensePolicyResult(false, listOf("x"), false, true, true, true, emptyList(), null, 0, 14, listOf("CL"), null, null, emptyList())
        val s = MapGovernanceState(signed = SignedVerification.Expired, license = lic, load = MapLoadState.Expired)
        val sig = MapAwarenessEvaluator.evaluate(s)
        // 'pack-expired' aparece desde signed Y desde load → dedup por id (una sola vez)
        assertThat(sig.count { it.id == "pack-expired" }).isEqualTo(1)
        // primer elemento es BLOCKER (orden por severidad desc)
        assertThat(sig.first().severity).isEqualTo(MapSignalSeverity.BLOCKER)
    }
}
