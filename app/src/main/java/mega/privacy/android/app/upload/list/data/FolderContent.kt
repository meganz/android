package mega.privacy.android.app.upload.list.data

import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.DiffUtil

sealed class FolderContent(val id: Long) {

    companion object {
        private const val HEADER_ID = "HEADER_ID"
    }

    abstract fun getSectionTitle(): String

    data class Data constructor(
        val parent: Data?,
        val document: DocumentFile
    ) : FolderContent(document.uri.hashCode().toLong()) {
        override fun getSectionTitle(): String =
            document.name?.substring(0, 1) ?: ""
    }

    class Header : FolderContent(HEADER_ID.hashCode().toLong()) {
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
            val isSameData = oldContent is Data && newContent is Data && oldContent == newContent
            val isSameHeader =
                oldContent is Header && newContent is Header && oldContent == newContent
            return isSameData || isSameHeader
        }
    }
}