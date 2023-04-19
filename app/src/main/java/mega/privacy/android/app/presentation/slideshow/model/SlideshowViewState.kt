package mega.privacy.android.app.presentation.slideshow.model

import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed

/**
 * Slideshow ViewState
 *
 * @property items Slideshow items
 * @property order Slideshow play order
 * @property speed Slideshow speed
 * @property repeat Loop play
 * @property isPlaying play or pause
 * @property shouldPlayFromFirst Should play slideshow from the first item
 * @property isFirstInSlideshow Is first time in slideshow
 */
data class SlideshowViewState(
    val items: List<Photo> = emptyList(),
    val order: SlideshowOrder? = null,
    val speed: SlideshowSpeed? = null,
    val repeat: Boolean = false,
    val isPlaying: Boolean = false,
    val shouldPlayFromFirst: Boolean = false,
    val isFirstInSlideshow: Boolean = true,
)

