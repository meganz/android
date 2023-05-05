package mega.privacy.android.data.model.node

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode

internal data class DefaultFolderNode(
    override val id: NodeId,
    override val name: String,
    override val parentId: NodeId,
    override val base64Id: String,
    override val label: Int,
    override val hasVersion: Boolean,
    override val childFolderCount: Int,
    override val childFileCount: Int,
    override val isFavourite: Boolean,
    override val exportedData: ExportedData?,
    override val isTakenDown: Boolean,
    override val isInRubbishBin: Boolean,
    override val isIncomingShare: Boolean,
    override val isShared: Boolean,
    override val isPendingShare: Boolean,
    override val device: String?,
    override val isNodeKeyDecrypted: Boolean,
    override val creationTime: Long,
    override val fetchChildren: suspend (SortOrder) -> List<UnTypedNode>,
) : FolderNode