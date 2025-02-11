package mega.privacy.android.app.uploadFolder.list.adapter

import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.util.CoilUtils
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.MimeTypeThumbnail
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemFolderContentGridBinding
import mega.privacy.android.app.uploadFolder.list.data.FolderContent
import mega.privacy.android.app.utils.Util.dp2px

/**
 * RecyclerView's ViewHolder to show FolderContent Data info in a grid view.
 *
 * @property binding    Item's view binding
 */
class FolderContentGridHolder(
    private val binding: ItemFolderContentGridBinding,
) : RecyclerView.ViewHolder(binding.root) {
    private val screenWidth = binding.root.resources.displayMetrics.widthPixels

    fun bind(item: FolderContent.Data) {
        binding.apply {
            videoView.isVisible = false
            videoIcon.isVisible = false

            root.setBackgroundResource(if (item.isSelected) R.drawable.background_item_grid_selected else R.drawable.background_item_grid)
            selectedIcon.isVisible = item.isSelected

            CoilUtils.dispose(fileThumbnail)
            if (item.isFolder) {
                folderThumbnail.visibility = if (item.isSelected) INVISIBLE else VISIBLE

                folderName.apply {
                    isVisible = true
                    text = item.name
                }

                fileIcon.isVisible = false
                fileThumbnail.isVisible = false
                thumbnailSeparator.isVisible = false
                fileName.isVisible = false
            } else {
                folderThumbnail.isVisible = false
                folderName.isVisible = false
                fileIcon.apply {
                    isVisible = true
                    setImageResource(MimeTypeThumbnail.typeForName(item.name).iconResourceId)
                }
                fileThumbnail.apply {
                    isVisible = true

                    val type = MimeTypeList.typeForName(item.name)
                    if (type.isVideo || type.isImage) {
                        load(item.uri) {
                            size(screenWidth / 2, height = dp2px(172f))
                            listener(
                                onSuccess = { _, _ -> showVideo(type) },
                                onError = { _, _ -> showVideo(type) }
                            )
                        }
                    }
                }

                thumbnailSeparator.isVisible = true
                fileName.apply {
                    isVisible = true
                    text = item.name
                }
            }
        }
    }

    /**
     * If the item is a video, then shows the video items.
     *
     * @param type MimeTypeList object to check if the item is a video.
     */
    private fun ItemFolderContentGridBinding.showVideo(type: MimeTypeList) {
        if (type.isVideo) {
            fileThumbnail.apply {
                videoView.isVisible = true
                videoIcon.isVisible = true
            }
        }
    }
}