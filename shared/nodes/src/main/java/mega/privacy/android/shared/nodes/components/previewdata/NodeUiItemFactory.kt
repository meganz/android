package mega.privacy.android.shared.nodes.components.previewdata

import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.nodes.extension.getSharesIcon
import mega.privacy.android.shared.nodes.mapper.toSharedSubtitle
import mega.privacy.android.shared.nodes.model.NodeSubtitleText
import mega.privacy.android.shared.nodes.model.NodeUiItem

@Suppress("UNCHECKED_CAST")
fun previewFolderNodeUiItem(
    id: Long = 1L,
    name: String = "Sample Folder $id",
    isSelected: Boolean = false,
    childFolderCount: Int = 2,
    childFileCount: Int = 5,
): NodeUiItem<TypedNode> =
    NodeUiItem(
        isSelected = isSelected,
        title = LocalizedText.Literal(name),
        subtitle = NodeSubtitleText.FolderSubtitle(
            childFolderCount = childFolderCount,
            childFileCount = childFileCount,
        ),
        iconRes = IconPackR.drawable.ic_folder_medium_solid,
        isFolderNode = true,
        node = previewTypedFolderNode(
            id = id,
            name = name,
            childFolderCount = childFolderCount,
            childFileCount = childFileCount,
            isShared = false,
            isIncomingShare = false,
        ),
    ) as NodeUiItem<TypedNode>

/**
 * Preview [NodeUiItem] for incoming shared folders, with [NodeSubtitleText.SharedSubtitle] and
 * access icons aligned with production mapping ([getSharesIcon]).
 *
 */
@Suppress("UNCHECKED_CAST")
fun previewIncomingShareFolderNodeUiItem(
    id: Long = 1L,
    name: String = "Shared folder $id",
    isSelected: Boolean = false,
    access: AccessPermission = AccessPermission.READWRITE,
    shareCount: Int = 1,
    user: String? = "preview.user@example.com",
    userFullName: String? = "Preview User",
    isVerified: Boolean = true,
    isPending: Boolean = false,
    isContactCredentialsVerified: Boolean = false,
    subtitle: NodeSubtitleText.SharedSubtitle? = null,
    isContactVerificationOn: Boolean = false,
    childFolderCount: Int = 2,
    childFileCount: Int = 5,
): NodeUiItem<TypedNode> {
    val shareData = ShareData(
        user = user,
        userFullName = userFullName,
        nodeHandle = id,
        access = access,
        timeStamp = 1_700_000_000_000L,
        isPending = isPending,
        isVerified = isVerified,
        isContactCredentialsVerified = isContactCredentialsVerified,
        count = shareCount,
    )
    val baseFolder = previewTypedFolderNode(
        id = id,
        name = name,
        childFolderCount = childFolderCount,
        childFileCount = childFileCount,
        isShared = true,
        isIncomingShare = true,
    )
    val shareFolderNode = ShareFolderNode(baseFolder, shareData)
    return NodeUiItem(
        isSelected = isSelected,
        title = LocalizedText.Literal(name),
        subtitle = subtitle ?: shareData.toSharedSubtitle(),
        iconRes = IconPackR.drawable.ic_folder_medium_solid,
        isFolderNode = true,
        accessPermissionIcon = shareFolderNode.getSharesIcon(isContactVerificationOn),
        showIsVerified = isContactVerificationOn && shareFolderNode.isIncomingShare &&
                shareData.isContactCredentialsVerified,
        showFavourite = false,
        node = shareFolderNode,
    ) as NodeUiItem<TypedNode>
}

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

private fun previewTypedFolderNode(
    id: Long,
    name: String,
    childFolderCount: Int,
    childFileCount: Int,
    isShared: Boolean,
    isIncomingShare: Boolean,
): TypedFolderNode =
    object : TypedFolderNode {
        override val isInRubbishBin = false
        override val isShared = isShared
        override val isPendingShare = false
        override val isSynced = false
        override val device = null
        override val childFolderCount = childFolderCount
        override val childFileCount = childFileCount
        override val isS4Container = false
        override val fetchChildren: suspend (SortOrder) -> List<UnTypedNode> = { emptyList() }
        override val type = FolderType.Default
        override val id = NodeId(id)
        override val name = name
        override val parentId = NodeId(0L)
        override val base64Id = "${id}L"
        override val restoreId: NodeId? = null
        override val label = 0
        override val nodeLabel = NodeLabel.BLUE
        override val isFavourite = false
        override val isMarkedSensitive = false
        override val isSensitiveInherited = false
        override val exportedData = null
        override val isTakenDown = false
        override val isIncomingShare = isIncomingShare
        override val isNodeKeyDecrypted = true
        override val creationTime = 1_700_000_000_000L
        override val serializedData = null
        override val isAvailableOffline = false
        override val versionCount = 0
        override val description = ""
        override val tags: List<String> = emptyList()
    }
