package mega.privacy.android.feature.cloudexplorer.presentation.selectsyncfolder

import mega.privacy.android.domain.entity.node.NodeId

internal sealed interface SelectSyncFolderAction {

    /**
     * The user tapped the select action on the current folder (also re-sent after a permission
     * dialog has been handled, to continue the flow).
     */
    data object CurrentFolderSelected : SelectSyncFolderAction

    /**
     * The disable battery optimisation dialog has been handled (allowed or dismissed), so the
     * flow can continue without showing it again.
     */
    data object DisableBatteryOptimizationsHandled : SelectSyncFolderAction

    /**
     * A restricted (disabled) folder was clicked.
     */
    data class RestrictedFolderClicked(val nodeId: NodeId) : SelectSyncFolderAction

    /**
     * The user confirmed removing the sync/backup connection of another device's folder. The
     * node (and its backup id) is read from the state.
     */
    data object RemoveConnectionConfirmed : SelectSyncFolderAction

    /**
     * The remove-connection dialog was dismissed without confirming, clearing the stored node.
     */
    data object RemoveConnectionNodeConsumed : SelectSyncFolderAction

    /** Event-consumed callbacks, resetting each one-shot event back to consumed. */
    data object DisableBatteryOptimizationsEventConsumed : SelectSyncFolderAction
    data object WarningEventConsumed : SelectSyncFolderAction
    data object FolderConfirmedEventConsumed : SelectSyncFolderAction
}
