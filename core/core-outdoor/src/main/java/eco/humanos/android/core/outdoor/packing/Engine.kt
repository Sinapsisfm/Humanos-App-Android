/**
 * core-outdoor / packing / Engine.kt
 *
 * Packing & Preparation Engine determinístico (PACK-001..014). Determinismo (sin reloj
 * ni azar, orden estable por code-unit), trazabilidad (sourceRuleId), fusión COHERENTE
 * por key (PACK-005, ganador determinístico), diff (PACK-014), edición humana (PACK-009).
 */
package eco.humanos.android.core.outdoor.packing

import eco.humanos.android.core.outdoor.domain.CampingPlan
import eco.humanos.android.core.outdoor.domain.GeneratedItem
import eco.humanos.android.core.outdoor.domain.ItemClassification
import eco.humanos.android.core.outdoor.domain.ItemOrigin
import eco.humanos.android.core.outdoor.domain.OutingPackingItem
import eco.humanos.android.core.outdoor.domain.PackingCategory
import eco.humanos.android.core.outdoor.domain.PackingDiff
import eco.humanos.android.core.outdoor.domain.PackingInput
import eco.humanos.android.core.outdoor.domain.PreparationTask

object PackingEngine {

    private val CATEGORY_ORDER = listOf(
        PackingCategory.SHELTER, PackingCategory.SLEEP, PackingCategory.CLOTHING,
        PackingCategory.CHILD, PackingCategory.COOKING, PackingCategory.FOOD,
        PackingCategory.WATER, PackingCategory.LIGHTING, PackingCategory.HYGIENE,
        PackingCategory.SUN_RAIN, PackingCategory.SAFETY, PackingCategory.TOOLS,
        PackingCategory.ENTERTAINMENT, PackingCategory.DOCUMENTS, PackingCategory.OTHER,
    )

    private fun rank(c: ItemClassification): Int = when (c) {
        ItemClassification.MANDATORY -> 3
        ItemClassification.RECOMMENDED -> 2
        ItemClassification.OPTIONAL -> 1
    }

    private fun categoryIndex(c: PackingCategory): Int =
        CATEGORY_ORDER.indexOf(c).let { if (it == -1) CATEGORY_ORDER.size else it }

    /** Comparador estable por code-unit (determinístico entre entornos). */
    private val itemComparator: Comparator<GeneratedItem> =
        compareBy({ categoryIndex(it.category) }, { it.name }, { it.key })

    /**
     * Fusión coherente y order-independent de dos ítems con la misma key (PACK-005):
     * el ganador (clasif. más fuerte → mayor cantidad → menor sourceRuleId) aporta TODOS
     * los campos descriptivos; la cantidad final es el máximo.
     */
    private fun merge(a: GeneratedItem, b: GeneratedItem): GeneratedItem {
        val ra = rank(a.classification)
        val rb = rank(b.classification)
        var winner = a
        if (rb > ra) winner = b
        else if (rb == ra) {
            if (b.quantity > a.quantity) winner = b
            else if (b.quantity == a.quantity && b.sourceRuleId < a.sourceRuleId) winner = b
        }
        return winner.copy(quantity = maxOf(a.quantity, b.quantity))
    }

    fun generateItems(
        input: PackingInput,
        rules: List<PackingRule> = CAMPING_RULES,
    ): List<GeneratedItem> {
        val byKey = LinkedHashMap<String, GeneratedItem>()
        for (rule in rules) {
            if (!rule.applies(input)) continue
            val candidate = GeneratedItem(
                key = rule.id,
                name = rule.name,
                category = rule.category,
                classification = rule.classification,
                quantity = maxOf(1, rule.quantity(input)),
                unit = rule.unit,
                sourceRuleId = rule.id,
                explanation = rule.explain(input),
            )
            val existing = byKey[candidate.key]
            byKey[candidate.key] = if (existing == null) candidate else merge(existing, candidate)
        }
        return byKey.values.sortedWith(itemComparator)
    }

    fun generatePrepTasks(
        input: PackingInput,
        taskRules: List<PrepTaskRule> = CAMPING_PREP_TASKS,
    ): List<PreparationTask> =
        taskRules.filter { it.applies(input) }
            .map { PreparationTask(id = it.id, title = it.title, sourceRuleId = it.id) }

