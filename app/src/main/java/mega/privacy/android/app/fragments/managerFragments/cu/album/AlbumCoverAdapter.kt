package mega.privacy.android.app.fragments.managerFragments.cu.album

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.databinding.ItemAlbumCoverBinding

/**
 * Adapter to show albums grid list.
 */
class AlbumCoverAdapter(
    private val coverWidth: Int,
    private val coverMargin: Int,
    private val listener: Listener
) : ListAdapter<AlbumCover, AlbumCoverViewHolder>(AlbumCoverDiffCallback()) {

    /**
     * DiffCallback for comparing AlbumCover
     */
    class AlbumCoverDiffCallback : DiffUtil.ItemCallback<AlbumCover>() {
        override fun areItemsTheSame(oldItem: AlbumCover, newItem: AlbumCover) =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: AlbumCover, newItem: AlbumCover) =
            oldItem.count == newItem.count && oldItem.thumbnail == newItem.thumbnail && oldItem.title == newItem.title
    }

    /**
     * Item Dimen
     */
    private var itemDimen = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumCoverViewHolder =
        AlbumCoverViewHolder(
            ItemAlbumCoverBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            coverWidth,
            coverMargin
        )

    override fun onBindViewHolder(holder: AlbumCoverViewHolder, position: Int) {
        holder.bind(getItem(position), listener)
    }

    /**
     * Set Item Dimen
     */
    fun setItemDimen(dimen: Int) {
        if (dimen > 0) itemDimen = dimen
    }

    interface Listener {

        /**
         * When cover gets to be click
         */
        fun onCoverClicked(album: AlbumCover)
    }
}
