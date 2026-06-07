package eco.humanos.android.core.database.converter

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Room [TypeConverter]s for non-primitive column types.
 *
 * Collections are persisted as JSON strings via kotlinx.serialization so the
 * domain models in `core-model` stay free of any Android/Room dependencies.
 */
class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String? =
        value?.let { json.encodeToString(stringListSerializer, it) }

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        value?.let { json.decodeFromString(stringListSerializer, it) } ?: emptyList()

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
        val stringListSerializer = ListSerializer(String.serializer())
    }
}
