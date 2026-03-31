package mega.privacy.android.shared.nodes.components.previewdata

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.shared.nodes.model.NodeViewItem
import mega.privacy.android.shared.nodes.model.SelectableNodeItem
import mega.privacy.android.shared.nodes.selection.SelectableTypedNode

class FolderNodePreviewDataProvider :
    PreviewParameterProvider<List<SelectableTypedNode<TypedFolderNode>>> {

    private val nodeUIItem1 = SelectableNodeItem<TypedFolderNode>(
        NodeViewItem(
            node = object : TypedFolderNode {
                override val isInRubbishBin = false
                override val isShared = false
                override val isPendingShare = false
                override val isSynced = false
                override val device = null
                override val childFolderCount = 1
                override val childFileCount = 1
                override val isS4Container = false
                override val fetchChildren: suspend (SortOrder) -> List<UnTypedNode> =
                    { emptyList() }
                override val type = FolderType.Default
                override val id = NodeId(1L)
                override val name = "My important files"
                override val parentId = NodeId(2L)
                override val base64Id = "1L"
                override val restoreId: NodeId? = null
                override val label = 1
                override val nodeLabel = NodeLabel.BLUE
                override val isFavourite = false
                override val isMarkedSensitive = false
                override val isSensitiveInherited: Boolean = false
                override val exportedData = null
                override val isTakenDown = false
                override val isIncomingShare = false
                override val isNodeKeyDecrypted = false
                override val creationTime = System.currentTimeMillis()
                override val serializedData = null
                override val isAvailableOffline: Boolean = false
                override val versionCount: Int = 0
                override val description: String = "Sample node description"
                override val tags: List<String> = listOf("tag1", "tag2")
            }),
        isSelected = false
    )

    private val nodeUIItem2 = SelectableNodeItem<TypedFolderNode>(
        NodeViewItem(node = object : TypedFolderNode {
            override val isInRubbishBin = false
            override val isShared = false
            override val isPendingShare = false
            override val isSynced = false
            override val device = null
            override val childFolderCount = 1
            override val childFileCount = 1
            override val isS4Container = false
            override val fetchChildren: suspend (SortOrder) -> List<UnTypedNode> = { emptyList() }
            override val type = FolderType.Default
            override val id = NodeId(2L)
            override val name = "Less important files"
            override val parentId = NodeId(2L)
            override val base64Id = "1L"
            override val restoreId: NodeId? = null
            override val label = 1
            override val nodeLabel = NodeLabel.BLUE
            override val isFavourite = true
            override val isMarkedSensitive = false
            override val isSensitiveInherited: Boolean = false
            override val exportedData = null
            override val isTakenDown = false
            override val isIncomingShare = false
            override val isNodeKeyDecrypted = false
            override val creationTime = System.currentTimeMillis()
            override val serializedData = null
            override val isAvailableOffline: Boolean = false
            override val versionCount: Int = 0
            override val description: String = "Sample node description"
            override val tags: List<String> = listOf("tag1", "tag2")
        }),
        isSelected = false,
    )

    private val nodeUIItem3 = SelectableNodeItem<TypedFolderNode>(
        NodeViewItem(node = object : TypedFolderNode {
            override val isInRubbishBin = false
            override val isShared = false
            override val isPendingShare = false
            override val isSynced = false
            override val device = null
            override val childFolderCount = 1
            override val childFileCount = 1
            override val isS4Container = false
            override val fetchChildren: suspend (SortOrder) -> List<UnTypedNode> = { emptyList() }
            override val type = FolderType.Default
            override val id = NodeId(3L)
            override val name = "stuff"
            override val parentId = NodeId(2L)
            override val base64Id = "1L"
            override val restoreId: NodeId? = null
            override val label = 1
            override val nodeLabel = NodeLabel.BLUE
            override val isFavourite = false
            override val isMarkedSensitive = false
            override val isSensitiveInherited: Boolean = false
            override val exportedData = null
            override val isTakenDown = false
            override val isIncomingShare = false
            override val isNodeKeyDecrypted = false
            override val creationTime = System.currentTimeMillis()
            override val serializedData = null
            override val isAvailableOffline: Boolean = false
            override val versionCount: Int = 0
            override val description: String = "Sample node description"
            override val tags: List<String> = listOf("tag1", "tag2")
        }),
        isSelected = false,
    )

    override val values: Sequence<List<SelectableTypedNode<TypedFolderNode>>> = sequenceOf(
        listOf(nodeUIItem1, nodeUIItem2, nodeUIItem3),
    )
}