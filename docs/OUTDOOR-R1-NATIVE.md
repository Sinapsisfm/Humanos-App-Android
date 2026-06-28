# HumanOS Outdoor — vertical nativa R1 (camping offline)

**Fecha:** 2026-06-27 · **Rama:** `feat/outdoor-camping-r1` (← `feat/fcm-push`) ·
**Estado:** módulos construidos, **48 tests verdes** (45 core + 3 feature), AISLADO (no cableado a nav/app),
nada pusheado. Decisión de cablear/PR/merge = Felipe.

## Módulos agregados

| Módulo | Tipo | Contenido | Tests |
|---|---|---|---|
| `:core:core-outdoor` | Kotlin/JVM puro (`humanos.kotlin.library`) | domain, packing engine determinístico, repo serializable, awareness, service, presentation, escenarios, lista de retorno | 45 |
| `:feature:feature-outdoor` | Android feature (`humanos.android.feature`) | `OutdoorPackingViewModel` (plano) + `OutdoorPackingScreen` (Compose stateless) | 3 |

Único archivo preexistente modificado: `settings.gradle.kts` (2 `include` aditivos).

## Verificación

```bash
export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"   # o el JBR local
./gradlew :core:core-outdoor:test            # 45 verde
./gradlew :feature:feature-outdoor:testDebugUnitTest   # 3 verde
```

## Integración con feature flag — HECHO (Fase B, DR-06 aprobado con flag)

Cableado de forma reversible y **OFF en release/producción**:

- **Flag:** `BuildConfig.OUTDOOR_R1_ENABLED` (mecanismo existente del repo). `app/build.gradle.kts`
  buildTypes: **debug = true**, **release = false**.
- **Dependencia:** `app` → `implementation(project(":feature:feature-outdoor"))`.
- **Ruta:** `HumanosNavHost.kt` registra `composable("outdoor")` **solo si el flag está ON**
  → en release la ruta no existe (inaccesible).
- **Entrada provisional:** `SettingsScreen` (pantalla nativa alcanzable desde el shell web,
  "Config") gana un parámetro **opcional** `onOpenOutdoor` (default `null`); el host lo
  provee solo con el flag ON → ítem "Outdoor R1 (laboratorio)". Default null → sin cambios
  en release ni en tests existentes. **No se rediseñó la navegación global** (la app es
  web-first; no hay bottom bar).
- **Entrada de UI:** `OutdoorPackingRoute()` (en `:feature:feature-outdoor`) construye un
  servicio in-memory + salida de demostración y renderiza la pantalla (no toca la DB de la app).

### Evidencia (verificada)
- Build flag ON: `:app:compileSideloadDebugKotlin` ✓.
- Build flag OFF: `:app:compileSideloadReleaseKotlin` ✓.
- Ruta inaccesible con flag OFF: gating en compile-time (`if (BuildConfig.OUTDOOR_R1_ENABLED)`)
  + entrada de Settings `null` en release. Probado por compilación release + lectura de código.
- Sin regresiones de nav: solo se agrega ruta flag-gated + un parámetro opcional default-null;
  rutas existentes intactas. Tests `:feature:feature-settings:testDebugUnitTest`,
  `:feature:feature-outdoor:testDebugUnitTest`, `:core:core-outdoor:test` verdes.
- **Pendiente (requiere dispositivo/emulador):** apertura real de la pantalla con flag ON y
  recorrido del flujo camping en runtime. No se declara verificado sin esa evidencia.

## Persistencia: próximo paso = adaptador Room (DR-04, gated)

El contrato `OutdoorRepository` ya existe; falta una impl Room. **Requiere gate** porque
toca `:core:core-database` (módulo compartido) y sube la versión de la DB:

1. Entidades en `:core:core-database`: `OutdoorOutingEntity`, `OutdoorPlanEntity`
   (o ítems normalizados), `OutdoorEventEntity` (+ TypeConverters JSON ya existen).
2. `OutdoorDao` + registrarlo en `HumanosDatabase` y **subir `version`** (migración
   aditiva; hoy la DB usa `fallbackToDestructiveMigration` en dev).
3. `RoomOutdoorRepository : OutdoorRepository` mapeando entity↔domain.
4. `@Module` Hilt proveyendo el DAO y el repo.

Hasta entonces, `InMemoryOutdoorRepository` (serializable) es la impl de referencia
verificada del mismo contrato.

## Fuentes

Canónicas en `humanos-eco/docs/outdoor/` (ADR-OUT-000, PRD-OUT-001, ADR-OUT-001..010).
Es un port del núcleo de referencia TypeScript `humanos-eco/lib/outdoor`. Registrado en
AgentOS prod (Domain `outdoor`, 13 tareas M0..M12, 10 decisiones ADR-OUT-*).
