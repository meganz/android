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

/**
 * Zip list adapter
 * @param onItemClick item click event listener
 */
class ZipListAdapter(private val onItemClick: (zipInfoUIO: ZipInfoUIO, position: Int) -> Unit) :
    ListAdapter<ZipInfoUIO, ZipListViewHolder>(DiffCallback), DragThumbnailGetter {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZipListViewHolder {
        val holder = ZipListViewHolder(
            parent.context, ItemFileListBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
        )
        with(holder.binding) {
            fileListFilename.layoutParams.height = RelativeLayout.LayoutParams.WRAP_CONTENT
            fileListFilename.layoutParams.width =
                getTextFileNameWidth(parent.context as ZipBrowserActivity)
        }
        return holder
    }

    override fun onBindViewHolder(holder: ZipListViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    /**
     * legacy logic to get the width of filename textview.
     * @param activity Activity
     * @return width of filename textview
     */
    private fun getTextFileNameWidth(activity: Activity): Int {
        val screenWidth = activity.getScreenWidth()
        val density = activity.resources.displayMetrics.density
        return (WIDTH_FILE_NAME_VIEW * screenWidth / density / dpWidthAbs * density).toInt()
    }

    override fun getNodePosition(handle: Long): Int {
        currentList.indexOfFirst {
            it.path.split("/").lastOrNull()?.hashCode()?.toLong() == handle
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

/**
 * ViewHolder of zip list adapter
 * @param context Context
 * @param binding dataBinding
 */
class ZipListViewHolder(val context: Context, val binding: ItemFileListBinding) :
    RecyclerView.ViewHolder(binding.root) {
    /**
     * Bind the data with view
     * @param item current ZipInfoUIO
     * @param onItemClick item click event listener
     */
    fun bind(item: ZipInfoUIO, onItemClick: (zipInfoUIO: ZipInfoUIO, position: Int) -> Unit) {
        binding.apply {
            fileListSavedOffline.visibility = View.INVISIBLE
            fileListPublicLink.visibility = View.INVISIBLE
            fileListThreeDotsLayout.visibility = View.INVISIBLE
            fileListThumbnail.visibility = View.VISIBLE

            fileListFilename.text = item.name
            fileListFilesize.text = item.folderInfo
            fileListThumbnail.setImageResource(item.imageResourceId)
            fileListItemLayout.setOnClickListener {
                onItemClick(item, absoluteAdapterPosition)
            }
        }
    }
}

/**
 * DiffCallback of zip list adapter
 */
object DiffCallback : DiffUtil.ItemCallback<ZipInfoUIO>() {
    override fun areItemsTheSame(oldItem: ZipInfoUIO, newItem: ZipInfoUIO): Boolean {
        return oldItem.path == newItem.path
    }

    override fun areContentsTheSame(oldItem: ZipInfoUIO, newItem: ZipInfoUIO): Boolean {
        return oldItem == newItem
    }
}