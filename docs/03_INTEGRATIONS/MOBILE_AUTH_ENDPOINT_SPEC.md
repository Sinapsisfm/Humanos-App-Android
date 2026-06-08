# Mobile Auth Exchange Endpoint — Implementation Spec for HumanOS

> Estado: SPEC LISTA, NO IMPLEMENTADA. Este endpoint se construye en el repo `humanos-eco`, NO en humanos-android.
> Creado: 2026-06-07 (Phase 2, ciclo 4)
> Desbloquea: el bridge real Firebase ID token -> HumanOS session (TASK-010, RISK-001, DEC-007)

## Por que existe

La app Android nativa autentica con Firebase (Google Sign-In) y obtiene un **Firebase ID token**. Pero HumanOS usa NextAuth con session cookies — un cliente mobile no puede usar esas cookies. Se necesita un endpoint que:

1. Reciba el Firebase ID token (verificable con Firebase Admin SDK)
2. Resuelva/cree el usuario HumanOS correspondiente
3. Devuelva un JWT de HumanOS (el "bridge token") que el app usa como Bearer en las demas llamadas

Actualmente el app Android tiene este flujo como MOCK (`FirebaseAuthRepository.getHumanosToken()` retorna null). Cuando este endpoint exista, se reemplaza el mock por una llamada real.

## Donde se implementa (humanos-eco)

Archivo nuevo: `humanos-eco/app/api/auth/mobile/exchange/route.ts`

Reutiliza codigo EXISTENTE de HumanOS (observado en lectura read-only):
- `lib/quebot/identity-bridge.ts` -> `generateBridgeToken({ userId, personId, email, name, orgIds })` (ya genera JWT HS256 con QUEBOT_BRIDGE_SECRET, TTL 15 min)
- `lib/identity/resolver.ts` -> `ensureCanonicalIdentity()` (resuelve/crea usuario canonico)
- Firebase Admin SDK (ya usado en el ecosistema para verificar tokens)

## Contrato

### Request
```
POST /api/auth/mobile/exchange
Content-Type: application/json
Authorization: Bearer <firebase-id-token>
```
(El Firebase ID token va en el header Authorization, NO en el body — evita logging accidental del token.)

Body opcional:
```json
{ "deviceId": "android-uuid", "platform": "android" }
```

### Response 200
```json
{
  "token": "<bridge-jwt-hs256>",
  "expiresIn": 900,
  "userId": "clx1abc123",
  "personId": "clx1per456",
  "email": "user@example.com",
  "displayName": "Felipe Mehr",
  "orgIds": ["org1"]
}
```

### Errores
- 401 `INVALID_FIREBASE_TOKEN` — token invalido/expirado/firma no verifica
- 403 `USER_NOT_PROVISIONED` — Firebase user valido pero sin cuenta HumanOS (decidir: auto-crear o rechazar)
- 429 `RATE_LIMITED` — demasiados intentos (rate limit por IP/uid)
- 503 `BRIDGE_SECRET_MISSING` — QUEBOT_BRIDGE_SECRET no configurado

## Implementacion (pseudocodigo route.ts)

```typescript
import { NextRequest, NextResponse } from "next/server";
import { getAuth } from "firebase-admin/auth";       // Firebase Admin SDK
import { generateBridgeToken } from "@/lib/quebot/identity-bridge";
import { ensureCanonicalIdentity } from "@/lib/identity/resolver";

export async function POST(req: NextRequest) {
  // 1. Extraer Firebase ID token del header
  const authHeader = req.headers.get("authorization");
  const firebaseToken = authHeader?.replace(/^Bearer /, "");
  if (!firebaseToken) {
    return NextResponse.json({ error: "Missing token", code: "INVALID_FIREBASE_TOKEN" }, { status: 401 });
  }

  // 2. Verificar el token con Firebase Admin SDK
  let decoded;
  try {
    decoded = await getAuth().verifyIdToken(firebaseToken);
  } catch {
    return NextResponse.json({ error: "Invalid token", code: "INVALID_FIREBASE_TOKEN" }, { status: 401 });
  }

  // 3. Resolver/crear el usuario HumanOS desde el email/uid de Firebase
  //    decoded.email, decoded.uid, decoded.name disponibles
  const identity = await ensureCanonicalIdentity({
    email: decoded.email,
    name: decoded.name,
    externalAuthProvider: "firebase",
    externalAuthId: decoded.uid,
  });
  if (!identity) {
    return NextResponse.json({ error: "Not provisioned", code: "USER_NOT_PROVISIONED" }, { status: 403 });
  }

  // 4. Generar el bridge JWT (reusa la funcion existente)
  const token = await generateBridgeToken({
    userId: identity.userId,
    personId: identity.personId,
    email: decoded.email,
    name: decoded.name,
    orgIds: identity.orgIds,
  });
  if (!token) {
    return NextResponse.json({ error: "Bridge not configured", code: "BRIDGE_SECRET_MISSING" }, { status: 503 });
  }

  // 5. Responder
  return NextResponse.json({
    token,
    expiresIn: 900,
    userId: identity.userId,
    personId: identity.personId ?? null,
    email: decoded.email,
    displayName: decoded.name,
    orgIds: identity.orgIds,
  });
}
```

## Seguridad

1. **Verificar SIEMPRE el Firebase token server-side** con Firebase Admin SDK. Nunca confiar en claims sin verificar.
2. **Verificar el `aud` (audience)** del Firebase token == project number de humanos-app (822563196400). Evita tokens de otros proyectos Firebase.
3. **Rate limiting** por IP y por uid (el endpoint actual de HumanOS ya tiene patrones de rate limit reutilizables).
4. **El bridge JWT es HS256 con QUEBOT_BRIDGE_SECRET** — mismo secret que ya usa el ecosistema. TTL corto (15 min). El app refresca cuando expira.
5. **NO logear el Firebase token ni el bridge JWT** (van en headers, no en body, para minimizar exposicion).
6. **CORS**: el endpoint solo lo llama el app nativo (no browser), no requiere CORS abierto. Si se llama desde WebView, restringir origin.

## Cambios en el app Android cuando exista el endpoint

En `FirebaseAuthRepository`:
- `getHumanosToken()`: llamar `POST /api/auth/mobile/exchange` con el Firebase token, cachear el bridge JWT + expiry
- `refreshHumanosToken()`: re-llamar el endpoint cuando el bridge JWT expire (o en 401)
- `AuthState.Authenticated.humanosToken`: poblar con el bridge JWT real (hoy null)
- Usar `integration-humanos` Retrofit service para la llamada
- El `core-network` AuthInterceptor inyecta el bridge JWT como Bearer en las demas llamadas a HumanOS

## Checklist de implementacion (para Felipe / cuando se autorice)

- [ ] Crear `app/api/auth/mobile/exchange/route.ts` en humanos-eco
- [ ] Verificar que `ensureCanonicalIdentity` soporta `externalAuthProvider: "firebase"` (puede requerir extension)
- [ ] Configurar Firebase Admin SDK en humanos-eco (service account de humanos-app)
- [ ] Agregar rate limiting al endpoint
- [ ] Tests: token valido, invalido, expirado, usuario no provisionado
- [ ] Deploy a Railway (humanos-eco)
- [ ] En humanos-android: reemplazar el mock de `getHumanosToken()` por la llamada real
