package mega.privacy.android.app.main.megachat.chatAdapters


import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemFileStorageImageBinding
import mega.privacy.android.app.fragments.homepage.getRoundingParamsWithoutBorder
import mega.privacy.android.app.main.megachat.data.FileGalleryItem
import mega.privacy.android.app.utils.setImageRequestFromUri

/**
 * RecyclerView's ViewHolder to show FileGalleryItem.
 *
 * @property binding    Item's view binding
 */
class FileStorageChatImageViewHolder(
    private val binding: ItemFileStorageImageBinding
) : RecyclerView.ViewHolder(binding.root) {

    /**
     * Bind view for the File storage chat toolbar.
     *
     * @param item FileGalleryItem
     */
    fun bind(item: FileGalleryItem) {
        binding.apply {
            icSelected.isVisible = item.isSelected
            if (item.isImage) {
                imageThumbnail.setImageRequestFromUri(item.fileUri)
                imageThumbnail.isVisible = true
                imageThumbnail.hierarchy.roundingParams = getRoundingParamsWithoutBorder(root.context)
                videoDuration.isVisible = false
                videoThumbnail.isVisible = false
            } else {
                videoThumbnail.setImageRequestFromUri(item.fileUri)
                videoThumbnail.isVisible = true
                videoThumbnail.hierarchy.roundingParams = getRoundingParamsWithoutBorder(root.context)
                videoDuration.isVisible = true
                videoDuration.text = item.duration
                imageThumbnail.isVisible = false
            }
        }
    }
}
