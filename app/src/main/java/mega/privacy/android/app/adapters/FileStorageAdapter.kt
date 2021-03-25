package mega.privacy.android.app.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.FileGalleryInfo
import mega.privacy.android.app.adapters.viewHolders.FileStorageHolder
import mega.privacy.android.app.databinding.ItemFileStorageBinding
import mega.privacy.android.app.lollipop.megachat.FileGalleryItem
import mega.privacy.android.app.utils.LogUtil.logDebug

class FileStorageAdapter (
    var context: Context,
    private var files: ArrayList<FileGalleryItem>?,
) :
    RecyclerView.Adapter<FileStorageHolder>(), View.OnClickListener {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileStorageHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemFileStorageBinding.inflate(inflater, parent, false)
        return FileStorageHolder(binding)
    }

    override fun getItemCount(): Int {
        return files!!.size
    }

    override fun onBindViewHolder(holder: FileStorageHolder, position: Int) {
        holder.bind(context, files!![position], false)
        holder.itemView.setOnClickListener(this@FileStorageAdapter)
    }

    fun setNodes(files: ArrayList<FileGalleryItem>?) {
        this.files = files
        notifyDataSetChanged()
    }

    override fun onClick(v: View?) {
        val holder = v?.tag as FileStorageHolder
        //(context as TimeZonePickerActivity).sendTimeZone(holder.timezone)
    }
}
