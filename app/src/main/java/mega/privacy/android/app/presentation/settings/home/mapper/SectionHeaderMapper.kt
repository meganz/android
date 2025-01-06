package mega.privacy.android.app.presentation.settings.home.mapper

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.home.model.SettingHeaderItem
import mega.privacy.android.navigation.settings.SettingSectionHeader
import javax.inject.Inject

internal class SectionHeaderMapper @Inject constructor() {
    operator fun invoke(section: SettingSectionHeader): SettingHeaderItem {
        return when (section) {
            SettingSectionHeader.Appearance -> SettingHeaderItem(
                getComposableResource(R.string.settings_appearance),
                section.toString()
            )

            SettingSectionHeader.About -> SettingHeaderItem(
                headerText = getComposableResource(R.string.settings_about),
                key = section.toString()
            )

            SettingSectionHeader.Features -> SettingHeaderItem(
                headerText = getComposableResource(R.string.settings_features),
                key = section.toString()
            )

            SettingSectionHeader.Help -> SettingHeaderItem(
                headerText = getComposableResource(R.string.settings_help),
                key = section.toString()
            )

            SettingSectionHeader.Media -> SettingHeaderItem(
                headerText = getComposableResource(R.string.settings_media),
                key = section.toString()
            )

            SettingSectionHeader.Security -> SettingHeaderItem(
                headerText = getComposableResource(R.string.settings_security_options_title),
                key = section.toString()
            )

            SettingSectionHeader.Storage -> SettingHeaderItem(
                headerText = getComposableResource(R.string.settings_storage),
                key = section.toString()
            )

            SettingSectionHeader.UserInterface -> SettingHeaderItem(
                headerText = getComposableResource(R.string.user_interface_setting),
                key = section.toString()
            )

            is SettingSectionHeader.Custom -> {
                SettingHeaderItem(
                    headerText = @Composable {
                        section.name
                    },
                    key = section.name
                )
            }
        }
    }

    private fun getComposableResource(resourceId: Int) =
        @Composable { stringResource(resourceId) }
}
