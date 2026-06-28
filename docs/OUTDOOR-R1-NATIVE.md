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

## Cómo cablear a la app (cuando Felipe lo apruebe — DR-06)

Hoy NO está cableado para no hacerlo visible al usuario sin aprobación. Para activarlo:

1. **`app/build.gradle.kts`** → agregar `implementation(project(":feature:feature-outdoor"))`.
2. **`app/.../navigation/TopLevelDestination.kt`** → agregar entrada:
   ```kotlin
   OUTDOOR(route = "outdoor", label = "Acampar", selectedIcon = ..., unselectedIcon = ...)
   ```
3. **`app/.../navigation/HumanosNavHost.kt`** → agregar:
   ```kotlin
   composable(TopLevelDestination.OUTDOOR.route) {
       // construir OutdoorService(repo, clock) + crear/abrir una salida y pasar el estado
       OutdoorPackingScreen(state = ..., onTogglePacked = ...)
   }
   ```
4. Para inyección real: `@HiltViewModel` + un `@Module` que provea `OutdoorRepository`
   (ver Room abajo) y un `Clock` (System). Hoy el ViewModel es plano para mantener el
   módulo aislado y unit-testeable.

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
