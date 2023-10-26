package mega.privacy.android.app.presentation.imagepreview.slideshow.model

import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed

/**
 * Slideshow ViewState
 *
 * @property imageNodes Slideshow imageNodes
 * @property order Slideshow play order
 * @property speed Slideshow speed
 * @property repeat Loop play
 * @property isPlaying play or pause
 * @property shouldPlayFromFirst Should play slideshow from the first item
 * @property isFirstInSlideshow Is first time in slideshow
 */
data class IPSlideshowState(
    val imageNodes: List<ImageNode> = emptyList(),
    val order: SlideshowOrder? = null,
    val speed: SlideshowSpeed? = null,
    val repeat: Boolean = false,
    val isPlaying: Boolean = false,
    val shouldPlayFromFirst: Boolean = false,
    val isFirstInSlideshow: Boolean = true,
)