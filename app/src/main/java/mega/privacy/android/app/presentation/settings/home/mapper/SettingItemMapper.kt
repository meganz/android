package mega.privacy.android.app.presentation.settings.home.mapper

import mega.privacy.android.app.presentation.settings.home.model.SettingModelItem
import mega.privacy.android.navigation.settings.SettingDescriptionValue
import mega.privacy.android.navigation.settings.SettingItem
import mega.privacy.android.navigation.settings.SettingSectionHeader

/**
 * Setting item mapper
 */
class SettingItemMapper {

    /**
     * Invoke
     *
     * @param item
     * @return [SettingModelItem]
     */
    operator fun invoke(section: SettingSectionHeader, item: SettingItem): SettingModelItem {
        return SettingModelItem(
            section = section,
            key = item.key,
            name = item.name,
            description = getDescription(item),
            isEnabled = getEnabledProvider(item),
            isDestructive = item.isDestructive,
        )
    }

    private fun getDescription(item: SettingItem) =
        when (val value = item.descriptionValue) {
            is SettingDescriptionValue.DynamicDescription -> ""
            is SettingDescriptionValue.StaticDescription -> value.description
            null -> null
        }

    private fun getEnabledProvider(item: SettingItem) =
        item.isEnabled?.let { { null } }

}
