package mega.privacy.android.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.adapters.viewHolders.GiphyViewHolder
import mega.privacy.android.app.interfaces.GiphyInterface
import mega.privacy.android.app.interfaces.GiphyInterface.Companion.EMPTY_SEARCH
import mega.privacy.android.app.interfaces.GiphyInterface.Companion.NON_EMPTY
import mega.privacy.android.domain.entity.chat.Giphy

class GiphyAdapter(private var gifs: List<Giphy>?, private val giphyInterface: GiphyInterface) :
    RecyclerView.Adapter<GiphyViewHolder>(), View.OnClickListener {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GiphyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.gif_item, parent, false)
        return GiphyViewHolder(view, giphyInterface)
    }

    override fun getItemCount(): Int {
        return gifs?.size ?: 0
    }

    override fun onBindViewHolder(holder: GiphyViewHolder, position: Int) {
        holder.bind(gifs?.get(position))
        holder.itemView.setOnClickListener(this@GiphyAdapter)
    }

    fun setGifs(newGifs: ArrayList<Giphy>?) {
        gifs = newGifs
        giphyInterface.setEmptyState(if (gifs?.isEmpty() == true) EMPTY_SEARCH else NON_EMPTY)
        notifyDataSetChanged()
    }

    fun addGifs(newGifs: ArrayList<Giphy>?) {
        val oldLatestPosition = itemCount
        gifs = newGifs
        giphyInterface.setEmptyState(if (gifs?.isEmpty() == true) EMPTY_SEARCH else NON_EMPTY)
        notifyItemRangeInserted(oldLatestPosition, gifs?.size!!)
    }

    override fun onClick(v: View?) {
        val holder = v?.tag as GiphyViewHolder
        giphyInterface.openGifViewer(holder.gifData)
    }
}