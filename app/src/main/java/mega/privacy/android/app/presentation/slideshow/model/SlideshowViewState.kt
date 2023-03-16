package mega.privacy.android.app.presentation.slideshow.model

import mega.privacy.android.domain.entity.photos.Photo

/**
 * Slideshow ViewState
 *
 * @property items slideshow items
 * @property order slideshow play order
 * @property speed slideshow speed
 * @property repeat loop play
 */
data class SlideshowViewState(
    val items: List<Photo> = emptyList(),
    val order: SlideshowOrder = SlideshowOrder.DEFAULT,
    val speed: SlideshowSpeed = SlideshowSpeed.DEFAULT,
    val repeat: Boolean = true,
)

