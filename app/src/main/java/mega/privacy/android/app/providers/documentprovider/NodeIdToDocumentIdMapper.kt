package mega.privacy.android.app.providers.documentprovider

import mega.privacy.android.domain.entity.node.NodeId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps a [NodeId] to a Cloud Drive document ID string.
 *
 * @param nodeId The node ID.
 * @param documentIdPrefix Prefix for document IDs (e.g. "mega_cloud_drive_root").
 * @return Document ID in the form "$documentIdPrefix:${nodeId.longValue}".
 */
@Singleton
class NodeIdToDocumentIdMapper @Inject constructor() {

    operator fun invoke(nodeId: NodeId, documentIdPrefix: String): String =
        "$documentIdPrefix:${nodeId.longValue}"
}
