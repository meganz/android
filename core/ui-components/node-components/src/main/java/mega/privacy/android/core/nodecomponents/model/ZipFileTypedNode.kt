package mega.privacy.android.core.nodecomponents.model

import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.UnMappedFileTypeInfo
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import java.io.File

/**
 * Synthetic [TypedFileNode] representing a file inside a zip archive.
 * Used as a placeholder node when the actual MEGA cloud node does not exist,
 * e.g. when the video player is launched from a zip file adapter.
 */
data class ZipFileTypedNode(val file: File) : TypedFileNode {
    override val id: NodeId get() = NodeId(file.name.hashCode().toLong())
    override val name: String get() = file.name
    override val parentId: NodeId get() = NodeId(-1L)
    override val base64Id: String get() = ""
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
    override val creationTime: Long get() = 0L
    override val serializedData: String? get() = null
    override val isAvailableOffline: Boolean get() = false
    override val versionCount: Int get() = 0
    override val description: String? get() = null
    override val tags: List<String>? get() = null
    override val size: Long get() = file.length()
    override val modificationTime: Long get() = 0L
    override val type: FileTypeInfo get() = UnMappedFileTypeInfo(extension = file.extension)
    override val thumbnailPath: String? get() = null
    override val previewPath: String? get() = null
    override val fullSizePath: String? get() = null
    override val fingerprint: String? get() = null
    override val originalFingerprint: String? get() = null
    override val hasThumbnail: Boolean get() = false
    override val hasPreview: Boolean get() = false
}
