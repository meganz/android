package mega.privacy.android.navigation.extensions

import android.os.Bundle
import androidx.navigation.NavType
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KType
import kotlin.reflect.typeOf

inline fun <reified T> serializableNavType(isNullableAllowed: Boolean = false): NavType<T> {
    val serializer = serializer<T>()
    return object : NavType<T>(isNullableAllowed) {
        override fun put(bundle: Bundle, key: String, value: T) {
            val json = Json.encodeToString(serializer, value)
            bundle.putString(key, json)
        }

        override fun get(bundle: Bundle, key: String): T? {
            return bundle.getString(key)?.let { Json.decodeFromString(serializer, it) }
        }

        override fun parseValue(value: String): T = Json.decodeFromString(serializer, value)
        override val name: String = T::class.java.simpleName
    }
}

inline fun <reified T> typeMapOf(): Pair<KType, NavType<T>> =
    typeOf<T>() to serializableNavType<T>(isNullableAllowed = typeOf<T>().isMarkedNullable)