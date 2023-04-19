package mega.privacy.android.app.presentation.slideshow.model

import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed

/**
 * Slideshow Setting ViewState
 *
 * @property order Slideshow play order
 * @property speed Slideshow speed
 * @property repeat Loop play
 */
data class SlideshowSettingViewState(
    val order: SlideshowOrder? = null,
    val speed: SlideshowSpeed? = null,
    val repeat: Boolean = false
)