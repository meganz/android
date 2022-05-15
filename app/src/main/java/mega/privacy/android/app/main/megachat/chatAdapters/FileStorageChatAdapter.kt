package mega.privacy.android.app.main.megachat.chatAdapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.databinding.ItemFileStorageBinding
import mega.privacy.android.app.main.megachat.data.FileGalleryItem
import mega.privacy.android.app.utils.AdapterUtils.isValidPosition
import mega.privacy.android.app.utils.LogUtil.logDebug

/**
 * RecyclerView's ListAdapter to show FileGalleryItem.
 *
 * @property onTakePictureCallback     Callback to be called when take picture button is clicked
 * @property onClickItemCallback       Callback to be called when a item is clicked
 */
class FileStorageChatAdapter(
        private val onTakePictureCallback: () -> Unit,
        private val onClickItemCallback: () -> Unit
) : ListAdapter<FileGalleryItem, FileStorageChatHolder>(FileGalleryItem.DiffCallback()) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileStorageChatHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemFileStorageBinding.inflate(layoutInflater, parent, false)
        return FileStorageChatHolder(binding).apply {
            binding.root.setOnClickListener {
                if (isValidPosition(bindingAdapterPosition)) {
                    onClickItemCallback.invoke()
                }
            }
            binding.takePictureButton.setOnClickListener {
                if (isValidPosition(bindingAdapterPosition)) {
                    onTakePictureCallback.invoke()
                }
            }
        }
    }

    private var files: List<String>? = null

    override fun onBindViewHolder(holder: FileStorageChatHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    override fun getItemId(position: Int): Long =
            getItem(position).id

}