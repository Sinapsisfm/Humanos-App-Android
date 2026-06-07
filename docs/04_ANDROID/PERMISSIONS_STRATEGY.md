# Permissions Strategy

> humanOS Native Android -- Runtime Permission Model
> Last updated: 2026-06-06

## Principles

1. **Minimal surface**: request only what the active feature needs, when it needs it.
2. **Rationale first**: always show a rationale dialog _before_ the system prompt.
3. **Graceful degradation**: every feature must work (or clearly explain why it cannot) when a permission is denied.
4. **Never bulk-request**: permissions are requested one at a time, triggered by user action.
5. **Re-ask ceiling**: after two denials, stop asking and surface a Settings deep-link instead.

## Permission Inventory

| Permission | Category | Phase | Rationale (shown to user) | Play Store Risk |
|---|---|---|---|---|
| `INTERNET` | Core MVP | 1 | Network access for sync and AI processing | Low |
| `CAMERA` | Core MVP | 1 | Photo capture for field inspections, document scanning, and visual context | Medium -- requires Privacy Policy disclosure |
| `RECORD_AUDIO` | Core MVP | 1 | Voice capture for dictation, voice commands, and audio notes | Medium -- requires Privacy Policy disclosure |
| `POST_NOTIFICATIONS` | Core MVP | 1 | Daily briefings, task reminders, and sync status alerts (Android 13+ runtime) | Low |
| `ACCESS_FINE_LOCATION` | Optional | 2 | Geo-reference captures to physical locations during field inspections | Medium -- requires prominent disclosure |
| `ACCESS_COARSE_LOCATION` | Optional | 2 | Area-level detection for contextual suggestions (city/zone) | Low |
| `READ_CONTACTS` | Optional | 2 | Context engine enrichment -- link captures and tasks to known contacts | High -- requires strong justification and Permissions Declaration Form |
| `READ_CALENDAR` | Optional | 2 | Context engine -- surface relevant tasks and briefings based on upcoming events | High -- requires Permissions Declaration Form |
| `ACTIVITY_RECOGNITION` | Sensitive | 3 | Health signals -- detect walking, running, cycling for energy/wellness tracking | Medium -- requires Privacy Policy update |
| `BODY_SENSORS` | Sensitive | 3 | Heart rate and other body sensor data for health dashboard | High -- requires Health data policy compliance |
| `health.permission.READ_STEPS` | Sensitive | 3 | Import step count from Health Connect for activity tracking | Medium |
| `health.permission.READ_SLEEP` | Sensitive | 3 | Import sleep data from Health Connect for energy correlation | Medium |
| `health.permission.READ_HEART_RATE` | Sensitive | 3 | Import heart rate from Health Connect for health dashboard | Medium |
| `health.permission.READ_ACTIVE_CALORIES` | Sensitive | 3 | Import calorie data from Health Connect for energy tracking | Medium |
| `health.permission.READ_BLOOD_PRESSURE` | Sensitive | 3 | Import blood pressure from Health Connect for health monitoring | Medium |
| `health.permission.READ_BLOOD_GLUCOSE` | Sensitive | 3 | Import glucose data from Health Connect for health monitoring | Medium |
| `health.permission.READ_WEIGHT` | Sensitive | 3 | Import weight data from Health Connect for health trends | Medium |
| `USE_BIOMETRIC` | Sensitive | 3 | Biometric authentication for vault access and sensitive data protection | Low |
| `BLUETOOTH_CONNECT` | Future | 4 | Connect to BLE devices (wearables, sensors) for real-time health data | Medium |
| `BLUETOOTH_SCAN` | Future | 4 | Discover nearby BLE devices for pairing | Medium |
| `NFC` | Future | 4 | NFC tag reading for asset tracking and quick-capture triggers | Low |
| `READ_EXTERNAL_STORAGE` | Requires Strong Justification | 4 | File access for importing documents (deprecated API, prefer SAF/MediaStore) | High -- likely rejection without strong justification |
| `MANAGE_EXTERNAL_STORAGE` | Requires Strong Justification | 4 | Broad file access for file management features | High -- almost certain rejection; requires Play Store policy review |
| `NOTIFICATION_LISTENER_SERVICE` | Requires Strong Justification | 4 | Context engine -- read notifications from other apps for intelligent routing | Very High -- requires Play Store policy review and Permissions Declaration Form |

## Category Definitions

| Category | Criteria | Approval Required |
|---|---|---|
| **Core MVP** | Feature cannot function without it; standard for app category | Architecture decision only |
| **Optional** | Feature degrades gracefully without it; improves UX significantly | Product owner approval |
| **Sensitive** | Accesses health, biometric, or personal data; regulated by platform policies | Product owner + privacy review |
| **Future** | Not needed in current roadmap; planned for later phases | Architecture review when phase begins |
| **Requires Strong Justification** | Play Store will scrutinize; rejection risk is high | Product owner + legal + Play Store pre-submission review |

## Runtime Request Flow

```
User triggers feature
    |
    v
Is permission already granted?
    |-- YES --> proceed
    |-- NO
        |
        v
    Has user denied twice before?
        |-- YES --> show Settings deep-link dialog
        |-- NO
            |
            v
        Show rationale dialog (custom UI, explains WHY)
            |
            v
        User taps "Continue"
            |
            v
        System permission prompt
            |-- GRANTED --> proceed, record grant timestamp
            |-- DENIED --> record denial count, degrade gracefully
```

## Implementation Notes

- Permission state tracked in `DataStore<PermissionPreferences>` (denial count, last request timestamp).
- `PermissionGateway` interface in `core-common` abstracts platform permission checks.
- Each feature module declares its required permissions in its Hilt module setup.
- Compose UI uses `rememberLauncherForActivityResult(RequestPermission())` pattern.
- Never call `shouldShowRequestPermissionRationale()` without first checking denial history in DataStore.

## Play Store Compliance

- Privacy Policy must enumerate every permission and its purpose before first submission.
- Permissions Declaration Form required for: `READ_CONTACTS`, `READ_CALENDAR`, `NOTIFICATION_LISTENER_SERVICE`, `MANAGE_EXTERNAL_STORAGE`.
- Data Safety Section must reflect all data types accessed via permissions.
- Health Connect permissions require additional Health data policy compliance form.

## References

- DEC-009: Sensor strategy (no centralized sensor module)
- ADR-0002: Module architecture (permissions per feature module)
- Android developer docs: [Permissions best practices](https://developer.android.com/training/permissions/requesting)
