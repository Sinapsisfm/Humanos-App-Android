/**
 * core-maps / SignedPackMetadata.kt
 *
 * Metadata FIRMADA de un pack de mapas (Signed Metadata V1). Kotlin/JVM puro.
 *
 * INTEGRIDAD ≠ AUTENTICIDAD: el digest sha256 del manifiesto (MapPackVerifier) da integridad;
 * esta metadata firmada (Ed25519) da AUTENTICIDAD (origen verificable) + anti-rollback +
 * vigencia + compatibilidad de versión. La firma cubre los bytes canónicos de [SignablePackMetadata]
 * (todo MENOS el campo `signature`). NO es TUF completo; adopta sus principios.
 *
 * Requisito: MAP_PACK_SIGNED_METADATA (P0, release-gated). Claves privadas SOLO en tests.
 */
package eco.humanos.android.core.maps

import kotlinx.serialization.Serializable

const val SIGNED_METADATA_SCHEMA_VERSION = 1

/** Versión semántica simple, comparable (para app version y pack version). */
@Serializable
data class SemVer(val major: Int, val minor: Int, val patch: Int) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        /** Parse "x.y.z" (fail-closed: null si no es válido). */
        fun parseOrNull(s: String): SemVer? {
            val parts = s.trim().split(".")
            if (parts.size != 3) return null
            val nums = parts.map { it.toIntOrNull() ?: return null }
            if (nums.any { it < 0 }) return null
            return SemVer(nums[0], nums[1], nums[2])
        }
    }
}

/** Entrada de archivo cubierta por la firma: ruta lógica + tamaño + sha256 del contenido. */
@Serializable
data class SignedFileEntry(val path: String, val size: Long, val sha256: String)

/** Huella real (medida en disco) de un archivo, para verificar contra lo firmado. */
data class FileFingerprint(val size: Long, val sha256: String)

/**
 * Carga FIRMABLE: todos los campos de la metadata EXCEPTO la firma. La firma se calcula sobre
 * los bytes canónicos de esta estructura (MapJson, determinístico). El orden de declaración fija
 * el orden canónico de claves.
 */
@Serializable
data class SignablePackMetadata(
    val schemaVersion: Int,
    val packId: String,
    val packVersion: SemVer,
    val channel: String,
    val issuedAt: String,
    val expiresAt: String?,
    val minimumAppVersion: SemVer,
    val maximumAppVersion: SemVer?,
    val signerKeyId: String,
    val files: List<SignedFileEntry>,
    val manifestHash: String,
    val licenseManifestHash: String,
    val contentType: String,
    val territory: String,
    val previousVersion: SemVer?,
    val rollbackFloor: SemVer,
)

/** Metadata + firma (base64 de la firma Ed25519 sobre los bytes canónicos de [metadata]). */
@Serializable
data class SignedPackMetadata(
    val metadata: SignablePackMetadata,
    /** base64 de la firma; vacío = sin firmar. */
    val signature: String,
)

/** Resultado de la verificación de autenticidad (fail-closed; nunca Valid ante duda). */
sealed interface SignedVerification {
    data object Valid : SignedVerification
    data object Unsigned : SignedVerification
    data object InvalidSignature : SignedVerification
    data object UnknownSigner : SignedVerification
    data object RevokedSigner : SignedVerification
    data object Expired : SignedVerification
    data object NotYetValid : SignedVerification
    data object RollbackDetected : SignedVerification
    data object IncompatibleAppVersion : SignedVerification
    data object ChannelMismatch : SignedVerification
    data class CorruptFile(val path: String) : SignedVerification
    data class MissingFile(val path: String) : SignedVerification
    /** Firma válida pero el hash firmado NO coincide con el contenido real (seam integridad↔autenticidad). */
    data class ContentHashMismatch(val field: String) : SignedVerification
    data class InvalidMetadata(val reason: String) : SignedVerification
}
