package eco.humanos.android.core.model.health

import kotlinx.serialization.Serializable

/**
 * Types of daily routine events that the system tracks to build
 * a behavioral model of the user's day.
 */
@Serializable
enum class RoutineSignalType {
    WAKE_UP,
    SLEEP_START,
    MEAL,
    EXERCISE,
    MEDICATION,
    FOCUS_SESSION,
}
