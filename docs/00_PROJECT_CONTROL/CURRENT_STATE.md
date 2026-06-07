# CURRENT_STATE -- humanOS Native Android

> Ultima actualizacion: 2026-06-06

## Estado general

**Fase actual:** Pre-desarrollo -- documentacion y planificacion completadas, skeleton pendiente.

El repositorio fue creado hoy. No existe aun ningun proyecto Android (ni `build.gradle.kts`, ni modulos, ni `google-services.json`). La fase de inspeccion READ-ONLY de HumanOS y QueBot fue completada en sesiones previas. Este proyecto es **independiente**: no comparte codigo fuente con HumanOS ni QueBot; se integra con ellos via interfaces Gateway.

## Decisiones clave tomadas

| Area | Decision | Detalle |
|------|----------|---------|
| Independencia | Proyecto standalone | HumanOS y QueBot son fuentes externas read-only. Sin monorepo compartido |
| Lenguaje | Kotlin | 100% Kotlin, sin Java |
| UI | Jetpack Compose + Material 3 | Sin XML layouts. Material 3 Dynamic Color |
| DI | Hilt | Dagger-Hilt con KSP |
| Persistencia local | Room + DataStore | Room para entidades estructuradas, DataStore para preferences |
| Background | WorkManager | Para sync diferida y tareas offline |
| Arquitectura modular | 14 modulos Phase 1 | app, core-model, core-database, core-datastore, core-network, core-security, core-ui, data-auth, feature-dashboard, feature-capture, feature-settings, integration-humanos, integration-quebot, testing-common |
| CI/CD | GitHub Actions | Firebase App Distribution para betas, Google Play para produccion |
| Firebase | Proyecto `humanos-app` | GCP project compartido. No existe `google-services.json` aun |
| compileSdk | 36 preferido (35 fallback) | API 36 = Android 16. Si SDK 36 no esta disponible en CI, fallback temporal a 35 con TASK para subir |
| Auth | Dual-token | Firebase ID token para QueBot, bridge JWT para HumanOS via `POST /api/auth/mobile/exchange` (endpoint NO existe aun) |

## Lo que existe

- Directorio `C:\Users\felip\Claude\humanos-android\` creado
- Carpeta `docs/00_PROJECT_CONTROL/` creada
- Documentacion de control de proyecto (este archivo y companeros)

## Lo que NO existe todavia

- Proyecto Gradle (ni root `build.gradle.kts` ni `settings.gradle.kts`)
- Ningun modulo Android
- `google-services.json` (requiere configurar app en Firebase Console de `humanos-app`)
- ADRs (Architecture Decision Records)
- Codigo Kotlin de ningun tipo
- CI pipeline (`.github/workflows/`)
- Tests

## Secuencia inmediata (next steps)

1. Completar estructura de documentacion (TASK-001) -- **en curso**
2. Crear ADRs 0001-0004 (TASK-002)
3. Crear proyecto Gradle con build-logic convention plugins (TASK-003)
4. Crear los 14 modulos Phase 1 vacios con dependencias (TASK-004)
5. Crear modelos Kotlin en `core-model` (TASK-005)

## Inspecciones realizadas (read-only)

- **HumanOS** (`C:\Users\felip\humanos-eco\`): Inspeccionado schema Prisma, API routes, auth middleware. Identificado que el endpoint `POST /api/auth/mobile/exchange` no existe y debe disenarse como contrato futuro.
- **QueBot legacy** (`C:\Users\felip\Claude\quebot_debug\`): Inspeccionado estructura PHP, flujo WhatsApp, EventForwarder. Confirmado que integracion sera via Firebase Auth token existente.
- **QueBot v2** (`C:\Users\felip\Claude\quebot-v2\`): Inspeccionado pipeline Anthropic SDK. No aplica directamente al Android nativo.

## Dependencias externas bloqueantes

| Dependencia | Bloquea | Estado | Mitigacion |
|-------------|---------|--------|------------|
| `google-services.json` | Firebase Auth real | No existe | Mock auth en Phase 1 |
| `POST /api/auth/mobile/exchange` | Auth bridge HumanOS | No implementado | Documentar contrato, mock gateway |
| Firebase project `humanos-app` confirmacion | Config definitiva | Supuesto, no confirmado | Q-001 abierta |
| applicationId definitivo | Play Store publish | No confirmado | Q-003 abierta, RISK-007 documentado |
