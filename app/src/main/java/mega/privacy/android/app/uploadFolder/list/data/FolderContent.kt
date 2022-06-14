package mega.privacy.android.app.uploadFolder.list.data

import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util

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
     * @property document       DocumentFile of the file or folder.
     * @property isSelected     True if the item is selected at UI, false otherwise.
     * @property isFolder       True if is a folder, false otherwise.
     * @property name           Name of the item.
     * @property uri            Uri of the item.
     * @property lastModified   Last modified date of the item.
     * @property size           Size of the item.
     * @property info           Info to show as complementary info of the item.
     *                          Folder content if is a folder, file size and last modified date if a file.
     */
    data class Data constructor(
        val parent: Data?,
        val document: DocumentFile,
        var isSelected: Boolean = false
    ) : FolderContent(document.uri.hashCode().toLong()) {
        val isFolder = document.isDirectory
        val name = document.name
        val uri = document.uri
        val lastModified = document.lastModified()
        val size = document.length()
        val info: String = if (document.isDirectory) {
            FileUtil.getFileFolderInfo(document)
        } else {
            TextUtil.getFileInfo(
                Util.getSizeString(size),
                TimeUtils.formatLongDateTime(lastModified / 1000)
            )
        }
        var nameCollision: NameCollision? = null

        override fun getSectionTitle(): String =
            document.name?.substring(0, 1) ?: ""
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
            newContent: FolderContent
        ): Boolean =
            oldContent.id == newContent.id

        override fun areContentsTheSame(
            oldContent: FolderContent,
            newContent: FolderContent
        ): Boolean {
            val isSameData =
                oldContent is Data && newContent is Data && oldContent.isSelected == newContent.isSelected
            val isSameHeader =
                oldContent is Header && newContent is Header && oldContent == newContent

            return isSameData || isSameHeader
        }
    }
}