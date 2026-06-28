/**
 * core-outdoor / packing / Rules.kt
 *
 * Ruleset de camping familiar (R1). Reglas VERSIONADAS y EXPLICABLES (no LLM).
 * Fixtures de PRODUCTO, sin contenido clínico. El "botiquín" es genérico sin contenido.
 * Versionar = cambiar RULESET_VERSION cuando cambie cualquier regla.
 */
package eco.humanos.android.core.outdoor.packing

import eco.humanos.android.core.outdoor.domain.ItemClassification
import eco.humanos.android.core.outdoor.domain.PackingCategory
import eco.humanos.android.core.outdoor.domain.PackingInput

// v0.2.0: agrega capa estacional fría-húmeda (Araucanía, KP-011 / PACK-015).
const val RULESET_VERSION = "camping-cl.v0.2.0"

class PackingRule(
    val id: String,
    val category: PackingCategory,
    val classification: ItemClassification,
    val name: String,
    val unit: String,
    val applies: (PackingInput) -> Boolean,
    val quantity: (PackingInput) -> Int,
    val explain: (PackingInput) -> String,
)

class PrepTaskRule(
    val id: String,
    val title: String,
    val applies: (PackingInput) -> Boolean,
)

// helpers puros
private fun days(i: PackingInput) = i.nights + 1
private fun people(i: PackingInput) = i.participants.size
private fun adults(i: PackingInput) = i.participants.count { it.ageClass.name == "ADULT" }
private fun youngChildren(i: PackingInput) =
    i.participants.count { it.ageClass.name == "INFANT" || it.ageClass.name == "CHILD" }
private fun minors(i: PackingInput) = i.participants.count { it.ageClass.name != "ADULT" }
private fun coldSeason(i: PackingInput) = i.season.name == "INVIERNO" || i.season.name == "OTONO"

