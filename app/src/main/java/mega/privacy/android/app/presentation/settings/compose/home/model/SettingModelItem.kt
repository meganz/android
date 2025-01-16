package mega.privacy.android.app.presentation.settings.compose.home.model

import androidx.navigation.NavHostController

/**
 * Setting model item
 *
 * @property key
 * @property name
 * @property description
 * @property isEnabled
 * @property isDestructive
 * @property onClick
 */
data class SettingModelItem(
    override val key: String,
    val name: String,
    val description: String?,
    val isEnabled: (() -> Boolean?)?,
    val isDestructive: Boolean,
    val onClick: (NavHostController) -> Unit,
) : SettingListItem

