package mega.privacy.android.app.interfaces

import mega.privacy.android.app.objects.GifData

interface GiphyInterface {

    companion object {
        const val NON_EMPTY = 0
        const val EMPTY_SEARCH = 1
        const val EMPTY_DOWN_SERVER = 2
    }

    /**
     * Opens the GIF viewer showing the selected GIF.
     *
     * @param gifData   Object containing all the necessary GIF data.
     */
    fun openGifViewer(gifData: GifData?)

    /**
     * Shows the empty state if the result of the request is empty or server is down.
     *
     * @param emptyState The state of the view. It can be:
     *   - NON_EMPTY: Hides empty view.
     *   - EMPTY_SEARCH: Shows empty view due to empty search.
     *   - EMPTY_DOWN_SERVER: Shows empty view due to down server.
     */
    fun setEmptyState(emptyState: Int)

    /**
     * Gets the height of a GIF to display on screen from its real width, real height and width available on the screen.
     * The width available on the screen will depend on the dimensions of the current device and the current columns displayed.
     *
     * @param gifWidth  Real width of the GIF.
     * @param gifHeight Real height of the GIF.
     * @return The height to display on screen.
     */
    fun getScreenGifHeight(gifWidth: Int, gifHeight: Int): Int
}