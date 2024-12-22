package mega.privacy.android.navigation.settings

import kotlinx.coroutines.flow.Flow

/**
 * Setting item
 *
 * @property key Unique identifier for the setting
 * @property name The value displayed on the line item
 * @property descriptionValue A static or live updated subtitle
 * @property isEnabled Flow that returns the enabled value for the setting. If present, a toggle switch is displayed on the line item
 * @property clickAction Toggle setting or navigate to secondary settings screen
 * @property isDestructive Display text in warning colour if true
 */
data class SettingItem(
    val key: String,
    val name: String,
    val descriptionValue: SettingDescriptionValue?,
    val isEnabled: Flow<Boolean>?,
    val clickAction: SettingClickActionType,
    val isDestructive: Boolean,
)

