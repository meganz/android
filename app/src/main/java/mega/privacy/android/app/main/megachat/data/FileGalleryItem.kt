package mega.privacy.android.app.main.megachat.data

import android.graphics.Bitmap
import android.net.Uri
import androidx.recyclerview.widget.DiffUtil

/**
 * View item that represents a Contact Request at UI level.
 *
 * @property id         File id
 * @property isImage    True, if it's image. False, if it's video.
 * @property title      File title
 * @property fileUri    File URI
 * @property dateAdded  Date added
 * @property thumbnail  Image/Video thumbnail
 * @property duration   Video duration
 */
data class FileGalleryItem constructor(
        val id: Long,
        var isImage: Boolean,
        val title: String? = null,
        var fileUri: Uri? = null,
        var dateAdded: String? = null,
        var thumbnail: Bitmap? = null,
        var duration: String? = "",
        var isSelected: Boolean = false
) {

    class DiffCallback : DiffUtil.ItemCallback<FileGalleryItem>() {

        override fun areItemsTheSame(oldItem: FileGalleryItem, newItem: FileGalleryItem): Boolean =
                oldItem.id == newItem.id

        override fun areContentsTheSame(
                oldItem: FileGalleryItem,
                newItem: FileGalleryItem
        ): Boolean =
                oldItem == newItem
    }
}