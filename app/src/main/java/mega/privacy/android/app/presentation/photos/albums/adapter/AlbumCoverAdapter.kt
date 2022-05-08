package mega.privacy.android.app.presentation.photos.albums.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.databinding.ItemAlbumCoverBinding
import mega.privacy.android.app.presentation.photos.model.AlbumCoverItem

/**
 * Adapter to show albums grid list.
 */
class AlbumCoverAdapter(
    private val coverWidth: Int,
    private val coverMargin: Int,
    private val listener: Listener
) : ListAdapter<AlbumCoverItem, AlbumCoverViewHolder>(AlbumCoverDiffCallback()) {

    /**
     * DiffCallback for comparing AlbumCover
     */
    class AlbumCoverDiffCallback : DiffUtil.ItemCallback<AlbumCoverItem>() {
        override fun areItemsTheSame(oldItem: AlbumCoverItem, newItem: AlbumCoverItem) =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: AlbumCoverItem, newItem: AlbumCoverItem) =
            oldItem.itemCount == newItem.itemCount && oldItem.coverThumbnail == newItem.coverThumbnail && oldItem.titleResId == newItem.titleResId
    }

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

    interface Listener {

        /**
         * When cover gets to be click
         */
        fun onCoverClicked(album: AlbumCoverItem)
    }
}
