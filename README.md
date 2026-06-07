# humanOS Native Android

App nativa Android para el ecosistema HumanOS. Sistema operativo personal que integra contexto, captura multimodal, tareas inteligentes, salud, terreno e IA local.

## Principios

- **Independiente**: este proyecto NO depende directamente de HumanOS ni QueBot. Las integraciones son via gateways/adapters con contratos explícitos.
- **Local-first**: los datos viven en el dispositivo. La sincronización con backends es opt-in y controlada.
- **Privacy-first**: cifrado local, Android Keystore, niveles de privacidad por dato, biometría.
- **Trazabilidad-first**: cada decisión, módulo, tarea y riesgo está documentado en `docs/`.
- **Contract-first**: toda integración futura pasa por interfaces definidas, nunca por dependencia directa.

## Stack

| Componente | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Build | Gradle Kotlin DSL + Convention Plugins |
| DI | Hilt |
| Persistencia | Room + DataStore |
| Background | WorkManager |
| Navegación | Navigation Compose |
| Async | Coroutines + Flow |
| Target | compileSdk 36 (Android 16), minSdk 26 (Android 8.0) |

## Estructura del proyecto

```
humanos-android/
  docs/               # Documentación de control, arquitectura, producto, ADRs
  app/                # Application module (Phase 1)
  core/               # Módulos core compartidos (Phase 1)
  data/               # Capa de datos (Phase 1+)
  feature/            # Feature modules con UI (Phase 1+)
  integrations/       # Adapters para HumanOS, QueBot, Health Connect (Phase 1+)
  testing/            # Utilidades de test compartidas (Phase 1)
  build-logic/        # Convention plugins Gradle (Phase 1)
```

## Estado actual

**Phase 1 — Skeleton** (en progreso)

Ver `docs/00_PROJECT_CONTROL/CURRENT_STATE.md` para el estado detallado.

## GCP Project

`humanos-app` — https://console.cloud.google.com/home/dashboard?project=humanos-app

## Reglas

1. NO modificar HumanOS (`humanos-eco`)
2. NO modificar QueBot (legacy PHP ni backend Python)
3. NO commitear secretos, keystores, ni `google-services.json`
4. Toda decisión debe registrarse en `docs/00_PROJECT_CONTROL/DECISIONS_LOG.md`
5. Todo pendiente debe registrarse en `docs/00_PROJECT_CONTROL/TASKS.md`
6. Todo riesgo debe registrarse en `docs/00_PROJECT_CONTROL/RISKS.md`
