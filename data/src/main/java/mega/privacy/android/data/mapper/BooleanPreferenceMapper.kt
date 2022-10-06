package mega.privacy.android.data.mapper

import androidx.datastore.preferences.core.Preferences

/**
 * Mapper for boolean preferences
 */
typealias BooleanPreferenceMapper = (@JvmSuppressWildcards Preferences) -> @JvmSuppressWildcards Map<@JvmSuppressWildcards String, @JvmSuppressWildcards Boolean>


/**
 * Map boolean preference
 *
 * @param preferences
 */
internal fun mapBooleanPreference(preferences: Preferences) =
    preferences.asMap()
        .mapKeys { it.key.name }
        .mapValues { it.value as? Boolean }
        .mapNotNull {
            runCatching { it.key to it.value as Boolean }.getOrNull()
        }.toMap()