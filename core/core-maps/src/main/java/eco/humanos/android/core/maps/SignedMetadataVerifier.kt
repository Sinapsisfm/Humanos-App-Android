/**
 * core-maps / SignedMetadataVerifier.kt
 *
 * Verificador FAIL-CLOSED de autenticidad de packs (Signed Metadata V1). Nunca devuelve Valid
 * ante duda. Orden: estructura → firmado → confianza/revocación → firma → canal → vigencia →
 * versión de app → anti-rollback → archivos.
 *
 * Determinístico: `now` y versiones se INYECTAN (sin reloj). No realiza IO de red.
 */
package eco.humanos.android.core.maps

object SignedMetadataVerifier {

    private val HEX64 = Regex("^[0-9a-f]{64}$")
    /** ISO-8601 UTC estricto (sufijo Z, sin offset/fracción) → comparación lexicográfica == cronológica. */
    private val ISO_UTC = Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$")

    /**
     * Verifica autenticidad/integridad de la metadata firmada (fail-closed).
     *
     * SEAM autenticidad↔integridad: `manifestHash`/`licenseManifestHash` van FIRMADOS aquí, pero
     * para cerrar el seam el llamador DEBE pasar los hashes REALES del contenido on-disk
     * (`actualManifestHash`/`actualLicenseManifestHash`); si difieren → ContentHashMismatch. Si no
     * se pasan, esa verificación queda como responsabilidad explícita del call-site.
     */
    fun verify(
        signed: SignedPackMetadata,
        trustStore: TrustStore,
        now: String,
        appVersion: SemVer,
        expectedChannel: String,
        lastAcceptedVersion: SemVer? = null,
        lastAcceptedManifestHash: String? = null,
        actualFiles: Map<String, FileFingerprint> = emptyMap(),
        actualManifestHash: String? = null,
        actualLicenseManifestHash: String? = null,
    ): SignedVerification {
        val m = signed.metadata

        // 0. `now` debe ser ISO-8601 UTC estricto (sin esto la comparación de vigencia no es fiable)
        if (!ISO_UTC.matches(now)) return SignedVerification.InvalidMetadata("now no es ISO-8601 UTC: $now")

        // 1. estructura (malformado → fail-closed)
        structuralError(m)?.let { return SignedVerification.InvalidMetadata(it) }

        // 2. sin firma
        if (signed.signature.isBlank()) return SignedVerification.Unsigned

        // 3. confianza / revocación del firmante
        val resolution = trustStore.resolve(m.signerKeyId)
        val publicKey = when (resolution) {
            is TrustResolution.Revoked -> return SignedVerification.RevokedSigner
            is TrustResolution.Unknown -> return SignedVerification.UnknownSigner
            is TrustResolution.Trusted -> resolution.publicKey
        }

        // 4. firma criptográfica
        if (!PackCrypto.verifySignature(m, signed.signature, publicKey)) {
            return SignedVerification.InvalidSignature
        }

        // 5. canal
        if (m.channel != expectedChannel) return SignedVerification.ChannelMismatch

        // 6. vigencia (ISO-8601 UTC, comparación lexicográfica)
        if (now < m.issuedAt) return SignedVerification.NotYetValid
        if (m.expiresAt != null && now > m.expiresAt) return SignedVerification.Expired

        // 7. compatibilidad de versión de app
        if (appVersion < m.minimumAppVersion) return SignedVerification.IncompatibleAppVersion
        if (m.maximumAppVersion != null && appVersion > m.maximumAppVersion) {
            return SignedVerification.IncompatibleAppVersion
        }

        // 8. anti-rollback: piso efectivo = max(rollbackFloor, última aceptada)
        val effectiveFloor = listOfNotNull(m.rollbackFloor, lastAcceptedVersion).max()
        if (m.packVersion < effectiveFloor) return SignedVerification.RollbackDetected
        // previousVersion declarada debe ser estrictamente anterior (consistencia de cadena)
        if (m.previousVersion != null && m.packVersion <= m.previousVersion) {
            return SignedVerification.RollbackDetected
        }
        // misma versión con contenido distinto al ya aceptado → sospechoso, rechazar
        if (lastAcceptedVersion != null && m.packVersion.compareTo(lastAcceptedVersion) == 0 &&
            lastAcceptedManifestHash != null && lastAcceptedManifestHash != m.manifestHash
        ) {
            return SignedVerification.RollbackDetected
        }

        // 9. seam integridad: el hash FIRMADO debe coincidir con el contenido real (si el caller lo provee)
        if (actualManifestHash != null && actualManifestHash != m.manifestHash) {
            return SignedVerification.ContentHashMismatch("manifest")
        }
        if (actualLicenseManifestHash != null && actualLicenseManifestHash != m.licenseManifestHash) {
            return SignedVerification.ContentHashMismatch("license")
        }

        // 10. archivos: ausencia / tamaño o hash incorrecto (la firma garantiza la lista esperada)
        for (f in m.files) {
            val actual = actualFiles[f.path] ?: return SignedVerification.MissingFile(f.path)
            if (actual.size != f.size || actual.sha256 != f.sha256) return SignedVerification.CorruptFile(f.path)
        }

        return SignedVerification.Valid
    }

    private fun structuralError(m: SignablePackMetadata): String? {
        if (m.schemaVersion != SIGNED_METADATA_SCHEMA_VERSION) return "schemaVersion ${m.schemaVersion} != $SIGNED_METADATA_SCHEMA_VERSION"
        if (m.packId.isBlank()) return "packId vacío"
        if (m.signerKeyId.isBlank()) return "signerKeyId vacío"
        if (m.channel.isBlank()) return "channel vacío"
        if (!ISO_UTC.matches(m.issuedAt)) return "issuedAt no es ISO-8601 UTC: ${m.issuedAt}"
        if (m.expiresAt != null && !ISO_UTC.matches(m.expiresAt)) return "expiresAt no es ISO-8601 UTC: ${m.expiresAt}"
        if (m.territory.isBlank()) return "territory vacío"
        if (m.contentType.isBlank()) return "contentType vacío"
        if (!HEX64.matches(m.manifestHash)) return "manifestHash no es sha256 hex"
        if (!HEX64.matches(m.licenseManifestHash)) return "licenseManifestHash no es sha256 hex"
        if (m.expiresAt != null && m.expiresAt < m.issuedAt) return "expiresAt anterior a issuedAt"
        for (f in m.files) {
            if (f.path.isBlank()) return "archivo con path vacío"
            if (f.size < 0) return "archivo con size negativo: ${f.path}"
            if (!HEX64.matches(f.sha256)) return "archivo con sha256 inválido: ${f.path}"
        }
        return null
    }
}
