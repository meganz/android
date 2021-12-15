package mega.privacy.android.app.zippreview.ui

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.components.dragger.DragThumbnailGetter
import mega.privacy.android.app.databinding.ItemFileListBinding
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util.dpWidthAbs
import mega.privacy.android.app.utils.getScreenWidth

class ZipListAdapter(private val onItemClick: (zipInfoUIO: ZipInfoUIO, position: Int) -> Unit) :
    ListAdapter<ZipInfoUIO, ZipListViewHolder>(DiffCallback), DragThumbnailGetter {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZipListViewHolder {
        val holder = ZipListViewHolder(
            parent.context, ItemFileListBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
        )
        holder.binding.also {
            it.fileListFilename.layoutParams.height = RelativeLayout.LayoutParams.WRAP_CONTENT
            it.fileListFilename.layoutParams.width =
                getTextFileNameWidth(parent.context as ZipBrowserActivity)
        }
        return holder
    }

    override fun onBindViewHolder(holder: ZipListViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    private fun getTextFileNameWidth(activity: Activity): Int {
        val screenWidth = activity.getScreenWidth()
        val density = activity.resources.displayMetrics.density
        return (WIDTH_FILE_NAME_VIEW * screenWidth / density / dpWidthAbs * density).toInt()
    }

    override fun getNodePosition(handle: Long): Int {
        currentList.indexOfFirst {
            it.zipFileName.split("/").lastOrNull()?.hashCode()?.toLong() == handle
        }
        return Constants.INVALID_POSITION
    }

    override fun getThumbnail(viewHolder: RecyclerView.ViewHolder): View? {
        return if (viewHolder is ZipListViewHolder) {
            viewHolder.binding.fileListThumbnail
        } else {
            null
        }
    }

    companion object {
        const val WIDTH_FILE_NAME_VIEW = 225
    }
}

class ZipListViewHolder(val context: Context, val binding: ItemFileListBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(item: ZipInfoUIO, onItemClick: (zipInfoUIO: ZipInfoUIO, position: Int) -> Unit) {
        binding.apply {
            fileListSavedOffline.visibility = View.INVISIBLE
            fileListPublicLink.visibility = View.INVISIBLE
            fileListThreeDotsLayout.visibility = View.INVISIBLE
            fileListThumbnail.visibility = View.VISIBLE

            fileListFilename.text = item.displayedFileName
            fileListFilesize.text = item.folderInfo
            fileListThumbnail.setImageResource(item.imageResourceId)
            fileListItemLayout.setOnClickListener {
                onItemClick(item, absoluteAdapterPosition)
            }
        }
    }
}

object DiffCallback : DiffUtil.ItemCallback<ZipInfoUIO>() {
    override fun areItemsTheSame(oldItem: ZipInfoUIO, newItem: ZipInfoUIO): Boolean {
        return oldItem.zipFileName == newItem.zipFileName
    }

    override fun areContentsTheSame(oldItem: ZipInfoUIO, newItem: ZipInfoUIO): Boolean {
        return oldItem == newItem
    }
}