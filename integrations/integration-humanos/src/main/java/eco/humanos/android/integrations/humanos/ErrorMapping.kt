package eco.humanos.android.integrations.humanos

import retrofit2.HttpException
import java.io.IOException
import java.net.UnknownHostException

/**
 * Turn a network/exchange failure into a short, human-readable diagnostic the
 * app can show the user (and Felipe can act on) instead of swallowing it.
 *
 * Lives in `integration-humanos` because it touches Retrofit's [HttpException];
 * callers in other modules (e.g. `data-auth`) just invoke the function.
 *
 * - HTTP errors → `"HTTP <code> — <server message>"` (the JSON `error` field
 *   when present, e.g. the exchange's `INVALID_FIREBASE_TOKEN` detail).
 * - DNS / offline → a connectivity hint.
 * - Anything else → the exception message.
 */
fun Throwable.toHumanosErrorMessage(): String = when (this) {
    is HttpException -> {
        val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
        val detail = body?.let(::extractServerError)
        "HTTP ${code()}" + if (!detail.isNullOrBlank()) " — $detail" else ""
    }

    is UnknownHostException -> "Sin conexión: no se pudo contactar humanos.eco"
    is IOException -> "Error de red: ${message ?: "tiempo de espera agotado"}"
    else -> message ?: (this::class.simpleName ?: "Error desconocido")
}

/** Pull the `error` field out of a `{ "error": "...", "code": "..." }` body. */
private fun extractServerError(json: String): String? =
    runCatching {
        Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
    }.getOrNull() ?: json.take(160).ifBlank { null }
