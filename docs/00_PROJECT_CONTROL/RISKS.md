# RISKS -- humanOS Native Android

> Registro de riesgos identificados. Cada riesgo tiene probabilidad, impacto, severidad resultante, y estrategia de mitigacion concreta.

## Niveles

- **Probabilidad:** Alta / Media / Baja
- **Impacto:** Alto / Medio / Bajo
- **Severidad:** Critico / Alto / Medio / Bajo (combinacion de probabilidad x impacto)

---

## RISK-001: No existe endpoint mobile/exchange en HumanOS

- **Fecha identificado:** 2026-06-06
- **Severidad:** Alto
- **Probabilidad:** Alta (es un hecho, no existe)
- **Impacto:** Alto (sin este endpoint, la app no puede autenticarse con HumanOS en produccion)
- **Descripcion:** El endpoint `POST /api/auth/mobile/exchange` que permite intercambiar un Firebase ID token por un HumanOS bridge JWT no existe en el repo `humanos-eco`. Es un prerequisito para autenticacion real con HumanOS desde la app mobile.
- **Mitigacion:**
  1. Phase 1 usa mock gateway que simula respuestas del endpoint
  2. El contrato del endpoint se documenta en TASK-010 como especificacion de referencia
  3. La implementacion del endpoint es tarea del repo `humanos-eco`, no de este repo
  4. Feature flag `USE_MOCK_AUTH` controla si se usa mock o endpoint real
- **Owner:** Felipe (decision de cuando implementar en HumanOS)
- **Refs:** DEC-007, DEC-008, TASK-010

---

## RISK-002: Android Keystore vs Play signing key management

- **Fecha identificado:** 2026-06-06
- **Severidad:** Medio
- **Probabilidad:** Media
- **Impacto:** Alto (perder la signing key = no poder actualizar la app en Play Store)
- **Descripcion:** La gestion de claves de firma Android tiene dos niveles: la upload key (que usa el desarrollador para firmar antes de subir) y la app signing key (que Google Play usa para firmar el APK/AAB final). Si se pierde la upload key, se puede rotar con Google. Si se opta por NO usar Play App Signing y se pierde la signing key, la app queda huerfana en Play Store.
- **Mitigacion:**
  1. Usar Play App Signing (Google administra la key maestra)
  2. La upload key se almacena en GCP Secret Manager, no en el repo ni en maquinas locales
  3. Backup de la upload key en ubicacion offline segura (USB cifrado o similar)
  4. Documentar el proceso de rotacion de upload key
- **Owner:** Felipe (configuracion inicial)
- **Refs:** DEC-005

---

## RISK-003: Permisos camara/microfono rechazados por usuario

- **Fecha identificado:** 2026-06-06
- **Severidad:** Medio
- **Probabilidad:** Media (Android 13+ hace facil rechazar permisos)
- **Impacto:** Medio (feature-capture pierde funcionalidad pero la app sigue siendo util)
- **Descripcion:** La captura universal (foto, audio) requiere permisos de camara y microfono. Desde Android 13, el sistema facilita rechazar permisos individuales y el usuario puede revocarlos en cualquier momento. Si el usuario rechaza, la app debe funcionar sin esas capacidades.
- **Mitigacion:**
  1. Graceful degradation: si no hay permiso de camara, captura de texto y archivo siguen disponibles
  2. Solicitar permisos in-context (cuando el usuario toca el boton de foto/audio), no al inicio
  3. Explicar claramente por que se necesita cada permiso (rationale dialog)
  4. UI que muestra estado de permisos en Settings y permite ir a configuracion del sistema
  5. Nunca solicitar permisos al inicio de la app (anti-patron que causa rechazo masivo)
- **Owner:** Equipo Android
- **Refs:** DEC-002, REQ-CAP-001, REQ-ANDROID-001

---

## RISK-004: ONNX Runtime Mobile performance en low-end devices

