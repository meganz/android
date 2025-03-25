package mega.privacy.android.app.adapters.viewHolders

import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import mega.privacy.android.app.R
import mega.privacy.android.app.interfaces.GiphyInterface
import mega.privacy.android.app.objects.Data
import mega.privacy.android.app.objects.GifData

class GiphyViewHolder(val view: View, private val giphyInterface: GiphyInterface) :
    RecyclerView.ViewHolder(view) {
    private val gifImage: AppCompatImageView = view.findViewById(R.id.gif_image)
    var gifData: GifData? = null

    fun bind(gif: Data?) {
        val imageAttributes = gif?.images?.fixedHeight ?: return
        val gifWidth = imageAttributes.width
        val gifHeight = imageAttributes.height

        gifData = GifData(
            imageAttributes.mp4,
            imageAttributes.webp,
            imageAttributes.mp4Size,
            imageAttributes.webpSize,
            gifWidth,
            gifHeight,
            gif.title
        )

        updateLayoutParams(gifWidth, gifHeight)
        view.tag = this@GiphyViewHolder

        val webpUri = imageAttributes.webp ?: return
        gifImage.load(webpUri) {
            size(
                giphyInterface.getScreenGifWidth(),
                giphyInterface.getScreenGifHeight(gifWidth, gifHeight)
            )
        }
    }

    /**
     * Updates the height of the view where the GIF will be shown to show the right one
     * depending on the available space on screen and dimensions of the GIF.
     *
     * @param gifWidth  Real width of the GIF.
     * @param gifHeight Real height of the GIF.
     */
    private fun updateLayoutParams(gifWidth: Int, gifHeight: Int) {
        if (gifWidth > 0 && gifHeight > 0) {
            val params = gifImage.layoutParams
            params.height = giphyInterface.getScreenGifHeight(gifWidth, gifHeight)
            gifImage.layoutParams = params
        }
    }
}