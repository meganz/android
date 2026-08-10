package mega.privacy.android.navigation.destination

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Navigation key for the Customise navigation settings screen.
 */
@Serializable
@Parcelize
data object CustomiseNavigationNavKey : NavKey, Parcelable
