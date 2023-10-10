package mega.privacy.android.app.presentation.imagepreview.model

import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId

data class ImagePreviewState(
    val imageNodes: List<ImageNode> = emptyList(),
    val currentPreviewPhotoId: NodeId = NodeId(0L),
    val showAppBar: Boolean = true,
    val shouldShowSlideshowOption: Boolean = false,
    val shouldShowLinkOption: Boolean = false,
    val shouldShowDownloadOption: Boolean = false,
    val shouldShowForwardOption: Boolean = false,
)