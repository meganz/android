package mega.privacy.android.navigation.destination

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * TransfersInfo data class to hold navigation info for the Transfers screen.
 *
 * @param tab The tab to open, if null it will be decided by view model logic
 */
@Serializable
@Parcelize
data class TransfersNavKey(val tab: Tab? = null) : NavKey, Parcelable {

    /**
     * Identifies a tab in the transfers section
     *
     */
    enum class Tab {

        /**
         * Active transfers tab
         */
        Active,

        /**
         * Completed transfers tab
         */
        Completed,

        /**
         * Failed transfers tab
         */
        Failed;
    }
}