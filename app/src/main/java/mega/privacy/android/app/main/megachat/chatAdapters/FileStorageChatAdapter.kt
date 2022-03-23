package mega.privacy.android.app.main.megachat.chatAdapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemFileStorageBinding

class FileStorageChatAdapter (
    var context: Context
) :
    RecyclerView.Adapter<FileStorageChatHolder>(), View.OnClickListener {
    private var files: List<String>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileStorageChatHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemFileStorageBinding.inflate(inflater, parent, false)
        return FileStorageChatHolder(binding)
    }

    override fun getItemCount(): Int {
        return if (files.isNullOrEmpty())
            0
        else
            files!!.size
    }

    override fun onBindViewHolder(holder: FileStorageChatHolder, position: Int) {
        files?.get(position)?.let { holder.bind(context, it) }
        holder.itemView.setOnClickListener(this@FileStorageChatAdapter)
    }

    override fun onClick(v: View?) {
    }
}