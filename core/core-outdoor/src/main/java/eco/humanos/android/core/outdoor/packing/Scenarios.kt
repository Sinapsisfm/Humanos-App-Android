/**
 * core-outdoor / packing / Scenarios.kt
 *
 * Composición de escenarios (D-001 dominio único, SCN-003 escenarios combinados). Una
 * salida primaria de camping puede agregar un escenario secundario (p.ej. trekking) sin
 * duplicar ítems compartidos (PACK-005): la fusión por key del engine los unifica.
 *
 * TREKKING_RULES comparte deliberadamente keys con camping (safety.first_aid,
 * water.treatment) para demostrar que NO se duplican al componer.
 */
package eco.humanos.android.core.outdoor.packing

import eco.humanos.android.core.outdoor.domain.ItemClassification
import eco.humanos.android.core.outdoor.domain.PackingCategory
import eco.humanos.android.core.outdoor.domain.PackingInput
import eco.humanos.android.core.outdoor.domain.ScenarioKind

private fun people(i: PackingInput) = i.participants.size

/** Capa de trekking (secundaria). Reusa keys compartidas para probar la no-duplicación. */
val TREKKING_RULES: List<PackingRule> = listOf(
    PackingRule("trekking.backpack", PackingCategory.TOOLS, ItemClassification.MANDATORY, "Mochila de trekking", "mochila",
        applies = { true }, quantity = { people(it) }, explain = { "1 mochila por persona para la caminata." }),
    PackingRule("trekking.poles", PackingCategory.TOOLS, ItemClassification.RECOMMENDED, "Bastones de trekking", "par",
        applies = { true }, quantity = { people(it) }, explain = { "Bastones por persona (terreno irregular)." }),
    PackingRule("trekking.navigation", PackingCategory.TOOLS, ItemClassification.MANDATORY, "Navegación (mapa/brújula)", "set",
        applies = { true }, quantity = { 1 }, explain = { "Navegación del grupo: no depender solo del teléfono." }),
    PackingRule("trekking.layers", PackingCategory.CLOTHING, ItemClassification.RECOMMENDED, "Capas extra de abrigo", "set",
        applies = { true }, quantity = { people(it) }, explain = { "Capas extra: el clima cambia rápido en ruta." }),
    // keys compartidas con camping → se fusionan, no se duplican:
    PackingRule("safety.first_aid", PackingCategory.SAFETY, ItemClassification.MANDATORY, "Botiquín básico (contenido no clínico)", "botiquín",
        applies = { true }, quantity = { 1 }, explain = { "Un botiquín por grupo (compartido con camping)." }),
    PackingRule("water.treatment", PackingCategory.WATER, ItemClassification.MANDATORY, "Kit de potabilización", "kit",
        applies = { true }, quantity = { 1 }, explain = { "En ruta no se asume agua potable." }),
)

/**
 * Devuelve el conjunto de reglas para un escenario primario + secundarios. El engine
 * fusiona por key, por lo que las reglas compartidas no producen ítems duplicados.
 */
fun rulesFor(
    primary: ScenarioKind,
    secondary: Set<ScenarioKind> = emptySet(),
): List<PackingRule> {
    val all = ArrayList<PackingRule>()
    if (primary == ScenarioKind.CAMPING) all.addAll(CAMPING_RULES)
    val scenarios = secondary + primary
    if (scenarios.contains(ScenarioKind.TREKKING)) all.addAll(TREKKING_RULES)
    // Fallback: si no hubo match (escenario aún no soportado), usar camping base.
    return if (all.isEmpty()) CAMPING_RULES else all
}
