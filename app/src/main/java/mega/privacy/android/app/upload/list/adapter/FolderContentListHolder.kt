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
                    setActualImageResource(if (item.isSelected) R.drawable.ic_select_folder else R.drawable.ic_folder_list)
                }
            } else {
                thumbnail.apply {
                    hierarchy.setPlaceholderImage(MimeTypeList.typeForName(item.name).iconResourceId)

                    if (item.isSelected) {
                        setActualImageResource(R.drawable.ic_select_folder)
                    } else {
                        setImageURI(null as Uri?)
                        setImageRequest(
                            ImageRequestBuilder.fromRequest(ImageRequest.fromUri(item.uri))
                                .setLocalThumbnailPreviewsEnabled(true).build()
                        )
                    }
                }
            }

            name.text = item.name
            fileInfo.text = item.info
        }
    }
}