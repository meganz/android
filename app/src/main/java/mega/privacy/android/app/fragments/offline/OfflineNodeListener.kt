package mega.privacy.android.app.fragments.offline

import mega.privacy.android.app.MegaOffline

internal interface OfflineNodeListener {
    fun showConfirmationRemoveOfflineNode(offline: MegaOffline)
}