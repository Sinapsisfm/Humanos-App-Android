# R3 — Fase AC: hardening + RoomMapRepository aislado + License Manifest

> Continuación **branch-only** del POC R3. Sigue siendo **POC sintético**, sin proveedor, sin
> red, sin tiles, flag OFF. No mergeado. No activa Room. No despliega.

## 1. Hardening de `:core:core-maps`
- **Datum explícito** `Datum.WGS84` en `GeoPoint`(validación)/`TrackPoint`/`RouteGeometry`/`MapRegion`/`MapPackManifest`.
- **Provenance + precisión** en `TrackPoint`: `datum`, `hAccuracyM`, `vAccuracyM`, `source` (`PositionSource`), `at` (timestamp inyectado).
- **Source** en `RouteGeometry` (GPX importado → `IMPORT_GPX`).
- **Antimeridiano:** `BoundingBox.of(points)` elige el span longitudinal **más corto** (excluye el mayor hueco); `crossesAntimeridian` + `contains` con wraparound.
- **Bounding boxes robustas + validación fail-closed:** `GeoPoint.isValid()` (rango + finitud, rechaza NaN/Inf); `of()` devuelve null ante punto inválido.
- **Límites de tamaño:** `MAX_GPX_BYTES` (5 MB), `MAX_GPX_ELEMENTS` (100k), `MAX_ROUTE_POINTS`, `MAX_PACK_ROUTES` → fail-closed.
- **GPX adversarial / XXE:** DTD + entidades externas desactivadas (ya estaba); + guarda de tamaño + validación de coordenadas (rechaza fuera de rango / no numéricas).
- **Integridad SHA-256:** `MapPackVerifier` (ya existía) + nuevo `Invalid` para geometría inválida; `verify` chequea esquema→geometría→tamaño→hash.
- **Schema v2** (`MAPS_SCHEMA_VERSION=2`): los packs v1 son rechazados (`SchemaMismatch`) → migración fail-closed.
- **Restore determinístico:** `MapSnapshotCodec` (códec único) compartido por in-memory y Room → paridad cross-adapter.

## 2. `RoomMapRepository` aislado y DESACTIVADO (`:data:data-maps`)
- Módulo Room nuevo, DB separada **`maps.db` v1** (`exportSchema=true` → `schemas/.../1.json`, tablas `map_pack` + `map_route`). NO migra `HumanosDatabase` ni `outdoor.db`.
- Implementa el contrato `MapRepository` (mismas operaciones que in-memory). Almacena packs/rutas como BLOB JSON canónico (paridad byte a byte con el snapshot).
- **DESACTIVADO:** sin módulo Hilt ni consumidor → no se usa por defecto (forma más fuerte de "flag OFF": ausencia de cableado). Sin `allowMainThreadQueries` en producción → fail-closed si se usa mal en el main thread; nota de activación off-main-thread documentada.

## 3. License Manifest estructural (sin proveedor real)
- `LicenseManifest` (provider/license/attribution/territory/zoom/issuedAt/expiresAt/offlineRestrictions/redistribution/sourceHash/source) + `LicenseManifestValidator` fail-closed.
- `LicenseManifest.unspecified()` = placeholder **sin proveedor** → `NotReadyForRelease` (el release gate debe bloquearlo). Validación: estructura (rangos/format/coherencia) → placeholder → vigencia → `ReadyForRelease`.
- **No se selecciona ningún proveedor.** Solo la estructura + el validador que un futuro gate usaría (MAP-002/DAT-004, ADR-OUT-003).

## 4. Evidencia de ejecución
- `:core:core-maps:test` → **53/53** (Geometry 7 · GpxParser 6 · MapPackVerifier 5 · MapRepository 7 · TrackRecorder 4 · Hardening 13 · Antimeridian 5 · LicenseManifest 7) — 0 fallos.
- `:data:data-maps:testDebugUnitTest` → **9/9** (Mappers 3 + DAO Robolectric 6: idempotencia, cobertura, paridad Room→in-memory, restore tras reabrir) — 0 fallos.
- `:feature:feature-outdoor:compileDebugKotlin` → OK (POC sigue compilando con core-maps endurecido).
- Room schema `maps.db` v1 exportado.

## 5. Gates que NO se tocaron
proveedor cartográfico · tiles · descarga masiva · servicios externos · merge R3 · activación Room · deploy · Qbot/SOS/comms/satélite/clínico · motor global · schedulers globales.

## 6. Rollback
Revert de los commits de esta rama (`poc/outdoor-maps-r3-hardening`). Todo aditivo: `:data:data-maps` es un módulo nuevo no referenciado; el hardening de core-maps es retrocompatible (campos con default). Quitarlo no afecta R1/R2 ni el POC R3 base.
