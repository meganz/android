package mega.privacy.android.app.presentation.offline.adapter

import mega.privacy.android.app.MegaOffline

internal interface OfflineNodeListener {
    fun showConfirmationRemoveOfflineNode(offline: MegaOffline)
}