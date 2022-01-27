package mega.privacy.android.app.upload.list.data

import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util

sealed class FolderContent(val id: Long) {

    companion object {
        private const val HEADER_ID = "HEADER_ID"
        private const val SEPARATOR_ID = "SEPARATOR_ID"
    }

    abstract fun getSectionTitle(): String

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

        override fun getSectionTitle(): String =
            document.name?.substring(0, 1) ?: ""
    }

    class Header : FolderContent(HEADER_ID.hashCode().toLong()) {
        override fun getSectionTitle(): String = ""
    }

    class Separator : FolderContent(SEPARATOR_ID.hashCode().toLong()) {
        override fun getSectionTitle(): String = ""
    }

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