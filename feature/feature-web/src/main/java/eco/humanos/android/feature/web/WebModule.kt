package eco.humanos.android.feature.web

/**
 * A rich HumanOS web module embedded in the app via the session bridge
 * (ADR-0006). [path] is the root-relative route the WebView lands on after
 * `/mobile-login` establishes the session. Adding a module is a single entry
 * here — no new architecture.
 *
 * Note: empresa.eco is host-routed on the web; the `/empresa` path is the
 * in-app entry point and may be refined with GPT as the embedded set grows.
 */
enum class WebModule(
    val key: String,
    val label: String,
    val path: String,
    val description: String,
) {
    CLAUDE("chat", "Claude (canal directo)", "/mobile-chat", "Escribile a Claude desde la app; responde sin que vayas al PC"),
    HOME("home", "HumanOS web", "/", "Todo el sitio, ya logueado"),
    EMPRESA("empresa", "Empresa", "/empresa", "PME: productos, riesgos, KPIs, proveedores"),
    ESTUDIANTE("estudiante", "Estudiante", "/estudiante", "Tareas, materiales y calendario académico"),
    SALUD("salud", "Salud", "/salud", "Programas GES/PSCV y seguimiento"),
    CARE("care", "Care", "/care", "Cuidado y acompañamiento"),
    LEGAL("legal", "Legal", "/legal", "Casos y trámites legales");

    companion object {
        fun fromKey(key: String): WebModule? = entries.firstOrNull { it.key == key }
    }
}
