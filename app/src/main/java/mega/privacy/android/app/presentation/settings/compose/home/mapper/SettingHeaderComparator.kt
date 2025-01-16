package mega.privacy.android.app.presentation.settings.compose.home.mapper

import mega.privacy.android.app.presentation.settings.compose.home.model.SettingHeaderItem
import mega.privacy.android.navigation.settings.SettingSectionHeader
import javax.inject.Inject

internal class SettingHeaderComparator @Inject constructor() : Comparator<SettingHeaderItem> {
    override fun compare(o1: SettingHeaderItem, o2: SettingHeaderItem): Int {
        val keyOrder = getValueFromKey(o1.key).compareTo(getValueFromKey(o2.key))
        return if (keyOrder == 0) {
            o1.key.compareTo(o2.key)
        } else {
            keyOrder
        }
    }

    private fun getValueFromKey(key: String): Int {
        return when (key) {
            SettingSectionHeader.Appearance.toString() -> 0
            SettingSectionHeader.Features.toString() -> 1
            SettingSectionHeader.Storage.toString() -> 2
            SettingSectionHeader.UserInterface.toString() -> 3
            SettingSectionHeader.Media.toString() -> 4
            SettingSectionHeader.Security.toString() -> 5
            SettingSectionHeader.Help.toString() -> 6
            SettingSectionHeader.About.toString() -> 7
            else -> 999
        }
    }

}
