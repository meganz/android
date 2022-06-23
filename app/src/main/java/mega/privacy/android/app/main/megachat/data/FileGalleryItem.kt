package mega.privacy.android.app.main.megachat.data

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
 * @property duration   Video duration
 * @property filePath   File Path
 */
data class FileGalleryItem constructor(
        val id: Long,
        var isImage: Boolean,
        var isTakePicture: Boolean,
        val hasCameraPermissions: Boolean? = false,
        val title: String? = null,
        var fileUri: Uri? = null,
        var dateAdded: String? = null,
        var duration: String? = "",
        var isSelected: Boolean = false,
        var filePath: String? = null
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