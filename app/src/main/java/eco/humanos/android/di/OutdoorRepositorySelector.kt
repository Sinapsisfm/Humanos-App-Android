/**
 * app / di / OutdoorRepositorySelector.kt
 *
 * Selección PURA del repositorio Outdoor (testeable en JVM, sin Hilt/Android). El módulo
 * Hilt delega aquí. Providers perezosos: el no elegido NO se construye (Room no abre la DB
 * si no está activo). El default es el repositorio actual (in-memory); Room solo si el flag
 * interno está activo.
 */
package eco.humanos.android.di

import eco.humanos.android.core.outdoor.repository.OutdoorRepository

fun selectOutdoorRepository(
    roomEnabled: Boolean,
    room: () -> OutdoorRepository,
    inMemory: () -> OutdoorRepository,
): OutdoorRepository = if (roomEnabled) room() else inMemory()
