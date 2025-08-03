package mega.privacy.android.navigation.contract.settings

import androidx.navigation3.runtime.NavKey
import mega.privacy.mobile.analytics.core.event.identifier.ItemSelectedEventIdentifier

interface SettingEntryPoint {
    val navData: NavData
    val analyticsEvent: ItemSelectedEventIdentifier

    data class NavData(
        val key: String,
        val title: Int,
        val icon: Int,
        val preferredOrdinal: Int,
        val destination: NavKey,
    )
}

class FeatureSettingEntryPoint(
    key: String,
    title: Int,
    icon: Int,
    val preferredOrdinal: Int,
    val destination: NavKey,
    override val analyticsEvent: ItemSelectedEventIdentifier,
) : SettingEntryPoint {
    override val navData = SettingEntryPoint.NavData(
        key,
        title,
        icon,
        preferredOrdinal,
        destination,
    )
}

class MoreSettingEntryPoint(
    key: String,
    title: Int,
    icon: Int,
    val preferredOrdinal: Int,
    val destination: NavKey,
    override val analyticsEvent: ItemSelectedEventIdentifier,
) : SettingEntryPoint {
    override val navData = SettingEntryPoint.NavData(
        key,
        title,
        icon,
        preferredOrdinal,
        destination,
    )
}