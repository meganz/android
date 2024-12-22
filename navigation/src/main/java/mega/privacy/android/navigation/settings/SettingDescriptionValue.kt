package mega.privacy.android.navigation.settings

import kotlinx.coroutines.flow.Flow

/**
 * Setting description value
 */
sealed interface SettingDescriptionValue {

    /**
     * Static description
     *
     * @property description
     */
    data class StaticDescription(val description: String) : SettingDescriptionValue

    /**
     * Dynamic description
     *
     * @property source
     */
    data class DynamicDescription(val source: Flow<String?>) : SettingDescriptionValue
}