package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * TransfersInfo data class to hold navigation info for the Transfers screen.
 *
 * @param tabIndex The index of the tab to open, if null it will be decided by view model logic
 */
@Serializable
class TransfersNavKey(val tabIndex: Int? = null) : NavKey