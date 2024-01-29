package mega.privacy.android.domain.entity.slideshow

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Slideshow speed options with [Duration] in seconds.
 *
 * @property id       Id of the speed option.
 * @property duration [Duration] of the speed option.
 */
enum class SlideshowSpeed(val id: Int, val duration: Duration) {

    /**
     * Slow speed option.
     */
    Slow(1, 8.seconds),

    /**
     * Normal speed option.
     */
    Normal(2, 4.seconds),

    /**
     * Fast speed option.
     */
    Fast(3, 2.seconds),
}
