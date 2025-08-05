package mega.privacy.android.app.appstate.mapper

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.serializerOrNull

private const val FIELD_TYPE = "type"
private const val FIELD_DATA = "data"

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
object NavKeySerializer : KSerializer<NavKey> {
    override val descriptor = buildClassSerialDescriptor("NavKey") {
        element<String>(FIELD_TYPE)
        element<JsonElement>(FIELD_DATA)
    }

    override fun serialize(encoder: Encoder, value: NavKey) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("NavKeySerializer only works with JSON")

        // Dynamically get serializer for the runtime class
        val kClass = value::class
        val serializer = kClass.serializerOrNull()
            ?: error("No serializer found for ${kClass.qualifiedName}")

        @Suppress("UNCHECKED_CAST")
        val jsonElement = jsonEncoder.json.encodeToJsonElement(
            serializer as KSerializer<Any>,
            value as Any
        )

        val obj = buildJsonObject {
            put(FIELD_TYPE, kClass.qualifiedName ?: error("Missing class name"))
            put(FIELD_DATA, jsonElement)
        }

        jsonEncoder.encodeJsonElement(obj)
    }

    override fun deserialize(decoder: Decoder): NavKey {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("NavKeySerializer only works with JSON")

        val element = jsonDecoder.decodeJsonElement().jsonObject
        val type = element[FIELD_TYPE]?.jsonPrimitive?.content
            ?: error("Missing 'type' field")
        val data = element[FIELD_DATA]
            ?: error("Missing 'data' field")

        val clazz = Class.forName(type).kotlin
        val serializer = clazz.serializerOrNull()
            ?: error("No serializer for class $type")

        @Suppress("UNCHECKED_CAST")
        return jsonDecoder.json.decodeFromJsonElement(
            serializer as KSerializer<Any>,
            data
        ) as NavKey
    }
}