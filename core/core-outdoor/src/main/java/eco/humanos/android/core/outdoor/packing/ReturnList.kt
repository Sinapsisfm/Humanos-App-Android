/**
 * core-outdoor / packing / ReturnList.kt
 *
 * Generador determinístico de la lista de retorno (CAMP-016): desmontaje, residuos,
 * inventario y revisión del sitio. Reutiliza `PreparationTask`. Pura, sin Android.
 */
package eco.humanos.android.core.outdoor.packing

import eco.humanos.android.core.outdoor.domain.PackingInput
import eco.humanos.android.core.outdoor.domain.PreparationTask

object ReturnListGenerator {

    fun build(input: PackingInput): List<PreparationTask> {
        val tasks = ArrayList<PreparationTask>()
        fun add(id: String, title: String) =
            tasks.add(PreparationTask(id = id, title = title, sourceRuleId = id))

        if (input.facility.accommodation.name == "TENT" || input.facility.accommodation.name == "MIXED") {
            add("return.teardown", "Desmontar y guardar carpa(s) secas")
        }
        if (input.nights > 0) {
            add("return.cookware", "Limpiar y guardar cocina y utensilios")
            add("return.fire", "Apagar y asegurar fuego/cocinilla; verificar gas")
        }
        add("return.waste", "Recoger toda la basura y residuos (no dejar rastro)")
        add("return.inventory", "Revisar inventario: contar equipo y registrar faltantes")
        add("return.site", "Revisar el sitio: dejarlo igual o mejor que como se encontró")
        if (input.facility.access.name == "VEHICLE") {
            add("return.vehicle", "Cargar el vehículo y verificar que no quede nada")
        }
        add("return.maintenance", "Tareas de mantenimiento: secar, cargar baterías, reparar")
        return tasks
    }
}
