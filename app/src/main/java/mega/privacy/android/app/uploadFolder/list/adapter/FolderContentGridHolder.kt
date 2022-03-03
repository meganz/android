package mega.privacy.android.app.uploadFolder.list.adapter

import android.graphics.drawable.Animatable
import android.net.Uri
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco.*
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.MimeTypeThumbnail
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemFolderContentGridBinding
import mega.privacy.android.app.listeners.OptionalRequestListener
import mega.privacy.android.app.uploadFolder.list.data.FolderContent

/**
 * RecyclerView's ViewHolder to show FolderContent Data info in a grid view.
 *
 * @property binding    Item's view binding
 */
class FolderContentGridHolder(
    private val binding: ItemFolderContentGridBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: FolderContent.Data) {
        binding.apply {
            videoView.isVisible = false
            videoIcon.isVisible = false

            root.setBackgroundResource(if (item.isSelected) R.drawable.background_item_grid_selected else R.drawable.background_item_grid)
            selectedIcon.isVisible = item.isSelected

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
                    setImageURI(null as Uri?)
                    isVisible = true

                    val type = MimeTypeList.typeForName(item.name)
                    if (type.isVideo || type.isImage) {
                        controller = newDraweeControllerBuilder()
                            .setImageRequest(
                                ImageRequestBuilder.fromRequest(ImageRequest.fromUri(item.uri))
                                    .setLocalThumbnailPreviewsEnabled(true)
                                    .setRequestListener(OptionalRequestListener(
                                        onProducerFinishWithSuccess = { _, _, _ ->
                                            showVideo(type)
                                        },
                                        onRequestSuccess = { _, _, _ -> showVideo(type) }
                                    ))
                                    .build()
                            )
                            .setControllerListener(object : BaseControllerListener<ImageInfo?>() {
                                override fun onIntermediateImageSet(
                                    id: String?,
                                    imageInfo: ImageInfo?
                                ) {
                                    showVideo(type)
                                }

                                override fun onFinalImageSet(
                                    id: String,
                                    imageInfo: ImageInfo?,
                                    animatable: Animatable?
                                ) {
                                    showVideo(type)
                                }
                            })
                            .build()
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