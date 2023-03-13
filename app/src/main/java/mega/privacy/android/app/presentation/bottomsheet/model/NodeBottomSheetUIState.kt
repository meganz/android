package mega.privacy.android.app.presentation.bottomsheet.model

import nz.mega.sdk.MegaNode

/**
 * Node bottom sheet ui state
 *
 * @property node
 * @property shareData
 * @property shareKeyCreated
 * @property isOnline
 */
data class NodeBottomSheetUIState(
    val node: MegaNode? = null,
    val shareData: NodeShareInformation? = null,
    val shareKeyCreated: Boolean? = null,
    val isOnline: Boolean = true,
)