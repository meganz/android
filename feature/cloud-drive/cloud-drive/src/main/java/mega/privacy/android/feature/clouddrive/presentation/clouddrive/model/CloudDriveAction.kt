package mega.privacy.android.feature.clouddrive.presentation.clouddrive.model

import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Cloud Drive action
 * This interface defines the UI actions that can be performed in the Cloud Drive screen.
 */
sealed interface CloudDriveAction {
    /**
     * Item clicked action
     */
    data class ItemClicked(val node: TypedNode) : CloudDriveAction

    /**
     * Change view type clicked action
     */
    data object ChangeViewTypeClicked : CloudDriveAction

    /**
     * Opened file node handled action
     */
    data object OpenedFileNodeHandled : CloudDriveAction

    /**
     * Navigate to folder event consumed action
     */
    data object NavigateToFolderEventConsumed : CloudDriveAction

    /**
     * Navigate back event consumed action
     */
    data object NavigateBackEventConsumed : CloudDriveAction

    /**
     * Consume transfer over quota warning action
     */
    data object OverQuotaConsumptionWarning : CloudDriveAction
}