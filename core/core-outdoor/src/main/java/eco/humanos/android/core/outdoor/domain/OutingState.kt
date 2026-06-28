/**
 * core-outdoor / domain / OutingState.kt
 *
 * Máquina de estados de la Salida (OUT-005): transiciones válidas; rechazo explícito
 * de transiciones inválidas. Determinístico y puro.
 */
package eco.humanos.android.core.outdoor.domain

object OutingStateMachine {
    private val allowed: Map<OutingStatus, Set<OutingStatus>> = mapOf(
        OutingStatus.DRAFT to setOf(OutingStatus.PREPARED, OutingStatus.CANCELLED, OutingStatus.ARCHIVED),
        OutingStatus.PREPARED to setOf(OutingStatus.ACTIVE, OutingStatus.DRAFT, OutingStatus.CANCELLED, OutingStatus.ARCHIVED),
        OutingStatus.ACTIVE to setOf(OutingStatus.PAUSED, OutingStatus.INCIDENT, OutingStatus.COMPLETED),
        OutingStatus.PAUSED to setOf(OutingStatus.ACTIVE, OutingStatus.INCIDENT, OutingStatus.COMPLETED),
        OutingStatus.INCIDENT to setOf(OutingStatus.ACTIVE, OutingStatus.COMPLETED),
        OutingStatus.COMPLETED to setOf(OutingStatus.ARCHIVED),
        OutingStatus.CANCELLED to setOf(OutingStatus.ARCHIVED),
        OutingStatus.ARCHIVED to emptySet(),
    )

    fun canTransition(from: OutingStatus, to: OutingStatus): Boolean =
        allowed[from]?.contains(to) == true

    /** Devuelve el nuevo estado o lanza si es inválido. */
    fun assertTransition(from: OutingStatus, to: OutingStatus): OutingStatus {
        require(canTransition(from, to)) { "Transición de salida inválida: $from → $to" }
        return to
    }
}

class InvalidOutingTransitionException(val from: OutingStatus, val to: OutingStatus) :
    IllegalArgumentException("Transición de salida inválida: $from → $to")
