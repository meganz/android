package mega.privacy.android.app.presentation.favourites

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemFavouriteBinding
import mega.privacy.android.app.presentation.favourites.model.Favourite

/**
 * The adapter regarding favourites
 * @param onItemClicked The item clicked listener
 * @param onThreeDotsClicked The three dots view clicked listener
 */
class FavouritesAdapter(
    private val onItemClicked: (info: Favourite) -> Unit,
    private val onThreeDotsClicked: (info: Favourite) -> Unit
) : ListAdapter<Favourite, FavouritesViewHolder>(FavouritesDiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavouritesViewHolder {
        return FavouritesViewHolder(
            parent.context,
            ItemFavouriteBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: FavouritesViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClicked, onThreeDotsClicked)
    }
}

/**
 * The view holder regarding favourites
 */
class FavouritesViewHolder(
    private val context: Context,
    private val binding: ItemFavouriteBinding
) : RecyclerView.ViewHolder(binding.root) {
    /**
     * bind data
     * @param info Favourite
     * @param onItemClicked The item clicked listener
     * @param onThreeDotsClicked The three dots view clicked listener
     */
    fun bind(
        info: Favourite,
        onItemClicked: (info: Favourite) -> Unit,
        onThreeDotsClicked: (info: Favourite) -> Unit
    ) {
        with(binding) {
            itemThumbnail.setImageResource(info.icon)
            itemFilename.text = info.name

            itemImgLabel.setImageDrawable(
                ResourcesCompat.getDrawable(context.resources, R.drawable.ic_circle_label, null)
                    ?.apply {
                        setTint(ResourcesCompat.getColor(context.resources, info.labelColour, null))
                    }
            )
            itemImgLabel.isVisible = info.showLabel
            fileListSavedOffline.isVisible = info.isAvailableOffline
            itemImgFavourite.isVisible = info.isFavourite
            itemPublicLink.isVisible = info.isExported
            itemTakenDown.isVisible = info.isTakenDown
            itemVersionsIcon.isVisible = info.hasVersion
            itemFileInfo.text = info.info
            itemFavouriteLayout.setOnClickListener {
                onItemClicked(info)
            }
            itemThreeDots.setOnClickListener {
                onThreeDotsClicked(info)
            }
        }
    }
}

/**
 * Favourites DiffCallback
 */
object FavouritesDiffCallback : DiffUtil.ItemCallback<Favourite>() {
    override fun areItemsTheSame(oldInfo: Favourite, newInfo: Favourite): Boolean {
        return oldInfo.handle == newInfo.handle
    }

    override fun areContentsTheSame(oldInfo: Favourite, newInfo: Favourite): Boolean {
        return oldInfo == newInfo
    }
}