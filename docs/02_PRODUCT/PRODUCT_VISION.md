# Product Vision

> humanOS Native Android
> Last updated: 2026-06-06

## One-Line Vision

humanOS Android is a personal operating system that captures, understands context, organizes daily life, and converts everything into next actions -- using the phone as a personal sensor.

## What humanOS Is

humanOS is not a chatbot with a UI bolted on. It is not a todo app with AI features. It is not a health tracker that also takes notes.

humanOS is an **agent that uses the app**. The user does not operate humanOS -- humanOS operates on behalf of the user. The app is the interface through which the agent observes, remembers, and acts.

### The Phone as a Personal Sensor

A modern smartphone carries:
- A camera (visual capture)
- A microphone (voice capture)
- GPS (location awareness)
- Accelerometer, gyroscope, barometer (motion and environment)
- Bluetooth/NFC (proximity to devices and places)
- Health Connect (body signals from wearables)
- A calendar, contacts, and notification stream (social context)

humanOS treats all of these as inputs to a unified context model. Every sensor reading, every capture, every interaction becomes a node in the context graph. The agent uses this graph to understand what matters right now and what action to take next.

## Core Capabilities

### 1. Context Engine
The user's world modeled as a graph. People, places, projects, events, tasks, captures, decisions -- all connected by weighted, typed relationships. The graph grows organically from daily use and becomes the foundation for every other capability.

### 2. Universal Capture
Anything the user sees, hears, thinks, or encounters can be captured in under 3 seconds. Text, voice, photo, shared link -- one tap, captured, automatically tagged with time, location, and inferred context. No filing, no categorization burden. The context engine handles organization later.

### 3. Proactive Daily Agent
Every morning: a briefing of what matters today, surfaced from the context graph and task list. Every evening: a review of what was accomplished, what was deferred, what context changed. Between those: proactive nudges when the agent detects something actionable (a task due, a person nearby, a pattern worth noting).

### 4. Operative Memory
The agent remembers what the user told it, what decisions were made, what was delegated and to whom, what was deferred and why. This is not search -- it is memory. "What did I decide about the pricing model?" returns the decision node with its rationale, date, and linked context.

### 5. Health and Energy Awareness
Steps, sleep, heart rate, medications, check-ins -- aggregated into a daily energy score. The agent adjusts its recommendations based on how the user is doing physically. Low energy day? Fewer tasks surfaced, simpler actions suggested.

### 6. Field Assistant
For users who work in the field (clinicians, engineers, consultants): GPS tracking, location-aware captures, geofence triggers, offline operation, and tap-to-check-in with NFC. The phone becomes a work companion that logs context automatically while the user focuses on their work.

### 7. Private Vault
Some things should never leave the device. The vault provides biometric-locked, encrypted, local-only storage for sensitive documents, notes, and records. No sync, no cloud, no backup. Hardware-backed encryption.

## Design Principles

1. **Capture is faster than forgetting.** If it takes more than 3 seconds to capture a thought, the user will forget it instead. Every capture path is optimized for speed.

2. **Context is inferred, not entered.** The user should never have to answer "what project is this for?" or "who is this about?" The context engine infers relationships from time, location, content, and recent activity.

3. **Privacy is a feature, not a setting.** The default is local-first. Sync is opt-in. The vault exists for the most sensitive data. The user controls what leaves the device.

4. **Offline is normal, not degraded.** The app works fully offline. Sync happens when connectivity is available. The user never sees a "no connection" error blocking their workflow.

5. **The agent acts, the user decides.** The agent surfaces information and suggests actions. It does not take irreversible actions without confirmation. Trust is built incrementally through accurate suggestions and transparent reasoning.

## Who This Is For

### Primary User: The Overloaded Professional
- Manages dozens of active relationships (patients, clients, team members, family)
- Works across multiple contexts per day (office, field, home)
- Makes high-stakes decisions that need to be recorded and recalled
- Cannot afford to lose information or forget commitments
- Values privacy because their work involves sensitive data

### Secondary User: The Structured Self-Optimizer
- Tracks health, habits, and energy
- Uses task management tools but wants them connected to context
- Wants a system that learns their patterns and adapts

### NOT For
- Users who want a simple todo list (use Todoist)
- Users who want a chatbot companion (use ChatGPT)
- Users who want a health tracker (use Samsung Health)
- Users who distrust on-device AI processing

## Relationship to HumanOS Web

humanOS Android is a **companion** to the HumanOS web platform, not a replacement.

| Concern | Web (`humanos.eco`) | Android |
|---|---|---|
| Primary use case | Desktop workflow, admin, team management | Field capture, mobile context, sensor data |
| Data authority | Server (PostgreSQL) | Local (Room) + sync |
| Auth | NextAuth (email/password, OAuth) | Firebase (Google Sign-In) + bridge JWT |
| Offline | Limited (PWA cache) | Full (Room as source of truth) |
| AI inference | Server-side (Anthropic, Vertex AI) | On-device (Phase 3) + server-side via QueBot |
| Health data | Displayed (synced from Android) | Collected (Health Connect, sensors) |

The Android app is the primary data collector. The web platform is the primary data analyzer and team coordinator. Both share the same context graph via sync.

## Relationship to QueBot

QueBot is the AI conversational layer. On Android, QueBot appears as a chat interface that can:
- Answer questions using the context graph
- Execute searches (products, legal databases, medical references)
- Generate summaries and reports
- Provide decision support

QueBot on Android uses SSE streaming for real-time responses, the same protocol as the web version. The Android app adds mobile-specific context (location, recent captures, health data) to QueBot queries for better answers.

## Success Metrics (North Stars)

| Metric | Target | Measured By |
|---|---|---|
| Captures per day | 5+ average across active users | Local analytics (privacy-preserving) |
| Time to capture | Under 3 seconds from intent to saved | UX instrumentation |
| Context accuracy | 80%+ of inferred context links are correct | User confirmation/dismissal ratio |
| Daily review completion | 60%+ of active users complete morning review | Local analytics |
| Offline resilience | Zero workflow-blocking errors when offline | Error tracking (local) |
