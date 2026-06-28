/**
 * core-maps / MapLoad.kt
 *
 * Frontera IO ASÍNCRONA para carga de packs (Fase D). El dominio expone un puerto `suspend`
 * (`SuspendMapReader`) con dispatcher INYECTADO — el core NO hardcodea Dispatchers.IO ni usa
 * GlobalScope. Una lectura Room síncrona (bloqueante) se mueve fuera del hilo llamante en el
 * adapter. Estados tipados (no una excepción genérica). Cancelable y determinístico en tests.
 */
package eco.humanos.android.core.maps

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/** Estado tipado de carga de un pack. Un estado por situación (no una excepción para todo). */
sealed interface MapLoadState {
    data object Idle : MapLoadState
    data object Loading : MapLoadState
    data class Ready(val pack: OfflineMapPack) : MapLoadState
    data object Empty : MapLoadState
    data object Missing : MapLoadState
    data class Invalid(val reason: String) : MapLoadState
    data class Corrupt(val reason: String) : MapLoadState
    data object Expired : MapLoadState
    data object Cancelled : MapLoadState
    data class Error(val reason: String) : MapLoadState

    companion object {
        /** Puente E→D: mapea un veredicto de autenticidad a un estado de carga. */
        fun fromSigned(v: SignedVerification): MapLoadState = when (v) {
            is SignedVerification.Valid -> Loading // autenticidad OK; la carga de contenido continúa
            is SignedVerification.Expired -> Expired
            is SignedVerification.MissingFile -> Missing
            is SignedVerification.CorruptFile -> Corrupt("archivo: ${v.path}")
            is SignedVerification.ContentHashMismatch -> Corrupt("hash firmado != real: ${v.field}")
            is SignedVerification.InvalidMetadata -> Invalid(v.reason)
            else -> Invalid(v::class.simpleName ?: "no-autenticado")
        }
    }
}

/** Puerto de lectura asíncrona (suspend). El adapter decide el hilo (dispatcher inyectado). */
interface SuspendMapReader {
    suspend fun pack(packId: String): OfflineMapPack?
}

/** Adapter por defecto: envuelve un MapRepository SÍNCRONO y ejecuta la lectura en `dispatcher`. */
class DispatchingMapReader(
    private val repo: MapRepository,
    private val dispatcher: CoroutineDispatcher,
) : SuspendMapReader {
    override suspend fun pack(packId: String): OfflineMapPack? =
        withContext(dispatcher) { repo.pack(packId) }
}

/** Caso de uso de carga: lee (async) + verifica integridad → estado tipado. Fail-closed. */
class MapLoadUseCase(private val reader: SuspendMapReader) {

    suspend fun loadPack(packId: String): MapLoadState {
        return try {
            val pack = reader.pack(packId) ?: return MapLoadState.Missing
            coroutineContext.ensureActive() // cooperativo: respeta cancelación tras la lectura
            when (val v = MapPackVerifier.verify(pack)) {
                is PackVerification.Valid ->
                    if (pack.routes.isEmpty() && pack.waypoints.isEmpty()) MapLoadState.Empty
                    else MapLoadState.Ready(pack)
                is PackVerification.Corrupt -> MapLoadState.Corrupt("hash")
                is PackVerification.SizeMismatch -> MapLoadState.Corrupt("size")
                is PackVerification.SchemaMismatch -> MapLoadState.Invalid("schema ${v.actual}")
                is PackVerification.Invalid -> MapLoadState.Invalid(v.reason)
            }
        } catch (c: CancellationException) {
            throw c // nunca tragar cancelación
        } catch (t: Throwable) {
            MapLoadState.Error(t.message ?: t::class.simpleName ?: "error")
        }
    }
}

/**
 * Coordinador "last-request-wins": cada `load` cancela el job en vuelo y publica el estado en un
 * StateFlow. Sin GlobalScope (el scope se inyecta). Útil para un ViewModel que recarga.
 */
class MapLoadCoordinator(
    private val scope: CoroutineScope,
    private val useCase: MapLoadUseCase,
) {
    private val _state = MutableStateFlow<MapLoadState>(MapLoadState.Idle)
    val state: StateFlow<MapLoadState> = _state.asStateFlow()
    private var job: Job? = null

    suspend fun load(packId: String) {
        job?.cancelAndJoin() // last-request-wins: cancela la carga anterior
        _state.value = MapLoadState.Loading
        job = scope.launch {
            val result = useCase.loadPack(packId)
            // si fui cancelado por una request posterior, no piso su estado
            coroutineContext.ensureActive()
            _state.value = result
        }
    }
}
