# Security and Privacy Architecture

> humanOS Native Android -- Security and Privacy Design
> Last updated: 2026-06-06

## Core Principles

1. **Local-first**: Data lives on the device by default. Sync is opt-in and explicit.
2. **No sensitive data to cloud by default**: Health records, vault items, and biometric data never leave the device unless the user explicitly enables sync for a specific category.
3. **Defense in depth**: Security is enforced at multiple layers (model, repository, network, UI) so a failure in one layer does not expose data.
4. **Audit everything**: All security-relevant operations produce `TraceEvent` records for forensic review.

## Key Storage -- Android Keystore

All cryptographic keys are stored in the Android Keystore, which provides hardware-backed (or TEE-backed) storage on supported devices.

### Keys Managed

| Key Alias | Purpose | Algorithm | Created When |
|---|---|---|---|
| `humanos_vault_master` | Encrypts VAULT-level data at rest | AES-256-GCM | First VAULT item created |
| `humanos_token_enc` | Encrypts auth tokens in EncryptedSharedPreferences | AES-256-SIV | App first launch |
| `humanos_biometric_gate` | Unlocks VAULT access, requires biometric auth | AES-256-GCM (user auth bound) | Biometric enrollment |

### Key Properties

- Keys are **device-bound** -- they cannot be extracted, even with root.
- Keys are **non-exportable** -- `setIsStrongBoxBacked(true)` when StrongBox is available.
- The `humanos_biometric_gate` key requires `setUserAuthenticationRequired(true)` with `setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG)`.
- Key attestation is verified on first creation to detect compromised Keystore implementations.

## Token Storage -- EncryptedSharedPreferences

Auth tokens (Firebase ID token, refresh token, bridge JWT) are stored using `EncryptedSharedPreferences` from the AndroidX Security library.

```kotlin
val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "humanos_secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

### Stored Values

| Key | Value | TTL |
|---|---|---|
| `firebase_id_token` | Firebase ID token (JWT) | 1 hour (Firebase-managed) |
| `firebase_refresh_token` | Firebase refresh token | Long-lived (Firebase-managed) |
| `bridge_jwt` | HumanOS identity bridge JWT | 15 minutes |
| `last_auth_timestamp` | Epoch millis of last successful auth | N/A |

Tokens are **never** stored in plain SharedPreferences, DataStore, Room, or logs.

## Privacy Levels

```kotlin
enum class PrivacyLevel {
    PUBLIC,   // Syncs to server, visible in shared/team views
    PRIVATE,  // Syncs to server, excluded from shared/team views
    VAULT,    // Never syncs, never leaves device, biometric required
}
```

### VAULT Enforcement

VAULT is the highest privacy level. It provides a local-only encrypted container for the most sensitive data.

| Property | Behavior |
|---|---|
| Storage | Encrypted with `humanos_vault_master` key (AES-256-GCM) before writing to Room |
| Sync | Excluded from all network operations. Repository layer strips VAULT items before serialization. |
| Access | Requires biometric authentication via `BiometricPrompt`. Session lasts 5 minutes (configurable). |
| Backup | Excluded from Android Auto Backup and Google Drive backup via `android:allowBackup="false"` for VAULT tables and manifest `<full-backup-content>` exclude rules. |
| Screenshots | VAULT screens set `FLAG_SECURE` to prevent screenshots and screen recording. |
| Recents | VAULT screens use a blank thumbnail in the recent apps screen. |

### Encryption Flow for VAULT Data

```
User creates VAULT item
    |
BiometricPrompt → unlocks humanos_biometric_gate key
    |
Generate per-item IV (12 bytes, SecureRandom)
    |
AES-256-GCM encrypt(plaintext, humanos_vault_master, IV)
    |
Store: IV || ciphertext || auth_tag in Room BLOB column
    |
Mark privacy_level = VAULT on the entity
```

### Decryption Flow

```
User navigates to VAULT item
    |
BiometricPrompt → unlocks humanos_biometric_gate key
    |
Read IV || ciphertext || auth_tag from Room
    |
AES-256-GCM decrypt(ciphertext, humanos_vault_master, IV, auth_tag)
    |
