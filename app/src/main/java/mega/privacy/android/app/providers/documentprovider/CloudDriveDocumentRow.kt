package mega.privacy.android.app.providers.documentprovider

/**
 * UI model for a single document row exposed by the Cloud Drive document provider.
 * Contains the fields required to populate DocumentsContract.Document cursor columns.
 *
 * @param documentId Document ID (e.g. "mega_cloud_drive_root:123").
 * @param displayName Display name for the document.
 * @param mimeType MIME type ([Document.MIME_TYPE_DIR] for folders).
 * @param size Size in bytes (0 for directories).
 * @param lastModified Last modified time (creation time for folders).
 * @param flags Document flags (e.g. 0).
 */
data class CloudDriveDocumentRow(
    val documentId: String,
    val displayName: String,
    val mimeType: String,
    val size: Long,
    val lastModified: Long,
    val flags: Int = 0,
)