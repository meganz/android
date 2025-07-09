package mega.privacy.android.app.presentation.recentactions.recentactionbucket

import android.content.Context
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.asImage
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import coil3.util.CoilUtils
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.components.dragger.DragThumbnailGetter
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.recentactions.recentactionbucket.RecentActionBucketAdapter.ViewHolderMultipleBucket
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelDrawable
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest.Companion.fromHandle
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.util.stream.Collectors

class RecentActionBucketAdapter(
    private val context: Context,
    private val fragment: Any,
    nodes: List<NodeItem>,
    private val isMedia: Boolean,
    private var isIncomingShare: Boolean,
    diffCallback: RecentActionBucketDiffCallback,
) : ListAdapter<NodeItem?, ViewHolderMultipleBucket?>(diffCallback), View.OnClickListener,
    OnLongClickListener, SectionTitleProvider, DragThumbnailGetter {
    private val outMetrics: DisplayMetrics = context.resources.displayMetrics

    var nodes: List<NodeItem> = nodes
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    // View Holder
    inner class ViewHolderMultipleBucket(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var document: Long = 0
        val multipleBucketLayout: LinearLayout =
            itemView.findViewById(R.id.multiple_bucket_layout)
        val mediaView: RelativeLayout = itemView.findViewById(R.id.media_layout)
        val thumbnailMedia: ImageView = itemView.findViewById(R.id.thumbnail_media)
        val videoLayout: RelativeLayout = itemView.findViewById(R.id.video_layout)
        val videoDuration: TextView = itemView.findViewById(R.id.duration_text)
        val listView: RelativeLayout = itemView.findViewById(R.id.list_layout)
        val thumbnailList: ImageView = itemView.findViewById(R.id.thumbnail_list)
        val nameText: TextView = itemView.findViewById(R.id.name_text)
        val infoText: TextView = itemView.findViewById(R.id.info_text)
        val imgLabel: ImageView = itemView.findViewById(R.id.img_label)
        val imgFavourite: ImageView = itemView.findViewById(R.id.img_favourite)
        val selectedIcon: ImageView = itemView.findViewById(R.id.icon_selected)
        val threeDots: ImageView = itemView.findViewById(R.id.three_dots)
    }

    override fun getNodePosition(handle: Long): Int {
        for (i in nodes.indices) {
            val node = nodes[i]
            if (node.node?.handle == handle) {
                return i
            }
        }

        return Constants.INVALID_POSITION
    }

    override fun getThumbnail(viewHolder: RecyclerView.ViewHolder): ImageView? {
        if (viewHolder is ViewHolderMultipleBucket) {
            return if (isMedia)
                viewHolder.thumbnailMedia
            else
                viewHolder.thumbnailList
        }

        return null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderMultipleBucket {
        Timber.d("onCreateViewHolder")
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_multiple_bucket, parent, false)
        val holder = ViewHolderMultipleBucket(v)

        holder.multipleBucketLayout.tag = holder
        holder.multipleBucketLayout.setOnClickListener(this)
        holder.multipleBucketLayout.setOnLongClickListener(this)
        holder.threeDots.tag = holder
        holder.threeDots.setOnClickListener(this)

        v.tag = holder
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolderMultipleBucket, position: Int) {
        Timber.d("onBindViewHolder")
        val node = getItemAtPosition(position)
        val megaNode = node?.node
        if (megaNode == null) return
        holder.document = megaNode.handle

        if (isMedia) {
            holder.mediaView.visibility = View.VISIBLE
            holder.mediaView.alpha = if (node.isSensitive && !isIncomingShare) 0.5f else 1f
            holder.listView.visibility = View.GONE
            holder.imgLabel.visibility = View.GONE
            holder.imgFavourite.visibility = View.GONE
            holder.selectedIcon.visibility = View.GONE

            if (FileUtil.isAudioOrVideo(megaNode)) {
                holder.videoLayout.visibility = View.VISIBLE
                holder.videoDuration.text = TimeUtils.getVideoDuration(megaNode.duration)
            } else {
                holder.videoLayout.visibility = View.GONE
            }

            holder.thumbnailMedia.visibility = View.VISIBLE
            holder.thumbnailMedia.let {
                CoilUtils.dispose(it)
            }

            var size: Int = if (Util.isScreenInPortrait(context)) {
                outMetrics.widthPixels / 4
            } else {
                outMetrics.widthPixels / 6
            }
            size -= Util.dp2px(2f, outMetrics)

            holder.thumbnailMedia.layoutParams?.width = size
            holder.thumbnailMedia.layoutParams?.height = size

            if (megaNode.hasThumbnail()) {
                val placeholder = ContextCompat.getDrawable(
                    context,
                    typeForName(megaNode.name).iconResourceId
                )?.asImage()
                val imageRequest = ImageRequest.Builder(context)
                    .placeholder(placeholder)
                    .data(fromHandle(megaNode.handle))
                    .target { drawable ->
                        holder
                            .thumbnailMedia
                            .setImageDrawable(
                                drawable.asDrawable(context.resources)
                            )
                    }
                    .transformations(
                        RoundedCornersTransformation(
                            radius = context.resources.getDimension(R.dimen.thumbnail_corner_radius),
                        )
                    )
                    .build()
                SingletonImageLoader.get(context).enqueue(imageRequest)
            } else {
                holder.thumbnailMedia.setImageResource(
                    typeForName(megaNode.name).iconResourceId
                )
            }

            if (node.selected) {
                holder.selectedIcon.visibility = View.VISIBLE
            } else {
                holder.selectedIcon.visibility = View.GONE
            }
        } else {
            holder.mediaView.visibility = View.GONE
            holder.listView.visibility = View.VISIBLE
            holder.listView.alpha = if (node.isSensitive && !isIncomingShare) 0.5f else 1f
            holder.nameText.text = megaNode.name
            holder.infoText.text = buildString {
                append(
                    Util.getSizeString(
                        megaNode.size,
                        context
                    )
                )
                append(" · ")
                append(TimeUtils.formatTime(megaNode.creationTime))
            }

            holder.thumbnailList.visibility = View.VISIBLE
            holder.thumbnailList.let {
                CoilUtils.dispose(it)
            }

            if (megaNode.label != MegaNode.NODE_LBL_UNKNOWN) {
                val drawable =
                    getNodeLabelDrawable(megaNode.label, holder.itemView.resources)
                holder.imgLabel.setImageDrawable(drawable)
                holder.imgLabel.visibility = View.VISIBLE
            } else {
                holder.imgLabel.visibility = View.GONE
            }

            holder.imgFavourite.visibility = if (megaNode.isFavourite) View.VISIBLE else View.GONE

            if (node.selected) {
                holder.thumbnailList.setImageResource(mega.privacy.android.core.R.drawable.ic_select_folder)
            } else {
                holder.thumbnailList.setImageDrawable(null)

                val placeHolderRes = typeForName(node.node?.name).iconResourceId

                if (megaNode.hasThumbnail()) {
                    val placeholder = ContextCompat.getDrawable(
                        context,
                        placeHolderRes
                    )?.asImage()
                    val imageRequest = ImageRequest.Builder(context)
                        .placeholder(placeholder)
                        .data(fromHandle(megaNode.handle))
                        .target { drawable ->
                            holder.thumbnailList.setImageDrawable(drawable.asDrawable(context.resources))
                        }
                        .transformations(
                            RoundedCornersTransformation(
                                radius = context.resources.getDimension(R.dimen.thumbnail_corner_radius),
                            )
                        )
                        .build()
                    SingletonImageLoader.get(context).enqueue(imageRequest)
                } else {
                    val imgResource: Int = if (megaNode.isFolder) {
                        mega.privacy.android.icon.pack.R.drawable.ic_folder_medium_solid
                    } else {
                        placeHolderRes
                    }
                    holder.thumbnailList.setImageResource(imgResource)
                }
            }
        }

        node.uiDirty = false
    }

    private fun getItemAtPosition(pos: Int): NodeItem? = nodes.getOrNull(pos)

    override fun getItemCount(): Int = nodes.size

    fun setIsIncomingShare(isIncomingShare: Boolean) {
        this.isIncomingShare = isIncomingShare
    }

    override fun onClick(v: View) {
        Timber.d("onClick")
        val holder = v.tag as ViewHolderMultipleBucket?
        if (holder == null) return

        val selectedNodes =
            this.nodes
                .stream()
                .filter { it != null && it.selected }
                .collect(Collectors.toList())
        val node = getItemAtPosition(holder.getAbsoluteAdapterPosition())
        if (node == null) return
        val id = v.id
        if (id == R.id.three_dots) {
            if (selectedNodes.isEmpty()) {
                if (!Util.isOnline(context)) {
                    (context as ManagerActivity).showSnackbar(
                        Constants.SNACKBAR_TYPE, context.getString(
                            R.string.error_server_connection_problem
                        ), -1
                    )
                    return
                }
                (context as ManagerActivity).showNodeOptionsPanel(
                    node.node,
                    NodeOptionsBottomSheetDialogFragment.Companion.RECENTS_MODE
                )
            } else {
                if (fragment is RecentActionBucketFragment) {
                    (fragment).handleItemClick(
                        holder.adapterPosition,
                        node,
                        true
                    )
                }
            }
        } else if (id == R.id.multiple_bucket_layout) {
            if (fragment is RecentActionBucketFragment) {
                (fragment).handleItemClick(
                    holder.adapterPosition,
                    node,
                    true
                )
            }
        }
    }

    @Suppress("deprecation")
    override fun onLongClick(v: View): Boolean {
        Timber.d("onClick")
        val holder = v.tag as ViewHolderMultipleBucket?
        if (holder == null) return false

        val node = getItemAtPosition(holder.getAbsoluteAdapterPosition())
        if (node == null) return false

        if (fragment is RecentActionBucketFragment) {
            (fragment).onNodeLongClicked(
                holder.adapterPosition,
                node
            )
        }

        return true
    }

    override fun getSectionTitle(position: Int, context: Context?): String {
        val node = getItemAtPosition(position)
        if (node == null) return ""

        val name = node.node?.name
        if (!name.isNullOrBlank()) return name.substring(0, 1)

        return ""
    }
}
