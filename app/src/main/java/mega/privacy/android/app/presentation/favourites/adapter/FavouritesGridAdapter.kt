package mega.privacy.android.app.presentation.favourites.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.viewbinding.ViewBinding
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemFavouriteBinding
import mega.privacy.android.app.databinding.ItemFavouriteGridBinding
import mega.privacy.android.app.databinding.SortByHeaderBinding
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.fragments.homepage.setNodeGridThumbnail
import mega.privacy.android.app.mediaplayer.playlist.PlaylistAdapter
import mega.privacy.android.app.presentation.favourites.model.Favourite
import mega.privacy.android.app.presentation.favourites.model.FavouriteFolder
import mega.privacy.android.app.presentation.favourites.model.FavouriteHeaderItem
import mega.privacy.android.app.presentation.favourites.model.FavouriteItem
import mega.privacy.android.app.presentation.favourites.model.FavouriteListItem
import mega.privacy.android.app.utils.Constants.HEADER_VIEW_TYPE
import mega.privacy.android.app.utils.Constants.ITEM_PLACEHOLDER_TYPE
import mega.privacy.android.app.utils.Constants.ITEM_VIEW_TYPE
import mega.privacy.android.app.utils.TimeUtils
import java.io.File

/**
 * The adapter regarding favourites
 * @param sortByHeaderViewModel SortByHeaderViewModel
 * @param onItemClicked The item clicked listener
 * @param onLongClicked The item long clicked listener
 * @param onThreeDotsClicked The three dots view clicked listener
 * @param getThumbnail the function that get Thumbnail
 */
class FavouritesGridAdapter(
    private val sortByHeaderViewModel: SortByHeaderViewModel? = null,
    private val onItemClicked: (info: Favourite) -> Unit,
    private val onLongClicked: (info: Favourite) -> Boolean = { _ -> false },
    private val onThreeDotsClicked: (info: Favourite) -> Unit,
    private val getThumbnail: (handle: Long, (file: File?) -> Unit) -> Unit,
) : ListAdapter<FavouriteItem, FavouritesGridViewHolder>(FavouritesDiffCallback) {

    override fun getItemViewType(position: Int): Int = getItem(position).type

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavouritesGridViewHolder {
        return FavouritesGridViewHolder(
            when (viewType) {
                ITEM_PLACEHOLDER_TYPE,
                ITEM_VIEW_TYPE,
                -> {
                    ItemFavouriteGridBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                }
                else -> {
                    SortByHeaderBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                }
            }
        )
    }

    override fun onBindViewHolder(holder: FavouritesGridViewHolder, position: Int) {
        holder.bind(
            item = getItem(position),
            sortByHeaderViewModel = sortByHeaderViewModel,
            onItemClicked = onItemClicked,
            onThreeDotsClicked = onThreeDotsClicked,
            onLongClicked = onLongClicked,
            getThumbnail = getThumbnail
        )
    }

    fun getSpanSizeLookup(spanCount: Int) = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            return if (getItemViewType(position) == HEADER_VIEW_TYPE) {
                spanCount
            } else {
                1
            }
        }
    }

}

/**
 * The view holder regarding favourites
 */
