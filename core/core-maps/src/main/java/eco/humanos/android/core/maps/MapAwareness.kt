/**
 * core-maps / MapAwareness.kt
 *
 * Evaluador AwarenessOS DETERMINÍSTICO para la gobernanza de mapas (Fase L, alcance maps).
 * Compone los resultados de autenticidad (SignedVerification), licencia (LicensePolicyResult) y
 * carga (MapLoadState) en señales explicables. SIN reloj/azar: `now` se inyecta. Dedup por id.
 *
 * NOTA: el LOOP recurrente (cooldown/confidence/acknowledgement/suppression/outcome) es la capa
 * de recurrencia, NO incluida aquí (ver cierre: diseñada, no implementada). Este evaluador es la
 * función pura sobre la que esa capa operaría; solo emite señales ante una condición real.
 */
package eco.humanos.android.core.maps

enum class MapSignalSeverity { INFO, CAUTION, BLOCKER }

/** Señal explicable. `sourceRuleId` da trazabilidad; `evidence` el porqué. */
data class MapSignal(
    val id: String,
    val severity: MapSignalSeverity,
    val title: String,
    val evidence: String,
    val sourceRuleId: String,
)

/** Estado de gobernanza de un pack para evaluar. Todo opcional: se evalúa lo que haya. */
data class MapGovernanceState(
    val signed: SignedVerification? = null,
    val license: LicensePolicyResult? = null,
    val load: MapLoadState? = null,
    /** umbral de "licencia próxima a vencer" en días; el cálculo usa `now` inyectado. */
    val licenseExpiryWarnByIso: String? = null,
)

object MapAwarenessEvaluator {

    const val AWARENESS_MAPS_VERSION = "awareness-maps.v0.1.0"

    /** Evalúa señales determinísticas. Orden: severidad desc, luego id. Dedup por id. */
    fun evaluate(state: MapGovernanceState): List<MapSignal> {
        val out = LinkedHashMap<String, MapSignal>()
        fun emit(s: MapSignal) { out.putIfAbsent(s.id, s) }

        // — autenticidad —
        when (val v = state.signed) {
            null -> {}
            is SignedVerification.Valid -> {}
            is SignedVerification.Unsigned ->
                emit(MapSignal("pack-unsigned", MapSignalSeverity.BLOCKER, "Pack sin firma", "No tiene metadata firmada (solo integridad).", "AWM-001"))
            is SignedVerification.Expired ->
                emit(MapSignal("pack-expired", MapSignalSeverity.BLOCKER, "Vigencia vencida", "La metadata firmada expiró.", "AWM-002"))
            is SignedVerification.RevokedSigner ->
                emit(MapSignal("signer-revoked", MapSignalSeverity.BLOCKER, "Firmante revocado", "El keyId firmante está revocado.", "AWM-003"))
            is SignedVerification.UnknownSigner ->
                emit(MapSignal("signer-unknown", MapSignalSeverity.BLOCKER, "Firmante desconocido", "El keyId no está en el almacén de confianza.", "AWM-004"))
            is SignedVerification.RollbackDetected ->
                emit(MapSignal("rollback", MapSignalSeverity.BLOCKER, "Rollback detectado", "Versión inferior al piso aceptado.", "AWM-005"))
            else ->
                emit(MapSignal("auth-failed", MapSignalSeverity.BLOCKER, "Autenticidad no verificada", "Estado: ${v::class.simpleName}", "AWM-006"))
        }

        // — licencia —
        state.license?.let { lic ->
            if (!lic.releaseReady) {
                emit(MapSignal("license-not-ready", MapSignalSeverity.BLOCKER, "Licencia no apta para release",
                    lic.releaseBlockers.take(3).joinToString("; ").ifEmpty { "ver política" }, "AWM-010"))
            }
            if (!lic.offlineEligible) {
                emit(MapSignal("license-no-offline", MapSignalSeverity.CAUTION, "Sin derecho offline",
                    "Al menos un dataset prohíbe uso offline.", "AWM-011"))
            }
            val warnBy = state.licenseExpiryWarnByIso
            val exp = lic.nearestExpiration
            if (warnBy != null && exp != null && exp <= warnBy) {
                emit(MapSignal("license-expiring", MapSignalSeverity.CAUTION, "Licencia próxima a vencer",
                    "Vence $exp (umbral $warnBy).", "AWM-012"))
            }
        }

        // — carga / integridad de contenido —
        when (val l = state.load) {
            null, is MapLoadState.Ready, MapLoadState.Idle, MapLoadState.Loading -> {}
            is MapLoadState.Corrupt ->
                emit(MapSignal("pack-corrupt", MapSignalSeverity.BLOCKER, "Pack corrupto", "Integridad: ${l.reason}", "AWM-020"))
            is MapLoadState.Missing ->
                emit(MapSignal("pack-missing", MapSignalSeverity.CAUTION, "Pack ausente", "No se encontró el pack solicitado.", "AWM-021"))
            is MapLoadState.Empty ->
                emit(MapSignal("pack-empty", MapSignalSeverity.CAUTION, "Pack sin contenido", "Sin rutas ni waypoints.", "AWM-022"))
            is MapLoadState.Expired ->
                emit(MapSignal("pack-expired", MapSignalSeverity.BLOCKER, "Vigencia vencida", "Carga reportó expiración.", "AWM-002"))
            is MapLoadState.Invalid ->
                emit(MapSignal("pack-invalid", MapSignalSeverity.BLOCKER, "Pack inválido", l.reason, "AWM-023"))
            is MapLoadState.Error ->
                emit(MapSignal("pack-error", MapSignalSeverity.CAUTION, "Error de carga", l.reason, "AWM-024"))
            MapLoadState.Cancelled -> {}
        }

        return out.values.sortedWith(compareByDescending<MapSignal> { it.severity.ordinal }.thenBy { it.id })
    }
}
