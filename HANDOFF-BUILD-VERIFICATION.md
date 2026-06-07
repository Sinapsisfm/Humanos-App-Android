# HANDOFF — Build Verification Required

> Creado: 2026-06-07 por Claude Code (Tanda 11)
> Para: Felipe Mehr
> Estado: BLOQUEANTE para nuevas features

## Contexto

El proyecto humanOS Native Android tiene 10 commits, ~190 archivos, 15 modulos Gradle, 27 modelos, ViewModels, Hilt DI, y CI workflows. Pero **nunca ha compilado** porque este equipo Windows no tiene Java ni Android SDK instalados.

GPT identifico correctamente que no debemos agregar mas features sin verificar el build primero.

## Que necesitas hacer (en orden)

### 1. Instalar herramientas (si no las tienes)

```bash
# Opcion A: Android Studio (recomendado)
# Descarga de https://developer.android.com/studio
# Incluye JDK, Android SDK, emulador

# Opcion B: Solo CLI
# Instalar JDK 21 (Temurin): https://adoptium.net/
# Instalar Android SDK Command-Line Tools
# Configurar ANDROID_HOME y JAVA_HOME
```

### 2. Abrir el proyecto

```bash
# En Android Studio: File > Open > C:\Users\felip\Claude\humanos-android
# Gradle sync deberia ejecutarse automaticamente
# Si pide instalar SDK 36, aceptar
```

### 3. Verificar build

```bash
cd C:\Users\felip\Claude\humanos-android
./gradlew assembleDebug
```

**Si compila:** el skeleton esta listo. Siguiente paso: crear repo en GitHub.

**Si hay errores:** los errores probables son:
- SDK 36 no disponible -> instalar via SDK Manager o cambiar a 35 temporalmente
- Dependencias no resueltas -> verificar internet + Gradle cache
- Plugin version mismatch -> actualizar AGP en libs.versions.toml

### 4. Crear repo en GitHub

```bash
# Instalar gh CLI si no esta: https://cli.github.com/
gh auth login
gh repo create felipemehr/humanos-android --private --source=. --push
```

### 5. Verificar CI

Despues del push, ir a https://github.com/felipemehr/humanos-android/actions y verificar que el workflow CI pasa.

### 6. Configurar Firebase (cuando estes listo)

1. Ir a https://console.firebase.google.com/project/humanos-app
2. Add app > Android
3. Package name: `eco.humanos.android.dev` (provisional)
4. Descargar `google-services.json`
5. Ponerlo en `app/google-services.json`
6. NO commitear (esta en .gitignore)

## Preguntas que necesito que respondas (Q-001 a Q-005)

| # | Pregunta | Default actual |
|---|----------|----------------|
| Q-001 | Firebase project: reusar humanos-app o crear nuevo? | humanos-app |
| Q-002 | CI auth: WIF o SA key JSON? | WIF |
| Q-003 | applicationId definitivo? | eco.humanos.android.dev (temporal) |
| Q-004 | Google Play Console existe? | No |
| Q-005 | Licencia MIT o propietaria? | MIT |

## Bugs corregidos en Tanda 11

- core-security faltaba dependencia a core-model (ERROR)
- core-network faltaba dependencia a core-security (ERROR)
- feature-dashboard faltaba dependencia a data-auth (WARNING)

## Estado del proyecto

- 10 commits, branch master
- 15 modulos Gradle Phase 1
- 27 modelos Kotlin
- 3 ViewModels con Hilt DI
- 3 pantallas Compose con navegacion
- 2 fake gateways (HumanOS + QueBot)
- CI workflow listo (.github/workflows/ci.yml)
- Documentacion completa (38+ docs)
- HumanOS NO modificado
- QueBot NO modificado
