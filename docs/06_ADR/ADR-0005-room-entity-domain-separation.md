# ADR-0005: Room Entity / Domain Model Separation

- **Estado:** Aceptada
- **Fecha:** 2026-06-07
- **Decisiones relacionadas:** DEC-002, DEC-009, DEC-012, DEC-013
- **Tandas:** 13 (Room wiring), 14 (data-capture)

## Contexto

El proyecto persiste datos localmente con Room (DEC-002). Room requiere anotaciones `@Entity`, `@PrimaryKey`, `@ColumnInfo`, etc., que vienen de `androidx.room` — una dependencia del framework Android.

Al mismo tiempo, `core-model` es un modulo de Kotlin puro (sin dependencias de Android) por diseño: contiene los modelos de dominio (`CaptureItem`, `TaskItem`, `TraceEvent`, etc.) con `@Serializable` de kotlinx.serialization. Esto permite:

- Testear los modelos sin instrumentacion Android
- Reusarlos en un eventual modulo KMP (Kotlin Multiplatform) o backend
- Mantener el dominio libre de detalles de persistencia

Surge la tension: **¿donde van las anotaciones de Room?**

## Opciones consideradas

### Opcion A: `@Entity` directamente en core-model
Poner las anotaciones Room sobre los data classes de `core-model`.

- **Pro:** Sin duplicacion. Un solo data class por concepto.
- **Contra:** Contamina `core-model` con `androidx.room`. Rompe la pureza del modulo. Imposibilita reuso en KMP. Acopla dominio a persistencia. Mezcla dos responsabilidades (forma de dato vs forma de almacenamiento).

### Opcion B: Entidades separadas en core-database con mappers (ELEGIDA)
`core-database` define sus propias clases `@Entity` que reflejan los modelos de dominio, con funciones de mapeo `toDomain()` / `toEntity()`.

- **Pro:** `core-model` queda puro. Separacion clara dominio/persistencia. La forma de almacenamiento puede evolucionar (indices, columnas desnormalizadas) sin tocar el dominio. Testeable.
- **Contra:** Duplicacion de campos (cada concepto tiene un domain model y un entity). Mappers a mantener. Mas codigo.

### Opcion C: Room con KMP
Room 2.7+ soporta Kotlin Multiplatform. Poner entities en un modulo comun KMP.

- **Pro:** Futuro-proof para multiplataforma.
- **Contra:** Complejidad prematura. El proyecto es Android-only en Phase 1-3. No justificado aun.

## Decision

Se elige **Opcion B**: entidades Room separadas en `core-database`, con mappers bidireccionales.

Estructura:

```
core-database/
  entity/
    CaptureEntity.kt    @Entity("captures")  + toDomain()/toEntity()
    TaskEntity.kt       @Entity("tasks")     + mappers
    TraceEventEntity.kt @Entity("trace_events") + mappers
  converter/
    Converters.kt       @TypeConverter List<String> <-> JSON
  dao/
    CaptureDao.kt, TaskDao.kt, TraceEventDao.kt
  HumanosDatabase.kt    @Database(version=1, exportSchema=true)
  di/DatabaseModule.kt  Hilt provides
```

Reglas:
1. Los modelos de dominio (`core-model`) NUNCA llevan anotaciones Room.
2. Cada entity refleja un modelo de dominio y provee `toDomain()` / `toEntity()`.
3. Los enums se almacenan como String (`.name`).
4. Las colecciones (`List<String>`) se almacenan via TypeConverter (JSON).
5. Los repositorios (en modulos `data-*`) consumen DAOs y exponen modelos de dominio, nunca entities.
6. El schema Room se exporta a `core-database/schemas/` y se commitea (validacion de migraciones futuras).

## Consecuencias

### Positivas
- `core-model` permanece puro, testeable, reusable.
- La capa de persistencia puede evolucionar independientemente (agregar indices, cambiar tipos de columna, desnormalizar) sin afectar el dominio ni la UI.
- Patron claro y replicable para cada nuevo concepto persistido.
- Migracion a KMP en el futuro es posible sin reescribir el dominio.

### Negativas
- Duplicacion: cada concepto tiene 2 representaciones (domain + entity) y mappers.
- Mas boilerplate. Mitigado porque los mappers son triviales y mecanicos.
- Riesgo de drift entre entity y domain si se agrega un campo en uno y no en otro. Mitigado con tests de round-trip (Tanda 16+).

## Validacion

- Build verificado: `./gradlew assembleDebug` BUILD SUCCESSFUL (Tandas 13-14).
- Room compiler valida queries de DAOs contra columnas de entities en compile time.
- Schema exportado a `core-database/schemas/eco.humanos.android.core.database.HumanosDatabase/1.json`.
- Pendiente (Tanda 16+): tests unitarios de round-trip `toDomain(toEntity(x)) == x`.
