package mega.privacy.android.app.presentation.recentactions

import android.content.Context
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.components.dragger.DragThumbnailGetter
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemBucketBinding
import mega.privacy.android.app.fragments.homepage.main.HomepageFragment
import mega.privacy.android.app.fragments.homepage.main.HomepageFragmentDirections.Companion.actionHomepageToRecentBucket
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.recentactions.RecentActionsAdapter.RecentActionViewHolder
import mega.privacy.android.app.presentation.recentactions.model.RecentActionItemType
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelDrawable
import mega.privacy.android.app.utils.MegaNodeUtil.getOutgoingOrIncomingParent
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import timber.log.Timber

/**
 * Adapter to display a list of recent actions
 *
 * @property context
 * @property fragment
 */
class RecentActionsAdapter(private val context: Context, private val fragment: Fragment) :
    RecyclerView.Adapter<RecentActionViewHolder>(), SectionTitleProvider,
    DragThumbnailGetter {

    private val megaApi: MegaApiAndroid = getInstance().megaApi
    private val outMetrics: DisplayMetrics = context.resources.displayMetrics

    private var recentActionItems: List<RecentActionItemType>? = null

    /**
     * The Homepage bottom sheet has a calculated background for elevation, while the
     * Recent fragment UI is transparent. This function is for calculating
     * the same background color as bottomSheet for the sticky "header"
     *
     * @return the header's background color value
     */
    private val headerColor: Int by lazy {
        val elevationPx = Util.dp2px(HomepageFragment.BOTTOM_SHEET_ELEVATION,
            context.resources.displayMetrics)
        getColorForElevation(context, elevationPx.toFloat())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentActionViewHolder {
        Timber.d("onCreateViewHolder")
        val binding = ItemBucketBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecentActionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentActionViewHolder, position: Int) = with(holder) {
        Timber.d("Position: %s", position)
        val item = getItemAtPosition(position) ?: return

        when (item) {
            is RecentActionItemType.Header -> {
                Timber.d("onBindViewHolder: TYPE_HEADER")
                binding.itemBucketLayout.visibility = View.GONE
                binding.headerLayout.visibility = View.VISIBLE
                binding.headerLayout.setBackgroundColor(headerColor)
                binding.headerText.text = TimeUtils.formatBucketDate(context, item.timestamp)
            }

            is RecentActionItemType.Item -> {
                Timber.d("onBindViewHolder: TYPE_BUCKET")
                binding.itemBucketLayout.visibility = View.VISIBLE
                binding.itemBucketLayout.setOnClickListener {
                    // If only one element in the bucket
                    if (item.bucket.nodes.size() == 1) {
                        (fragment as RecentActionsFragment).openFile(
                            holder.bindingAdapterPosition,
                            item.bucket.nodes[0])
                    }
                    // If more element in the bucket
                    else {
                        (fragment as RecentActionsFragment).viewModel.select(item.bucket)

                        val currentDestination = findNavController(it).currentDestination
                        if (currentDestination != null && currentDestination.id == R.id.homepageFragment) {
                            findNavController(it).navigate(actionHomepageToRecentBucket(),
                                NavOptions.Builder().build())
                        }
                    }
                }
                binding.headerLayout.visibility = View.GONE

                val bucket = item.bucket
                val nodeList = bucket.nodes
                if (nodeList == null || nodeList.size() == 0) return
                val node = nodeList[0] ?: return
                val parentNode: MegaNode =
                    megaApi.getNodeByHandle(bucket.parentHandle) ?: return
                binding.nameText.text =
                    if (parentNode.name == "Cloud Drive") {
                        context.getString(R.string.section_cloud_drive)
                    } else {
                        parentNode.name ?: ""
                    }

                if (bucket.userEmail == megaApi.myEmail) {
                    binding.secondLineText.visibility = View.GONE
                } else {
                    val userAction = if (bucket.isUpdate) {
                        context.getString(R.string.update_action_bucket, item.userName)
                    } else {
                        context.getString(R.string.create_action_bucket, item.userName)
                    }
                    binding.secondLineText.visibility = View.VISIBLE
                    binding.secondLineText.text = formatUserAction(userAction)
                }
                val parentRootNode = getOutgoingOrIncomingParent(parentNode)
                when {
                    parentRootNode == null -> {
                        binding.sharedImage.visibility = View.GONE
                    }

                    parentRootNode.isInShare -> {
                        binding.sharedImage.visibility = View.VISIBLE
                        binding.sharedImage.setImageResource(R.drawable.ic_folder_incoming_list)
                    }

                    parentRootNode.isOutShare || megaApi.isPendingShare(parentRootNode) -> {
                        binding.sharedImage.visibility = View.VISIBLE
                        binding.sharedImage.setImageResource(R.drawable.ic_folder_outgoing_list)
                    }
                }

                binding.timeText.text = TimeUtils.formatTime(item.timestamp)
                binding.thumbnailView.visibility = View.VISIBLE

                val params = binding.thumbnailView.layoutParams as RelativeLayout.LayoutParams
                params.height = Util.dp2px(48f, outMetrics)
                params.width = params.height
                val margin = Util.dp2px(12f, outMetrics)
                params.setMargins(margin, margin, margin, 0)
                binding.thumbnailView.layoutParams = params
                binding.thumbnailView.setImageResource(MimeTypeList.typeForName(node.name).iconResourceId)

                // only one item in the recent action
                if (nodeList.size() == 1) {
                    binding.threeDots.visibility = View.VISIBLE
                    binding.threeDots.setOnClickListener {
                        if (!Util.isOnline(context)) {
                            (context as ManagerActivity).showSnackbar(Constants.SNACKBAR_TYPE,
                                context.getString(
                                    R.string.error_server_connection_problem),
                                -1)
                        } else {
                            (context as ManagerActivity).showNodeOptionsPanel(node,
                                NodeOptionsBottomSheetDialogFragment.RECENTS_MODE)
                        }
                    }
                    binding.firstLineText.text = node.name
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
                        binding.firstLineText.text = getMediaTitle(nodeList)
                        binding.thumbnailView.setImageResource(R.drawable.media)
                    } else {
                        binding.firstLineText.text = context.getString(R.string.title_bucket,
                            node.name,
                            nodeList.size() - 1)
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
                if (
                    item.bucket.nodes != null
                    && item.bucket.nodes.size() > 0
                    && item.bucket.nodes.get(0).handle == handle
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

    override fun getSectionTitle(position: Int): String {
        return if (recentActionItems.isNullOrEmpty() || position < 0 || position >= itemCount)
            ""
        else TimeUtils.formatBucketDate(context, recentActionItems!![position].timestamp)
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
     * Format the user action with decoration
     *
     * @param userAction the string to decorate
     * @return spanned string with decoration
     */
    private fun formatUserAction(userAction: String): Spanned {
        val formattedUserAction = try {
            userAction
                .replace("[A]",
                    "<font color=\'" + getColorHexString(context,
                        R.color.grey_300_grey_600) + "\'>")
                .replace("[/A]", "</font>")
        } catch (e: Exception) {
            Timber.e(e, "Exception formatting string")
            ""
        }
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            Html.fromHtml(formattedUserAction)
        } else
            Html.fromHtml(formattedUserAction, Html.FROM_HTML_MODE_LEGACY)
    }

    /**
     * Format the title in case there are multiple nodes in one recent action item
     *
     * @param nodeList list of nodes contained in the recent action item
     * @return a string corresponding to the title
     */
    private fun getMediaTitle(nodeList: MegaNodeList): String {
        val partition = (0 until nodeList.size())
            .map { nodeList[it] }
            .partition { MimeTypeList.typeForName(it.name).isImage }
        val numImages = partition.first.size
        val numVideos = partition.second.size

        val mediaTitle = when {
            numImages > 0 && numVideos == 0 -> {
                context.resources.getQuantityString(R.plurals.title_media_bucket_only_images,
                    numImages,
                    numImages)
            }
            numImages == 0 && numVideos > 0 -> {
                context.resources.getQuantityString(R.plurals.title_media_bucket_only_videos,
                    numVideos,
                    numVideos)
            }
            else -> {
                context.resources.getQuantityString(R.plurals.title_media_bucket_images_and_videos,
                    numImages,
                    numImages) +
                        context.resources.getQuantityString(R.plurals.title_media_bucket_images_and_videos_2,
                            numVideos,
                            numVideos)
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