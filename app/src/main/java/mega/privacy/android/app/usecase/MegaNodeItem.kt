package mega.privacy.android.app.usecase

import nz.mega.sdk.MegaNode

data class MegaNodeItem constructor(
    val node: MegaNode,
    val isAvailableOffline: Boolean
)
