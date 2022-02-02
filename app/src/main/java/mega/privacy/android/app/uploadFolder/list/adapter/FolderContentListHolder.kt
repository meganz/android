package mega.privacy.android.app.uploadFolder.list.adapter

import android.net.Uri
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemFolderContentBinding
import mega.privacy.android.app.uploadFolder.list.data.FolderContent

/**
 * RecyclerView's ViewHolder to show FolderContent Data info in a list view.
 *
 * @property binding    Item's view binding
 */
class FolderContentListHolder(
    private val binding: ItemFolderContentBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: FolderContent.Data) {
        binding.apply {
            val isSelected = item.isSelected
            selectedIcon.isVisible = isSelected

            thumbnail.apply {
                isVisible = !isSelected

                if (!isSelected) {
                    if (item.isFolder) {
                        hierarchy.setPlaceholderImage(R.drawable.ic_folder_list)
                        setActualImageResource(R.drawable.ic_folder_list)
                    } else {
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
            }

            name.text = item.name
            fileInfo.text = item.info
        }
    }
}