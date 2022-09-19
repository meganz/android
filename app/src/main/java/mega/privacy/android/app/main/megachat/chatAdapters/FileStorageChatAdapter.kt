package mega.privacy.android.app.main.megachat.chatAdapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemFileStorageCameraBinding
import mega.privacy.android.app.databinding.ItemFileStorageImageBinding
import mega.privacy.android.domain.entity.chat.FileGalleryItem
import mega.privacy.android.app.utils.AdapterUtils.isValidPosition

/**
 * RecyclerView's ListAdapter to show FileGalleryItem.
 *
 * @property onTakePictureCallback     Callback to be called when take picture button is clicked
 * @property onClickItemCallback       Callback to be called when a item is clicked
 */
class FileStorageChatAdapter(
    private val onTakePictureCallback: () -> Unit,
    private val onClickItemCallback: (FileGalleryItem) -> Unit,
    private val onLongClickItemCallback: (FileGalleryItem) -> Unit,
    private val customLifecycle: LifecycleOwner,
) : ListAdapter<FileGalleryItem, RecyclerView.ViewHolder>(FileGalleryItemDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_CAMERA = 0
        private const val VIEW_TYPE_IMAGE = 1
    }

    init {
        setHasStableIds(true)
    }

    /**
     * Method to create view holder
     *
     * @param parent ViewGroup
     * @param viewType Type of View
     *
     * @return Holder FileStorageChatHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_CAMERA -> {
                val binding = ItemFileStorageCameraBinding.inflate(layoutInflater, parent, false)
                FileStorageChatCameraViewHolder(binding).apply {
                    binding.lifecycleOwner = customLifecycle
                    binding.root.setOnClickListener {
                        if (isValidPosition(bindingAdapterPosition)) {
                            onTakePictureCallback.invoke()
                        }
                    }
                    binding.takePictureButton.setOnClickListener {
                        if (isValidPosition(bindingAdapterPosition)) {
                            onTakePictureCallback.invoke()
                        }
                    }
                }
            }
            else -> {
                val binding = ItemFileStorageImageBinding.inflate(layoutInflater, parent, false)
                FileStorageChatImageViewHolder(binding).apply {
                    binding.root.setOnLongClickListener {
                        if (isValidPosition(bindingAdapterPosition)) {
                            val file = (getItem(bindingAdapterPosition) as FileGalleryItem)
                            onLongClickItemCallback.invoke(file)
                        }
                        true
                    }
                    binding.root.setOnClickListener {
                        if (isValidPosition(bindingAdapterPosition)) {
                            val file = (getItem(bindingAdapterPosition) as FileGalleryItem)
                            onClickItemCallback.invoke(file)
                        }
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int =
        when (position) {
            0 -> VIEW_TYPE_CAMERA
            else -> VIEW_TYPE_IMAGE
        }

    /**
     * Bind view for the File storage chat toolbar.
     *
     * @param holder FileStorageChatHolder
     * @param position Item position
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (getItemViewType(position)) {
            VIEW_TYPE_CAMERA -> (holder as FileStorageChatCameraViewHolder).bind(item.hasCameraPermissions
                ?: false)
            else -> (holder as FileStorageChatImageViewHolder).bind(item)
        }
    }

    /**
     * Get the Id of the item
     *
     * @param position Item position
     * @return Id of item
     */
    override fun getItemId(position: Int): Long =
        getItem(position).id

    class FileGalleryItemDiffCallback : DiffUtil.ItemCallback<FileGalleryItem>() {

        override fun areItemsTheSame(oldItem: FileGalleryItem, newItem: FileGalleryItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: FileGalleryItem,
            newItem: FileGalleryItem,
        ): Boolean =
            oldItem == newItem
    }
}
