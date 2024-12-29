package mega.privacy.android.app.presentation.settings.home.model

import androidx.navigation.NavHostController
import mega.privacy.android.navigation.settings.SettingSectionHeader

/**
 * Setting model item
 *
 * @property section
 * @property key
 * @property name
 * @property description
 * @property isEnabled
 * @property isDestructive
 * @property onClick
 */
data class SettingModelItem(
    val section: SettingSectionHeader,
    val key: String,
    val name: String,
    val description: String?,
    val isEnabled: (() -> Boolean?)?,
    val isDestructive: Boolean,
    val onClick: (NavHostController) -> Unit,
)