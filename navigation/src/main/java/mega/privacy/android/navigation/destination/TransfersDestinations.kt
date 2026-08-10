package mega.privacy.android.navigation.destination

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.navkey.NoNodeNavKey

/**
 * TransfersInfo data class to hold navigation info for the Transfers screen.
 *
 * @param tab The tab to open, if null it will be decided by view model logic
 */
@Serializable
@Parcelize
data class TransfersNavKey(val tab: Tab? = null) : NoNodeNavKey, Parcelable {

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