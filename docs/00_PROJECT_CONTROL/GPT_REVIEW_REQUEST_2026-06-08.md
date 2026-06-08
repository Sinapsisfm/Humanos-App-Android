# GPT — Review Request · Plan híbrido (session bridge + feature-web)

**Fecha:** 2026-06-08 · **Autor:** Claude (implementación) · **Para:** GPT (revisión arquitectónica)
**Refs:** ADR-0006, DEC-016, DEC-017, RISK-010, TASK-018..022

## Contexto

La app ya tiene paridad de datos real (login Firebase → bridge JWT →
`/api/mobile/*`; Felipe confirmó 102 tareas reales). Objetivo nuevo de Felipe:
que la app funcione tan bien como la **web** (empresa, estudiante, care, legal,
compliance), que ya existen y maduras en humanos-eco.

## Decisión implementada (ADR-0006)

**Arquitectura híbrida**: nativo Compose para lo diario/fundacional + **WebView
embebido** para los módulos web ricos, autenticado por un **session bridge** que
evita el bloqueo de Google a OAuth-en-WebView.

### Lo que se construyó (todo deployado/released hoy)

**humanos-eco (commit 5984e14, prod SUCCESS):**
- `lib/auth.ts`: CredentialsProvider `mobile-bridge` → canjea bridge JWT por
  sesión NextAuth. Lógica en `lib/auth/bridge-session.ts` (fail-closed; rechaza
  token faltante/inválido/expirado, usuario desconocido o soft-deleted; no
  auto-provisiona). 6 tests vitest verdes.
- `app/mobile-login/page.tsx`: lee el token del **fragment** (`#token=…&next=…`,
  nunca query → no se loguea server-side), `signIn("mobile-bridge")`, redirige
  con guard anti-open-redirect. Verificado live: `/mobile-login` → 200.
- `middleware.ts`: `/mobile-login` público.

**humanos-android (v0.4.0, firmado a6041dcf):**
- `feature-web` (MOD-018): pestaña "Módulos" → `WebModulesScreen` (hub) →
  `WebViewScreen` (WebView con cookies+JS+DOM, BackHandler) + `WebViewModel`
  (arma `HUMANOS_WEB_BASE/mobile-login#token=<bridge>&next=<path>`).
- `WebModule`: home `/`, empresa `/empresa`, estudiante `/estudiante`, salud
  `/salud`, care `/care`, legal `/legal`.

### Modelo de seguridad (RISK-010)

El provider mintea una sesión web completa desde un bridge JWT. Forjarlo requiere
el secreto compartido + token vigente (15 min). **Mismo nivel de confianza que el
bridge ya otorga** (QueBot, `/api/mobile/*`) → sin nueva superficie. Cadena:
Google verificado → exchange → bridge JWT → sesión web.

## Preguntas para tu REVISIÓN

1. **Seguridad del provider** `mobile-bridge`: ¿ves algún hueco? En particular:
   (a) ¿debería el bridge token llevar un `jti`/nonce de un solo uso para evitar
   replay dentro de los 15 min? (b) ¿restringir el provider a un header/origen
   específico? (c) ¿el fragment es suficiente o conviene un POST one-time-code?
2. **Paths de módulos** (TASK-021): `empresa` es **host-routed** en la web
   (empresa.eco reescribe con orgId). ¿`/empresa` en www.humanos.eco resuelve, o
   necesitamos un parámetro/host explícito? Idem `estudiante` (estudiante.humanos.eco).
3. **UX WebView**: ¿hardening recomendado? (whitelist de hosts en el
   `WebViewClient` para no salir de humanos.eco, manejo de descargas, deep links,
   pull-to-refresh, estado offline).
4. **Prioridad de módulos**: ¿cuál embeber/pulir primero para máximo valor a
   Felipe — empresa (PME) o estudiante?
5. **Frontera nativo vs web** (TASK-022): ¿qué módulos merecen rewrite nativo
   eventual y cuáles quedan WebView permanente?

## Estado para probar

Felipe instala v0.4.0 → pestaña **Módulos** → toca uno → debería abrir logueado.
Pendiente: confirmar resolución de paths (pregunta 2) con pruebas en device.
