package mega.privacy.android.app.presentation.settings.home.model

import androidx.compose.runtime.Composable

/**
 * Setting section
 *
 * @property sectionHeader
 * @property sectionItems
 */
data class SettingSection(
    val sectionHeader: @Composable () -> String,
    val sectionItems: List<SettingModelItem>
)
