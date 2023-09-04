package mega.privacy.android.core.ui.buildscripts.deserializers

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import mega.privacy.android.core.ui.buildscripts.model.json.JsonColor
import mega.privacy.android.core.ui.buildscripts.model.json.JsonColorRef
import mega.privacy.android.core.ui.buildscripts.model.json.JsonCoreUiObject
import mega.privacy.android.core.ui.buildscripts.model.json.JsonCoreUiObjectUnknown
import mega.privacy.android.core.ui.buildscripts.model.json.JsonGroup
import mega.privacy.android.core.ui.buildscripts.model.json.JsonNumber
import java.lang.reflect.Type

/**
 * Deserializer to convert an unspecified [JsonCoreUiObject] to its corresponding type.
 * It checks the "$type" field in the json element to determine which type to convert to,
 * if it has no "$type" it presupposes it's a group.
 * The name of the parsed object comes from the parent, for example:
 *      "white": {
 *         "$type": "color",
 *         "$value": "rgba(255, 255, 255, 1.0)"
 *       }
 * Will be converted to JsonColor(name = "white", color = Color(255, 255, 255, 255))
 */
internal class JsonCoreUiObjectDeserializer : JsonDeserializer<JsonCoreUiObject> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): JsonCoreUiObject {
        if (json.isJsonObject) {
            val jsonObject = json.asJsonObject
            return when (jsonObject.getType()) {
                null -> {
                    val children = jsonObject.keySet().map { name ->
                        context.deserialize<JsonCoreUiObject>(
                            jsonObject[name],
                            JsonCoreUiObject::class.java
                        ).also {
                            it.name = name
                        }
                    }
                    if (children.isEmpty()) {
                        JsonCoreUiObjectUnknown()
                    } else {
                        JsonGroup(null, children)
                    }
                }

                TYPE_COLOR -> {
                    if (jsonObject.getValue()?.startsWith("{") == true) {
                        context.deserialize(jsonObject, JsonColorRef::class.java)
                    } else {
                        context.deserialize(jsonObject, JsonColor::class.java)
                    }

                }

                TYPE_NUMBER -> context.deserialize(jsonObject, JsonNumber::class.java)
                else -> JsonCoreUiObjectUnknown()
            }
        }
        return JsonCoreUiObjectUnknown()
    }

    private fun JsonObject.getType(): String? {
        if (has(TYPE_FIELD)) {
            val typeElement = get(TYPE_FIELD)
            if (typeElement.isJsonPrimitive && typeElement.asJsonPrimitive.isString) {
                return typeElement.asJsonPrimitive.asString
            }
        }
        return null
    }

    private fun JsonObject.getValue(): String? {
        if (has(TYPE_VALUE)) {
            val typeElement = get(TYPE_VALUE)
            if (typeElement.isJsonPrimitive && typeElement.asJsonPrimitive.isString) {
                return typeElement.asJsonPrimitive.asString
            }
        }
        return null
    }

    companion object {
        private const val TYPE_FIELD = "\$type"
        private const val TYPE_VALUE = "\$value"
        private const val TYPE_COLOR = "color"
        private const val TYPE_NUMBER = "number"
    }
}