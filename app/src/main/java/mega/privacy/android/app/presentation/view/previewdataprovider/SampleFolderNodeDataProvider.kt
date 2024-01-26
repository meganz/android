package mega.privacy.android.app.presentation.view.previewdataprovider

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.UnTypedNode

internal class SampleFolderNodeDataProvider :
    PreviewParameterProvider<List<NodeUIItem<TypedFolderNode>>> {

    private val nodeUIItem1 = NodeUIItem<TypedFolderNode>(
        isSelected = false,
        isInvisible = false,
        node = object : TypedFolderNode {
            override val isInRubbishBin = false
            override val isShared = false
            override val isPendingShare = false
            override val device = null
            override val childFolderCount = 1
            override val childFileCount = 1
            override val fetchChildren: suspend (SortOrder) -> List<UnTypedNode> = { emptyList() }
            override val type = FolderType.Default
            override val id = NodeId(1L)
            override val name = "My important files"
            override val parentId = NodeId(2L)
            override val base64Id = "1L"
            override val restoreId: NodeId? = null
            override val label = 1
            override val isFavourite = false
            override val exportedData = null
            override val isTakenDown = false
            override val isIncomingShare = false
            override val isNodeKeyDecrypted = false
            override val creationTime = System.currentTimeMillis()
            override val serializedData = null
            override val isAvailableOffline: Boolean = false
            override val versionCount: Int = 0
        })

    private val nodeUIItem2 = NodeUIItem<TypedFolderNode>(
        isSelected = false,
        isInvisible = false,
        node = object : TypedFolderNode {
            override val isInRubbishBin = false
            override val isShared = false
            override val isPendingShare = false
            override val device = null
            override val childFolderCount = 1
            override val childFileCount = 1
            override val fetchChildren: suspend (SortOrder) -> List<UnTypedNode> = { emptyList() }
            override val type = FolderType.Default
            override val id = NodeId(2L)
            override val name = "Less important files"
            override val parentId = NodeId(2L)
            override val base64Id = "1L"
            override val restoreId: NodeId? = null
            override val label = 1
            override val isFavourite = true
            override val exportedData = null
            override val isTakenDown = false
            override val isIncomingShare = false
            override val isNodeKeyDecrypted = false
            override val creationTime = System.currentTimeMillis()
            override val serializedData = null
            override val isAvailableOffline: Boolean = false
            override val versionCount: Int = 0
        })

    private val nodeUIItem3 = NodeUIItem<TypedFolderNode>(
        isSelected = false,
        isInvisible = false,
        node = object : TypedFolderNode {
            override val isInRubbishBin = false
            override val isShared = false
            override val isPendingShare = false
            override val device = null
            override val childFolderCount = 1
            override val childFileCount = 1
            override val fetchChildren: suspend (SortOrder) -> List<UnTypedNode> = { emptyList() }
            override val type = FolderType.Default
            override val id = NodeId(3L)
            override val name = "stuff"
            override val parentId = NodeId(2L)
            override val base64Id = "1L"
            override val restoreId: NodeId? = null
            override val label = 1
            override val isFavourite = false
            override val exportedData = null
            override val isTakenDown = false
            override val isIncomingShare = false
            override val isNodeKeyDecrypted = false
            override val creationTime = System.currentTimeMillis()
            override val serializedData = null
            override val isAvailableOffline: Boolean = false
            override val versionCount: Int = 0
        })

    override val values: Sequence<List<NodeUIItem<TypedFolderNode>>> = sequenceOf(
        listOf(nodeUIItem1, nodeUIItem2, nodeUIItem3),
    )
}