/**
 * core-maps / PackCrypto.kt
 *
 * Primitivas de firma/verificación Ed25519 para packs (Signed Metadata V1). Usa `java.security`
 * (estándar, JDK 15+ / Android API 33+); NO criptografía casera, sin dependencias nuevas.
 *
 * CAVEAT de productivización: `java.security` provee Ed25519 desde Android API 33; minSdk del app
 * es 26. El POC corre y se testea en JVM (JDK 21). Para productivo en API<33 habrá que usar una
 * librería Ed25519 dedicada (gate futuro, NO en este POC).
 *
 * La app/dispositivo SOLO necesita CLAVES PÚBLICAS (verificación). La firma (privada) ocurre en
 * build/servidor; aquí `sign` es una utilidad que recibe la clave privada por parámetro — NUNCA
 * se embebe una clave privada en el código ni en el repo productivo. Las claves privadas de test
 * se generan en memoria dentro de los tests.
 */
package eco.humanos.android.core.maps

import kotlinx.serialization.encodeToString
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

object PackCrypto {

    const val ALGORITHM = "Ed25519"

    /** Bytes canónicos firmables = serialización determinística de la metadata (sin firma). */
    fun canonicalBytes(meta: SignablePackMetadata): ByteArray =
        MapJson.encodeToString(meta).toByteArray(Charsets.UTF_8)

    /** Genera un par de claves Ed25519. Uso dev/test (la clave privada nunca se persiste en repo). */
    fun generateKeyPair(): KeyPair = KeyPairGenerator.getInstance(ALGORITHM).generateKeyPair()

    /** keyId estable = sha256 hex del encoding X.509 de la clave pública. */
    fun keyId(publicKey: PublicKey): String =
        MessageDigest.getInstance("SHA-256").digest(publicKey.encoded)
            .joinToString("") { "%02x".format(it) }

    fun encodePublicKey(publicKey: PublicKey): String =
        Base64.getEncoder().encodeToString(publicKey.encoded)

    fun decodePublicKey(base64: String): PublicKey {
        val der = Base64.getDecoder().decode(base64)
        return KeyFactory.getInstance(ALGORITHM).generatePublic(X509EncodedKeySpec(der))
    }

    /** Firma (build/servidor): clave privada por parámetro, nunca embebida. Devuelve base64. */
    fun sign(meta: SignablePackMetadata, privateKey: PrivateKey): String {
        val sig = Signature.getInstance(ALGORITHM)
        sig.initSign(privateKey)
        sig.update(canonicalBytes(meta))
        return Base64.getEncoder().encodeToString(sig.sign())
    }

    /**
     * Verifica la firma con la clave pública. La verificación criptográfica de la librería evita
     * comparaciones de bytes inseguras. Devuelve false ante cualquier error (fail-closed).
     */
    fun verifySignature(meta: SignablePackMetadata, signatureB64: String, publicKey: PublicKey): Boolean =
        try {
            val sig = Signature.getInstance(ALGORITHM)
            sig.initVerify(publicKey)
            sig.update(canonicalBytes(meta))
            sig.verify(Base64.getDecoder().decode(signatureB64))
        } catch (t: Throwable) {
            false
        }
}

/** Resolución de un signerKeyId contra el almacén de confianza. */
sealed interface TrustResolution {
    data class Trusted(val publicKey: PublicKey) : TrustResolution
    data object Revoked : TrustResolution
    data object Unknown : TrustResolution
}

/**
 * Almacén de confianza: claves activas + anteriores (solapamiento durante rotación) + revocadas.
 * El dispositivo solo contiene claves públicas. La revocación tiene prioridad sobre todo.
 */
data class TrustStore(
    val active: Map<String, PublicKey>,
    val previous: Map<String, PublicKey> = emptyMap(),
    val revoked: Set<String> = emptySet(),
) {
    fun resolve(keyId: String): TrustResolution = when {
        keyId in revoked -> TrustResolution.Revoked
        active[keyId] != null -> TrustResolution.Trusted(active.getValue(keyId))
        previous[keyId] != null -> TrustResolution.Trusted(previous.getValue(keyId))
        else -> TrustResolution.Unknown
    }
}
