package mega.privacy.android.app.providers.documentprovider

import mega.privacy.android.domain.entity.node.NodeId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps a Cloud Drive document ID string to a node handle, or null if the format is invalid.
 *
 * @param documentId Full document ID (e.g. "mega_cloud_drive_root:123").
 * @param documentIdPrefix Prefix that document IDs must start with (e.g. "mega_cloud_drive_root").
 * @return The nodeId from the handle after the colon, or null if documentId does not start with "$documentIdPrefix:" or the suffix is not a valid Long.
 */
@Singleton
class DocumentIdToNodeIdMapper @Inject constructor() {

    operator fun invoke(documentId: String, documentIdPrefix: String): NodeId? =
        if (!documentId.startsWith("$documentIdPrefix:")) null
        else documentId.substring(documentIdPrefix.length + 1).toLongOrNull()?.let { NodeId(it) }
}
