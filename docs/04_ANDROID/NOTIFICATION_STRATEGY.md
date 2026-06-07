# Notification Strategy

> humanOS Native Android -- Push and Local Notifications
> Last updated: 2026-06-06

## Overview

humanOS uses a dual notification system: **Firebase Cloud Messaging (FCM)** for server-initiated push notifications and **local notifications** via `NotificationManager` for on-device events. Both systems use Android notification channels for user control.

## Firebase Cloud Messaging (FCM)

### Setup

- Firebase project: `humanos-app`
- Configuration file: `google-services.json` (from Firebase Console, placed in `app/` module)
- FCM dependency: `com.google.firebase:firebase-messaging-ktx`
- Token registration: on app start and on token refresh, send FCM token to HumanOS backend via `POST /api/mobile/fcm-token`

### Remote Push Use Cases

| Notification Type | Trigger | Priority | Channel |
|---|---|---|---|
| Urgent task assigned | HumanOS backend event | HIGH | `tasks` |
| Agent completed action | Agent pipeline finish | DEFAULT | `daily_agent` |
| Security alert | Suspicious login, token revocation | MAX | `security` |
| System announcement | Platform-wide message | LOW | `sync` |

### FCM Message Handling

```kotlin
class HumanosMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"] ?: return
        val handler = notificationHandlerFactory.getHandler(type)
        handler.handle(message.data)
    }

    override fun onNewToken(token: String) {
        // Persist locally and sync to HumanOS backend
        tokenRepository.updateFcmToken(token)
    }
}
```

### Data-Only Messages

All FCM messages are **data messages** (not notification messages). This ensures the app handles them consistently whether in foreground or background, and allows custom notification construction with deep links.

## Local Notifications

### Use Cases

| Notification Type | Trigger | Channel | Priority |
|---|---|---|---|
| Daily briefing ready | `DailyBriefingWorker` completes | `daily_agent` | DEFAULT |
| Task reminder | Scheduled reminder time reached | `tasks` | HIGH |
| Capture processing complete | `CaptureUploadWorker` succeeds | `capture` | LOW |
| Sync status | Sync failure after retries | `sync` | LOW |
| Sync complete (large) | Bulk sync finished (> 50 items) | `sync` | LOW |

## Notification Channels

Channels are created at app startup in `Application.onCreate()`. Users can customize each channel independently in system Settings.

| Channel ID | Name (user-visible) | Description | Default Importance | Default Sound | Default Vibrate |
|---|---|---|---|---|---|
| `daily_agent` | Daily Briefing | Your daily AI briefing and agent updates | DEFAULT | Default | Yes |
| `tasks` | Tasks | Task assignments, reminders, and deadlines | HIGH | Default | Yes |
| `capture` | Captures | Photo, audio, and document processing status | LOW | None | No |
| `sync` | Sync | Data synchronization status and errors | LOW | None | No |
| `security` | Security | Security alerts and authentication events | MAX | Alarm | Yes |

### Channel Registration

```kotlin
object NotificationChannels {
    
    fun createAll(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        
        val channels = listOf(
            NotificationChannel(
                "daily_agent",
                "Daily Briefing",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Your daily AI briefing and agent updates"
            },
            NotificationChannel(
                "tasks",
                "Tasks",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Task assignments, reminders, and deadlines"
            },
            NotificationChannel(
                "capture",
                "Captures",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Photo, audio, and document processing status"
                setSound(null, null)
            },
            NotificationChannel(
                "sync",
                "Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Data synchronization status and errors"
                setSound(null, null)
            },
            NotificationChannel(
                "security",
                "Security",
                NotificationManager.IMPORTANCE_MAX
            ).apply {
                description = "Security alerts and authentication events"
            }
        )
        
        manager.createNotificationChannels(channels)
    }
}
```

## Deep Links

Notifications include deep links that navigate directly to the relevant screen when tapped.

| Deep Link | Destination | Parameters |
|---|---|---|
| `humanos://tasks/{id}` | Task detail screen | Task ID |
| `humanos://captures/{id}` | Capture detail screen | Capture ID |
| `humanos://briefing` | Daily briefing screen | None |
| `humanos://briefing/{date}` | Briefing for specific date | ISO date |
| `humanos://sync` | Sync status screen | None |
| `humanos://security` | Security settings screen | None |

### Deep Link Implementation

```kotlin
// In Navigation Compose
NavHost(navController, startDestination = "home") {
    // ...
    composable(
        route = "tasks/{taskId}",
        deepLinks = listOf(
            navDeepLink { uriPattern = "humanos://tasks/{taskId}" }
        ),
        arguments = listOf(
            navArgument("taskId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
        TaskDetailScreen(taskId = taskId)
    }
}
```

### PendingIntent Construction

```kotlin
fun buildTaskNotification(
    context: Context,
    taskId: String,
    title: String,
    body: String
): Notification {
    val deepLink = Uri.parse("humanos://tasks/$taskId")
    val intent = Intent(Intent.ACTION_VIEW, deepLink).apply {
        setPackage(context.packageName)
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        taskId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    
    return NotificationCompat.Builder(context, "tasks")
        .setSmallIcon(R.drawable.ic_notification_task)
        .setContentTitle(title)
        .setContentText(body)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()
}
```

## POST_NOTIFICATIONS Permission (Android 13+)

Starting with Android 13 (API 33), the `POST_NOTIFICATIONS` runtime permission is required. humanOS handles this as follows:

1. **Do not request on first launch**. Let the user explore the app first.
2. **Request when the user enables a notification-dependent feature** (e.g., daily briefing, task reminders).
3. **Show rationale dialog** explaining which notifications the user will receive.
4. **Degrade gracefully** if denied: features still work, but the user must check manually.

## Notification Grouping

When multiple notifications of the same type are pending, they are grouped:

```kotlin
// Individual notification
NotificationCompat.Builder(context, "tasks")
    .setGroup("task_reminders")
    // ...

// Summary notification (shown when 4+ individual notifications)
NotificationCompat.Builder(context, "tasks")
    .setGroup("task_reminders")
    .setGroupSummary(true)
    .setContentTitle("3 task reminders")
    .setStyle(NotificationCompat.InboxStyle()
        .addLine("Review patient chart - Due in 1 hour")
        .addLine("Submit weekly report - Due today")
        .addLine("Team meeting prep - Due in 2 hours")
    )
```

## Notification Actions

Key notifications include inline actions:

| Notification Type | Action 1 | Action 2 |
|---|---|---|
| Task reminder | Mark Complete | Snooze 1 hour |
| Daily briefing | Open Briefing | Dismiss |
| Capture complete | View Capture | -- |
| Sync failure | Retry Now | -- |
| Security alert | Review | -- |

## Testing

- **Unit tests**: Mock `NotificationManager`, verify channel creation and notification construction.
- **Instrumented tests**: Use `NotificationManagerCompat` to verify notifications are posted.
- **FCM testing**: Use Firebase Console "Test Message" feature or `fcm-cli` for development.
- **Deep link testing**: `adb shell am start -a android.intent.action.VIEW -d "humanos://tasks/test-123"`.

## References

- PERMISSIONS_STRATEGY.md: POST_NOTIFICATIONS permission handling
- BACKGROUND_EXECUTION.md: WorkManager triggers for local notifications
- ADR-0002: Module architecture (core-notification module)
- Firebase docs: [Cloud Messaging](https://firebase.google.com/docs/cloud-messaging/android/client)
- Android developer docs: [Notifications](https://developer.android.com/develop/ui/views/notifications)
