# Draft PR — Android (feat/outdoor-camping-r1)

**Crear (draft):** https://github.com/Sinapsisfm/Humanos-App-Android/pull/new/feat/outdoor-camping-r1
**Comando (si hay `gh`):**
```bash
gh pr create --repo Sinapsisfm/Humanos-App-Android --draft \
  --base feat/fcm-push --head feat/outdoor-camping-r1 \
  --title "feat(outdoor): vertical R1+R2 camping (core+UI+Room, aislado, flags OFF en release)" \
  --body-file docs/outdoor-evidence/PR-DRAFT-android.md
```
**NO MERGE** (gate humano).

---

## Título
feat(outdoor): vertical R1+R2 camping (core + UI + Trip Gate + AwarenessOS + Room, detrás de flags OFF en release)

## Alcance
Vertical Outdoor (camping familiar offline) aislada y reversible, OFF para usuarios en release.
R1: dominio determinístico (packing engine, repo, awareness, escenarios, lista retorno).
R2: Trip Gate + Complexity Classifier, AwarenessOS v0 situacional, UI Compose con revelación
progresiva, Hilt selección de repositorio (in-memory default / Room tras flag), adaptador Room
aislado, process-death/restore.

## Módulos
- `:core:core-outdoor` (Kotlin/JVM puro, sin Android/Room/Hilt/Compose).
- `:feature:feature-outdoor` (Compose: ViewModel + screen + ruta Hilt).
- `:data:data-outdoor` (Room, DB separada `outdoor.db` v1, aditiva).
- `app`: dep + Hilt `OutdoorDataModule` + ruta flag-gated + entrada opcional en Settings.

## Flags (OFF en release)
- `OUTDOOR_R1_ENABLED`: debug=true / release=false (ruta + entrada Settings solo con ON).
- `OUTDOOR_ROOM_ENABLED`: false en debug y release (Room NO default; in-memory por defecto).

## Pruebas
- **87 unit Kotlin** (core 64 / feature 13 / data 8 / app 2), 0 fallos.
- **4 instrumented en AVD limpio** (API 34): data 2/2 (Room DAO/persist), feature 2/2 (render Compose).
- App debug instala y lanza sin crash en AVD (Hilt resuelve al inicio).
- Compila `sideloadDebug` + `sideloadRelease` + `playRelease`. Schema Room exportado.

## Evidencia AVD / screenshots
`docs/outdoor-evidence/R2-ANDROID-EVIDENCE.md` + `TEST-*.xml` (connected) + `avd-app-launch.png`.
Sin tocar BlueStacks (ANDROID_SERIAL=emulator-5560).

## Decisiones
DR-06 (flag), Room dev-aislado (no activación sin evidencia), R2 determinístico. Reviewer
independiente: APPROVE (tras corregir un REQUEST-CHANGES previo: Hilt/Room wiring + SavedStateHandle).

## Riesgos
- Room aún no default (activación = decisión separada con su evidencia).
- App web-first: walkthrough manual UI completo + release-flag-OFF-on-device pendientes (no bloqueante de código).

## Rollback
Revertir wiring (`ffba1de`) deja módulos aislados; borrar módulos `core/feature/data-outdoor` +
includes/deps + ruta/flag + param Settings. Sin migración destructiva (DB separada).

## Deliberadamente NO incluido
Mapas (R3), Qbot, comunicaciones, SOS/satélite, contenido clínico, merge, activación Room/prod.
