package mega.privacy.android.app.main.megachat.chatAdapters

import android.content.Context
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemFileStorageBinding
import mega.privacy.android.app.fragments.homepage.getRoundingParams

class FileStorageChatHolder (private val binding: ItemFileStorageBinding) :
    RecyclerView.ViewHolder(binding.root) {


    fun bind(context: Context, item: String) {
        binding.apply {
            imageThumbnail.setImageURI(item)
            imageThumbnail.isVisible = true
            imageThumbnail.hierarchy.roundingParams = getRoundingParams(context)
            videoThumbnail.isVisible = false
            videoDuration.isVisible = false

        }
    }
}