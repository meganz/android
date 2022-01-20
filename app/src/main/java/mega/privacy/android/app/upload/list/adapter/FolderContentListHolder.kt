package mega.privacy.android.app.upload.list.adapter

import android.net.Uri
import androidx.recyclerview.widget.RecyclerView
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemFolderContentBinding
import mega.privacy.android.app.upload.list.data.FolderContent

class FolderContentListHolder(
    private val binding: ItemFolderContentBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: FolderContent.Data) {
        binding.apply {
            if (item.isFolder) {
                thumbnail.apply {
                    hierarchy.setPlaceholderImage(R.drawable.ic_folder_list)
                    setImageURI(null as Uri?)
                }
            } else {
                thumbnail.apply {
                    setImageURI(null as Uri?)
                    hierarchy.setPlaceholderImage(MimeTypeList.typeForName(item.name).iconResourceId)
                    setImageRequest(
                        ImageRequestBuilder.fromRequest(ImageRequest.fromUri(item.uri))
                            .setLocalThumbnailPreviewsEnabled(true).build()
                    )
                }
            }

            name.text = item.name
            fileInfo.text = item.info
        }
    }
}