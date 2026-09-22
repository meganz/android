package mega.privacy.android.core.nodecomponents.extension

import mega.privacy.android.core.nodecomponents.model.OfflineTypedNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.offline.IsNodeUnderOfflineRootUseCase

/**
 * Check if the node is not an S4 container
 */
internal fun TypedNode.isNotS4Container(): Boolean =
    (this as? TypedFolderNode)?.isS4Container != true

/**
 * Check if the node is available offline
 *
 * Nodes listed in the Offline section are available offline by construction. A folder needs the
 * offline database to tell apart one the user made available offline from one stored only to
 * mirror the cloud structure of an offline descendant; having no offline copy at all rules that
 * out without a lookup. Files are never stored as ancestors, so the node answers for itself.
 */
internal suspend fun TypedNode.isAvailableOffline(
    isNodeUnderOfflineRootUseCase: IsNodeUnderOfflineRootUseCase,
): Boolean = when {
    this is OfflineTypedNode -> true
    this is FolderNode -> isAvailableOffline && isNodeUnderOfflineRootUseCase(id)
    else -> isAvailableOffline
}
