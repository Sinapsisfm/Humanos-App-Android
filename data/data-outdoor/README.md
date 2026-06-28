# :data:data-outdoor — adaptador Room (aislado, GATE de evidencia)

Persistencia offline en dispositivo para Outdoor (CORE-002), implementando el MISMO
contrato `OutdoorRepository` que el adaptador in-memory. **Base de datos SEPARADA**
(`outdoor.db`, v1) → **estrictamente aditiva**: no toca la `HumanosDatabase` compartida
ni migra datos existentes (cero riesgo sobre datos actuales).

## Qué hay (verificado)
- Entidades + DAO + `OutdoorDatabase` (v1, `exportSchema=true`). **Compila** → KSP Room
  validó el esquema; **schema exportado y versionado** en `schemas/…/1.json`.
- `RoomOutdoorRepository : OutdoorRepository` (síncrono, idempotente vía INSERT IGNORE).
- Mappers entidad↔dominio (JSON) **unit-testeados en JVM**: `:data:data-outdoor:testDebugUnitTest` = 4 verde.

## GATE (lo que FALTA — no se declara validado)
- **Pruebas instrumentadas NO ejecutadas** (no hay emulador/dispositivo ni Robolectric en
  el entorno). `src/androidTest/.../OutdoorDaoInstrumentedTest.kt` está **preparado** pero
  sin correr. Ejecutar con:
  ```bash
  ./gradlew :data:data-outdoor:connectedDebugAndroidTest   # requiere emulador/dispositivo
  ```
- **Prueba de migración**: la DB está en v1 (sin migración aún). Cualquier v1→v2 debe ser
  aditiva y acompañarse de un test con `MigrationTestHelper` (room-testing) — instrumentado.

## Restricciones (respetadas)
- **NO conectado como repositorio por defecto**: no existe módulo Hilt que provea
  `RoomOutdoorRepository`; la app/feature siguen usando `InMemoryOutdoorRepository`.
- **NO marcado como validado** (faltan pruebas instrumentadas).
- **NO merge**. Sin migraciones destructivas. Sin alterar datos existentes.

## Activación (futura, tras evidencia)
1. Correr `connectedDebugAndroidTest` y conservar evidencia.
2. Crear un `@Module` Hilt que provea `OutdoorDatabase` + `OutdoorDao` + `OutdoorRepository`
   = `RoomOutdoorRepository`.
3. Reemplazar el repo in-memory de la ruta/demo por el de Room detrás del flag.