    fun buildCampingPlan(
        input: PackingInput,
        rules: List<PackingRule> = CAMPING_RULES,
        taskRules: List<PrepTaskRule> = CAMPING_PREP_TASKS,
    ): CampingPlan {
        val items = generateItems(input, rules).map { g ->
            OutingPackingItem(
                key = g.key, name = g.name, category = g.category,
                classification = g.classification, quantity = g.quantity, unit = g.unit,
                sourceRuleId = g.sourceRuleId, explanation = g.explanation, origin = ItemOrigin.RULE,
            )
        }
        return CampingPlan(RULESET_VERSION, items, generatePrepTasks(input, taskRules))
    }

    fun computeDiff(prev: List<GeneratedItem>, next: List<GeneratedItem>): PackingDiff {
        val prevByKey = prev.associateBy { it.key }
        val nextByKey = next.associateBy { it.key }
        val added = ArrayList<GeneratedItem>()
        val removed = ArrayList<GeneratedItem>()
        val changed = ArrayList<PackingDiff.Changed>()
        for ((key, after) in nextByKey) {
            val before = prevByKey[key]
            if (before == null) added.add(after)
            else if (before.quantity != after.quantity || before.classification != after.classification) {
                changed.add(PackingDiff.Changed(key, before, after))
            }
        }
        for ((key, before) in prevByKey) if (!nextByKey.containsKey(key)) removed.add(before)
        return PackingDiff(
            added.sortedWith(itemComparator),
            removed.sortedWith(itemComparator),
            changed.sortedWith(compareBy({ categoryIndex(it.after.category) }, { it.after.name }, { it.after.key })),
        )
    }

    // ── Edición humana (PACK-009) ──
    sealed interface UserEdit {
        data class Remove(val key: String, val reason: String? = null) : UserEdit
        data class SetQuantity(val key: String, val quantity: Int) : UserEdit
        data class SetResponsible(val key: String, val responsibleId: String?) : UserEdit
        data class SetPacked(val key: String, val packed: Boolean) : UserEdit
        data class SetBought(val key: String, val bought: Boolean) : UserEdit
        data class AddManual(val name: String, val category: PackingCategory, val quantity: Int = 1, val unit: String = "unidad") : UserEdit
    }

    fun applyUserEdits(
        items: List<OutingPackingItem>,
        edits: List<UserEdit>,
    ): List<OutingPackingItem> {
        val byKey = LinkedHashMap<String, OutingPackingItem>()
        for (it in items) byKey[it.key] = it
        for (edit in edits) {
            when (edit) {
                is UserEdit.Remove -> byKey[edit.key]?.let { byKey[edit.key] = it.copy(removed = true, removalReason = edit.reason) }
                is UserEdit.SetQuantity -> byKey[edit.key]?.let { byKey[edit.key] = it.copy(quantity = maxOf(1, edit.quantity)) }
                is UserEdit.SetResponsible -> byKey[edit.key]?.let { byKey[edit.key] = it.copy(responsibleId = edit.responsibleId) }
                is UserEdit.SetPacked -> byKey[edit.key]?.let { byKey[edit.key] = it.copy(packed = edit.packed) }
                is UserEdit.SetBought -> byKey[edit.key]?.let { byKey[edit.key] = it.copy(bought = edit.bought) }
                is UserEdit.AddManual -> {
                    val trimmed = edit.name.trim()
                    if (trimmed.isNotEmpty()) {
                        val key = "manual:${trimmed.lowercase()}"
                        byKey[key] = OutingPackingItem(
                            key = key, name = trimmed, category = edit.category,
                            classification = ItemClassification.OPTIONAL, quantity = maxOf(1, edit.quantity),
                            unit = edit.unit, sourceRuleId = "user.manual",
                            explanation = "Agregado manualmente por el usuario.", origin = ItemOrigin.MANUAL,
                        )
                    }
                }
            }
        }
        return byKey.values.sortedWith(compareBy({ categoryIndex(it.category) }, { it.name }, { it.key }))
    }
}
