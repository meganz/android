package mega.privacy.android.app.providers.documentprovider

import android.provider.DocumentsContract.Document
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps [TypedNode] to [CloudDriveDocumentRow] for the Cloud Drive document provider UI.
 */
@Singleton
class CloudDriveDocumentRowMapper @Inject constructor(
    private val nodeIdToDocumentIdMapper: NodeIdToDocumentIdMapper,
) {

    /**
     * Maps a [TypedNode] (child item) to a document row. Use when building child document lists.
     *
     * @param node The typed node (folder or file).
     * @param documentIdPrefix Prefix for document IDs (e.g. "mega_cloud_drive_root").
     */
    operator fun invoke(node: TypedNode, documentIdPrefix: String): CloudDriveDocumentRow {
        val documentId = nodeIdToDocumentIdMapper(node.id, documentIdPrefix)
        return mapToRowWithDocumentId(node, documentId)
    }

    /**
     * Maps a [TypedNode] (single document) to a document row with the given document ID.
     * Use when resolving one document by ID (documentId is used as-is).
     *
     * @param node The typed node (folder or file).
     * @param documentId The document ID for this document (used as-is in the row).
     */
    private fun mapToRowWithDocumentId(node: TypedNode, documentId: String): CloudDriveDocumentRow =
        when (node) {
            is TypedFileNode -> CloudDriveDocumentRow(
                documentId = documentId,
                displayName = node.name,
                mimeType = node.type.mimeType,
                size = node.size,
                lastModified = node.modificationTime,
                flags = 0,
            )

            is TypedFolderNode -> CloudDriveDocumentRow(
                documentId = documentId,
                displayName = node.name,
                mimeType = Document.MIME_TYPE_DIR,
                size = 0L,
                lastModified = node.creationTime,
                flags = 0,
            )

            else -> CloudDriveDocumentRow(
                documentId = documentId,
                displayName = node.name,
                mimeType = Document.MIME_TYPE_DIR,
                size = 0L,
                lastModified = 0L,
                flags = 0,
            )
        }
}
