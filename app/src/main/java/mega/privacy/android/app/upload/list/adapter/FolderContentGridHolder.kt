package mega.privacy.android.app.upload.list.adapter

import android.net.Uri
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.MimeTypeThumbnail
import mega.privacy.android.app.databinding.ItemFolderContentGridBinding
import mega.privacy.android.app.upload.list.data.FolderContent

class FolderContentGridHolder(
    private val binding: ItemFolderContentGridBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: FolderContent.Data) {
        binding.apply {
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
                    setImageURI(null as Uri?)
                    isVisible = true
                    hierarchy.setPlaceholderImage(MimeTypeThumbnail.typeForName(item.name).iconResourceId)

                    val type = MimeTypeList.typeForName(item.name)
                    if (type.isVideo || type.isImage) {
                        setImageRequest(
                            ImageRequestBuilder.fromRequest(ImageRequest.fromUri(item.uri))
                                .setLocalThumbnailPreviewsEnabled(true)
                                .build()
                        )
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
}