package mega.privacy.android.app.presentation.imagepreview.model

import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.domain.entity.node.ImageNode

data class ImagePreviewState(
    val isInitialized: Boolean = false,
    val imageNodes: List<ImageNode> = emptyList(),
    val currentImageNode: ImageNode? = null,
    val currentImageNodeIndex: Int = 0,
    val isCurrentImageNodeAvailableOffline: Boolean = false,
    val showAppBar: Boolean = true,
    val showSlideshowOption: Boolean = false,
    val inFullScreenMode: Boolean = false,
    val transferMessage: String = "",
    val resultMessage: String = "",
    val copyMoveException: Throwable? = null,
    val nameCollision: NameCollision? = null,
)