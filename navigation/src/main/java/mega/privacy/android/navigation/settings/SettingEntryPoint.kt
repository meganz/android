package mega.privacy.android.navigation.settings

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize


interface SettingEntryPoint : Parcelable  {
    val key: String
    val title: Int
    val icon: Int
    val preferredOrdinal: Int
    val destination: Parcelable
}

@Parcelize
data class FeatureSettingEntryPoint(
    override val key: String,
    @StringRes override val title: Int,
    @DrawableRes override val icon: Int,
    override val preferredOrdinal: Int,
    override val destination: Parcelable,
) : SettingEntryPoint

@Parcelize
data class MoreSettingEntryPoint(
    override val key: String,
    @StringRes override val title: Int,
    @DrawableRes override val icon: Int,
    override val preferredOrdinal: Int,
    override val destination: Parcelable,
) : SettingEntryPoint