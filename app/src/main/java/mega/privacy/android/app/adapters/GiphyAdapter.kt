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

class GiphyAdapter(val gifs: ArrayList<Data>): RecyclerView.Adapter<GiphyAdapter.GifViewHolder>() {

    class GifViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private val gifView: SimpleDraweeView = view.findViewById(R.id.gif_view)

        fun bind(gif: Data) {
            loadGif(gifView, Uri.parse(gif.images!!.fixed_height?.webp))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GifViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.gif_item, parent, false)
        return GifViewHolder(view)
    }

    override fun getItemCount(): Int {
        return gifs.size
    }

    override fun onBindViewHolder(holder: GifViewHolder, position: Int) {
        return holder.bind(gifs[position])
    }

}