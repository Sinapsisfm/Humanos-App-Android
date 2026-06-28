package eco.humanos.android.core.maps

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.security.KeyPair

/** Matriz adversarial de autenticidad (Signed Metadata V1, Ed25519). Fail-closed. */
class SignedMetadataTest {

    private lateinit var kp: KeyPair
    private lateinit var keyId: String
    private lateinit var store: TrustStore

    private val HASH_A = "a".repeat(64)
    private val MANIFEST_H = "b".repeat(64)
    private val LICENSE_H = "c".repeat(64)

    private lateinit var base: SignablePackMetadata

    @Before fun setup() {
        kp = PackCrypto.generateKeyPair()
        keyId = PackCrypto.keyId(kp.public)
        store = TrustStore(active = mapOf(keyId to kp.public))
        base = SignablePackMetadata(
            schemaVersion = SIGNED_METADATA_SCHEMA_VERSION,
            packId = "pack1", packVersion = SemVer(1, 2, 0), channel = "stable",
            issuedAt = "2026-01-01T00:00:00Z", expiresAt = "2030-01-01T00:00:00Z",
            minimumAppVersion = SemVer(1, 0, 0), maximumAppVersion = null,
            signerKeyId = keyId,
            files = listOf(SignedFileEntry("tiles/a.pmtiles", 10, HASH_A)),
            manifestHash = MANIFEST_H, licenseManifestHash = LICENSE_H,
            contentType = "map/pmtiles", territory = "CL",
            previousVersion = null, rollbackFloor = SemVer(1, 0, 0),
        )
    }

    private fun signed(m: SignablePackMetadata = base) = SignedPackMetadata(m, PackCrypto.sign(m, kp.private))
    private fun goodFiles() = mapOf("tiles/a.pmtiles" to FileFingerprint(10, HASH_A))

    private fun verify(
        s: SignedPackMetadata, now: String = "2026-06-28T00:00:00Z", app: SemVer = SemVer(1, 5, 0),
        channel: String = "stable", lastVer: SemVer? = null, lastHash: String? = null,
        files: Map<String, FileFingerprint> = goodFiles(), st: TrustStore = store,
    ) = SignedMetadataVerifier.verify(s, st, now, app, channel, lastVer, lastHash, files)

    @Test fun valid() { assertThat(verify(signed())).isEqualTo(SignedVerification.Valid) }

    @Test fun unsigned() {
        assertThat(verify(signed().copy(signature = ""))).isEqualTo(SignedVerification.Unsigned)
    }

    @Test fun payload_altered_after_signing() {
        assertThat(verify(signed().let { it.copy(metadata = it.metadata.copy(territory = "AR")) }))
            .isEqualTo(SignedVerification.InvalidSignature)
    }

    @Test fun manifest_hash_altered() {
        assertThat(verify(signed().let { it.copy(metadata = it.metadata.copy(manifestHash = "d".repeat(64))) }))
            .isEqualTo(SignedVerification.InvalidSignature)
    }

    @Test fun license_hash_altered() {
        assertThat(verify(signed().let { it.copy(metadata = it.metadata.copy(licenseManifestHash = "d".repeat(64))) }))
            .isEqualTo(SignedVerification.InvalidSignature)
    }

    @Test fun signature_bytes_altered() {
        assertThat(verify(signed().copy(signature = "AAAA"))).isEqualTo(SignedVerification.InvalidSignature)
    }

    @Test fun wrong_public_key_for_keyid() {
        val other = PackCrypto.generateKeyPair()
        val badStore = TrustStore(active = mapOf(keyId to other.public)) // mismo keyId, clave equivocada
        assertThat(verify(signed(), st = badStore)).isEqualTo(SignedVerification.InvalidSignature)
    }

    @Test fun unknown_signer() {
        assertThat(verify(signed(), st = TrustStore(active = emptyMap())))
            .isEqualTo(SignedVerification.UnknownSigner)
    }

    @Test fun revoked_signer_has_priority() {
        val revStore = store.copy(revoked = setOf(keyId))
        assertThat(verify(signed(), st = revStore)).isEqualTo(SignedVerification.RevokedSigner)
    }

    @Test fun rotated_key_in_previous_still_valid() {
        val rotated = TrustStore(active = emptyMap(), previous = mapOf(keyId to kp.public))
        assertThat(verify(signed(), st = rotated)).isEqualTo(SignedVerification.Valid)
    }

    @Test fun expired() {
        assertThat(verify(signed(), now = "2031-01-01T00:00:00Z")).isEqualTo(SignedVerification.Expired)
    }

    @Test fun not_yet_valid() {
        assertThat(verify(signed(), now = "2025-01-01T00:00:00Z")).isEqualTo(SignedVerification.NotYetValid)
    }

    @Test fun rollback_below_floor() {
        val m = base.copy(packVersion = SemVer(0, 9, 0), rollbackFloor = SemVer(1, 0, 0))
        assertThat(verify(signed(m))).isEqualTo(SignedVerification.RollbackDetected)
    }

    @Test fun rollback_below_last_accepted() {
        // packVersion 1.2.0 pero ya se aceptó 1.3.0 → rollback
        assertThat(verify(signed(), lastVer = SemVer(1, 3, 0))).isEqualTo(SignedVerification.RollbackDetected)
    }

    @Test fun same_version_different_content_rejected() {
        assertThat(verify(signed(), lastVer = SemVer(1, 2, 0), lastHash = "f".repeat(64)))
            .isEqualTo(SignedVerification.RollbackDetected)
    }

    @Test fun channel_mismatch() {
        assertThat(verify(signed(), channel = "beta")).isEqualTo(SignedVerification.ChannelMismatch)
    }

