package mega.privacy.android.app.presentation.slideshow.model

/**
 * Slideshow play order strategy
 */
enum class SlideshowOrder {

    Shuffle,

    Newest,

    Oldest;

    companion object {
        /**
         * The default Slideshow play order
         */
        val DEFAULT = Shuffle
    }
}
