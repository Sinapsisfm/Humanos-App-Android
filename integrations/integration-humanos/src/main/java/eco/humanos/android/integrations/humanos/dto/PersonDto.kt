package eco.humanos.android.integrations.humanos.dto

import kotlinx.serialization.Serializable

/** Envelope for `GET /api/mobile/person` → `{ "person": {...} }`. */
@Serializable
data class PersonEnvelope(val person: PersonDto)

/**
 * The signed-in user's HumanOS profile. Mirrors the web `/api/person` contract
 * so the mobile account screen can reuse it. `birthDate` is an ISO string.
 */
@Serializable
data class PersonDto(
    val id: String,
    val firstName: String = "",
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val rut: String? = null,
    val birthDate: String? = null,
    val bio: String? = null,
    val role: String? = null,
    val avatarUrl: String? = null,
    val loadScore: Int = 0,
    val lifeAdminScore: Int = 0,
    val householdId: String? = null,
    val household: HouseholdDto? = null,
) {
    val fullName: String
        get() = listOfNotNull(firstName.takeIf { it.isNotBlank() }, lastName?.takeIf { it.isNotBlank() })
            .joinToString(" ")
            .ifBlank { email ?: "Sin nombre" }
}

@Serializable
data class HouseholdDto(
    val id: String,
    val name: String? = null,
    val address: String? = null,
    val city: String? = null,
    val country: String? = null,
)
