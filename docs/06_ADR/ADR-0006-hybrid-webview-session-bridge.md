# ADR-0006: Hybrid Native + Embedded-WebView Architecture (Session Bridge)

**Status:** Accepted

**Date:** 2026-06-08

**Deciders:** Felipe Mehr (Sinapsis SpA) · implementación Claude · revisión GPT

## Context

The native app (Phase 1–3 so far) reached real data parity for the **daily /
foundational** surface: dashboard (snapshot, counts, today's check-in), tasks
(list + create + toggle, server-synced via `/api/mobile/*`), capture
(text/voice/photo), Google Sign-In → bridge-JWT. See `CHANGELOG_PROJECT.md`
entries for v0.3.0–v0.3.2.

Felipe's goal (2026-06-08): the app should "funcionar tan bien como humanos,
empresa, estudiante, etc. **web**". The web (humanos-eco, Next.js 15) already
ships rich, mature modules that would take **months** to rebuild natively:

- **empresa.eco** — PME suite (13+ Prisma models: products, risks, KPIs,
  vendors, stakeholders, frameworks, …)
- **estudiante.humanos.eco** — student vertical (Classroom import, study plans,
  calendar, materials)
- **care** — 3-role caregiving module
- **legal** — SII / PJUD flows
- compliance (Ley 21.719), workspace, etc.

### The Capacitor finding (rescued idea, not the artifact)

humanos-eco contains a Capacitor project (`capacitor.config.ts`, `android/`,
`@capacitor/* v8`). Its `build-apk.yml` produces an `out/index.html` that **only
redirects to `https://www.humanos.eco`** — i.e. a thin WebView shell over the
live site. **Not shippable standalone:** Google blocks OAuth inside embedded
WebViews (`disallowed_useragent`), so login would break, and it offers zero
native capability. But the *idea* — render the existing web UI inside the app —
is the fastest path to module parity **if** we solve auth without OAuth-in-WebView.

### The auth gap

The web authenticates with **NextAuth** (JWT session cookie). The native app
holds a **Firebase session + bridge JWT**, never a NextAuth cookie. A WebView
opened cold to `/empresa` is logged out, and we cannot do Google OAuth inside it.

## Decision

Adopt a **hybrid architecture**:

| Surface | Implementation | Why |
|---|---|---|
| Daily / foundational (dashboard, tasks, capture, health, check-in, context) | **Native Compose** (already underway) | Native UX, offline, sensors, camera/voice, push |
| Rich existing web modules (empresa, estudiante, care, legal, compliance, workspace) | **Embedded WebView**, authenticated via a **session bridge** | Months → days to parity; one source of truth (the web) |

### Session bridge (the unlock) — DEC-016

The native app already proved identity (Firebase Google → verified) and holds a
short-lived bridge JWT (15 min, HS256, `verifyBridgeToken`). We convert that into
a real NextAuth web session **without** OAuth-in-WebView:

1. **NextAuth gains a `mobile-bridge` Credentials provider** (humanos-eco
   `lib/auth.ts`). Its `authorize({ token })` runs `verifyBridgeToken(token)`,
   loads the user by `payload.uid`, and returns the NextAuth user. The existing
   `jwt`/`session` callbacks then issue the normal JWT session — so an embedded
   session is **indistinguishable** from a web login downstream (role,
   revocation, Person claim all apply).
2. A public **`/mobile-login`** page reads the bridge token (URL fragment, never
   a logged query param) and calls `signIn("mobile-bridge", { token, callbackUrl })`.
   NextAuth POSTs the token to `/api/auth/callback/mobile-bridge` over HTTPS,
   sets the session cookie, and redirects to the requested module.
3. The native **WebView screen** (`feature-web`) loads
   `https://www.humanos.eco/mobile-login#token=<bridgeJWT>&next=<modulePath>`
   with cookies + JS + DOM storage enabled. After the redirect it is a
   logged-in browser scoped to that module.

```
Native (Firebase verified) ──bridge JWT──▶ WebView /mobile-login#token=…&next=/empresa
                                                │ signIn("mobile-bridge", token)
                                                ▼
                              NextAuth verifyBridgeToken → user → JWT session cookie
                                                │ redirect
                                                ▼
                                      WebView shows /empresa, logged in
```

### Security analysis (RISK-010)

- The provider mints a **full web session from a bridge JWT**. Forging one needs
  `QUEBOT_BRIDGE_SECRET`/`QUEBOT_BACKEND_TOKEN` (the shared bridge secret) and a
  non-expired token (15 min, issuer `humanos.eco`, audience `quebot`). This is
  the **same trust level the bridge already grants** to QueBot and to
  `/api/mobile/*` — **no new attack surface** beyond the existing bridge.
- The bridge token is only obtainable after Firebase Google sign-in (verified
  email). Chain: Google (verified) → exchange → bridge JWT → web session.
- The provider **does not auto-provision**: it only resolves an existing user
  (they exist, since the exchange that issued the token provisions them).
- Token never travels in a server-logged URL (fragment only); `signIn` sends it
  in an HTTPS POST body.
- Mitigation if the bridge secret ever rotates/leaks: the provider fails closed
  (invalid token → `null` → no session), and the secret rotation already
  restarts both Railway services (see root CLAUDE.md).

### Boundaries (keeps ADR-0001 intact)

- The WebView is a **consumer** of the web, not a fork of it. No web code is
  copied into the Android repo.
- `feature-web` is isolated; it depends only on `data-auth` (for the bridge
  token) and `core-ui`. Native modules never depend on it.
- Each embedded module is a single declarative entry (module path + label);
  adding one is config, not new architecture.

## Consequences

### Easier
- **Parity in days, not months** for empresa/estudiante/care/legal/etc.
- **Single source of truth**: web fixes/features appear in the app instantly.
- **Incremental**: embed one module at a time; native rewrites happen later only
  where native UX genuinely wins.

### Harder / risks
- **WebView UX** isn't native (scroll, back-button, offline). Mitigated by
  keeping daily-use surfaces native and reserving WebView for rich/occasional
  modules.
- **Auth coupling**: a second way to mint a session touches the core NextAuth
  config. Mitigated by an additive provider + unit tests + the security analysis
  above (RISK-010).
- **Token lifetime**: 15-min bridge token; a long WebView session relies on the
  NextAuth cookie (30-day JWT) once established, so expiry mid-use is a non-issue
  after first login. Re-entry re-mints.

## References

- DEC-016: Hybrid native + embedded-WebView via session bridge
- DEC-017: NextAuth `mobile-bridge` Credentials provider
- RISK-010: Embedded session bridge mints a full web session from a bridge JWT
- TASK-018..TASK-022: session-bridge + feature-web rollout (see TASKS.md)
- ADR-0001: Project separation (web is an external, read/consumed source)
- ADR-0003: Read-only integration strategy / dual-token
- `app/api/auth/mobile/exchange/route.ts`, `lib/quebot/identity-bridge.ts` (humanos-eco)
