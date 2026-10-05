package mega.privacy.android.navigation.destination

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Navigation key for the Customise navigation settings screen.
 *
 * @property returnToSettings True when the screen was opened from Settings, so leaving it
 * reopens Settings instead of returning to the previous destination.
 */
@Serializable
@Parcelize
data class CustomiseNavigationNavKey(
    val returnToSettings: Boolean = false,
) : NavKey, Parcelable
