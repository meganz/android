package mega.privacy.android.app.uploadFolder.list.data

import android.net.Uri
import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.namecollision.data.NameCollisionUiEntity

/**
 * View item which represents a Folder Content, related to upload folders, at UI level.
 * This sealed class can be either a `Data`, a `Header` or a `Separator`
 */
sealed class FolderContent(val id: Long) {

    companion object {
        private const val HEADER_ID = "HEADER_ID"
        private const val SEPARATOR_ID = "SEPARATOR_ID"
    }

    abstract fun getSectionTitle(): String

    /**
     * View item which represents a file or a folder at UI level.
     *
     * @property parent         Data as parent folder if any, null otherwise.
     * @property isSelected     True if the item is selected at UI, false otherwise.
     * @property isFolder       True if is a folder, false otherwise.
     * @property name           Name of the item.
     * @property uri            Uri of the item.
     * @property lastModified   Last modified date of the item.
     * @property size           Size of the item.
     * @property numberOfFiles  Number of files if is a folder, 0 otherwise.
     * @property numberOfFolders Number of folders if is a folder, 0 otherwise.
     */
    data class Data(
        val parent: Data?,
        val isFolder: Boolean,
        val name: String,
        val lastModified: Long,
        val size: Long,
        val numberOfFiles: Int,
        val numberOfFolders: Int,
        var isSelected: Boolean = false,
        val uri: Uri,
    ) : FolderContent(uri.hashCode().toLong()) {
        var nameCollision: NameCollisionUiEntity? = null

        override fun getSectionTitle(): String = name.substring(0, 1)
    }

    /**
     * View item which represents the item header at UI level.
     */
    class Header : FolderContent(HEADER_ID.hashCode().toLong()) {
        override fun getSectionTitle(): String = ""
    }

    /**
     * View item which represents the item which separates folders and files at UI level.
     */
    class Separator : FolderContent(SEPARATOR_ID.hashCode().toLong()) {
        override fun getSectionTitle(): String = ""
    }

    /**
     * Diff callback used to compare FolderContent items before perform an update in the RecyclerView.
     */
    class DiffCallback : DiffUtil.ItemCallback<FolderContent>() {
        override fun areItemsTheSame(
            oldContent: FolderContent,
            newContent: FolderContent,
        ): Boolean =
            oldContent.id == newContent.id

        override fun areContentsTheSame(
            oldContent: FolderContent,
            newContent: FolderContent,
        ): Boolean {
            val isSameData =
                oldContent is Data && newContent is Data && oldContent.isSelected == newContent.isSelected
            val isSameHeader =
                oldContent is Header && newContent is Header && oldContent == newContent

            return isSameData || isSameHeader
        }
    }
}