val CAMPING_RULES: List<PackingRule> = listOf(
    PackingRule("shelter.tent", PackingCategory.SHELTER, ItemClassification.MANDATORY, "Carpa", "carpa",
        applies = { it.facility.accommodation.name == "TENT" || it.facility.accommodation.name == "MIXED" },
        quantity = { maxOf(1, Math.ceil(people(it) / 3.0).toInt()) },
        explain = { "Alojamiento ${it.facility.accommodation}: ~1 carpa cada 3 personas (${people(it)} personas)." }),
    PackingRule("sleep.bag", PackingCategory.SLEEP, ItemClassification.MANDATORY, "Saco de dormir", "saco",
        applies = { it.nights > 0 }, quantity = { people(it) },
        explain = { "1 saco por persona para ${it.nights} noche(s)." }),
    PackingRule("sleep.mat", PackingCategory.SLEEP, ItemClassification.RECOMMENDED, "Aislante / colchoneta", "aislante",
        applies = { it.nights > 0 && it.facility.accommodation.name != "CABIN" }, quantity = { people(it) },
        explain = { "Aislamiento del suelo: 1 por persona." }),
    PackingRule("lighting.lantern", PackingCategory.LIGHTING, ItemClassification.MANDATORY, "Farol / linterna de campamento", "farol",
        applies = { it.nights > 0 }, quantity = { maxOf(1, Math.ceil(people(it) / 4.0).toInt()) },
        explain = { "${it.nights} noche(s) con oscuridad: iluminación base del campamento." }),
    PackingRule("lighting.headlamp", PackingCategory.LIGHTING, ItemClassification.RECOMMENDED, "Linterna frontal", "frontal",
        applies = { it.nights > 0 && adults(it) > 0 }, quantity = { adults(it) },
        explain = { "1 frontal por adulto (${adults(it)})." }),
    PackingRule("cooking.stove", PackingCategory.COOKING, ItemClassification.MANDATORY, "Cocinilla", "cocinilla",
        applies = { it.nights > 0 }, quantity = { 1 }, explain = { "Cocción de alimentos en el sitio." }),
    PackingRule("cooking.fuel", PackingCategory.COOKING, ItemClassification.MANDATORY, "Combustible / gas", "cartucho",
        applies = { it.nights > 0 }, quantity = { maxOf(1, Math.ceil(days(it) / 3.0).toInt()) },
        explain = { "~1 cartucho cada 3 días (${days(it)} días)." }),
    PackingRule("cooking.cookware", PackingCategory.COOKING, ItemClassification.RECOMMENDED, "Set de cocina (olla, utensilios)", "set",
        applies = { it.nights > 0 }, quantity = { 1 }, explain = { "Un set por grupo." }),
    PackingRule("food.rations", PackingCategory.FOOD, ItemClassification.MANDATORY, "Raciones de comida", "ración-persona-día",
        applies = { true }, quantity = { people(it) * days(it) },
        explain = { "Consumible: ${people(it)} personas × ${days(it)} días." }),
    PackingRule("water.storage", PackingCategory.WATER, ItemClassification.MANDATORY, "Agua / bidones", "L",
        applies = { true }, quantity = { people(it) * days(it) * 3 },
        explain = { "~3 L por persona/día (${people(it)}×${days(it)} días)." }),
    PackingRule("water.treatment", PackingCategory.WATER, ItemClassification.MANDATORY, "Kit de potabilización", "kit",
        applies = { !it.facility.potableWater }, quantity = { 1 },
        explain = { "No hay agua potable confirmada en el sitio: tratar el agua antes de consumir." }),
    PackingRule("food.cooler", PackingCategory.FOOD, ItemClassification.MANDATORY, "Cooler / conservadora", "cooler",
        applies = { it.nights > 0 && !it.facility.refrigeration }, quantity = { maxOf(1, Math.ceil(people(it) / 4.0).toInt()) },
        explain = { "Sin refrigeración en el sitio: fuente de frío propia para conservar alimentos." }),
    PackingRule("food.ice", PackingCategory.FOOD, ItemClassification.RECOMMENDED, "Hielo / geles refrigerantes", "paquete",
        applies = { it.nights > 0 && !it.facility.refrigeration }, quantity = { maxOf(1, Math.ceil(days(it) / 2.0).toInt()) },
        explain = { "Reposición de frío cada ~2 días (${days(it)} días)." }),
    PackingRule("clothing.warm_layer", PackingCategory.CLOTHING, ItemClassification.MANDATORY, "Abrigo (capa térmica)", "abrigo",
        applies = { true }, quantity = { people(it) },
        explain = { "Capa de abrigo por persona${if (coldSeason(it)) " (temporada fría)" else ""}." }),
    PackingRule("clothing.warm_child", PackingCategory.CHILD, ItemClassification.MANDATORY, "Abrigo de niño/a", "abrigo",
        applies = { youngChildren(it) > 0 }, quantity = { youngChildren(it) },
        explain = { "${youngChildren(it)} niño(s): abrigo adicional por niño (más sensibles al frío)." }),
    PackingRule("sun_rain.rain", PackingCategory.SUN_RAIN, ItemClassification.RECOMMENDED, "Impermeable / poncho", "impermeable",
        applies = { true }, quantity = { people(it) },
        explain = { "Protección de lluvia por persona${if (coldSeason(it)) " (temporada lluviosa)" else ""}." }),
    PackingRule("sun_rain.sun", PackingCategory.SUN_RAIN, ItemClassification.RECOMMENDED, "Protección solar", "unidad",
        applies = { true }, quantity = { maxOf(1, Math.ceil(people(it) / 2.0).toInt()) },
        explain = { "Protector solar: ~1 cada 2 personas." }),
    PackingRule("hygiene.kit", PackingCategory.HYGIENE, ItemClassification.RECOMMENDED, "Set de higiene", "set",
        applies = { true }, quantity = { maxOf(1, Math.ceil(people(it) / 2.0).toInt()) },
        explain = { "Higiene básica: ~1 set cada 2 personas." }),
    PackingRule("hygiene.toilet_kit", PackingCategory.HYGIENE, ItemClassification.RECOMMENDED, "Kit de baño seco / pala", "kit",
        applies = { !it.facility.toilets }, quantity = { 1 },
        explain = { "Sitio sin baños: manejo responsable de residuos humanos." }),
    PackingRule("safety.first_aid", PackingCategory.SAFETY, ItemClassification.MANDATORY, "Botiquín básico (contenido no clínico)", "botiquín",
        applies = { true }, quantity = { 1 },
        explain = { "Un botiquín por grupo. El contenido clínico requiere revisión profesional (no incluido en R1)." }),
    PackingRule("child.entertainment", PackingCategory.ENTERTAINMENT, ItemClassification.OPTIONAL, "Entretención para menores", "set",
        applies = { minors(it) > 0 }, quantity = { 1 }, explain = { "${minors(it)} menor(es) en el grupo." }),
    PackingRule("tools.multitool", PackingCategory.TOOLS, ItemClassification.RECOMMENDED, "Multiherramienta / cuchillo", "unidad",
        applies = { true }, quantity = { 1 }, explain = { "Herramienta multiuso del grupo." }),
    PackingRule("tools.powerbank", PackingCategory.TOOLS, ItemClassification.RECOMMENDED, "Batería externa (powerbank)", "unidad",
        applies = { it.facility.electricity.name != "AVAILABLE" }, quantity = { maxOf(1, Math.ceil(adults(it) / 2.0).toInt()) },
        explain = { "Electricidad ${it.facility.electricity}: respaldo de carga." }),
    PackingRule("documents.id", PackingCategory.DOCUMENTS, ItemClassification.RECOMMENDED, "Documentos y llaves", "set",
        applies = { true }, quantity = { 1 }, explain = { "Identificación, llaves y datos de contacto." }),
    // ── Capa estacional fría-húmeda (Araucanía base, KP-011) — se activa por estación ──
    PackingRule("winter.waterproof_shell", PackingCategory.SUN_RAIN, ItemClassification.MANDATORY, "Capa impermeable (shell)", "shell",
        applies = { coldSeason(it) }, quantity = { people(it) },
        explain = { "Araucanía frío-húmedo: capa impermeable obligatoria por persona en temporada fría." }),
    PackingRule("winter.gloves_hat", PackingCategory.CLOTHING, ItemClassification.MANDATORY, "Guantes y gorro", "set",
        applies = { it.season.name == "INVIERNO" }, quantity = { people(it) },
        explain = { "Invierno: guantes y gorro por persona (pérdida de calor por extremidades/cabeza)." }),
    PackingRule("winter.insulation", PackingCategory.SLEEP, ItemClassification.MANDATORY, "Aislamiento térmico reforzado", "set",
        applies = { it.season.name == "INVIERNO" && it.nights > 0 }, quantity = { people(it) },
        explain = { "Invierno con pernoctación: aislamiento térmico reforzado para el frío." }),
    PackingRule("winter.traction", PackingCategory.TOOLS, ItemClassification.RECOMMENDED, "Tracción para hielo", "par",
        applies = { it.season.name == "INVIERNO" }, quantity = { maxOf(1, Math.ceil(adults(it) / 2.0).toInt()) },
        explain = { "Posible hielo/barro: tracción ligera (cadenas/grampones) para el grupo." }),
)

val CAMPING_PREP_TASKS: List<PrepTaskRule> = listOf(
    PrepTaskRule("task.shopping", "Comprar consumibles de la lista") { true },
    PrepTaskRule("task.water_plan", "Planificar potabilización del agua antes de salir") { !it.facility.potableWater },
    PrepTaskRule("task.cold_chain", "Preparar fuente de frío (cooler + hielo) y plan de alimentos sin cadena de frío") { it.nights > 0 && !it.facility.refrigeration },
    PrepTaskRule("task.charge", "Cargar dispositivos y baterías externas antes de salir") { it.facility.electricity.name != "AVAILABLE" },
    PrepTaskRule("task.return_list", "Preparar lista de retorno: desmontaje, residuos e inventario") { true },
)
