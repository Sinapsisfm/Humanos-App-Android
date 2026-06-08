package eco.humanos.android.core.security.vault

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import eco.humanos.android.core.security.SecurityConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production [HumanOSSessionStore] backed by Keystore-protected
 * [EncryptedSharedPreferences] (AndroidX Security / Jetpack Security).
 *
 * The session is serialized to JSON ([kotlinx.serialization]) and the resulting
 * string is stored under a single key, with both keys and values encrypted by a
 * [MasterKey] held in the Android Keystore (AES-256). The bridge token therefore
 * never touches Room, plain preferences, DataStore, or logs.
 *
 * ## Logging policy
 * Only generic lifecycle breadcrumbs are emitted (`session saved`,
 * `session cleared`, `expired session purged`). **No token value, JWT,
 * userId, email, or any field of [HumanOSSession] is ever logged.**
 *
 * ## Threading
 * All Keystore / disk access runs on [Dispatchers.IO]. The
 * [EncryptedSharedPreferences] instance is created lazily and reused.
 *
 * ## Reactivity
 * [observeIsAuthenticated] is driven by an in-memory [MutableStateFlow] that is
 * seeded from disk on first access and updated on every [save] / [clear] /
 * expiry purge, so collectors see state transitions without polling disk.
 */
@Singleton
class EncryptedHumanOSSessionStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : HumanOSSessionStore {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Lazily-created encrypted prefs. Construction does Keystore + disk I/O, so
     * it is only touched from suspend functions on [Dispatchers.IO] (and the
     * one-time flow seeding, which also runs under a lock).
     */
    @Volatile
    private var cachedPrefs: SharedPreferences? = null

    /** Mirrors "a non-expired session exists"; drives [observeIsAuthenticated]. */
    private val authState = MutableStateFlow(false)

    /** Guards a single disk-seed of [authState] for the process lifetime. */
    private val seeded = AtomicBoolean(false)

    private fun prefs(): SharedPreferences {
        cachedPrefs?.let { return it }
        return synchronized(this) {
            cachedPrefs ?: createEncryptedPrefs().also { cachedPrefs = it }
        }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            SecurityConstants.ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /** Decode the stored payload, or null when absent / corrupt (purging corrupt data). */
    private fun readStored(): HumanOSSession? {
        val raw = prefs().getString(KEY_SESSION, null) ?: return null
        return runCatching {
            json.decodeFromString(HumanOSSession.serializer(), raw)
        }.getOrElse {
            prefs().edit().remove(KEY_SESSION).commit()
            Log.w(TAG, "stored session unreadable; purged")
            null
        }
    }

    override suspend fun save(session: HumanOSSession) = withContext(Dispatchers.IO) {
        val payload = json.encodeToString(HumanOSSession.serializer(), session)
        prefs().edit().putString(KEY_SESSION, payload).commit()
        seeded.set(true)
        authState.value = session.isValidAt(System.currentTimeMillis())
        Log.i(TAG, "session saved")
        Unit
    }

    override suspend fun get(): HumanOSSession? = withContext(Dispatchers.IO) {
        val session = readStored()
        if (session == null) {
            seeded.set(true)
            authState.value = false
            return@withContext null
        }
        if (!session.isValidAt(System.currentTimeMillis())) {
            // Expired: behave as if absent and drop it from disk.
            prefs().edit().remove(KEY_SESSION).commit()
            seeded.set(true)
            authState.value = false
            Log.i(TAG, "expired session purged")
            return@withContext null
        }
        seeded.set(true)
        authState.value = true
        session
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        prefs().edit().remove(KEY_SESSION).commit()
        seeded.set(true)
        authState.value = false
        Log.i(TAG, "session cleared")
        Unit
    }

    override fun observeIsAuthenticated(): Flow<Boolean> {
        // Seed once from disk so the first collector sees the real state.
        if (seeded.compareAndSet(false, true)) {
            synchronized(this) {
                val current = readStored()
                authState.value = current != null && current.isValidAt(System.currentTimeMillis())
            }
        }
        return authState.asStateFlow()
    }

    private companion object {
        const val TAG = "HumanOSVault"
        const val KEY_SESSION = "humanos_session_json"
    }
}
