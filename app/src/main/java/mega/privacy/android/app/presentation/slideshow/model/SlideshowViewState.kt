package mega.privacy.android.app.presentation.slideshow.model

import mega.privacy.android.domain.entity.photos.Photo

/**
 * Slideshow ViewState
 *
 * @property items Slideshow items
 * @property currentPlayingChunkedIndex Indicate handle chunked playing slideshow in case avoiding OOM
 * @property order Slideshow play order
 * @property speed Slideshow speed
 * @property repeat Loop play
 */
data class SlideshowViewState(
    val items: List<Photo> = emptyList(),
    val currentPlayingChunkedIndex: Int = 0,
    val order: SlideshowOrder = SlideshowOrder.Newest,
    val speed: SlideshowSpeed = SlideshowSpeed.DEFAULT,
    val repeat: Boolean = true,
)

