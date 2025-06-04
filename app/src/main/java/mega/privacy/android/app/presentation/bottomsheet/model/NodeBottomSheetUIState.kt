package mega.privacy.android.app.presentation.bottomsheet.model

import mega.privacy.android.app.utils.wrapper.LegacyNodeWrapper
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.node.TypedFolderNode

/**
 * Node bottom sheet ui state
 * @property canMoveNode
 * @property canRestoreNode
 * @property isOnline
 * @property isAvailableOffline
 * @property legacyNodeWrapper
 * @property shareData
 * @property nodeDeviceCenterInformation
 * @property shareKeyCreated
 * @property accountType
 * @property isBusinessAccountExpired
 * @property isHiddenNodesOnboarded
 * @property isHidingActionAllowed
 * @property isUserAttributeFolder          True if the node is a user attribute folder (CU, MU or "My chat files") or false otherwise
 * @property isSyncedFolder                 True if the node is synced (root of a Sync) or False otherwise
 */
data class NodeBottomSheetUIState(
    val canMoveNode: Boolean = false,
    val canRestoreNode: Boolean = false,
    val isOnline: Boolean = true,
    val legacyNodeWrapper: LegacyNodeWrapper? = null,
    val isAvailableOffline: Boolean = false,
    val shareData: NodeShareInformation? = null,
    val nodeDeviceCenterInformation: NodeDeviceCenterInformation? = null,
    val shareKeyCreated: Boolean? = null,
    val accountType: AccountType? = null,
    val isBusinessAccountExpired: Boolean = false,
    val isHiddenNodesOnboarded: Boolean? = null,
    val isHidingActionAllowed: Boolean = false,
    val isUserAttributeFolder: Boolean = false,
    val isSyncedFolder: Boolean = false,
) {
    /**
     * True if Sync action is allowed for the node or False otherwise
     */
    val isSyncActionAllowed =
        legacyNodeWrapper?.typedNode is TypedFolderNode == true && !isUserAttributeFolder && !isSyncedFolder
}

