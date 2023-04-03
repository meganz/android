package mega.privacy.android.app.presentation.recentactions

import android.content.Context
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.components.dragger.DragThumbnailGetter
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemBucketBinding
import mega.privacy.android.app.fragments.homepage.main.HomepageFragment
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault
import mega.privacy.android.app.presentation.extensions.getQuantityStringOrDefault
import mega.privacy.android.app.presentation.recentactions.RecentActionsAdapter.RecentActionViewHolder
import mega.privacy.android.app.presentation.recentactions.model.RecentActionItemType
import mega.privacy.android.app.presentation.recentactions.model.RecentActionsSharesType
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelDrawable
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.node.Node
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * Adapter to display a list of recent actions
 *
 */
class RecentActionsAdapter @Inject constructor() : RecyclerView.Adapter<RecentActionViewHolder>(),
    SectionTitleProvider, DragThumbnailGetter {

    companion object {
        /**
         * Cloud drive folder name
         */
        private const val CLOUD_DRIVE_FOLDER_NAME = "Cloud Drive"
    }

    private var recentActionItems: List<RecentActionItemType>? = null

    /**
     * Lambda function to be invoked when an item is clicked with
     *
     * Parameters:
     * RecentActionItemType.Item: the item clicked
     * Int the position of the item in the list
     */
    private var onItemClickListener: ((RecentActionItemType.Item, Int) -> Unit)? = null

    /**
     * Lambda function to be invoked when a three dots button is clicked
     *
     * Parameters:
     * MegaNode: the node associated with the three dots
     */
    private var onThreeDotsClickListener: ((Node) -> Unit)? = null

    /**
     * The Homepage bottom sheet has a calculated background for elevation, while the
     * Recent fragment UI is transparent. This function is for calculating
     * the same background color as bottomSheet for the sticky "header"
     *
     * @return the header's background color value
     */
    private var headerColor: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentActionViewHolder {
        val binding = ItemBucketBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        val elevationPx = Util.dp2px(HomepageFragment.BOTTOM_SHEET_ELEVATION)
        headerColor = getColorForElevation(parent.context, elevationPx.toFloat())

        return RecentActionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentActionViewHolder, position: Int) = with(holder) {
        val item = getItemAtPosition(position) ?: return

        val context = holder.itemView.context

        when (item) {
            is RecentActionItemType.Header -> {
                Timber.d("onBindViewHolder: TYPE_HEADER")
                binding.itemBucketLayout.visibility = View.GONE
                binding.headerLayout.visibility = View.VISIBLE
                binding.headerLayout.setBackgroundColor(headerColor)
                binding.headerText.text = TimeUtils.formatBucketDate(item.timestamp, context)
            }

            is RecentActionItemType.Item -> {
                Timber.d("onBindViewHolder: TYPE_BUCKET")
                binding.itemBucketLayout.visibility = View.VISIBLE
                binding.itemBucketLayout.setOnClickListener {
                    onItemClickListener?.invoke(item, holder.bindingAdapterPosition)
                }
                binding.headerLayout.visibility = View.GONE

                val bucket = item.bucket
                val nodeList = bucket.nodes
                val node = nodeList[0]

                // folder name
                binding.nameText.text =
                    when (item.parentFolderName) {
                        CLOUD_DRIVE_FOLDER_NAME -> context.getFormattedStringOrDefault(R.string.section_cloud_drive)
                        else -> item.parentFolderName
                    }

                // owner description
                if (item.currentUserIsOwner) {
                    binding.secondLineText.visibility = View.GONE
                } else {
                    val userAction = if (bucket.isUpdate) {
                        context.getFormattedStringOrDefault(
                            R.string.update_action_bucket,
                            item.userName
                        )
                    } else {
                        context.getFormattedStringOrDefault(
                            R.string.create_action_bucket,
                            item.userName
                        )
                    }
                    binding.secondLineText.visibility = View.VISIBLE
                    binding.secondLineText.text = formatUserAction(context, userAction)
                }

                // shares icon
                when (item.parentFolderSharesType) {
                    RecentActionsSharesType.NONE -> {
                        binding.sharedImage.visibility = View.GONE
                    }

                    RecentActionsSharesType.INCOMING_SHARES -> {
                        binding.sharedImage.visibility = View.VISIBLE
                        binding.sharedImage.setImageResource(R.drawable.ic_folder_incoming_list)
                    }

                    RecentActionsSharesType.OUTGOING_SHARES,
                    RecentActionsSharesType.PENDING_OUTGOING_SHARES,
                    -> {
                        binding.sharedImage.visibility = View.VISIBLE
                        binding.sharedImage.setImageResource(R.drawable.ic_folder_outgoing_list)
                    }
                }

                // time
                binding.timeText.text = TimeUtils.formatTime(item.timestamp)

                // thumbnail
                binding.thumbnailView.visibility = View.VISIBLE
                val params = binding.thumbnailView.layoutParams as RelativeLayout.LayoutParams
                params.height = Util.dp2px(48f)
                params.width = params.height
                val margin = Util.dp2px(12f)
                params.setMargins(margin, margin, margin, 0)
                binding.thumbnailView.layoutParams = params
                binding.thumbnailView.setImageResource(MimeTypeList.typeForName(node.name).iconResourceId)

                // only one item in the recent action
                if (nodeList.size == 1) {
                    binding.threeDots.visibility = View.VISIBLE
                    binding.threeDots.setOnClickListener {
                        onThreeDotsClickListener?.invoke(node)
                    }
                    if (!item.isKeyVerified) {
                        binding.firstLineText.text =
                            context.resources.getQuantityString(
                                R.plurals.cloud_drive_undecrypted_file,
                                nodeList.size
                            )
                        binding.nameText.text =
                            context.getString(R.string.shared_items_verify_credentials_undecrypted_folder)
                    } else {
                        binding.firstLineText.text = node.name
                    }

                    if (node.label != MegaNode.NODE_LBL_UNKNOWN) {
                        val drawable =
                            getNodeLabelDrawable(node.label, holder.itemView.resources)
                        binding.imgLabel.setImageDrawable(drawable)
                        binding.imgLabel.visibility = View.VISIBLE
                    } else {
                        binding.imgLabel.visibility = View.GONE
                    }

                    binding.imgFavourite.visibility =
                        if (node.isFavourite) View.VISIBLE else View.GONE
                }
                // multiple items in the recent action
                else {
                    binding.threeDots.visibility = View.INVISIBLE
                    binding.threeDots.setOnClickListener(null)
                    binding.imgLabel.visibility = View.GONE
                    binding.imgFavourite.visibility = View.GONE

                    if (bucket.isMedia) {
                        binding.firstLineText.text = getMediaTitle(context, nodeList)
                        binding.thumbnailView.setImageResource(R.drawable.media)
                    } else {
                        if (!item.isKeyVerified) {
                            binding.firstLineText.text =
                                context.getQuantityStringOrDefault(
                                    R.plurals.cloud_drive_undecrypted_file,
                                    nodeList.size
                                )
                            binding.nameText.text =
                                context.getString(R.string.shared_items_verify_credentials_undecrypted_folder)
                        } else {
                            binding.firstLineText.text =
                                context.getFormattedStringOrDefault(
                                    R.string.title_bucket,
                                    node.name,
                                    nodeList.size - 1
                                )
                        }
                    }
                }

                if (bucket.isUpdate) {
                    binding.actionImage.setImageResource(R.drawable.ic_versions_small)
                } else {
                    binding.actionImage.setImageResource(R.drawable.ic_recents_up)
                }
            }
        }
    }

    override fun getNodePosition(handle: Long): Int {
        return recentActionItems
            ?.asSequence()
            ?.filterIsInstance<RecentActionItemType.Item>()
            ?.mapIndexed { index, item ->
                if (item.bucket.nodes.isNotEmpty()
                    && item.bucket.nodes[0].id.longValue == handle
                ) index
                else null
            }
            ?.filterNotNull()
            ?.singleOrNull()
            ?: Constants.INVALID_POSITION
    }


    override fun getItemCount(): Int = recentActionItems?.size ?: 0

    override fun getThumbnail(viewHolder: RecyclerView.ViewHolder): View? =
        if (viewHolder is RecentActionViewHolder) {
            viewHolder.binding.thumbnailView
        } else null

    override fun getSectionTitle(position: Int, context: Context): String {
        return if (recentActionItems.isNullOrEmpty() || position < 0 || position >= itemCount)
            ""
        else TimeUtils.formatBucketDate(recentActionItems!![position].timestamp, context)
    }

    /**
     * Set the recent action list and update the adapter
     *
     * @param recentItems
     */
    fun setItems(recentItems: List<RecentActionItemType>?) {
        recentActionItems = recentItems
        notifyDataSetChanged()
    }

    /**
     * Set the on item click listener
     *
     * @param listener function to trigger
     */
    fun setOnItemClickListener(listener: (RecentActionItemType.Item, Int) -> Unit) {
        onItemClickListener = listener
    }

    /**
     * Set the on three dots click listener
     *
     * @param listener function to trigger
     */
    fun setOnThreeDotsClickListener(listener: (Node) -> Unit) {
        onThreeDotsClickListener = listener
    }

    /**
     * Format the user action with decoration
     *
     * @param context
     * @param userAction the string to decorate
     * @return spanned string with decoration
     */
    private fun formatUserAction(context: Context, userAction: String): Spanned {
        val formattedUserAction = try {
            userAction
                .replace(
                    "[A]",
                    "<font color=\'" + getColorHexString(
                        context,
                        R.color.grey_300_grey_600
                    ) + "\'>"
                )
                .replace("[/A]", "</font>")
        } catch (e: Exception) {
            Timber.e(e, "Exception formatting string")
            ""
        }
        return Html.fromHtml(formattedUserAction, Html.FROM_HTML_MODE_LEGACY)
    }

    /**
     * Format the title in case there are multiple nodes in one recent action item
     *
     * @param context
     * @param nodeList list of nodes contained in the recent action item
     * @return a string corresponding to the title
     */
    private fun getMediaTitle(context: Context, nodeList: List<Node>): String {
        val partition = (nodeList.indices)
            .map { nodeList[it] }
            .partition { MimeTypeList.typeForName(it.name).isImage }
        val numImages = partition.first.size
        val numVideos = partition.second.size

        val mediaTitle = when {
            numImages > 0 && numVideos == 0 -> {
                context.getQuantityStringOrDefault(
                    R.plurals.title_media_bucket_only_images,
                    numImages,
                    numImages
                )
            }
            numImages == 0 && numVideos > 0 -> {
                context.getQuantityStringOrDefault(
                    R.plurals.title_media_bucket_only_videos,
                    numVideos,
                    numVideos
                )
            }
            else -> {
                context.getQuantityStringOrDefault(
                    R.plurals.title_media_bucket_images_and_videos,
                    numImages,
                    numImages
                ) +
                        context.getQuantityStringOrDefault(
                            R.plurals.title_media_bucket_images_and_videos_2,
                            numVideos,
                            numVideos
                        )
            }
        }
        return mediaTitle
    }

    /**
     * Return the recent action item at given position
     *
     * @param position the given position
     * @return the recent action item at position [position], null if [position] is invalid index or item is null
     */
    private fun getItemAtPosition(position: Int): RecentActionItemType? =
        recentActionItems?.getOrNull(position)

    /**
     * ViewHolder for a recent action item
     *
     * @property binding
     */
    inner class RecentActionViewHolder(val binding: ItemBucketBinding) :
        RecyclerView.ViewHolder(binding.root)
}
