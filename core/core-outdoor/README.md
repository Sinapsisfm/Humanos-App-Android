# :core:core-outdoor

Núcleo de dominio **Outdoor** (R1: camping familiar offline) — Kotlin/JVM puro, sin
dependencias de Android. Es el motor determinístico del proyecto HumanOS Outdoor
(bandera de AgentOS). Aislado y **NO cableado** a `:app`/navegación: el build de la app
no se ve afectado.

## Contenido

- `domain/` — modelo (OutdoorOuting, FacilityProfile, Participant con `ageClass` no fecha
  de nacimiento), eventos con `sourceType`, máquina de estados de salida.
- `packing/` — ruleset versionado (`camping-cl.v0.2.0`) + `PackingEngine` determinístico:
  generación con trazabilidad (`sourceRuleId`), fusión coherente por key, diff, edición
  humana. Mismas entradas ⇒ misma salida (sin reloj ni azar).
- `repository/` — `OutdoorRepository` (contrato) + `InMemoryOutdoorRepository`
  serializable: persistencia, restore tras cierre forzado, idempotencia de eventos,
  export que excluye datos sensibles por defecto.
- `awareness/` — `AwarenessEvaluator` determinístico (omisiones/contradicciones de
  camping) + contrato de señales + `AwarenessSink` no-op (frontera a un AwarenessOS
  futuro; sin runtime: no se afirma integración).

## Invariantes

Offline-first (el store local es la fuente de lectura; en la app será Room),
determinístico, trazable, privacidad local por defecto, **sin contenido clínico**, sin
capacidades falsas, vocabulario sin "seguro" (no certifica seguridad).

## Tests

```bash
# requiere JAVA_HOME (p.ej. Android Studio JBR)
./gradlew :core:core-outdoor:test
```

45 tests (JUnit4 + Truth): determinismo, restore/idempotencia, no-duplicación, edición,
awareness, estados.

## Próximo

- Adaptador **Room** implementando `OutdoorRepository` (gate: versión DB en
  `:core:core-database`).
- `:feature:feature-outdoor` (Compose) que consuma este núcleo.

Fuentes canónicas: `humanos-eco/docs/outdoor/` (ADR-OUT-000, PRD-OUT-001, ADR-OUT-001..010).
Es un port del núcleo de referencia TypeScript `humanos-eco/lib/outdoor`.
