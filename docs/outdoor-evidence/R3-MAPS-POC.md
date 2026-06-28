# R3 — POC cartográfico AISLADO (Fase T)

> **No es cobertura cartográfica real.** POC de **arquitectura**: contratos de dominio + UI
> con **datos exclusivamente sintéticos**. Sin tiles, sin proveedor, sin red, sin MapLibre,
> sin tocar CSP/geolocalización. **No levanta el gate de licencias/proveedor** (ADR-OUT-003
> sigue `proposed-blocked`). Sin merge.

## Qué se construyó

### `:core:core-maps` — Kotlin/JVM puro (sin Android, sin MapLibre)
Contratos de dominio cartográfico, desacoplados de cualquier SDK/proveedor:

| Contrato | Rol |
|---|---|
| `GeoPoint` / `BoundingBox` | coordenada WGS84 / caja envolvente (`contains`) |
| `RouteGeometry` | ruta planificada; `bounds()`, `lengthMeters()` (haversine determinístico) |
| `TrackPoint` / `TrackRecorder` | punto grabado / grabador (distancia, `toRoute()`) |
| `Waypoint` | punto de interés (camp/agua/…) |
| `MapRegion` / `MapCoverage` | región y cobertura agregada (`covers`, `coveringRegion`) |
| `OfflineMapPack` + `MapPackManifest` | pack offline autosuficiente (rutas+waypoints+manifiesto) |
| `MapRepository` + `InMemoryMapRepository` | puerto de almacenamiento + impl en memoria con `serialize()/restore()` (process death) |
| `MapPackVerifier` | integridad **fail-closed**: sha256 + tamaño; `Valid/Corrupt/SchemaMismatch/SizeMismatch` |
| `GpxParser` | parser GPX DOM **endurecido anti-XXE** (DTD/entidades externas off) sobre fixture ficticio |

Separación explícita: **dominio ↔ adapter cartográfico ↔ UI ↔ proveedor ↔ almacenamiento**.
Un `RoomMapRepository` o un adapter MapLibre implementarían estas interfaces más adelante
**sin tocar el dominio** (mismo patrón que `OutdoorRepository → RoomOutdoorRepository`).

### `feature-outdoor` — UI POC detrás de flag
- `OutdoorMapsPocScreen` (Compose **stateless**): Canvas propio que proyecta lat/lon→píxeles
  (equirectangular, norte arriba) y dibuja región, ruta, waypoints y track. Banner visible
  **"DATOS SINTÉTICOS — no es cobertura real. Sin tiles, sin red."**
- `OutdoorMapsDemo` / `OutdoorMapsPocRoute`: datos sintéticos definidos en repo.
- Flag **`OUTDOOR_R3_MAPS_ENABLED` = false** en debug **y** release (OFF por defecto). La ruta
  `outdoor/maps` se registra **solo** con `OUTDOOR_R1_ENABLED && OUTDOOR_R3_MAPS_ENABLED`.

## Datos usados (solo sintéticos)
- GPX **ficticio** (1 track de 3 puntos + 1 waypoint), tiempos ISO fijos → determinístico.
- GeoJSON mínimo (LineString) generado en repo.
- Geometrías sintéticas para tests. **Cero** OSM / tiles / proveedor / red.

## Evidencia de ejecución
- `:core:core-maps:test` → **28/28** verde (Geometry 7, GpxParser 5, MapPackVerifier 5,
  MapRepository 7, TrackRecorder 4). 0 fallos.
- `:feature:feature-outdoor:testDebugUnitTest` → **15/15** verde, incluye **+2** render POC
  (`OutdoorMapsPocScreenTest`, Robolectric): canvas existe + banner "DATOS SINTÉTICOS".
- `:app:compileSideloadDebugKotlin` → compila con el flag + ruta gated.

## Riesgos / límites declarados
- Parser GPX validado con fixture (no con GPX adversarial real).
- Proyección equirectangular simple (no apta para áreas grandes / alta latitud).
- Aún sin datum / precisión / timestamp canónicos (MAP exige lat/lon+datum+precisión+ts).
- **Gate abierto**: motor (MapLibre u otro), contenedor de tiles (PMTiles/MBTiles) y fuente
  cartográfica con **license manifest** siguen pendientes — el POC NO los decide.

## Rollback
Borrar `core/core-maps/`, `feature/.../OutdoorMapsPoc*.kt`, su test, la línea
`include(":core:core-maps")` de `settings.gradle.kts`, la dep en `feature-outdoor`, el
`buildConfigField OUTDOOR_R3_MAPS_ENABLED` y el bloque `outdoor/maps` del NavHost. Todo el
R3 es aditivo y aislado; eliminarlo no afecta R1/R2.
