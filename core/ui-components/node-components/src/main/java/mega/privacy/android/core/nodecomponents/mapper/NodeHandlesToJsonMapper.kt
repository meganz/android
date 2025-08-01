package mega.privacy.android.core.nodecomponents.mapper

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper to convert List<Long> to JSON and back using Kotlinx Serialization
 */
@Singleton
class NodeHandlesToJsonMapper @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Convert List<Long> to JSON string
     * @param nodeHandles List of node handles as Long values
     * @return JSON string representation
     */
    operator fun invoke(nodeHandles: List<Long>?): String {
        val jsonArray = buildJsonArray {
            (nodeHandles ?: emptyList()).forEach { handle ->
                add(JsonPrimitive(handle))
            }
        }
        return json.encodeToString(JsonArray.serializer(), jsonArray)
    }

    /**
     * Convert JSON string back to List<Long>
     * @param jsonString JSON string representation of node handles
     * @return List of node handles as Long values
     */
    operator fun invoke(jsonString: String?): List<Long> {
        return if (jsonString.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                val jsonArray = json.decodeFromString(JsonArray.serializer(), jsonString)
                jsonArray.map { element ->
                    element.jsonPrimitive.long
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}