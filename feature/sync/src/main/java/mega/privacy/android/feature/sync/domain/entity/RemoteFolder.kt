package mega.privacy.android.feature.sync.domain.entity

import mega.privacy.android.domain.entity.node.NodeId

/**
 * Represents a folder on Remote Storage.
 * @param id the id of the remote folder
 * @param name name of the folder
 */
data class RemoteFolder(
    val id: NodeId,
    val name: String,
)
