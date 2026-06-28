/**
 * core-outdoor / presentation / PackingPresentation.kt
 *
 * Helpers de presentación PUROS (sin Android) que un ViewModel/UI consume: agrupar por
 * categoría, separar compras de carga (CAMP-019), progreso de empaque, agrupar por
 * responsable, resumen por clasificación. Determinístico y testeable.
 */
package eco.humanos.android.core.outdoor.presentation

import eco.humanos.android.core.outdoor.domain.ItemClassification
import eco.humanos.android.core.outdoor.domain.OutingPackingItem
import eco.humanos.android.core.outdoor.domain.PackingCategory

data class CategoryGroup(
    val category: PackingCategory,
    val items: List<OutingPackingItem>,
)

data class PackingProgress(
    val total: Int,
    val packed: Int,
) {
    val ratio: Double get() = if (total == 0) 0.0 else packed.toDouble() / total
}

data class PackingSummary(
    val active: Int,
    val mandatory: Int,
    val recommended: Int,
    val optional: Int,
    val mandatoryPending: Int, // obligatorios aún no empacados
)

object PackingPresentation {

    /** Ítems activos (no removidos, cantidad > 0), preservando el orden de entrada. */
    fun active(items: List<OutingPackingItem>): List<OutingPackingItem> =
        items.filter { !it.removed && it.quantity > 0 }

    /** Agrupa por categoría, manteniendo el orden ya establecido por el engine. */
    fun groupByCategory(items: List<OutingPackingItem>): List<CategoryGroup> {
        val groups = LinkedHashMap<PackingCategory, MutableList<OutingPackingItem>>()
        for (it in active(items)) groups.getOrPut(it.category) { mutableListOf() }.add(it)
        return groups.entries.map { CategoryGroup(it.key, it.value) }
    }

    /** Lista de compras: activos aún no comprados (separada de la carga, CAMP-019). */
    fun pendingPurchases(items: List<OutingPackingItem>): List<OutingPackingItem> =
        active(items).filter { !it.bought }

    /** Progreso de empaque sobre ítems activos. */
    fun progress(items: List<OutingPackingItem>): PackingProgress {
        val act = active(items)
        return PackingProgress(total = act.size, packed = act.count { it.packed })
    }

    /** Agrupa por responsable (null = sin asignar), claves ordenadas de forma estable. */
    fun byResponsible(items: List<OutingPackingItem>): Map<String?, List<OutingPackingItem>> {
        val map = LinkedHashMap<String?, MutableList<OutingPackingItem>>()
        for (it in active(items)) map.getOrPut(it.responsibleId) { mutableListOf() }.add(it)
        return map
    }

    fun summary(items: List<OutingPackingItem>): PackingSummary {
        val act = active(items)
        return PackingSummary(
            active = act.size,
            mandatory = act.count { it.classification == ItemClassification.MANDATORY },
            recommended = act.count { it.classification == ItemClassification.RECOMMENDED },
            optional = act.count { it.classification == ItemClassification.OPTIONAL },
            mandatoryPending = act.count { it.classification == ItemClassification.MANDATORY && !it.packed },
        )
    }
}
