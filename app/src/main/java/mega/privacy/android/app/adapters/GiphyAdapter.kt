package mega.privacy.android.app.adapters

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.GiphyActivity
import mega.privacy.android.app.objects.Data
import mega.privacy.android.app.objects.GifData
import mega.privacy.android.app.utils.FrescoUtils.loadGif

class GiphyAdapter(private var gifs: ArrayList<Data>?, private val context: Context): RecyclerView.Adapter<GiphyAdapter.GifViewHolder>(), View.OnClickListener {

    class GifViewHolder(val view: View, val context: Context) : RecyclerView.ViewHolder(view) {
        private var gifView: RelativeLayout = view.findViewById(R.id.gif_view)
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
            loadGif(gifImage, Uri.parse(imageAttributes?.webp))
            view.tag = this@GifViewHolder
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
                val params = gifView.layoutParams
                params.height = (context as GiphyActivity).getScreenGifHeight(gifWidth, gifHeight)
                gifView.layoutParams = params
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GifViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.gif_item, parent, false)
        return GifViewHolder(view, context)
    }

    override fun getItemCount(): Int {
        return gifs?.size ?: 0
    }

    override fun onBindViewHolder(holder: GifViewHolder, position: Int) {
        holder.bind(gifs?.get(position))
        holder.itemView.setOnClickListener(this@GiphyAdapter)
    }

    fun setGifs(newGifs: ArrayList<Data>?) {
        gifs = newGifs
        (context as GiphyActivity).setEmptyState(gifs?.isEmpty() ?: true)
        notifyDataSetChanged()
    }

    override fun onClick(v: View?) {
        val holder = v?.tag as GifViewHolder
        (context as GiphyActivity).openGifViewer(holder.gifData)
    }
}