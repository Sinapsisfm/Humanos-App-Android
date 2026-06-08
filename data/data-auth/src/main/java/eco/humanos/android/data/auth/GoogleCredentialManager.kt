package eco.humanos.android.data.auth

import android.content.Context

/**
 * Produces a Google ID token via the modern Jetpack Credential Manager flow,
 * which [AuthRepository.signInWithGoogle] then exchanges for a Firebase session.
 * This replaces the deprecated `GoogleSignInClient` from play-services-auth.
 *
 * Modeled as an interface so the UI layer depends on an abstraction (and can be
 * unit-tested with a fake). The real implementation is [GoogleCredentialManagerImpl].
 */
interface GoogleCredentialManager {

    /**
     * Launches the Google credential picker and returns the resulting Google
     * ID token.
     *
     * @param activityContext an `Activity` context — required because the flow
     *   shows UI. Passing the application context will throw at runtime.
     * @return [Result.success] with the raw ID token, or [Result.failure] if the
     *   user cancels, no credential is available, or parsing fails.
     */
    suspend fun getGoogleIdToken(activityContext: Context): Result<String>
}
