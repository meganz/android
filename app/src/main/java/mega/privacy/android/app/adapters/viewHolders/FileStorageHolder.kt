package mega.privacy.android.app.adapters.viewHolders

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.facebook.common.util.UriUtil
import mega.privacy.android.app.databinding.ItemFileStorageBinding
import mega.privacy.android.app.fragments.homepage.getRoundingParams
import mega.privacy.android.app.lollipop.megachat.FileGalleryItem

class FileStorageHolder(private val binding: ItemFileStorageBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(context: Context, item: FileGalleryItem, firstItem : Boolean) {
        binding.apply {
            fileStorageThumbnail.setImageURI(UriUtil.parseUriOrNull(item.fileUri));
            fileStorageThumbnail.hierarchy.roundingParams = getRoundingParams(context)
        }
    }
}
