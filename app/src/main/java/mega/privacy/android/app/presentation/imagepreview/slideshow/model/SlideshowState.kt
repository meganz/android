package mega.privacy.android.app.presentation.imagepreview.slideshow.model

import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed

/**
 * Slideshow ViewState
 *
 * @property isInitialized
 * @property imageNodes Slideshow imageNodes
 * @property currentImageNode Current imageNode
 * @property currentImageNodeIndex Current imageNode index
 * @property order Slideshow play order
 * @property speed Slideshow speed
 * @property repeat Loop play
 * @property isPlaying play or pause
 */
data class SlideshowState(
    val isInitialized: Boolean = false,
    val imageNodes: List<ImageNode> = emptyList(),
    val currentImageNode: ImageNode? = null,
    val currentImageNodeIndex: Int = 0,
    val order: SlideshowOrder? = null,
    val speed: SlideshowSpeed? = null,
    val repeat: Boolean = false,
    val isPlaying: Boolean = false,
)

internal data class ImageResultStatus(
    val progress: Int,
    val isFullyLoaded: Boolean,
    val imagePath: String?,
    val errorImagePath: String?,
)
