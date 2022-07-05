package mega.privacy.android.app.presentation.photos.albums.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.databinding.ItemAlbumCoverBinding
import mega.privacy.android.app.presentation.photos.model.titleId
import mega.privacy.android.domain.entity.Album

/**
 * Adapter to show albums grid list.
 */
class AlbumCoverAdapter(
    private val coverWidth: Int,
    private val coverMargin: Int,
    private val listener: Listener,
) : ListAdapter<Album, AlbumCoverViewHolder>(AlbumCoverDiffCallback()) {

    /**
     * DiffCallback for comparing AlbumCover
     */
    class AlbumCoverDiffCallback : DiffUtil.ItemCallback<Album>() {
        override fun areItemsTheSame(oldItem: Album, newItem: Album) =
            oldItem.titleId == newItem.titleId

        override fun areContentsTheSame(oldItem: Album, newItem: Album) =
            oldItem.itemCount == newItem.itemCount && oldItem.thumbnail == newItem.thumbnail
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
        fun onCoverClicked(album: Album)
    }
}
