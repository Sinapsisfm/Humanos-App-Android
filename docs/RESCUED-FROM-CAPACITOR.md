# Rescate desde el shell Capacitor → app nativa (2026-06-21)

## Contexto

Antes de esta app nativa Kotlin (`eco.humanos.android`, Jetpack Compose,
multi-módulo), hubo trabajo de micrófono y notificaciones construido sobre el
**shell Capacitor** del repo `humanos-eco`, en las ramas:

- `feature/native-mic-stt`
- `feature/android-notifications`

Ese trabajo **NO llega a esta app nativa**: vivía en el WebView/Capacitor de
`humanos-eco` (otro repo, otra arquitectura). No es portable tal cual — son
plugins JS/Capacitor, no código Android nativo. Por lo tanto se considera
muerto para este proyecto.

## Qué se rescató conceptualmente acá

La **UX** de "avisar cuando el agente responde en el chat", reimplementada de
forma 100% nativa:

- **WorkManager** (`AgentReplyPollWorker`, periódico, mínimo 15 min) consulta el
  hilo founder ↔ Claude vía el endpoint ya existente
  `GET /api/mobile/message?since=…` reutilizando el gateway Retrofit y el
  **bridge JWT** existentes (sin token hardcodeado, sin stack HTTP nuevo).
- Compara el id/timestamp del último mensaje `assistant` contra un valor
  guardado en **DataStore** (`NotificationPreferences` en `core-datastore`).
- Si hay un mensaje nuevo del agente, dispara una **notificación local** vía
  `NotificationManagerCompat` + canal "Mensajes del agente"
  (`NotificationHelper` en el nuevo módulo `core-notifications`).
- Permiso `POST_NOTIFICATIONS` declarado en el manifest; en API 33+ se pide en
  runtime desde `MainActivity`.
- El poller se agenda al arranque desde `HumanosApp` (Application).

**Sin Firebase Cloud Messaging. Sin secretos. Sin tocar `google-services.json`.
Sin cambios de backend** (el endpoint `GET /api/mobile/message` ya existía,
TASK-027 / ADR-0006).

## Lo que queda PENDIENTE (mitad app-cerrada, requiere OK de Felipe)

La mitad "push real cuando la app está cerrada / sin abrir hace rato" necesita
**FCM (Firebase Cloud Messaging)**, que está **fuera de alcance** por requerir
secretos + coordinación de Firebase:

- Proyecto Firebase `humanos-app` (id `822563196400`).
- Service account / server key para enviar pushes desde el backend.
- Un endpoint backend nuevo que registre el token FCM del dispositivo y empuje
  notificaciones cuando el agente responde (hoy el agente solo escribe en
  `ConversationMessage` vía `POST /api/mobile/message`).
- `FirebaseMessagingService` nativo + manejo de token + canal.

Esa mitad **NO** se implementó acá a propósito: toca secretos y configuración de
Firebase = decisión y autorización de Felipe.

## Limitación conocida del enfoque WorkManager (sin FCM)

WorkManager periódico tiene piso de 15 min y el sistema puede diferirlo bajo
Doze/Battery Optimization. Es decir: la notificación local puede llegar con
atraso (minutos) y no es "instantánea" como sería un push FCM. Es el costo de la
mitad segura sin secretos. La inmediatez real es justamente lo que aportaría la
mitad FCM pendiente.

---

_Dated 2026-06-21. Rama `feature/native-notifications`._
