# R2 — Evidence bundle (Android real, AVD limpio)

**Fecha:** 2026-06-28 · **Commit:** `feat/outdoor-camping-r1` (post-`de51486`)
**No se tocaron los dispositivos BlueStacks** (LastZ, `emulator-5554`/`127.0.0.1:5555`):
toda la ejecución se dirigió a `emulator-5560` vía `ANDROID_SERIAL`.

## AVD
- Nombre: `HumanOS_Outdoor_Test_API_34` · serial `emulator-5560` (puerto dedicado 5560).
- Imagen: `system-images;android-34;aosp_atd;x86_64` (AOSP ATD, license-clean, sin cuenta Google).
- API 34 · ABI x86_64 · `ro.kernel.qemu=1` · fingerprint `Android/sdk_slim_x86_64/...:14/...:userdebug/test-keys`.
- Aceleración: **WHPX operacional** (ya instalada; NO se modificó virtualización/Hyper-V).
- RAM emulador: 1024 MB (reducido por presión de commit del host por BlueStacks; el default 2560 MB no cabía).
- Boot headless: `-no-window -no-audio -no-boot-anim -no-snapshot -gpu swiftshader_indirect -memory 1024 -cores 2`.

## Comandos clave
```
# cmdline-tools oficiales + imagen (sdkmanager), licencias aceptadas
sdkmanager "system-images;android-34;aosp_atd;x86_64" "platforms;android-34"
avdmanager create avd -n HumanOS_Outdoor_Test_API_34 -k "system-images;android-34;aosp_atd;x86_64" -d pixel_6
emulator -avd HumanOS_Outdoor_Test_API_34 -port 5560 -no-window ... -memory 1024
# tests dirigidos SOLO al AVD limpio
set ANDROID_SERIAL=emulator-5560
gradlew :data:data-outdoor:connectedDebugAndroidTest
gradlew :feature:feature-outdoor:connectedDebugAndroidTest
gradlew :app:installSideloadDebug ; adb -s emulator-5560 shell monkey -p eco.humanos.android -c android.intent.category.LAUNCHER 1
```

## Resultados (PASS/FAIL)
| Evidencia | Resultado |
|---|---|
| Room DAO idempotente (instrumented) | **PASS** (`appendEvent_isIdempotent`) |
| Room persiste/lee outing+plan (instrumented) | **PASS** (`outing_and_plan_persist_and_read`) |
| → `:data:data-outdoor:connectedDebugAndroidTest` | **2/2 en AVD, BUILD SUCCESSFUL** |
| Render Compose pantalla Outdoor (instrumented) | **PASS** (título + detalle técnico + sin "seguro") |
| Aviso "Preparación incompleta" en UI (instrumented) | **PASS** |
| → `:feature:feature-outdoor:connectedDebugAndroidTest` | **2/2 en AVD, BUILD SUCCESSFUL** |
| App debug instala en AVD | **PASS** (`installSideloadDebug`) |
| App lanza sin crash (Hilt resuelve OutdoorDataModule al inicio) | **PASS** (pid vivo, sin FATAL EXCEPTION/ANR en logcat) |
| Schema Room exportado v1 | **PASS** (`schemas/.../1.json`) |
| Reportes | `docs/outdoor-evidence/TEST-*.xml` + `avd-app-launch.png` |

## Cubierto por capa (no manual)
- Persistencia/restore tras proceso muerto: Room reopen (Robolectric) + serialize/restore (unit) + DAO en AVD.
- Process death / recreación: tests de VM (SavedStateHandle) + restore por niveles.
- Trip Gate / AwarenessOS: core (12+7 tests) + render en AVD.

## Pendiente (gate externo, NO bloqueante de código Outdoor)
- **Walkthrough manual UI completo** (navegar web-shell → Settings → Outdoor → flujo) y **flag OFF en release instalado**:
  requieren automatización de UI sobre el WebView (web-first) y un APK release firmado. La visibilidad por flag
  está **probada en compile** (release compila con la ruta ausente) y la pantalla **renderiza en AVD**. No se
  declara como ejecutado el walkthrough manual.
- `connectedAndroidTest` queda reproducible en cualquier AVD/dispositivo limpio.

> `app/google-services.json` se copió localmente (gitignored) SOLO para permitir el arranque sin fallo de Firebase;
> NO se commitea.
