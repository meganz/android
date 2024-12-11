package mega.privacy.android.app.presentation.settings.model

import androidx.compose.runtime.Composable

/**
 * Setting ui item
 *
 * Item representing a settings item. Can be a single preference or preference category..
 */
interface SettingUiItem {

    /**
     * View
     *
     * Composable function for the preference layout
     */
    @Composable
    fun SettingItemView()
}
