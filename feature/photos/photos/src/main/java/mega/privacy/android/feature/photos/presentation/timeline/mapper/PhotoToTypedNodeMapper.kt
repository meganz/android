package mega.privacy.android.feature.photos.presentation.timeline.mapper

import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.photos.Photo
import java.time.ZoneId
import javax.inject.Inject

class PhotoToTypedNodeMapper @Inject constructor() {

    operator fun invoke(photo: Photo): TypedNode = object : TypedNode {
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
    }
}
