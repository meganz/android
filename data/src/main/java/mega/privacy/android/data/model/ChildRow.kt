package mega.privacy.android.data.model

import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/**
 * Lightweight in-memory snapshot of a directory child obtained from a single
 * `ContentResolver.query()` (or [DocumentFile.listFiles] for `file://` URIs).
 *
 * Holding the snapshot avoids the per-property IPC cost of touching
 * [DocumentFile.getName] / [DocumentFile.length] / [DocumentFile.lastModified] /
 * [DocumentFile.isDirectory] on a `TreeDocumentFile`.
 */
internal data class ChildRow(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val size: Long,
    val lastModified: Long,
    val isFolder: Boolean,
    /** Document id for SAF rows; null for `file://` rows. */
    val docId: String?,
)
