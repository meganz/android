package mega.privacy.android.feature.clouddrive.presentation.clouddrive.model

import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Cloud Drive action
 * This interface defines the UI actions that can be performed in the Cloud Drive screen.
 */
sealed interface CloudDriveAction {
    /**
     * Item clicked action
     */
    data class ItemClicked(val nodeUiItem: NodeUiItem<TypedNode>) : CloudDriveAction

    /**
     * Item long clicked action
     */
    data class ItemLongClicked(val nodeUiItem: NodeUiItem<TypedNode>) : CloudDriveAction

    /**
     * Change view type clicked action
     */
    data object ChangeViewTypeClicked : CloudDriveAction

    /**
     * Opened file node handled action
     */
    data object OpenedFileNodeHandled : CloudDriveAction

    /**
     * Select all items action
     */
    data object SelectAllItems : CloudDriveAction

    /**
     * Deselect all items action
     */
    data object DeselectAllItems : CloudDriveAction

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