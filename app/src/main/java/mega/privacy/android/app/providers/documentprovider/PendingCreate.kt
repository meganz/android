package mega.privacy.android.app.providers.documentprovider

import mega.privacy.android.domain.entity.node.NodeId

internal data class PendingCreate(
    val parentNodeId: NodeId,
    val parentDocumentId: String,
    val displayName: String,
    val mimeType: String,
)