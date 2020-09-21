package mega.privacy.android.app.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import mega.privacy.android.app.R
import mega.privacy.android.app.objects.Data
import mega.privacy.android.app.utils.FrescoUtils.loadGif

class GiphyAdapter(private val gifs: ArrayList<Data>?): RecyclerView.Adapter<GiphyAdapter.GifViewHolder>() {

    class GifViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private val gifView: SimpleDraweeView = view.findViewById(R.id.gif_view)

        fun bind(gif: Data?) {
            val imageAttributes = gif?.images?.fixed_height
            val gifWidth = imageAttributes?.width ?: 0
            val gifHeight = imageAttributes?.height ?: 0

            updateLayoutParams(gifWidth, gifHeight)
            loadGif(gifView, Uri.parse(imageAttributes?.webp))
        }

        private fun updateLayoutParams(gifWidth: Int, gifHeight: Int) {
            if (gifWidth > 0 && gifHeight > 0) {
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GifViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.gif_item, parent, false)
        return GifViewHolder(view)
    }

    override fun getItemCount(): Int {
        return gifs?.size ?: 0
    }

    override fun onBindViewHolder(holder: GifViewHolder, position: Int) {
        holder.bind(gifs?.get(position))
    }
}