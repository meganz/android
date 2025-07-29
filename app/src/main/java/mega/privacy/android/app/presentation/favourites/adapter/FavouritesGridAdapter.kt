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
import coil3.load
import coil3.request.error
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemFavouriteBinding
import mega.privacy.android.app.databinding.ItemFavouriteGridBinding
import mega.privacy.android.app.databinding.SortByHeaderBinding
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.presentation.favourites.adapter.FavouritesAdapter.Companion.ANIMATION_DURATION
import mega.privacy.android.app.presentation.favourites.model.Favourite
import mega.privacy.android.app.presentation.favourites.model.FavouriteFolder
import mega.privacy.android.app.presentation.favourites.model.FavouriteHeaderItem
import mega.privacy.android.app.presentation.favourites.model.FavouriteItem
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.HEADER_VIEW_TYPE
import mega.privacy.android.app.utils.Constants.ITEM_PLACEHOLDER_TYPE
import mega.privacy.android.app.utils.Constants.ITEM_VIEW_TYPE
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest

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
) : ListAdapter<FavouriteItem, FavouritesGridViewHolder>(FavouritesDiffCallback) {

    private var selectionMode = false
    private var accountType: AccountType? = null
    private var isBusinessAccountExpired: Boolean = false
    private var hiddenNodeEnabled: Boolean = false

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
            selectionMode = selectionMode,
            accountType = accountType,
            isBusinessAccountExpired = isBusinessAccountExpired,
            hiddenNodeEnabled = hiddenNodeEnabled,
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

    /**
     * Checks if the adapter is in selection mode
     */
    fun updateSelectionMode(isSelectionMode: Boolean) {
        selectionMode = isSelectionMode
    }

    fun updateAccountType(
        accountType: AccountType?,
        isBusinessAccountExpired: Boolean,
        hiddenNodeEnabled: Boolean,
    ) {
        this.accountType = accountType
        this.isBusinessAccountExpired = isBusinessAccountExpired
        this.hiddenNodeEnabled = hiddenNodeEnabled
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
        selectionMode: Boolean,
        accountType: AccountType?,
        isBusinessAccountExpired: Boolean,
        hiddenNodeEnabled: Boolean,
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
                            }
                            folderGridTakenDown.isVisible = info.typedNode.isTakenDown
                            textViewSettings(folderGridFilename, info)
                            if (selectionMode) {
                                folderGridCheckIcon.visibility =
                                    if (info.isSelected) View.VISIBLE else View.INVISIBLE
                                folderGridThreeDots.visibility = View.GONE
                            } else {
                                folderGridThreeDots.visibility = View.VISIBLE
                                folderGridCheckIcon.visibility = View.GONE
                            }
                            folderGridThreeDots.setOnClickListener {
                                onThreeDotsClicked(info)
                            }
                        } else {
                            itemGridFile.setBackgroundResource(backgroundColor)
                            itemThumbnail.load(ThumbnailRequest(info.typedNode.id)) {
                                this.transformations(
                                    RoundedCornersTransformation(
                                        Util.dp2px(Constants.THUMB_CORNER_RADIUS_DP).toFloat()
                                    )
                                )
                                this.error(info.icon)
                            }

                            if (selectionMode) {
                                fileGridCheckIcon.visibility =
                                    if (info.isSelected) View.VISIBLE else View.INVISIBLE
                                fileGridThreeDots.visibility = View.GONE
                            } else {
                                fileGridThreeDots.visibility = View.VISIBLE
                                fileGridCheckIcon.visibility = View.GONE
                            }
                            if (MimeTypeList.typeForName(info.typedNode.name).isVideo) {
                                videoInfo.isVisible = true
                                audioInfo.isVisible = false
                                videoDuration.text =
                                    TimeUtils.getVideoDuration((info.node.duration))
                            } else if (MimeTypeList.typeForName(info.typedNode.name).isAudio) {
                                audioInfo.isVisible = true
                                videoInfo.isVisible = false
                                audioDuration.text =
                                    TimeUtils.getVideoDuration((info.node.duration))
                            } else {
                                videoInfo.isVisible = false
                                audioInfo.isVisible = false
                            }
                            takenDown.isVisible = info.typedNode.isTakenDown
                            textViewSettings(filename, info)
                            filenameContainer.setOnClickListener {
                                onThreeDotsClicked(info)
                            }
                        }
                        itemGirdFavourite.apply {
                            handleSensitiveEffect(
                                view = itemGirdFavourite,
                                shouldApplySensitiveMode = hiddenNodeEnabled
                                        && accountType?.isPaid == true
                                        && !isBusinessAccountExpired,
                                favouriteNode = info.typedNode,
                            )
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
                    sortByLayout.setOnClickListener {
                        sortByHeaderViewModel?.showSortByDialog()
                    }

                    listModeSwitch.setOnClickListener {
                        sortByHeaderViewModel?.switchViewType()
                    }
                    sortedBy.text = root.context.getString(
                        (item as FavouriteHeaderItem).orderStringId ?: R.string.sortby_name
                    )
                    listModeSwitch.setImageResource(
                        if (sortByHeaderViewModel?.isListView() == true)
                            mega.privacy.android.icon.pack.R.drawable.ic_grid_4_small_thin_outline
                        else
                            mega.privacy.android.icon.pack.R.drawable.ic_list_small_small_thin_outline
                    )
                }

                else -> {}
            }
        }
    }

    private fun handleSensitiveEffect(
        view: View,
        shouldApplySensitiveMode: Boolean,
        favouriteNode: TypedNode,
    ) {
        val isSensitive =
            shouldApplySensitiveMode && (favouriteNode.isMarkedSensitive || favouriteNode.isSensitiveInherited)
        view.setAlpha(if (isSensitive) 0.5f else 1f)
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

    override fun animate(listener: Animation.AnimationListener, isSelected: Boolean) {
        (binding as? ItemFavouriteBinding)?.let {
            val animationId =
                if (isSelected) R.anim.multiselect_flip_reverse else R.anim.multiselect_flip
            val flipAnimation = AnimationUtils.loadAnimation(binding.root.context, animationId)
            flipAnimation.duration = ANIMATION_DURATION
            flipAnimation.setAnimationListener(listener)
            it.imageSelected.startAnimation(flipAnimation)
        }
    }
}