package mega.privacy.android.app.presentation.settings.home.mapper

import androidx.navigation.NavHostController
import mega.privacy.android.app.presentation.settings.home.model.SettingModelItem
import mega.privacy.android.navigation.settings.SettingClickActionType
import mega.privacy.android.navigation.settings.SettingDescriptionValue
import mega.privacy.android.navigation.settings.SettingItem
import javax.inject.Inject

/**
 * Setting item mapper
 */
class SettingItemMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param item
     * @return [SettingModelItem]
     */
    operator fun invoke(
        item: SettingItem,
        suspendHandler: (suspend () -> Unit) -> Unit,
    ): SettingModelItem {
        return SettingModelItem(
            key = item.key,
            name = item.name,
            description = getDescription(item),
            isEnabled = getEnabledProvider(item),
            isDestructive = item.isDestructive,
            onClick = mapOnClick(item.clickAction, suspendHandler)
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

    private fun mapOnClick(
        clickAction: SettingClickActionType,
        suspendHandler: (suspend () -> Unit) -> Unit,
    ): (NavHostController) -> Unit {
        return when (clickAction) {
            is SettingClickActionType.NavigationAction -> {
                { controller -> controller.navigate(clickAction.target) }
            }

            is SettingClickActionType.FunctionAction -> {
                { _ -> suspendHandler(clickAction.function) }
            }
        }
    }

}
