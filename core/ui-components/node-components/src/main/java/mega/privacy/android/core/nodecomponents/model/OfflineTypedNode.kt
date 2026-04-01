package mega.privacy.android.core.nodecomponents.model

import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.UnMappedFileTypeInfo
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.offline.OfflineFileInformation

/**
 * Wrapper to represent an offline file as a TypedNode when the cloud node has been deleted.
 * Used as a fallback when getNodeByIdUseCase returns null but offline file still exists.
 */
sealed interface OfflineTypedNode : TypedNode

/**
 * Represents an offline file node when the cloud node has been deleted.
 */
data class OfflineTypedFileNode(
    private val offlineInfo: OfflineFileInformation,
) : OfflineTypedNode, TypedFileNode {

    // Node properties
    override val id: NodeId get() = NodeId(offlineInfo.handle.toLongOrNull() ?: -1L)
    override val name: String get() = offlineInfo.name
    override val parentId: NodeId get() = NodeId(offlineInfo.parentId.toLong())
    override val base64Id: String get() = offlineInfo.handle
    override val restoreId: NodeId? get() = null
    override val label: Int get() = 0
    override val nodeLabel: NodeLabel? get() = null
    override val isFavourite: Boolean get() = false
    override val isMarkedSensitive: Boolean get() = false
    override val isSensitiveInherited: Boolean get() = false
    override val exportedData: ExportedData? get() = null
    override val isTakenDown: Boolean get() = false
    override val isIncomingShare: Boolean get() = false
    override val isNodeKeyDecrypted: Boolean get() = true

    /** OfflineFileInformation does not expose creation time; modification time is used as approximation. */
    override val creationTime: Long get() = offlineInfo.lastModifiedTime ?: 0L
    override val serializedData: String? get() = null
    override val isAvailableOffline: Boolean get() = true
    override val versionCount: Int get() = 0
    override val description: String? get() = null
    override val tags: List<String>? get() = null

    // FileNode properties
    override val size: Long get() = offlineInfo.totalSize
    override val modificationTime: Long get() = offlineInfo.lastModifiedTime ?: 0L
    override val type: FileTypeInfo
        get() = offlineInfo.fileTypeInfo ?: UnMappedFileTypeInfo(
            extension = offlineInfo.name.substringAfterLast(".", "")
        )
    override val thumbnailPath: String? get() = offlineInfo.thumbnail
    override val previewPath: String? get() = null
    override val fullSizePath: String? get() = null
    override val fingerprint: String? get() = null
    override val originalFingerprint: String? get() = null
    override val hasThumbnail: Boolean get() = offlineInfo.thumbnail != null
    override val hasPreview: Boolean get() = false

    companion object {
        /**
         * Create an OfflineTypedFileNode from OfflineFileInformation
         */
        fun from(offlineInfo: OfflineFileInformation): OfflineTypedFileNode =
            OfflineTypedFileNode(offlineInfo)
    }
}

/**
 * Represents an offline folder node when the cloud node has been deleted.
 */
data class OfflineTypedFolderNode(
    private val offlineInfo: OfflineFileInformation,
) : OfflineTypedNode, TypedFolderNode {

    // Node properties
    override val id: NodeId get() = NodeId(offlineInfo.handle.toLongOrNull() ?: -1L)
    override val name: String get() = offlineInfo.name
    override val parentId: NodeId get() = NodeId(offlineInfo.parentId.toLong())
    override val base64Id: String get() = offlineInfo.handle
    override val restoreId: NodeId? get() = null
    override val label: Int get() = 0
    override val nodeLabel: NodeLabel? get() = null
    override val isFavourite: Boolean get() = false
    override val isMarkedSensitive: Boolean get() = false
    override val isSensitiveInherited: Boolean get() = false
    override val exportedData: ExportedData? get() = null
    override val isTakenDown: Boolean get() = false
    override val isIncomingShare: Boolean get() = false
    override val isNodeKeyDecrypted: Boolean get() = true

    /** OfflineFileInformation does not expose creation time; modification time is used as approximation. */
    override val creationTime: Long get() = offlineInfo.lastModifiedTime ?: 0L
    override val serializedData: String? get() = null
    override val isAvailableOffline: Boolean get() = true
    override val versionCount: Int get() = 0
    override val description: String? get() = null
    override val tags: List<String>? get() = null

    // FolderNode properties
    override val isInRubbishBin: Boolean get() = false
    override val isShared: Boolean get() = false
    override val isPendingShare: Boolean get() = false
    override val isSynced: Boolean get() = false
    override val device: String? get() = null
    override val childFolderCount: Int get() = 0
    override val childFileCount: Int get() = 0
    override val isS4Container: Boolean get() = false

    /**
     * Returns an empty list as offline folder children are not loaded through this interface.
     * Offline children are loaded separately by the offline feature module.
     */
    override val fetchChildren: suspend (SortOrder) -> List<UnTypedNode> get() = { emptyList() }

    // TypedFolderNode properties
    override val type: FolderType get() = FolderType.Default

    companion object {
        /**
         * Create an OfflineTypedFolderNode from OfflineFileInformation
         */
        fun from(offlineInfo: OfflineFileInformation): OfflineTypedFolderNode =
            OfflineTypedFolderNode(offlineInfo)
    }
}
