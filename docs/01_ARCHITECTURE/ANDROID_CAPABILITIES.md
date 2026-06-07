# Android Capabilities

> humanOS Native Android -- Device Capability Map
> Last updated: 2026-06-06

## Overview

humanOS Android leverages the phone as a personal sensor array. Each capability is introduced in a specific phase to keep the MVP focused while building toward a fully-aware personal operating system.

## Capability Matrix

| Capability | Android API / Library | Permission(s) | Phase | Module | Use Case |
|---|---|---|---|---|---|
| **Camera** | CameraX | `CAMERA` | Phase 2 | feature-capture | Photo capture for notes, document scanning, OCR input. |
| **Microphone** | MediaRecorder / AudioRecord | `RECORD_AUDIO` | Phase 2 | feature-capture | Voice memos, transcription input for captures. |
| **Location (foreground)** | FusedLocationProviderClient | `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | Phase 2 | feature-terrain, feature-capture | Geotagging captures, field assistant, place detection. |
| **Location (background)** | FusedLocationProviderClient | `ACCESS_BACKGROUND_LOCATION` | Phase 3 | feature-terrain | Geofence triggers, automatic context switching, route tracking. |
| **Health Connect** | Health Connect API | Health Connect permissions (per data type) | Phase 3 | data-health, feature-health | Steps, sleep, heart rate, blood pressure, medications, exercise. |
| **Push Notifications** | FCM (Firebase Cloud Messaging) | `POST_NOTIFICATIONS` (Android 13+) | Phase 2 | core-notifications | Reminders, sync alerts, daily review prompts, proactive agent nudges. |
| **Local Notifications** | NotificationManager + AlarmManager | `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM` | Phase 2 | core-notifications | Task due reminders, medication reminders, timer completions. |
| **Biometrics** | BiometricPrompt | none (user enrollment required) | Phase 1 | core-security | VAULT access gate, sensitive action confirmation. |
| **Bluetooth / BLE** | BluetoothAdapter, BluetoothLeScanner | `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` | Phase 3 | core-bluetooth | Wearable data bridge (heart rate, activity from smartwatch). |
| **NFC** | NfcAdapter | `NFC` | Phase 3 | feature-terrain | Tap-to-check-in at locations, read NFC tags for quick actions. |
| **Accelerometer** | SensorManager (TYPE_ACCELEROMETER) | none | Phase 3 | data-health | Activity detection (walking, running, stationary), fall detection. |
| **Gyroscope** | SensorManager (TYPE_GYROSCOPE) | none | Phase 3 | data-health | Orientation tracking, combined with accelerometer for motion analysis. |
| **Barometer** | SensorManager (TYPE_PRESSURE) | none | Phase 3 | feature-terrain | Altitude estimation, floor detection, weather correlation. |
| **Share Sheet (inbound)** | Intent filters (ACTION_SEND) | none | Phase 2 | feature-capture | Receive shared text, URLs, images from other apps as captures. |
| **Share Sheet (outbound)** | ShareCompat / Intent | none | Phase 2 | feature-tasks, feature-capture | Share tasks, notes, captures to other apps. |
| **Background Work** | WorkManager | none (special) | Phase 2 | domain-sync | Periodic sync, deferred uploads, retry with exponential backoff. |
| **Foreground Service** | ForegroundService | `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION` | Phase 3 | feature-terrain | Continuous location tracking during field work sessions. |
| **App Widgets** | Glance (Jetpack) | none | Phase 3 | feature-widgets | Home screen widgets: daily summary, quick capture, next task, energy score. |
| **Picture-in-Picture** | PiP mode | none | Phase 3 | feature-quebot | QueBot chat overlay while using other apps. |
| **File Provider** | FileProvider | none | Phase 2 | feature-capture | Share captured photos/documents with other apps securely. |

## Phase Breakdown Summary

### Phase 1 (current)

| Capability | Notes |
|---|---|
| Biometrics | `BiometricPrompt` wrapper in `core-security`. Used for VAULT access. Graceful fallback if no biometric hardware. |

Only biometrics are active in Phase 1. All other capabilities have their interfaces defined in the relevant modules but are not wired to real implementations.

### Phase 2

| Capability | Notes |
|---|---|
| Camera | CameraX with ImageCapture use case. Photo preview + crop. No video in Phase 2. |
| Microphone | MediaRecorder for voice memos. WAV/AAC format. Max 10 minutes. Transcription via QueBot API. |
| Location (foreground) | Foreground-only. Used for geotagging captures and showing nearby places. |
| Push Notifications | FCM token registration on auth. Server sends daily review reminders, sync alerts. |
| Local Notifications | Task due reminders, medication schedule (from Health data). Exact alarms for time-critical items. |
| Share Sheet | Inbound: text, URLs, images → create Capture. Outbound: share tasks and notes. |
| Background Work | WorkManager: periodic sync (every 15 min when connected), deferred upload queue, cleanup jobs. |
| File Provider | Secure file sharing for captured images and documents. |

### Phase 3

| Capability | Notes |
|---|---|
| Health Connect | Read-only access to steps, sleep, heart rate, blood pressure, medications, exercise. Daily aggregations cached in Room. |
| Location (background) | Geofence registration for work/home/clinic. Automatic context switching on arrival/departure. |
| Bluetooth / BLE | Scan and connect to wearables. Read heart rate, step count, battery level. Continuous background sync via foreground service. |
| NFC | Read NDEF tags for quick actions (e.g., tap badge to check in, tap tag to open project). |
| Sensors | Accelerometer + gyroscope for activity detection. Barometer for altitude. Batched readings via SensorManager. |
| Foreground Service | Long-running location tracking for field work sessions. Persistent notification with session controls. |
| Widgets | Glance widgets with RemoteViews fallback. Refresh via WorkManager periodic tasks. |
| Picture-in-Picture | QueBot chat in floating window. Minimum size 240x135 dp. |

## Permission Request Strategy

Permissions are never requested at app launch. Each permission is requested **in context** when the user first uses a feature that requires it.

### Request Flow

```
User taps "Add Photo" in Capture screen
    |
    v
Check CAMERA permission status
    |
  granted? ──yes──→ Open CameraX preview
    |
    no
    |
    v
Show rationale dialog:
  "humanOS needs camera access to capture photos
   for your notes and documents."
    |
  [Allow] → requestPermission(CAMERA)
  [Not Now] → dismiss, show manual option (gallery picker)
    |
    v
System permission dialog
    |
  granted? ──yes──→ Open CameraX preview
    |
  denied (first time)? ──→ Show feature degraded, offer alternative
    |
  denied (permanent)? ──→ Show settings deep-link dialog
```

### Permission Grouping

Related permissions are requested together when it makes contextual sense:

| Feature Trigger | Permissions Requested Together |
|---|---|
| First voice memo | `RECORD_AUDIO` |
| First photo capture | `CAMERA` |
| First location tag | `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` |
| First geofence setup | `ACCESS_BACKGROUND_LOCATION` (after foreground already granted) |
| First BLE scan | `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT` |
| First notification | `POST_NOTIFICATIONS` |

## Hardware Compatibility

| Feature | Minimum Hardware | Fallback |
|---|---|---|
| Biometrics | Fingerprint or face sensor | Device PIN/pattern (reduced security, VAULT disabled) |
| Camera | Any rear camera | Gallery picker for image selection |
| NFC | NFC chip | Manual entry, QR code alternative |
| BLE | Bluetooth 4.0+ | Manual data entry |
| Barometer | Pressure sensor | GPS altitude (less accurate) |
| Health Connect | Android 14+ or Health Connect app installed | Manual health data entry |
