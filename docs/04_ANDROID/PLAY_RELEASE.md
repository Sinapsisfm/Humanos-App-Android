# HumanOS Android — Publicación en Google Play

> Estado: **en progreso** (rama `feature/play-store-release`). Pedido de Felipe 2026-06-09 (canal app). Cubre los 10 requisitos técnicos + los 4 entregables.

## 1. Estructura de variantes (flavors)

Se agregó una dimensión de flavor **`distribution`** con dos flavors:

| Flavor | Distribución | Auto-update APK | `REQUEST_INSTALL_PACKAGES` | `AD_ID` | Artefacto |
|---|---|---|---|---|---|
| **`sideload`** | Sideload / debug (actual) | ✅ sí (GitHub Releases) | ✅ sí | (Firebase) | APK |
| **`play`** | Google Play | ❌ no | ❌ no | ❌ removido | AAB |

Variantes resultantes: `sideloadDebug`, `sideloadRelease`, `playDebug`, `playRelease`.
`applicationId` = **`eco.humanos.android`** estable en ambos (req 6). `debuggable=false` en release (lo da el build type `release` del convention plugin; minify + proguard ya activos) (req 2).

Flag `BuildConfig.ENABLE_APK_AUTOUPDATE` = `true` (sideload) / `false` (play) para apagar el chequeo de update en la UI del flavor Play (req 4). **Pendiente:** gatear la UI de update en `feature-settings` con este flag (ver §8).

## 2. Comandos de build (req 1, 10)

```bash
# AAB para Play (reproducible):
./gradlew clean bundlePlayRelease
#   -> app/build/outputs/bundle/playRelease/app-play-release.aab

# APK sideload (auto-update, lo que ya existía):
./gradlew assembleSideloadDebug
#   -> app/build/outputs/apk/sideload/debug/app-sideload-debug.apk
```

> El workflow `release-apk.yml` se actualizó a `assembleSideloadDebug` + la nueva ruta del APK, para no romper el auto-update del sideload al agregar flavors.

## 3. Firma de release (upload key) (req 8)

El `signingConfig("release")` lee de **`keystore.properties`** en la raíz del repo (**gitignored**, NUNCA se commitea). Si falta, el release queda **sin firmar** (permite validar el build igual).

**Felipe debe (una vez):**
```bash
keytool -genkeypair -v -keystore humanos-upload.jks \
  -alias humanos-upload -keyalg RSA -keysize 2048 -validity 9125
```
Luego copiar `keystore.properties.example` → `keystore.properties` y completar `storeFile`/`storePassword`/`keyAlias`/`keyPassword`. **Respaldar el `.jks` + passwords en lugar seguro** (si se pierden, hay que pedirle a Google Play reset de la upload key). Recomendado además: activar **Play App Signing** (Google guarda la app signing key; vos solo manejás la upload key).

> El agente NO genera ni guarda la clave privada de firma: es material sensible que maneja Felipe.

## 4. Permisos finales del manifest (entregable) (req 3, 5)

Confirmado contra el manifest mergeado real de `playRelease` (sin atributo `android:debuggable` → **false**).

**Play (AAB) — permisos finales:**
| Permiso | Origen | Nota |
|---|---|---|
| `INTERNET` | app | API HumanOS + Firebase |
| `ACCESS_NETWORK_STATE` | Firebase/GMS | conectividad |
| `WAKE_LOCK` | Firebase/GMS | trabajos background de GMS |
| `…finsky…BIND_GET_INSTALL_REFERRER_SERVICE` | Firebase | install referrer (analítica de instalación) |
| `…gsf.permission.READ_GSERVICES` | GMS | lectura de config de Google services |

**Removidos en el flavor Play** (`tools:node="remove"`): `REQUEST_INSTALL_PACKAGES` (queda solo en sideload), `com.google.android.gms.permission.AD_ID`, `ACCESS_ADSERVICES_AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION` — HumanOS no usa ads ni ad-attribution.

**Solo `sideload`:** además `REQUEST_INSTALL_PACKAGES` (instalar el APK del auto-update).

> Si querés reducir aún más (ej. `BIND_GET_INSTALL_REFERRER_SERVICE`, `WAKE_LOCK`): evaluar si Firebase Analytics es necesario (§5 flag). Sin Analytics, casi todos estos desaparecen.

## 5. Data Safety (Play Console) — datos recolectados (entregable)

