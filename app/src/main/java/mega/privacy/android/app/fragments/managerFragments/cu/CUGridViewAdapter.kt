package mega.privacy.android.app.fragments.managerFragments.cu

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.components.dragger.DragThumbnailGetter
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemCameraUploadsImageBinding
import mega.privacy.android.app.databinding.ItemCameraUploadsTitleBinding
import mega.privacy.android.app.databinding.ItemCameraUploadsVideoBinding
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.data.GalleryItemSizeConfig
import mega.privacy.android.app.utils.Constants

class CUGridViewAdapter(
    private val listener: Listener,
    private var spanCount: Int,
    private var itemSizeConfig: GalleryItemSizeConfig
) : RecyclerView.Adapter<CuGridViewHolder>(), SectionTitleProvider,
    DragThumbnailGetter {

    private val mNodes = mutableListOf<GalleryItem>()

    fun setSpanCount(spanCount: Int) {
        this.spanCount = spanCount
    }

    fun setCuItemSizeConfig(itemSizeConfig: GalleryItemSizeConfig) {
        this.itemSizeConfig = itemSizeConfig
    }


    override fun getNodePosition(handle: Long): Int {
        var i = 0
        while (i < mNodes.size) {
            if (mNodes[i].node?.handle == handle) {
                return i
            }

            i++
        }

        return Constants.INVALID_POSITION
    }

    fun getNodeAtPosition(position: Int) =
        if (position >= 0 && position < mNodes.size) mNodes[position] else null


    override fun getThumbnail(viewHolder: RecyclerView.ViewHolder): View? {
        if (viewHolder is CuImageViewHolder) {
            return viewHolder.binding().thumbnail
        } else if (viewHolder is CuVideoViewHolder) {
            return viewHolder.binding().thumbnail
        }

        return null
    }

    override fun getItemId(position: Int) = if (getItemViewType(position) == GalleryItem.TYPE_HEADER) {
        mNodes[position].modifyDate.hashCode().toLong()
    } else {
        // Type isn't header, node object is non-null.
        mNodes[position].node!!.handle
    }

    override fun getItemViewType(position: Int) = mNodes[position].type


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CuGridViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            GalleryItem.TYPE_HEADER -> CuTitleViewHolder(
                ItemCameraUploadsTitleBinding.inflate(inflater, parent, false)
            )
            GalleryItem.TYPE_VIDEO -> CuVideoViewHolder(
                ItemCameraUploadsVideoBinding.inflate(inflater, parent, false),
                itemSizeConfig
            )
            GalleryItem.TYPE_IMAGE -> CuImageViewHolder(
                ItemCameraUploadsImageBinding.inflate(inflater, parent, false),
                itemSizeConfig
            )
            else -> CuImageViewHolder(
                ItemCameraUploadsImageBinding.inflate(inflater, parent, false),
                itemSizeConfig
            )
        }
    }

    override fun onBindViewHolder(holder: CuGridViewHolder, position: Int) {
        holder.bind(position, mNodes[position], listener)
    }

    override fun getItemCount() = mNodes.size

    @SuppressLint("NotifyDataSetChanged")
    fun setNodes(nodes: List<GalleryItem>) {
        mNodes.clear()
        mNodes.addAll(nodes)
        notifyDataSetChanged()
    }

    fun getSpanSize(position: Int) = if (position < 0 || position >= mNodes.size) {
        1
    } else {
        if (mNodes[position].type == GalleryItem.TYPE_HEADER) {
            spanCount
        } else {
            1
        }
    }

    fun showSelectionAnimation(position: Int, node: GalleryItem, holder: RecyclerView.ViewHolder?) {
        if (holder == null || position < 0 || position >= mNodes.size || mNodes[position].node == null || mNodes[position].node?.handle != node.node?.handle) {
            return
        }

        when (holder) {
            is CuImageViewHolder -> {
                showSelectionAnimation(
                    holder.binding().icSelected, position,
                    node.selected
                )
            }

            is CuVideoViewHolder -> {
                showSelectionAnimation(
                    holder.binding().icSelected, position,
                    node.selected
                )
            }
        }
    }

    private fun showSelectionAnimation(view: View, position: Int, showing: Boolean) {
        if (showing) {
            view.visibility = View.VISIBLE
        }

        val flipAnimation = AnimationUtils.loadAnimation(
            view.context,
            R.anim.multiselect_flip
        ).apply {
            duration = 200
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}
                override fun onAnimationEnd(animation: Animation) {
                    if (!showing) {
                        view.visibility = View.GONE
                    }
                    notifyItemChanged(position)
                }

                override fun onAnimationRepeat(animation: Animation) {}
            })
        }

        view.startAnimation(flipAnimation)
    }

    override fun getSectionTitle(position: Int) = if (position < 0 || position >= mNodes.size) {
        ""
    } else {
        mNodes[position].modifyDate
    }

    interface Listener {
        fun onNodeClicked(position: Int, node: GalleryItem?)
        fun onNodeLongClicked(position: Int, node: GalleryItem?)
    }
}