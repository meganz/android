package mega.privacy.android.app.interfaces

import mega.privacy.android.app.objects.GifData

interface GiphyInterface {
    /**
     * Opens the GIF viewer showing the selected GIF.
     *
     * @param gifData   Object containing all the necessary GIF data.
     */
    fun openGifViewer(gifData: GifData?)

    /**
     * Shows the empty state if the result of the request is empty.
     *
     * @param emptyList True if the request is empty, false otherwise.
     */
    fun setEmptyState(emptyList: Boolean)

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