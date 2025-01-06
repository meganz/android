package mega.privacy.android.app.presentation.settings.home.model

import androidx.compose.runtime.Composable

/**
 * Setting header item
 *
 * @property headerText
 * @property key
 */
internal class SettingHeaderItem(
    val headerText: @Composable () -> String,
    override val key: String,
) : SettingListItem{
    override fun equals(other: Any?) = key == (other as? SettingHeaderItem)?.key
    override fun hashCode() = key.hashCode()
}