- **Fecha identificado:** 2026-06-06
- **Severidad:** Bajo (Phase 3, no inmediato)
- **Probabilidad:** Baja
- **Impacto:** Medio (degradacion de UX en inferencia local)
- **Descripcion:** Phase 3 contempla IA local on-device usando ONNX Runtime Mobile para inferencia de modelos pequenos (clasificacion, NER). En dispositivos de gama baja (< 4GB RAM, CPU lento), la inferencia puede ser inaceptablemente lenta o causar OOM.
- **Mitigacion:**
  1. Benchmark en dispositivos representativos antes de habilitar por defecto
  2. Gateway pattern permite fallback automatico a `REMOTE_ONLY` si el dispositivo no cumple requisitos minimos
  3. Deteccion de capacidades del dispositivo (RAM, CPU cores) en runtime
  4. Modelos cuantizados (INT8) para reducir footprint
- **Owner:** Equipo Android (Phase 3)
- **Refs:** DEC-002, REQ-AI-001

---

## RISK-005: Health Connect API breaking changes

- **Fecha identificado:** 2026-06-06
- **Severidad:** Bajo (Phase 3, no inmediato)
- **Probabilidad:** Baja (API estabilizada en Android 14)
- **Impacto:** Bajo (Health Connect es feature opcional)
- **Descripcion:** Health Connect (la API unificada de datos de salud de Android) fue promovida a estable en Android 14, pero sigue evolucionando. Versiones futuras podrian cambiar schemas de datos, permisos, o comportamiento de sync.
- **Mitigacion:**
  1. Health Connect es un modulo opcional (`integration-healthconnect`), no esta en el path critico
  2. Usar la version de la libreria de compatibilidad (`androidx.health.connect:connect-client`) que abstrae diferencias de version
  3. Feature flag para habilitar/deshabilitar Health Connect sin afectar el resto de la app
  4. No almacenar datos de salud en Room local; solo leerlos on-demand desde Health Connect
- **Owner:** Equipo Android (Phase 3)
- **Refs:** DEC-002, REQ-HEALTH-001

---

## RISK-006: Firebase project humanos-app quota limits

- **Fecha identificado:** 2026-06-06
- **Severidad:** Medio
- **Probabilidad:** Media
- **Impacto:** Medio (throttling o costos inesperados si se exceden quotas)
- **Descripcion:** El proyecto Firebase `humanos-app` puede tener otros servicios activos (web apps, Cloud Functions, Firestore) que ya consumen quota. Agregar una app Android con potencialmente miles de usuarios puede empujar el proyecto a limites de quota de Auth (10K users gratis), Firestore reads, o Cloud Messaging.
- **Mitigacion:**
  1. ✅ HECHO (2026-06-07): Budget alert "humanos-app Firebase guard" creado en GCP Billing. $10/mes con alertas a $5 (50%), $9 (90%), $10 (100%) por email a admins + propietarios. Scope: solo proyecto humanos-app
  2. Monitorear billing y quota dashboards en GCP Console
  3. Si quota es un problema, evaluar Firebase project separado para la app Android (implica resolver Q-001)
  4. Phase 1-2 con Auth/Analytics no genera carga real significativa (capa gratis Blaze: 50K MAU)
- **Owner:** Felipe
- **Refs:** Q-001, RISK-009

---

## RISK-009: Blaze enciende servicios de consumo variable sin control

- **Fecha identificado:** 2026-06-07 (observacion de GPT al habilitar Firebase)
- **Severidad:** Medio
- **Probabilidad:** Baja (con mitigacion)
- **Impacto:** Alto (factura inesperada si se activan servicios sin limite)
- **Descripcion:** Al habilitar Firebase, humanos-app quedo en plan Blaze (pago por uso) porque el GCP project ya tenia billing habilitado. Auth (50K MAU) y Analytics son gratis en Blaze. El riesgo NO es Auth/Analytics — es encender Cloud Functions, Cloud Storage, Firestore, o logging sin limites, que cobran por uso por encima de la capa gratis.
- **Mitigacion:**
  1. ✅ HECHO: Budget alert a $10/mes (RISK-006 mitigacion 1)
  2. NO activar Cloud Functions / Storage / Firestore / Cloud Run nuevos sin decision explicita de Felipe + revision de pricing
  3. Cualquier servicio de consumo variable requiere su propio limite/quota antes de activarse
  4. Phase 2 usa solo Auth + Analytics (gratis). El bridge HumanOS no usa servicios GCP de humanos-app
- **Owner:** Felipe + Claude (no encender servicios sin confirmacion)
- **Refs:** RISK-006, DEC-005

---

## RISK-007: applicationId elegido prematuramente