| Categoría | Dato | Recolectado | Vinculado al usuario | Propósito | Origen |
|---|---|---|---|---|---|
| Info personal | Nombre, Email | Sí | Sí | Gestión de cuenta / auth | Google Sign-In (Firebase Auth) |
| Actividad en la app | Tareas, check-ins, contenido HumanOS | Sí | Sí | Funcionalidad de la app | Backend humanos.eco (bridge) |
| Info y rendimiento | Crash logs, diagnósticos | Sí (si Analytics on) | Sí | Diagnóstico/estabilidad | Firebase Analytics/Crashlytics |
| IDs de dispositivo | App instance ID | Sí (si Analytics on) | Sí | Analítica | Firebase |
| ~~Advertising ID~~ | — | **No** | — | — | AD_ID removido |

**Prácticas de seguridad a declarar:** datos cifrados en tránsito (HTTPS); el usuario puede pedir eliminación de datos (flujo ARCO / Ley 21.719 ya en humanos-eco).

⚠️ **FLAGS para Felipe (sensibles, requieren decisión):**
- **Datos de salud:** los módulos salud/care muestran datos clínicos (vía WebView). Si el usuario los ve/maneja en la app, Play exige declarar **"Health and fitness"** y el privacy policy debe cubrirlos (coherente con Ley 21.719). Confirmar alcance.
- **Menores (estudiante):** si usan menores de edad, aplican Play Families Policy + Ley 21.719 (datos de menores). El privacy policy y el Data Safety deben contemplarlo.
- ¿Firebase **Analytics** está realmente activo? Si no se usa, conviene removerlo (y simplifica Data Safety + saca AD_ID de raíz).

## 6. Recomendación de política de privacidad mínima (entregable)

Hostear en URL estable (ej. `https://www.humanos.eco/legal/privacidad-app`) — Play Console la exige. Debe cubrir:
1. **Qué se recolecta:** identidad (nombre/email), contenido HumanOS (tareas, notas, y si aplica salud), diagnósticos/analítica.
2. **Para qué:** autenticación, funcionalidad, estabilidad.
3. **Terceros:** Google/Firebase (auth, analítica). Sin venta de datos. Sin publicidad.
4. **Base legal y derechos (Ley 21.719):** acceso, rectificación, cancelación, oposición (ARCO); cómo eliminar cuenta/datos; contacto del DPO.
5. **Retención** y **seguridad** (cifrado en tránsito).
6. **Menores** (si aplica estudiante) y **datos sensibles de salud** (si aplica).
7. **Contacto** y fecha de última actualización.

> HumanOS ya tiene el framework Ley 21.719 (DPO, EIPD, ARCO) en humanos-eco; conviene publicar una página de privacidad específica de la app que reuse ese contenido.

## 7. Audit de seguridad del release (req 9)

Revisión del código fuente (grep): **sin secretos hardcodeados, sin endpoints `http://` inseguros (los `http://` son namespaces XML), sin claves privadas en el repo.** Los matches de "bearer/token" son nombres de variables/KDoc + un fake de dev (`fake-bearer-token-for-dev`). El token real (bridge JWT) vive en el vault cifrado, no en código. `google-services.json` está gitignored. Minify (R8) en release ofusca y ayuda a stripear logs.
**Pendiente:** confirmar reglas proguard que stripeen `Log.*` en release si se quiere garantía dura.

## 8. Auto-update solo en sideload (req 4) — pendiente de cierre

Hecho: flag `ENABLE_APK_AUTOUPDATE` por flavor + `REQUEST_INSTALL_PACKAGES` fuera de Play. **Falta:** ocultar/desactivar el botón de "buscar actualización" en `feature-settings` cuando el flag es false (o vía `bool` de recurso override en `app/src/play/res`), para que el flavor Play no ofrezca update por APK.

## 9. Pendiente / próximos pasos
- [ ] Felipe: generar upload key + `keystore.properties` (§3) y decidir Play App Signing.
- [ ] Confirmar permisos exactos del manifest mergeado de `playRelease` (pegar acá).
- [ ] Gatear UI de update en `feature-settings` (§8).
- [ ] Decidir esquema de `versionCode` para Play (actual 16; Play exige monotónico creciente).
- [ ] Confirmar uso de Firebase Analytics + alcance de datos de salud/menores (§5).
- [ ] Validar que `bundlePlayRelease` pasa con minify (R8) — y firmar con la upload key.
