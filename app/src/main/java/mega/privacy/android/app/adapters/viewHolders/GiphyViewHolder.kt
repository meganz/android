package mega.privacy.android.app.adapters.viewHolders

import android.net.Uri
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import mega.privacy.android.app.R
import mega.privacy.android.app.interfaces.GiphyInterface
import mega.privacy.android.app.objects.Data
import mega.privacy.android.app.objects.GifData
import mega.privacy.android.app.utils.FrescoUtils

class GiphyViewHolder(val view: View, private val giphyInterface: GiphyInterface) : RecyclerView.ViewHolder(view) {
    private var gifImage: SimpleDraweeView = view.findViewById(R.id.gif_image)
    var gifData: GifData? = null

    fun bind(gif: Data?) {
        val imageAttributes = gif?.images?.fixedHeight
        val gifWidth = imageAttributes?.width ?: 0
        val gifHeight = imageAttributes?.height ?: 0

        gifData = GifData(imageAttributes?.mp4,
                imageAttributes?.webp,
                imageAttributes?.mp4Size ?: 0,
                imageAttributes?.webpSize ?: 0,
                gifWidth,
                gifHeight,
                gif?.title)

        updateLayoutParams(gifWidth, gifHeight)
        FrescoUtils.loadGif(gifImage, Uri.parse(imageAttributes?.webp))
        view.tag = this@GiphyViewHolder
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