- **Fecha identificado:** 2026-06-06
- **Severidad:** Alto
- **Probabilidad:** Media
- **Impacto:** Alto (irreversible en Play Store despues del primer upload)
- **Descripcion:** El `applicationId` (ej. `eco.humanos.android`) se vuelve permanente una vez que se sube el primer build a Google Play Store. No se puede cambiar despues. Si se elige un applicationId que luego no se alinea con la marca, el dominio, o la estructura corporativa, no hay forma de corregirlo sin publicar una app completamente nueva (perdiendo reviews, ratings, install base).
- **Mitigacion:**
  1. No subir a Play Store ni Firebase App Distribution hasta que Felipe confirme explicitamente el applicationId (TASK-009)
  2. El skeleton Gradle puede usar un applicationId provisional (ej. `eco.humanos.android.dev`) marcado claramente como temporal
  3. Documentar alternativas evaluadas en Q-003 para que la decision sea informada
  4. Verificar que el applicationId no este tomado en Play Store antes de confirmar
- **Owner:** Felipe
- **Refs:** TASK-009, Q-003

---

## RISK-008: Licencia MIT publicada sin claridad sobre propiedad intelectual

- **Fecha identificado:** 2026-06-07
- **Severidad:** Medio
- **Probabilidad:** Media
- **Impacto:** Medio (potencial exposicion de IP si se hace open-source sin intencion)
- **Descripcion:** El repo se inicializo con licencia MIT. Si el proyecto contiene logica de negocio propietaria de Sinapsis SpA (algoritmos de Context Engine, logica de agent, IP de QueBot adaptada), la licencia MIT permitiria uso irrestricto por terceros.
- **Mitigacion:**
  1. Felipe debe confirmar si el proyecto sera open-source (MIT) o propietario (Q-005)
  2. Si propietario, cambiar LICENSE a copyright notice sin permiso de uso
  3. Si open-source, asegurar que no se incluya IP propietaria de HumanOS/QueBot
  4. Phase 1 skeleton no contiene logica propietaria, pero esto cambia en Phase 2+
- **Owner:** Felipe
- **Refs:** Q-005

---

## Matriz de riesgos

| ID | Riesgo | Probabilidad | Impacto | Severidad | Phase |
|----|--------|-------------|---------|-----------|-------|
| RISK-001 | Endpoint mobile/exchange no existe | Alta | Alto | Alto | 1 |
| RISK-007 | applicationId prematuro | Media | Alto | Alto | 1 |
| RISK-002 | Keystore / signing key mgmt | Media | Alto | Medio | 1 |
| RISK-003 | Permisos camara/mic rechazados | Media | Medio | Medio | 1 |
| RISK-006 | Firebase quota limits | Media | Medio | Medio | 1 |
| RISK-008 | Licencia MIT sin claridad IP | Media | Medio | Medio | 1 |
| RISK-009 | Blaze servicios consumo variable | Baja | Alto | Medio | 2 |
| RISK-004 | ONNX Runtime low-end devices | Baja | Medio | Bajo | 3 |
| RISK-005 | Health Connect breaking changes | Baja | Bajo | Bajo | 3 |

---

## RISK-010 — Session bridge emite una sesion web completa desde un bridge JWT (2026-06-08)

**Probabilidad:** Baja · **Impacto:** Alto · **Severidad:** Medio · **Phase:** 2 · **Ref:** ADR-0006, DEC-017

El provider `mobile-bridge` mintea una sesion NextAuth completa a partir de un
bridge JWT. Forjar uno requiere el secreto compartido (QUEBOT_BRIDGE_SECRET /
QUEBOT_BACKEND_TOKEN) + token no expirado (15 min, issuer humanos.eco / audience
quebot). Es el MISMO nivel de confianza que el bridge ya otorga a QueBot y a
`/api/mobile/*` — sin nueva superficie de ataque.

**Mitigaciones:** fail-closed ante token invalido/expirado, usuario desconocido o
soft-deleted; no auto-provisiona; el token viaja en el fragment (no se loguea
server-side) y en un POST HTTPS; 6 tests de seguridad (`bridge-session.test.ts`);
la rotacion del secreto reinicia ambos servicios Railway.

| RISK-010 | Session bridge emite sesion web desde bridge JWT | Baja | Alto | Medio | 2 |
