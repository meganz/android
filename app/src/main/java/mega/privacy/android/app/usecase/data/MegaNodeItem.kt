package mega.privacy.android.app.usecase.data

import nz.mega.sdk.MegaNode

data class MegaNodeItem constructor(
    val node: MegaNode,
    val hasFullAccess: Boolean,
    val isFromRubbishBin: Boolean,
    val isFromInbox: Boolean,
    val isFromRoot: Boolean,
    val isAvailableOffline: Boolean
)
