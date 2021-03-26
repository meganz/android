package mega.privacy.android.app.adapters.viewHolders

import android.content.Context
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemFileStorageBinding
import mega.privacy.android.app.fragments.homepage.getRoundingParams
import mega.privacy.android.app.lollipop.megachat.FileGalleryItem
import java.util.concurrent.TimeUnit

class FileStorageHolder(private val binding: ItemFileStorageBinding) :
    RecyclerView.ViewHolder(binding.root) {


    fun bind(context: Context, item: FileGalleryItem, firstItem: Boolean) {
        binding.apply {
            if (item.isImage) {
                imageThumbnail.isVisible = true
                imageThumbnail.hierarchy.roundingParams = getRoundingParams(context)
                videoThumbnail.isVisible = false
                videoDuration.isVisible = false
            } else {
                imageThumbnail.isVisible = false
                videoThumbnail.isVisible = true
                videoThumbnail.hierarchy.roundingParams = getRoundingParams(context)
                videoDuration.isVisible = true
                videoDuration.text = getDuration(item.duration)
            }
        }
    }

    private fun getDuration(duration: Long): String {
        return java.lang.String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(duration),
            TimeUnit.MILLISECONDS.toSeconds(duration) -
                    TimeUnit.MINUTES.toSeconds(
                        TimeUnit.MILLISECONDS.toMinutes(
                            duration
                        )
                    )
        )
    }
}
