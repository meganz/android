package mega.privacy.android.app.presentation.slideshow.model

/**
 * Slideshow play speed strategy
 *
 * Mode (second) eg. Slow(8s)
 */
enum class SlideshowSpeed(val second: Int) {

    Slow(8),

    Normal(4),

    Fast(2);

    companion object {
        /**
         * The default Slideshow play speed
         */
        val DEFAULT = Normal
    }
}
