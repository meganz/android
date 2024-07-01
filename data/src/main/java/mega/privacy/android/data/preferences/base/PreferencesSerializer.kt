package mega.privacy.android.data.preferences.base

import android.annotation.SuppressLint
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.preferences.PreferencesMapCompat
import androidx.datastore.preferences.PreferencesProto
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * It's copy version of [androidx.datastore.preferences.core.PreferencesSerializer] because it's internal and we can't use it.
 * Serializer for [Preferences].
 */
internal object PreferencesSerializer : Serializer<Preferences> {
    override val defaultValue: Preferences
        get() = emptyPreferences()

    @SuppressLint("RestrictedApi")
    @Throws(IOException::class, CorruptionException::class)
    override suspend fun readFrom(input: InputStream): Preferences {
        val preferencesProto = PreferencesMapCompat.readFrom(input)

        val mutablePreferences = mutablePreferencesOf()

        preferencesProto.preferencesMap.forEach { (name, value) ->
            addProtoEntryToPreferences(name, value, mutablePreferences)
        }

        return mutablePreferences.toPreferences()
    }

    @Throws(IOException::class, CorruptionException::class)
    override suspend fun writeTo(t: Preferences, output: OutputStream) {
        val preferences = t.asMap()
        val protoBuilder = PreferencesProto.PreferenceMap.newBuilder()

        for ((key, value) in preferences) {
            protoBuilder.putPreferences(key.name, getValueProto(value))
        }

        protoBuilder.build().writeTo(output)
    }

    private fun getValueProto(value: Any): PreferencesProto.Value {
        return when (value) {
            is Boolean -> PreferencesProto.Value.newBuilder().setBoolean(value).build()
            is Float -> PreferencesProto.Value.newBuilder().setFloat(value).build()
            is Double -> PreferencesProto.Value.newBuilder().setDouble(value).build()
            is Int -> PreferencesProto.Value.newBuilder().setInteger(value).build()
            is Long -> PreferencesProto.Value.newBuilder().setLong(value).build()
            is String -> PreferencesProto.Value.newBuilder().setString(value).build()
            is Set<*> ->
                @Suppress("UNCHECKED_CAST")
                PreferencesProto.Value.newBuilder().setStringSet(
                    PreferencesProto.StringSet.newBuilder().addAllStrings(value as Set<String>)
                ).build()

            else -> throw IllegalStateException(
                "PreferencesSerializer does not support type: ${value.javaClass.name}"
            )
        }
    }

    private fun addProtoEntryToPreferences(
        name: String,
        value: PreferencesProto.Value,
        mutablePreferences: MutablePreferences,
    ) {
        return when (value.valueCase) {
            PreferencesProto.Value.ValueCase.BOOLEAN ->
                mutablePreferences[booleanPreferencesKey(name)] =
                    value.boolean

            PreferencesProto.Value.ValueCase.FLOAT -> mutablePreferences[floatPreferencesKey(name)] =
                value.float

            PreferencesProto.Value.ValueCase.DOUBLE -> mutablePreferences[doublePreferencesKey(name)] =
                value.double

            PreferencesProto.Value.ValueCase.INTEGER -> mutablePreferences[intPreferencesKey(name)] =
                value.integer

            PreferencesProto.Value.ValueCase.LONG -> mutablePreferences[longPreferencesKey(name)] =
                value.long

            PreferencesProto.Value.ValueCase.STRING -> mutablePreferences[stringPreferencesKey(name)] =
                value.string

            PreferencesProto.Value.ValueCase.STRING_SET ->
                mutablePreferences[stringSetPreferencesKey(name)] =
                    value.stringSet.stringsList.toSet()

            PreferencesProto.Value.ValueCase.BYTES -> mutablePreferences[byteArrayPreferencesKey(
                name
            )] = value.bytes.toByteArray()

            PreferencesProto.Value.ValueCase.VALUE_NOT_SET ->
                throw CorruptionException("Value not set.")

            null -> throw CorruptionException("Value case is null.")
        }
    }
}