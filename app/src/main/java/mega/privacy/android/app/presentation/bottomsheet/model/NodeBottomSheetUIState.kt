package mega.privacy.android.app.presentation.bottomsheet.model

import nz.mega.sdk.MegaNode

/**
 * Node bottom sheet ui state
 * @property canMoveNode
 * @property canRestoreNode
 * @property isOnline
 * @property node
 * @property shareData
 * @property nodeDeviceCenterInformation
 * @property shareKeyCreated
 */
data class NodeBottomSheetUIState(
    val canMoveNode: Boolean = false,
    val canRestoreNode: Boolean = false,
    val isOnline: Boolean = true,
    val node: MegaNode? = null,
    val shareData: NodeShareInformation? = null,
    val nodeDeviceCenterInformation: NodeDeviceCenterInformation? = null,
    val shareKeyCreated: Boolean? = null,
)