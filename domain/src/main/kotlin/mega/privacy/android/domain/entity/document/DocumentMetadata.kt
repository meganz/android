package mega.privacy.android.domain.entity.document

/**
 * Helper class to return simple metadata from a DocumentFile
 * @param name of the document
 * @param isFolder true if the document is a folder (aka directory) false if it's a file
 */
data class DocumentMetadata(
    val name: String,
    val isFolder: Boolean = false,
) {
    val isFile = !isFolder
}