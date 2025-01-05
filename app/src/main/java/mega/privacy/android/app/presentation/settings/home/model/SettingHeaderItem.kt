package mega.privacy.android.app.presentation.settings.home.model

import androidx.compose.runtime.Composable

/**
 * Setting header item
 *
 * @property headerText
 * @property key
 */
data class SettingHeaderItem(
    val headerText: @Composable () -> String,
    override val key: String,
) : SettingListItem