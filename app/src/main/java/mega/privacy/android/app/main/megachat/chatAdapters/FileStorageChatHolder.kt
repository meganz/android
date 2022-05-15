package mega.privacy.android.app.main.megachat.chatAdapters

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.databinding.ItemFileStorageBinding
import mega.privacy.android.app.fragments.homepage.getRoundingParams
import mega.privacy.android.app.fragments.homepage.getRoundingParamsWithoutBorder
import mega.privacy.android.app.main.megachat.data.FileGalleryItem
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.setImageRequestFromUri

/**
 * RecyclerView's ViewHolder to show FileGalleryItem.
 *
 * @property binding    Item's view binding
 */
class FileStorageChatHolder(
        private val binding: ItemFileStorageBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: FileGalleryItem, position: Int) {
        binding.apply {
            takePictureButton.isVisible = position == 0
            icSelected.isVisible = item.isSelected

            if(item.isImage) {
                imageThumbnail.setImageRequestFromUri(item.fileUri)
                imageThumbnail.isVisible = true
                imageThumbnail.hierarchy.roundingParams = getRoundingParamsWithoutBorder(MegaApplication.getInstance().applicationContext)

                videoDuration.isVisible = false
                videoThumbnail.isVisible = false
            } else {
                videoThumbnail.setImageRequestFromUri(item.fileUri)
                videoThumbnail.isVisible = true
                videoThumbnail.hierarchy.roundingParams = getRoundingParamsWithoutBorder(MegaApplication.getInstance().applicationContext)
                videoDuration.isVisible = true
                videoDuration.text = item.duration
                imageThumbnail.isVisible = false
            }
        }
    }
}