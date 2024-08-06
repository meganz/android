package mega.privacy.android.app.presentation.favourites.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.viewbinding.ViewBinding
import coil.load
import coil.transform.RoundedCornersTransformation
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemFavouriteBinding
import mega.privacy.android.app.databinding.SortByHeaderBinding
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.presentation.favourites.adapter.FavouritesAdapter.Companion.ANIMATION_DURATION
import mega.privacy.android.app.presentation.favourites.model.Favourite
import mega.privacy.android.app.presentation.favourites.model.FavouriteHeaderItem
import mega.privacy.android.app.presentation.favourites.model.FavouriteItem
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.ITEM_VIEW_TYPE
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest

/**
 * The adapter regarding favourites
 * @param sortByHeaderViewModel SortByHeaderViewModel
 * @param onItemClicked The item clicked listener
 * @param onLongClicked The item long clicked listener
 * @param onThreeDotsClicked The three dots view clicked listener
 */
class FavouritesAdapter(
    private val sortByHeaderViewModel: SortByHeaderViewModel? = null,
    private val onItemClicked: (info: Favourite) -> Unit,
    private val onLongClicked: (info: Favourite) -> Boolean = { _ -> false },
    private val onThreeDotsClicked: (info: Favourite) -> Unit,
) : ListAdapter<FavouriteItem, FavouritesViewHolder>(FavouritesDiffCallback) {

    private var selectionMode = false
    private var accountType: AccountType? = null
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
            } else {
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
            sortByHeaderViewModel = sortByHeaderViewModel,
            onItemClicked = onItemClicked,
            onThreeDotsClicked = onThreeDotsClicked,
            onLongClicked = onLongClicked,
            selectionMode = selectionMode,
            accountType = accountType,
        )
    }

    /**
     * Checks if the adapter is in selection mode
     */
    fun updateSelectionMode(isSelectionMode: Boolean) {
        selectionMode = isSelectionMode
    }

    fun updateAccountType(accountType: AccountType?) {
        this.accountType = accountType
    }

    companion object {
        /**
         * Animation duration
         */
        const val ANIMATION_DURATION = 250L
    }
}

/**
 * The view holder regarding favourites
 */
class FavouritesViewHolder(
    private val context: Context,
    private val binding: ViewBinding,
) : Selectable(binding.root) {

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
        sortByHeaderViewModel: SortByHeaderViewModel?,
        onItemClicked: (info: Favourite) -> Unit,
        onThreeDotsClicked: (info: Favourite) -> Unit,
        onLongClicked: (info: Favourite) -> Boolean,
        selectionMode: Boolean,
        accountType: AccountType?,
    ) {
        with(binding) {
            when (this) {
                is ItemFavouriteBinding -> {
                    item.favourite?.let { favourite: Favourite ->
                        itemThumbnail.load(ThumbnailRequest(NodeId(favourite.node.handle))) {
                            transformations(
                                RoundedCornersTransformation(
                                    Util.dp2px(Constants.THUMB_CORNER_RADIUS_DP).toFloat()
                                )
                            )
                            placeholder(favourite.icon)
                            error(favourite.icon)
                        }
                        textViewSettings(textView = itemFilename, favourite = favourite)
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
                                            favourite.labelColour,
                                            null
                                        )
                                    )
                                }
                        )
                        itemThumbnail.isVisible = !favourite.isSelected
                        imageSelected.visibility = if (favourite.isSelected) {
                            View.VISIBLE
                        } else {
                            View.INVISIBLE
                        }
                        itemImgLabel.isVisible = favourite.showLabel
                        fileListSavedOffline.isVisible = favourite.isAvailableOffline
                        itemImgFavourite.isVisible = favourite.typedNode.isFavourite
                        handleSensitiveEffect(
                            view = itemFavouriteLayout,
                            accountType = accountType,
                            favouriteNode = favourite.typedNode
                        )

                        itemPublicLink.isVisible = favourite.typedNode.exportedData != null
                        itemTakenDown.isVisible = favourite.typedNode.isTakenDown
                        itemVersionsIcon.isVisible = favourite.typedNode.hasVersion
                        itemFileInfo.text = favourite.info(context)
                        itemFavouriteLayout.setOnClickListener {
                            onItemClicked(favourite)
                        }
                        itemThreeDots.visibility =
                            if (selectionMode) View.INVISIBLE else View.VISIBLE
                        itemThreeDots.setOnClickListener {
                            onThreeDotsClicked(favourite)
                        }

                        itemFavouriteLayout.setOnLongClickListener {
                            onLongClicked(favourite)
                        }
                    }
                }

                is SortByHeaderBinding -> {
                    orderNameStringId =
                        (item as FavouriteHeaderItem).orderStringId ?: R.string.sortby_name
                    this.sortByHeaderViewModel = sortByHeaderViewModel
                }

                else -> {}
            }
        }
    }

    private fun handleSensitiveEffect(
        view: View,
        accountType: AccountType?,
        favouriteNode: TypedNode,
    ) {
        val isSensitive =
            accountType?.isPaid == true && (favouriteNode.isMarkedSensitive || favouriteNode.isSensitiveInherited)
        view.setAlpha(if (isSensitive) 0.5f else 1f)
    }

    override fun animate(listener: Animation.AnimationListener, isSelected: Boolean) {
        (binding as? ItemFavouriteBinding)?.let {
            val flipAnimation = if (isSelected) AnimationUtils.loadAnimation(
                context,
                R.anim.multiselect_flip_reverse
            ) else AnimationUtils.loadAnimation(
                context,
                R.anim.multiselect_flip
            )
            flipAnimation.duration = ANIMATION_DURATION
            flipAnimation.setAnimationListener(listener)
            it.imageSelected.startAnimation(flipAnimation)
        }
    }

    /**
     * TextView set text and text color
     * @param textView TextView
     * @param favourite Favourite
     */
    private fun textViewSettings(textView: TextView, favourite: Favourite) {
        textView.apply {
            text = favourite.typedNode.name
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (favourite.typedNode.isTakenDown) R.color.red_800_red_400 else R.color.grey_087_white_087
                )
            )
        }
    }
}

/**
 * Favourites DiffCallback
 */
object FavouritesDiffCallback : DiffUtil.ItemCallback<FavouriteItem>() {
    override fun areItemsTheSame(oldInfo: FavouriteItem, newInfo: FavouriteItem): Boolean {
        return oldInfo.favourite?.typedNode?.id == newInfo.favourite?.typedNode?.id
    }

    override fun areContentsTheSame(oldInfo: FavouriteItem, newInfo: FavouriteItem): Boolean {
        return oldInfo == newInfo
    }

    override fun getChangePayload(oldItem: FavouriteItem, newItem: FavouriteItem): Any? {
        return when {
            oldItem.favourite?.isSelected == false && newItem.favourite?.isSelected == true -> NOT_SELECTED_TO_SELECTED
            oldItem.favourite?.isSelected == true && newItem.favourite?.isSelected == false -> SELECTED_TO_NOT_SELECTED
            else -> null
        }
    }
}