class FavouritesGridViewHolder(
    private val binding: ViewBinding,
) : Selectable(binding.root) {

    /**
     * bind data
     * @param item FavouriteItem
     * @param sortByHeaderViewModel SortByHeaderViewModel
     * @param onItemClicked The item clicked listener
     * @param onThreeDotsClicked The three dots view clicked listener
     * @param onLongClicked The long clicked listener
     */
    fun bind(
        item: FavouriteItem,
        sortByHeaderViewModel: SortByHeaderViewModel?,
        onItemClicked: (info: Favourite) -> Unit,
        onThreeDotsClicked: (info: Favourite) -> Unit,
        onLongClicked: (info: Favourite) -> Boolean,
        getThumbnail: (handle: Long, (file: File?) -> Unit) -> Unit,
    ) {
        with(binding) {
            when (this) {
                is ItemFavouriteGridBinding -> {
                    item.favourite?.let { info ->
                        val backgroundColor = if (info.isSelected) {
                            R.drawable.background_item_grid_selected
                        } else {
                            R.drawable.background_item_grid
                        }
                        itemGridFolder.isVisible = info is FavouriteFolder
                        itemGridFile.isVisible = info !is FavouriteFolder
                        if (info is FavouriteFolder) {
                            itemGridFolder.setBackgroundResource(backgroundColor)
                            with(folderGridIcon) {
                                setImageResource(info.icon)
                                isVisible = !info.isSelected
                            }
                            folderGridTakenDown.isVisible = info.typedNode.isTakenDown
                            textViewSettings(folderGridFilename, info)
                            folderIcSelected.isVisible = info.isSelected
                            folderGridThreeDots.setOnClickListener {
                                onThreeDotsClicked(info)
                            }
                        } else {
                            itemGridFile.setBackgroundResource(backgroundColor)
                            info.thumbnailPath?.let { thumbnailPath ->
                                if (item is FavouriteListItem && isThumbnailAvailable(info.typedNode.name)) {
                                    File(thumbnailPath).let { file ->
                                        if (file.exists()) {
                                            setNodeGridThumbnail(itemThumbnail, file, info.icon)
                                        } else {
                                            getThumbnail(info.typedNode.id.longValue) { thumbnail ->
                                                thumbnail?.let { fileByGetThumbnail ->
                                                    setNodeGridThumbnail(
                                                        itemThumbnail,
                                                        fileByGetThumbnail,
                                                        info.icon
                                                    )
                                                }
                                            }
                                            setNodeGridThumbnail(itemThumbnail, null, info.icon)
                                        }
                                    }
                                } else {
                                    setNodeGridThumbnail(itemThumbnail, null, info.icon)
                                }
                            } ?: setNodeGridThumbnail(itemThumbnail, null, info.icon)

                            icSelected.visibility = if (info.isSelected) {
                                View.VISIBLE
                            } else {
                                View.INVISIBLE
                            }
                            if (MimeTypeList.typeForName(info.typedNode.name).isVideo) {
                                videoInfo.isVisible = true
                                videoDuration.text =
                                    TimeUtils.getVideoDuration((info.node.duration))
                            } else {
                                videoInfo.isVisible = false
                            }
                            takenDown.isVisible = info.typedNode.isTakenDown
                            textViewSettings(filename, info)
                            filenameContainer.setOnClickListener {
                                onThreeDotsClicked(info)
                            }
                        }
                        itemGirdFavourite.apply {
                            setOnLongClickListener {
                                onLongClicked(info)
                            }
                            setOnClickListener {
                                onItemClicked(info)
                            }
                        }
                    } ?: run {
                        itemGridFolder.isVisible = false
                        itemGridFile.isVisible = false
                    }
                }
                is SortByHeaderBinding -> {
                    orderNameStringId =
                        (item as FavouriteHeaderItem).orderStringId ?: R.string.sortby_name
                    enterMediaDiscovery.isVisible = false
                    this.sortByHeaderViewModel = sortByHeaderViewModel
                }
                else -> {}
            }
        }
    }

    /**
     * TextView set text and text color
     * @param textView TextView
     * @param info Favourite
     */
    private fun textViewSettings(textView: TextView, info: Favourite) {
        with(textView) {
            text = info.typedNode.name
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (info.typedNode.isTakenDown) R.color.red_800_red_400 else R.color.grey_087_white_087
                )
            )
        }
    }

    /**
     * Check whether needs to get thumbnail
     * @param name node name
     * @return true needs to get thumbnail
     */
    private fun isThumbnailAvailable(name: String) =
        MimeTypeList.typeForName(name).run {
            isAudio || isVideo || isImage || isPdf || isMp4Video || isGIF
        }

    override fun animate(listener: Animation.AnimationListener, isSelected: Boolean) {
        (binding as? ItemFavouriteBinding)?.let {
            val flipAnimation = if (isSelected) AnimationUtils.loadAnimation(binding.root.context,
                R.anim.multiselect_flip_reverse) else AnimationUtils.loadAnimation(binding.root.context,
                R.anim.multiselect_flip)
            flipAnimation.duration = PlaylistAdapter.ANIMATION_DURATION
            flipAnimation.setAnimationListener(listener)
            it.imageSelected.startAnimation(flipAnimation)
        }
    }
}