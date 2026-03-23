package mega.privacy.android.shared.nodes.components.previewdata

import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.nodes.model.NodeSubtitleText
import mega.privacy.android.shared.nodes.model.NodeUiItem

@Suppress("UNCHECKED_CAST")
fun previewFolderNodeUiItem(
    id: Long = 1L,
    name: String = "Sample Folder $id",
    isSelected: Boolean = false,
): NodeUiItem<TypedNode> =
    NodeUiItem(
        isSelected = isSelected,
        title = LocalizedText.Literal(name),
        subtitle = NodeSubtitleText.FolderSubtitle(
            childFolderCount = 2,
            childFileCount = 5,
        ),
        iconRes = IconPackR.drawable.ic_folder_medium_solid,
        isFolderNode = true,
        node = object : TypedFolderNode {
            override val isInRubbishBin = false
            override val isShared = false
            override val isPendingShare = false
            override val isSynced = false
            override val device = null
            override val childFolderCount = 2
            override val childFileCount = 5
            override val isS4Container = false
            override val fetchChildren: suspend (SortOrder) -> List<UnTypedNode> = { emptyList() }
            override val type = FolderType.Default
            override val id = NodeId(id)
            override val name = name
            override val parentId = NodeId(0L)
            override val base64Id = "1L"
            override val restoreId: NodeId? = null
            override val label = 0
            override val nodeLabel = NodeLabel.BLUE
            override val isFavourite = false
            override val isMarkedSensitive = false
            override val isSensitiveInherited = false
            override val exportedData = null
            override val isTakenDown = false
            override val isIncomingShare = false
            override val isNodeKeyDecrypted = true
            override val creationTime = 1_700_000_000_000L
            override val serializedData = null
            override val isAvailableOffline = false
            override val versionCount = 0
            override val description = ""
            override val tags: List<String> = emptyList()
        },
    ) as NodeUiItem<TypedNode>

@Suppress("UNCHECKED_CAST")
fun previewFileNodeUiItem(
    id: Long = 10L,
    name: String = "notes$id.txt",
    isSelected: Boolean = false,
): NodeUiItem<TypedNode> = NodeUiItem(
    isSelected = isSelected,
    title = LocalizedText.Literal(name),
    subtitle = NodeSubtitleText.FileSubtitle(
        fileSizeValue = 12_288L,
        modificationTime = 1_700_000_000_000L,
        showPublicLinkCreationTime = false,
    ),
    iconRes = IconPackR.drawable.ic_text_medium_solid,
    isFolderNode = false,
    node = object : TypedFileNode {
        override val id = NodeId(id)
        override val name = name
        override val parentId = NodeId(0L)
        override val base64Id = "10L"
        override val restoreId: NodeId? = null
        override val label = 0
        override val nodeLabel = NodeLabel.RED
        override val isFavourite = true
        override val isMarkedSensitive = false
        override val isSensitiveInherited = false
        override val exportedData = null
        override val isTakenDown = false
        override val isIncomingShare = false
        override val isNodeKeyDecrypted = true
        override val creationTime = 1_700_000_000_000L
        override val serializedData = null
        override val isAvailableOffline = true
        override val versionCount = 1
        override val description = "Meeting notes"
        override val tags: List<String> = listOf("work")
        override val size = 12_288L
        override val modificationTime = 1_700_000_000_000L
        override val type = TextFileTypeInfo("text/plain", "txt")
        override val thumbnailPath: String? = null
        override val previewPath: String? = null
        override val fullSizePath: String? = null
        override val fingerprint: String? = null
        override val originalFingerprint: String? = null
        override val hasThumbnail = false
        override val hasPreview = false
    },
) as NodeUiItem<TypedNode>
