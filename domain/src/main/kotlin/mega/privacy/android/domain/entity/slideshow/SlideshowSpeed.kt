package mega.privacy.android.domain.entity.slideshow

/**
 * Slideshow speed options with [duration] in seconds.
 */
enum class SlideshowSpeed(val id: Int, val duration: Int) {
    Slow(1, 8),
    Normal(2, 4),
    Fast(3, 2),
}
