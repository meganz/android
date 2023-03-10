package mega.privacy.android.app.presentation.bottomsheet.model

import mega.privacy.android.domain.entity.ShareData
import nz.mega.sdk.MegaNode

data class NodeBottomSheetUIState(
    val node: MegaNode? = null,
    val shareData: ShareData? = null,
    val shareKeyCreated: Boolean? = null,
    val isOnline: Boolean = true
)