Display plaintext in UI (FLAG_SECURE active)
    |
Clear plaintext from memory after 5-minute timeout
```

## Network Security

| Control | Implementation |
|---|---|
| TLS | All network calls require TLS 1.2+. Certificate pinning for `humanos.eco` and QueBot domains via OkHttp `CertificatePinner`. |
| Auth headers | Bearer token injected by OkHttp `Interceptor`. Never logged. |
| Token refresh | 401 responses trigger automatic token refresh via `Authenticator`. If refresh fails, user is redirected to sign-in. |
| Request signing | Not implemented in Phase 1. Planned for Phase 3 (HMAC-SHA256 on request body). |
| Network security config | `res/xml/network_security_config.xml` disables cleartext traffic and pins CA certificates. |

### OkHttp Interceptor Chain

```
Request
  → AuthInterceptor (adds Bearer token)
  → LoggingInterceptor (redacts auth headers, truncates bodies)
  → CertificatePinner (TLS pin validation)
  → Network
```

## Audit Trail -- TraceEvent

Every security-relevant action creates a `TraceEvent` stored in Room.

```kotlin
data class TraceEvent(
    val id: String,
    val timestamp: Instant,
    val eventType: TraceEventType,
    val actorId: String?,          // User ID or "SYSTEM"
    val targetType: String?,       // Entity type affected
    val targetId: String?,         // Entity ID affected
    val detail: String?,           // Human-readable description
    val metadata: Map<String, String>,
)

enum class TraceEventType {
    AUTH_LOGIN,
    AUTH_LOGOUT,
    AUTH_TOKEN_REFRESH,
    AUTH_BIOMETRIC_SUCCESS,
    AUTH_BIOMETRIC_FAILURE,
    VAULT_ACCESS,
    VAULT_ITEM_CREATED,
    VAULT_ITEM_DELETED,
    PRIVACY_LEVEL_CHANGED,
    SYNC_STARTED,
    SYNC_COMPLETED,
    SYNC_FAILED,
    DATA_EXPORTED,
    DATA_DELETED,
    PERMISSION_GRANTED,
    PERMISSION_DENIED,
}
```

### Retention

- TraceEvents are retained locally for 90 days.
- VAULT-related TraceEvents are never synced (they reference VAULT item IDs).
- Non-VAULT TraceEvents can optionally sync to HumanOS for cross-device audit (Phase 2).

## Application Security Hardening

| Measure | Status |
|---|---|
| `android:allowBackup="false"` | Phase 1 |
| `android:usesCleartextTraffic="false"` | Phase 1 |
| ProGuard/R8 obfuscation | Phase 1 |
| Root/emulator detection (SafetyNet/Play Integrity) | Phase 2 |
| Tapjacking protection (`filterTouchesWhenObscured`) | Phase 1 |
| Content provider export disabled | Phase 1 |
| `FLAG_SECURE` on VAULT screens | Phase 1 |
| Certificate pinning | Phase 1 |
| Dependency vulnerability scanning (Dependabot) | Phase 1 |

## Data Classification

| Classification | Examples | Storage | Sync | Encryption at Rest |
|---|---|---|---|---|
| Public | App settings, theme preference | DataStore | Yes | No (non-sensitive) |
| Internal | Task titles, project names, captures | Room | Yes (PRIVATE default) | Device encryption only |
| Confidential | Health data, medical records, financial notes | Room | Optional (user choice) | AES-256-GCM |
| Restricted (VAULT) | Passwords, legal documents, personal journals | Room (encrypted BLOB) | Never | AES-256-GCM + biometric gate |

## Incident Response

If a security issue is detected at runtime:

1. **Token compromise**: Clear all tokens, force re-authentication, log `AUTH_TOKEN_REFRESH` failure.
2. **Keystore failure**: Disable VAULT features, notify user, do not attempt fallback to software keys.
3. **Certificate pin failure**: Block all network calls to the affected domain, log `SYNC_FAILED`, notify user.
4. **Root detection (Phase 2)**: Warn user, disable VAULT features, continue non-VAULT operations.
