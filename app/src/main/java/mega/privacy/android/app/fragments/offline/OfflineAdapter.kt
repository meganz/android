package mega.privacy.android.app.fragments.offline

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.OfflineItemGridFileBinding
import mega.privacy.android.app.databinding.OfflineItemGridFolderBinding
import mega.privacy.android.app.databinding.OfflineItemListBinding
import mega.privacy.android.app.utils.Constants.INVALID_POSITION

private const val TYPE_LIST = 1
private const val TYPE_GRID_FOLDER = 2
private const val TYPE_GRID_FILE = 3

class OfflineAdapter(
    private val isList: Boolean,
    private val listener: OfflineAdapterListener
) : RecyclerView.Adapter<OfflineViewHolder>() {
    private val nodes = ArrayList<OfflineNode>()

    fun setNodes(nodes: List<OfflineNode>) {
        this.nodes.clear()
        this.nodes.addAll(nodes)
        notifyDataSetChanged()
    }

    fun getOfflineNodes(): ArrayList<MegaOffline> {
        val offlineNodes = ArrayList<MegaOffline>()
        for (node in nodes) {
            offlineNodes.add(node.node)
        }
        return offlineNodes
    }

    fun getNodePosition(handle: Long): Int {
        for (i in 0 until nodes.size) {
            if (nodes[i].node.handle == handle.toString()) {
                return i
            }
        }

        return INVALID_POSITION
    }

    fun setThumbnailVisibility(holder: ViewHolder, visibility: Int) {
        if (holder is OfflineGridFileViewHolder) {
            holder.getThumbnailView().visibility = visibility
        } else if (holder is OfflineListViewHolder) {
            holder.getThumbnailView().visibility = visibility
        }
    }

    fun getThumbnailLocationOnScreen(holder: ViewHolder): IntArray? {
        var thumbnail: View? = null
        if (holder is OfflineGridFileViewHolder) {
            thumbnail = holder.getThumbnailView()
        } else if (holder is OfflineListViewHolder) {
            thumbnail = holder.getThumbnailView()
        }
        if (thumbnail == null) {
            return null
        }
        val topLeft = IntArray(2)
        thumbnail.getLocationOnScreen(topLeft)
        return intArrayOf(topLeft[0], topLeft[1], thumbnail.width, thumbnail.height)
    }

    fun showSelectionAnimation(
        position: Int,
        node: OfflineNode,
        holder: ViewHolder?
    ) {
        if (holder == null || position < 0 || position >= nodes.size ||
            nodes[position] == OfflineNode.PLACE_HOLDER ||
            nodes[position].node.handle !== node.node.handle
        ) {
            return
        }

        if (node.selected) {
            notifyItemChanged(position)
        }

        when (holder) {
            is OfflineListViewHolder -> {
                showSelectionAnimation(holder.getThumbnailView(), position)
            }
            is OfflineGridFolderViewHolder -> {
                showSelectionAnimation(holder.getThumbnailView(), position)
            }
            is OfflineGridFileViewHolder -> {
                if (node.selected) {
                    holder.getIcSelected().isVisible = true
                }
                showSelectionAnimation(holder.getIcSelected(), position)
            }
        }
    }

    private fun showSelectionAnimation(
        view: View,
        position: Int
    ) {
        val flipAnimation: Animation = AnimationUtils.loadAnimation(
            view.context,
            R.anim.multiselect_flip
        )
        flipAnimation.duration = 200
        flipAnimation.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                notifyItemChanged(position)
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
        view.startAnimation(flipAnimation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfflineViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_GRID_FOLDER -> OfflineGridFolderViewHolder(
                OfflineItemGridFolderBinding.inflate(inflater, parent, false)
            )
            TYPE_GRID_FILE -> OfflineGridFileViewHolder(
                OfflineItemGridFileBinding.inflate(inflater, parent, false)
            )
            else -> OfflineListViewHolder(OfflineItemListBinding.inflate(inflater, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            isList -> TYPE_LIST
            nodes[position] == OfflineNode.PLACE_HOLDER
                    || nodes[position].node.isFolder -> TYPE_GRID_FOLDER
            else -> TYPE_GRID_FILE
        }
    }

    override fun getItemCount(): Int {
        return nodes.size
    }

    override fun onBindViewHolder(holder: OfflineViewHolder, position: Int) {
        holder.bind(position, nodes[position], listener)
    }
}

interface OfflineAdapterListener {
    fun onNodeClicked(position: Int, node: OfflineNode)

    fun onNodeLongClicked(position: Int, node: OfflineNode)

    fun onOptionsClicked(position: Int, node: OfflineNode)
}
