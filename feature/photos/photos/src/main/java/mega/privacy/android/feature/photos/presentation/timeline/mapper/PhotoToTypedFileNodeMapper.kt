package mega.privacy.android.feature.photos.presentation.timeline.mapper

import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.photos.Photo
import java.time.ZoneId
import javax.inject.Inject

class PhotoToTypedFileNodeMapper @Inject constructor() {

    operator fun invoke(photo: Photo): TypedFileNode = object : TypedFileNode {
        override val id: NodeId = NodeId(longValue = photo.id)
        override val name: String = photo.name
        override val parentId: NodeId = NodeId(longValue = photo.parentId)
        override val base64Id: String = photo.base64Id!!
        override val restoreId: NodeId? = photo.restoreId
        override val label: Int = photo.label
        override val nodeLabel: NodeLabel? = photo.nodeLabel
        override val isFavourite: Boolean = photo.isFavourite
        override val isMarkedSensitive: Boolean = photo.isSensitive
        override val isSensitiveInherited: Boolean = photo.isSensitiveInherited
        override val exportedData: ExportedData? = photo.exportedData
        override val isTakenDown: Boolean = photo.isTakenDown
        override val isIncomingShare: Boolean = photo.isIncomingShare
        override val isNodeKeyDecrypted: Boolean = photo.isNodeKeyDecrypted
        override val creationTime: Long =
            photo.creationTime.atZone(ZoneId.systemDefault()).toEpochSecond()
        override val serializedData: String? = photo.serializedData
        override val isAvailableOffline: Boolean = photo.isAvailableOffline
        override val versionCount: Int = photo.versionCount
        override val description: String? = photo.description
        override val tags: List<String>? = photo.tags

        // FileNode properties
        override val size: Long = photo.size
        override val modificationTime: Long =
            photo.modificationTime.atZone(ZoneId.systemDefault()).toEpochSecond()
        override val type: FileTypeInfo = photo.fileTypeInfo
        override val thumbnailPath: String? = photo.thumbnailFilePath
        override val previewPath: String? = photo.previewFilePath
        override val fullSizePath: String? = null
        override val fingerprint: String? = null
        override val originalFingerprint: String? = null
        override val hasThumbnail: Boolean = photo.thumbnailFilePath != null
        override val hasPreview: Boolean = photo.previewFilePath != null
    }
}
