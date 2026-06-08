package eco.humanos.android.core.model.common

/**
 * Compile-time configuration for the HumanOS backend integration.
 *
 * This is the single switch that flips the app from in-memory fakes to the
 * real HumanOS REST API. It lives in `core-model` so every layer (DI modules,
 * repositories, gateways) can read it without taking a dependency on a
 * networking module.
 *
 * ## Going live (flag-flip steps)
 * 1. Confirm `POST /api/auth/mobile/exchange` is deployed to humanos.eco
 *    (see `docs/03_INTEGRATIONS/MOBILE_AUTH_ENDPOINT_SPEC.md`).
 * 2. Flip [USE_REAL_HUMANOS_AUTH] to `true`.
 * 3. In `HumanosModule`, swap the `@Binds` from `FakeHumanosGateway` to
 *    `RealHumanosGateway` (the swap point is documented inline there).
 * 4. Rebuild. `FirebaseAuthRepository` will then perform the real Firebase ->
 *    HumanOS bridge-token exchange after Google Sign-In.
 *
 * While the flag is `false` (the default) the app behaves exactly as before:
 * fakes everywhere, `humanosToken = null`, no network calls to HumanOS.
 */
object IntegrationConfig {

    /**
     * Flip to `true` once `POST /api/auth/mobile/exchange` is deployed to
     * humanos.eco. Controls whether [eco.humanos.android] performs the real
     * Firebase -> HumanOS bridge token exchange (and, paired with the
     * `HumanosModule` swap, whether real network gateways are used).
     *
     * Default `false` keeps the mock auth path and fake gateways.
     */
    const val USE_REAL_HUMANOS_AUTH: Boolean = true

    /**
     * Base URL of the HumanOS REST API. Must end with a trailing slash so
     * Retrofit resolves relative endpoint paths (e.g. `"tasks"`) correctly.
     */
    const val HUMANOS_BASE_URL: String = "https://www.humanos.eco/api/"
}
