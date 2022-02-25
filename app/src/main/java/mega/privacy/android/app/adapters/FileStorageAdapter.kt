package mega.privacy.android.app.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.adapters.viewHolders.FileStorageHolder
import mega.privacy.android.app.databinding.ItemFileStorageBinding

class FileStorageAdapter (
    var context: Context
) :
    RecyclerView.Adapter<FileStorageHolder>(), View.OnClickListener {
    private var files: List<String>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileStorageHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemFileStorageBinding.inflate(inflater, parent, false)
        return FileStorageHolder(binding)
    }

    override fun getItemCount(): Int {
        return if (files.isNullOrEmpty())
            0
        else
            files!!.size
    }

    override fun onBindViewHolder(holder: FileStorageHolder, position: Int) {
        files?.get(position)?.let { holder.bind(context, it) }
        holder.itemView.setOnClickListener(this@FileStorageAdapter)
    }

    fun setNodes(files: List<String>?) {
        this.files = files
        notifyDataSetChanged()
    }

    override fun onClick(v: View?) {
    }
}
