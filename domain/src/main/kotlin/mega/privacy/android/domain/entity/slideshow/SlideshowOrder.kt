package mega.privacy.android.domain.entity.slideshow

/**
 * Slideshow order options.
 */
enum class SlideshowOrder(
    val id: Int,
    val degree: Int,
) {
    Shuffle(1, 0),
    Newest(2, 1),
    Oldest(3, 2),
}
