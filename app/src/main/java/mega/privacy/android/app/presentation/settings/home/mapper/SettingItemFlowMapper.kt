package mega.privacy.android.app.presentation.settings.home.mapper

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.presentation.settings.home.model.SettingModelItem
import mega.privacy.android.navigation.settings.SettingDescriptionValue
import mega.privacy.android.navigation.settings.SettingItem

internal class SettingItemFlowMapper {
    operator fun invoke(item: SettingItem): Flow<(List<SettingModelItem>) -> List<SettingModelItem>>? =
        item.isEnabled?.map { newValue ->
            { itemList: List<SettingModelItem> ->
                itemList.updateBoolean(
                    item.key,
                    newValue
                )
            }
        }
            ?: (item.descriptionValue as? SettingDescriptionValue.DynamicDescription)?.source?.map { newValue ->
                { itemList: List<SettingModelItem> ->
                    itemList.updateDescription(
                        item.key,
                        newValue.orEmpty()
                    )
                }
            }

    private fun List<SettingModelItem>.updateBoolean(key: String, newValue: Boolean) =
        this.update({ it.key == key }, { it.copy(isEnabled = { newValue }) })

    private fun List<SettingModelItem>.updateDescription(key: String, newValue: String?) =
        this.update({ it.key == key }, { it.copy(description = newValue.orEmpty()) })

    private fun <T> List<T>.update(predicate: (T) -> Boolean, transform: (T) -> T): List<T> {
        return map { item ->
            if (predicate(item)) transform(item) else item
        }
    }
}