    @Test fun app_below_minimum() {
        assertThat(verify(signed(), app = SemVer(0, 9, 0))).isEqualTo(SignedVerification.IncompatibleAppVersion)
    }

    @Test fun app_above_maximum() {
        val m = base.copy(maximumAppVersion = SemVer(1, 4, 0))
        assertThat(verify(signed(m), app = SemVer(1, 5, 0))).isEqualTo(SignedVerification.IncompatibleAppVersion)
    }

    @Test fun invalid_metadata_schema() {
        assertThat(verify(signed().let { it.copy(metadata = it.metadata.copy(schemaVersion = 99)) }))
            .isInstanceOf(SignedVerification.InvalidMetadata::class.java)
    }

    @Test fun invalid_metadata_bad_manifest_hash() {
        assertThat(verify(signed().let { it.copy(metadata = it.metadata.copy(manifestHash = "xyz")) }))
            .isInstanceOf(SignedVerification.InvalidMetadata::class.java)
    }

    @Test fun missing_file() {
        assertThat(verify(signed(), files = emptyMap()))
            .isEqualTo(SignedVerification.MissingFile("tiles/a.pmtiles"))
    }

    @Test fun corrupt_file_wrong_size() {
        assertThat(verify(signed(), files = mapOf("tiles/a.pmtiles" to FileFingerprint(999, HASH_A))))
            .isEqualTo(SignedVerification.CorruptFile("tiles/a.pmtiles"))
    }

    @Test fun corrupt_file_wrong_hash() {
        assertThat(verify(signed(), files = mapOf("tiles/a.pmtiles" to FileFingerprint(10, "e".repeat(64)))))
            .isEqualTo(SignedVerification.CorruptFile("tiles/a.pmtiles"))
    }

    @Test fun canonical_payload_deterministic_for_equal_values() {
        val a = PackCrypto.canonicalBytes(base)
        val b = PackCrypto.canonicalBytes(base.copy())
        assertThat(a).isEqualTo(b)
    }

    // ── fixes del review cripto (4 MEDIUM) ──
    @Test fun no_expiry_is_valid_even_far_future_now() {
        // expiresAt=null → la verificación de vigencia se omite genuinamente
        assertThat(verify(signed(base.copy(expiresAt = null)), now = "2031-01-01T00:00:00Z"))
            .isEqualTo(SignedVerification.Valid)
    }

    @Test fun malformed_now_with_offset_fails_closed() {
        assertThat(verify(signed(), now = "2026-06-28T00:00:00-04:00"))
            .isInstanceOf(SignedVerification.InvalidMetadata::class.java)
    }

    @Test fun issued_at_with_offset_is_invalid_metadata() {
        assertThat(verify(signed(base.copy(issuedAt = "2026-01-01T00:00:00-04:00"))))
            .isInstanceOf(SignedVerification.InvalidMetadata::class.java)
    }

    @Test fun bad_license_hash_is_invalid_metadata() {
        assertThat(verify(signed().let { it.copy(metadata = it.metadata.copy(licenseManifestHash = "xyz")) }))
            .isInstanceOf(SignedVerification.InvalidMetadata::class.java)
    }

    @Test fun blank_pack_id_is_invalid_metadata() {
        assertThat(verify(signed().let { it.copy(metadata = it.metadata.copy(packId = "")) }))
            .isInstanceOf(SignedVerification.InvalidMetadata::class.java)
    }

    @Test fun expiry_before_issued_is_invalid_metadata() {
        assertThat(verify(signed(base.copy(issuedAt = "2026-06-01T00:00:00Z", expiresAt = "2026-01-01T00:00:00Z"))))
            .isInstanceOf(SignedVerification.InvalidMetadata::class.java)
    }

    @Test fun bad_file_sha256_is_invalid_metadata() {
        assertThat(verify(signed(base.copy(files = listOf(SignedFileEntry("a", 1, "nothex"))))))
            .isInstanceOf(SignedVerification.InvalidMetadata::class.java)
    }

    @Test fun content_hash_binding_mismatch_detected() {
        // firma válida pero manifestHash firmado != hash real on-disk → ContentHashMismatch
        val r = SignedMetadataVerifier.verify(
            signed(), store, "2026-06-28T00:00:00Z", SemVer(1, 5, 0), "stable",
            actualFiles = goodFiles(), actualManifestHash = "9".repeat(64),
        )
        assertThat(r).isEqualTo(SignedVerification.ContentHashMismatch("manifest"))
    }

    @Test fun content_hash_binding_match_is_valid() {
        assertThat(SignedMetadataVerifier.verify(signed(), store, "2026-06-28T00:00:00Z", SemVer(1,5,0), "stable",
            actualFiles = goodFiles(), actualManifestHash = MANIFEST_H, actualLicenseManifestHash = LICENSE_H))
            .isEqualTo(SignedVerification.Valid)
    }

    @Test fun previous_version_not_before_pack_is_rollback() {
        assertThat(verify(signed(base.copy(previousVersion = SemVer(1, 2, 0))))) // == packVersion → inconsistente
            .isEqualTo(SignedVerification.RollbackDetected)
    }

    @Test fun semver_compare_and_parse() {
        assertThat(SemVer.parseOrNull("1.2.3")).isEqualTo(SemVer(1, 2, 3))
        assertThat(SemVer.parseOrNull("1.2")).isNull()
        assertThat(SemVer.parseOrNull("a.b.c")).isNull()
        assertThat(SemVer(1, 2, 0) < SemVer(1, 10, 0)).isTrue()
    }
}
