package mega.privacy.android.app.presentation.imagepreview.model

import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId

data class ImagePreviewState(
    val imageNodes: List<ImageNode> = emptyList(),
    val currentImageNodeId: NodeId = NodeId(0L),
    val showAppBar: Boolean = true,
    val showSlideshowOption: Boolean = false,
    val inFullScreenMode: Boolean = true,
    val transferMessage: String = "",
)