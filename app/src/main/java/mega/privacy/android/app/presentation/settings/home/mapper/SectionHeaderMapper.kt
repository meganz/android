package mega.privacy.android.app.presentation.settings.home.mapper

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.navigation.settings.SettingSectionHeader

internal class SectionHeaderMapper {
    operator fun invoke(section: SettingSectionHeader): @Composable () -> String {
        return when (section) {
            SettingSectionHeader.Appearance -> getComposableResource(R.string.settings_appearance)
            SettingSectionHeader.About -> getComposableResource(R.string.settings_about)
            SettingSectionHeader.Features -> getComposableResource(R.string.settings_features)
            SettingSectionHeader.Help -> getComposableResource(R.string.settings_help)
            SettingSectionHeader.Media -> getComposableResource(R.string.settings_media)
            SettingSectionHeader.Security -> getComposableResource(R.string.settings_security_options_title)
            SettingSectionHeader.Storage -> getComposableResource(R.string.settings_storage)
            SettingSectionHeader.UserInterface -> getComposableResource(R.string.user_interface_setting)
            is SettingSectionHeader.Custom -> {
                @Composable {
                    section.name
                }
            }
        }
    }

    private fun getComposableResource(resourceId: Int) =
        @Composable { stringResource(resourceId) }
}
