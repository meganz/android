package mega.privacy.android.data.model.node

import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId

internal data class DefaultFileNode(
    override val id: NodeId,
    override val name: String,
    override val parentId: NodeId,
    override val base64Id: String,
    override val restoreId: NodeId?,
    override val size: Long,
    @Deprecated("Use nodeLabel instead")
    override val label: Int,
    override val nodeLabel: NodeLabel?,
    override val creationTime: Long,
    override val modificationTime: Long,
    override val type: FileTypeInfo,
    override val thumbnailPath: String? = null,
    override val previewPath: String? = null,
    override val fullSizePath: String? = null,
    override val isFavourite: Boolean,
    override val isMarkedSensitive: Boolean,
    override val isSensitiveInherited: Boolean,
    override val exportedData: ExportedData?,
    override val isTakenDown: Boolean,
    override val isIncomingShare: Boolean,
    override val fingerprint: String?,
    override val originalFingerprint: String?,
    override val isNodeKeyDecrypted: Boolean,
    override val hasThumbnail: Boolean,
    override val hasPreview: Boolean,
    override val serializedData: String?,
    override val isAvailableOffline: Boolean,
    override val versionCount: Int,
    override val description: String? = null,
    override val tags: List<String>?
) : FileNode
