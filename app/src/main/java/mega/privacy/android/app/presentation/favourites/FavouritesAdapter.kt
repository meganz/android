package mega.privacy.android.app.presentation.favourites

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemFavouriteBinding
import mega.privacy.android.app.databinding.SortByHeaderBinding
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.mediaplayer.playlist.PlaylistAdapter
import mega.privacy.android.app.presentation.favourites.model.Favourite
import mega.privacy.android.app.presentation.favourites.model.FavouriteHeaderItem
import mega.privacy.android.app.presentation.favourites.model.FavouriteItem
import mega.privacy.android.app.presentation.favourites.model.FavouriteListItem
import mega.privacy.android.app.utils.Constants.ITEM_VIEW_TYPE
import java.io.File

/**
 * The adapter regarding favourites
 * @param sortByHeaderViewModel SortByHeaderViewModel
 * @param onItemClicked The item clicked listener
 * @param onLongClicked The item long clicked listener
 * @param onThreeDotsClicked The three dots view clicked listener
 * @param getThumbnail the function that get Thumbnail
 */
class FavouritesAdapter(
    private val sortByHeaderViewModel: SortByHeaderViewModel? = null,
    private val onItemClicked: (info: Favourite, icon: ImageView, position: Int) -> Unit,
    private val onLongClicked: (info: Favourite, icon: ImageView, position: Int) -> Boolean = { _, _, _ -> false },
    private val onThreeDotsClicked: (info: Favourite) -> Unit,
    private val getThumbnail: (handle: Long, (file: File?) -> Unit) -> Unit
) : ListAdapter<FavouriteItem, FavouritesViewHolder>(FavouritesDiffCallback) {

    override fun getItemViewType(position: Int): Int = getItem(position).type

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavouritesViewHolder {
        return FavouritesViewHolder(
            parent.context,
            if (viewType == ITEM_VIEW_TYPE) {
                ItemFavouriteBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            }else {
                SortByHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            }
        )
    }

    override fun onBindViewHolder(holder: FavouritesViewHolder, position: Int) {
        holder.bind(
            item = getItem(position),
            position = position,
            sortByHeaderViewModel = sortByHeaderViewModel,
            onItemClicked = onItemClicked,
            onThreeDotsClicked = onThreeDotsClicked,
            onLongClicked = onLongClicked,
            getThumbnail = getThumbnail
        )
    }

    /**
     * Start the animation
     * @param context Context
     * @param imageView the ImageView that adds the animation
     * @param position the position of item that adds the animation
     */
    fun startAnimation(context: Context, imageView: ImageView, position: Int) {
        val flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip)
        flipAnimation.duration = PlaylistAdapter.ANIMATION_DURATION
        flipAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                notifyItemChanged(position)
            }

            override fun onAnimationEnd(animation: Animation) {
                notifyItemChanged(position)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        imageView.startAnimation(flipAnimation)
    }
}

/**
 * The view holder regarding favourites
 */
class FavouritesViewHolder(
    private val context: Context,
    private val binding: ViewBinding
) : RecyclerView.ViewHolder(binding.root) {

    /**
     * bind data
     * @param item FavouriteItem
     * @param position position of current item
     * @param sortByHeaderViewModel SortByHeaderViewModel
     * @param onItemClicked The item clicked listener
     * @param onThreeDotsClicked The three dots view clicked listener
     * @param onLongClicked The long clicked listener
     */
    fun bind(
        item: FavouriteItem,
        position: Int,
        sortByHeaderViewModel: SortByHeaderViewModel?,
        onItemClicked: (info: Favourite, icon: ImageView, position: Int) -> Unit,
        onThreeDotsClicked: (info: Favourite) -> Unit,
        onLongClicked: (info: Favourite, icon: ImageView, position: Int) -> Boolean,
        getThumbnail: (handle: Long, (file: File?) -> Unit) -> Unit
    ) {
        with(binding) {
            when (this) {
                is ItemFavouriteBinding -> {
                    item.favourite?.let { info ->
                        info.thumbnailPath?.let { thumbnailPath ->
                            if (item is FavouriteListItem && isThumbnailAvailable(info)) {
                                File(thumbnailPath).let { file ->
                                    if (file.exists()) {
                                        itemThumbnail.setImageURI(Uri.fromFile(file))
                                    } else {
                                        getThumbnail(info.handle) { thumbnail ->
                                            thumbnail?.let { fileByGetThumbnail ->
                                                itemThumbnail.setImageURI(
                                                    Uri.fromFile(
                                                        fileByGetThumbnail
                                                    )
                                                )
                                            }
                                        }
                                        itemThumbnail.setImageResource(info.icon)
                                    }
                                }
                            } else {
                                itemThumbnail.setImageResource(info.icon)
                            }
                        } ?: itemThumbnail.setImageResource(info.icon)
                        itemFilename.text = info.name
                        itemImgLabel.setImageDrawable(
                                ResourcesCompat.getDrawable(
                                        context.resources,
                                        R.drawable.ic_circle_label,
                                        null
                                )
                                        ?.apply {
                                            setTint(
                                                    ResourcesCompat.getColor(
                                            context.resources,
                                            info.labelColour,
                                            null
                                        )
                                    )
                                }
                        )
                        itemThumbnail.isVisible = !info.isSelected
                        imageSelected.visibility = if (info.isSelected) {
                            View.VISIBLE
                        } else {
                            View.INVISIBLE
                        }
                        itemImgLabel.isVisible = info.showLabel
                        fileListSavedOffline.isVisible = info.isAvailableOffline
                        itemImgFavourite.isVisible = info.isFavourite
                        itemPublicLink.isVisible = info.isExported
                        itemTakenDown.isVisible = info.isTakenDown
                        itemVersionsIcon.isVisible = info.hasVersion
                        itemFileInfo.text = info.info
                        itemFavouriteLayout.setOnClickListener {
                            onItemClicked(info, imageSelected, position)
                        }
                        itemThreeDots.setOnClickListener {
                            onThreeDotsClicked(info)
                        }

                        itemFavouriteLayout.setOnLongClickListener {
                            onLongClicked(info, imageSelected, position)
                        }
                    }
                }
                is SortByHeaderBinding -> {
                    orderNameStringId =
                        (item as FavouriteHeaderItem).orderStringId ?: R.string.sortby_name
                    enterMediaDiscovery.isVisible = false
                    this.sortByHeaderViewModel = sortByHeaderViewModel
                }
                else -> { }
            }
        }
    }

    /**
     * Check whether needs to get thumbnail
     * @param favourite favourite item
     * @return true needs to get thumbnail
     */
    private fun isThumbnailAvailable(favourite: Favourite) =
        !favourite.isFolder && MimeTypeList.typeForName(favourite.name).run {
            isAudio || isVideo || isImage || isPdf || isMp4Video || isGIF
        }
}

/**
 * Favourites DiffCallback
 */
object FavouritesDiffCallback : DiffUtil.ItemCallback<FavouriteItem>() {
    override fun areItemsTheSame(oldInfo: FavouriteItem, newInfo: FavouriteItem): Boolean {
        return oldInfo.favourite?.handle == newInfo.favourite?.handle
    }

    override fun areContentsTheSame(oldInfo: FavouriteItem, newInfo: FavouriteItem): Boolean {
        return oldInfo == newInfo
    }
}