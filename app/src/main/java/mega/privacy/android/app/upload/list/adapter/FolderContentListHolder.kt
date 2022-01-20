package mega.privacy.android.app.upload.list.adapter

import android.net.Uri
import androidx.recyclerview.widget.RecyclerView
import com.facebook.imagepipeline.request.ImageRequest
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
                thumbnail.hierarchy.setPlaceholderImage(R.drawable.ic_folder_list)
                thumbnail.setImageURI(null as Uri?)
            } else {
                thumbnail.hierarchy.setPlaceholderImage(MimeTypeList.typeForName(item.name).iconResourceId)
                thumbnail.setImageRequest(ImageRequest.fromUri(item.uri))
            }

            name.text = item.name
            fileInfo.text = item.info
        }
    }
}