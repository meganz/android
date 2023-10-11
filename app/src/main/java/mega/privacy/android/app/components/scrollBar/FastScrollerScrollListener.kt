package mega.privacy.android.app.components.scrollBar

/**
 * Interface to listen fast scroller events
 */
interface FastScrollerScrollListener {
    /**
     * Called when view is scrolled
     */
    fun onScrolled()

    /**
     * Called when view is scrolled to top
     */
    fun onScrolledToTop()
}