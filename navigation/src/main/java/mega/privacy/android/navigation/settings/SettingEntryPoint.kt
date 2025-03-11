package mega.privacy.android.navigation.settings

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize
import mega.privacy.mobile.analytics.core.event.identifier.ItemSelectedEventIdentifier


interface SettingEntryPoint {
    val navData: NavData
    val analyticsEvent: ItemSelectedEventIdentifier

    @Parcelize
    data class NavData(
        val key: String,
        val title: Int,
        val icon: Int,
        val preferredOrdinal: Int,
        val destination: Parcelable,
    ) : Parcelable
}

class FeatureSettingEntryPoint(
    key: String,
    @StringRes title: Int,
    @DrawableRes icon: Int,
    val preferredOrdinal: Int,
    val destination: Parcelable,
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
    @StringRes title: Int,
    @DrawableRes icon: Int,
    val preferredOrdinal: Int,
    val destination: Parcelable,
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
