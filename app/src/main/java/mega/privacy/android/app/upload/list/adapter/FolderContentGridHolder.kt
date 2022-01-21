package mega.privacy.android.app.upload.list.adapter

import android.graphics.drawable.Animatable
import android.net.Uri
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco.*
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.MimeTypeThumbnail
import mega.privacy.android.app.databinding.ItemFolderContentGridBinding
import mega.privacy.android.app.listeners.OptionalRequestListener
import mega.privacy.android.app.upload.list.data.FolderContent
import mega.privacy.android.app.utils.Util

class FolderContentGridHolder(
    private val binding: ItemFolderContentGridBinding
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        private const val THUMBNAIL_PADDING = 1F
        private const val PLACEHOLDER_HORIZONTAL_PADDING = 39F
        private const val PLACEHOLDER_TOP_PADDING = 22F
        private const val PLACEHOLDER_BOTTOM_PADDING = 21F
    }

    private val thumbnailPadding by lazy { Util.dp2px(THUMBNAIL_PADDING) }
    private val placeHolderHorizontalPadding by lazy { Util.dp2px(PLACEHOLDER_HORIZONTAL_PADDING) }
    private val placeHolderTopPadding by lazy { Util.dp2px(PLACEHOLDER_TOP_PADDING) }
    private val placeHolderBottomPadding by lazy { Util.dp2px(PLACEHOLDER_BOTTOM_PADDING) }

    fun bind(item: FolderContent.Data) {
        binding.apply {
            videoView.isVisible = false
            videoIcon.isVisible = false

            if (item.isFolder) {
                folderThumbnail.isVisible = true
                folderName.apply {
                    isVisible = true
                    text = item.name
                }

                fileThumbnail.isVisible = false
                thumbnailSeparator.isVisible = false
                fileName.isVisible = false
            } else {
                folderThumbnail.isVisible = false
                folderName.isVisible = false
                fileThumbnail.apply {
                    setPadding(
                        placeHolderHorizontalPadding,
                        placeHolderTopPadding,
                        placeHolderHorizontalPadding,
                        placeHolderBottomPadding
                    )

                    setImageURI(null as Uri?)
                    isVisible = true
                    hierarchy.setPlaceholderImage(MimeTypeThumbnail.typeForName(item.name).iconResourceId)

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

    private fun ItemFolderContentGridBinding.showVideo(type: MimeTypeList) {
        fileThumbnail.apply {
            setPadding(thumbnailPadding, thumbnailPadding, thumbnailPadding, 0)

            if (type.isVideo) {
                videoView.isVisible = true
                videoIcon.isVisible = true
            }
        }
    }
}