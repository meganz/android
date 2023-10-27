package mega.privacy.android.data.mapper.node

import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * The mapper class to convert from MegaNode to ImageNode
 */
internal class ImageNodeMapper @Inject constructor(
    private val fileTypeInfoMapper: FileTypeInfoMapper,
    private val thumbnailFromServerMapper: ThumbnailFromServerMapper,
    private val previewFromServerMapper: PreviewFromServerMapper,
    private val fullImageFromServerMapper: FullImageFromServerMapper,
    private val offlineAvailabilityMapper: OfflineAvailabilityMapper
) {
    suspend operator fun invoke(
        megaNode: MegaNode,
        hasVersion: suspend (MegaNode) -> Boolean,
        requireSerializedData: Boolean = false,
        offline: Offline?
    ) = if (megaNode.isFolder) {
        throw IllegalStateException("Node is a folder")
    } else {
        val hasVersion = hasVersion(megaNode)
        val isAvailableOffline = offline?.let { offlineAvailabilityMapper(megaNode, it) } ?: false
        object : ImageNode {
            override val id = NodeId(megaNode.handle)
            override val name = megaNode.name
            override val size = megaNode.size
            override val label = megaNode.label
            override val parentId = NodeId(megaNode.parentHandle)
            override val base64Id = megaNode.base64Handle
            override val creationTime = megaNode.creationTime
            override val modificationTime = megaNode.modificationTime
            override val hasVersion = hasVersion
            override val thumbnailPath = null
            override val previewPath = null
            override val fullSizePath = null
            override val type = fileTypeInfoMapper(megaNode)
            override val isFavourite = megaNode.isFavourite
            override val exportedData = megaNode.takeIf { megaNode.isExported }?.let {
                ExportedData(it.publicLink, it.publicLinkCreationTime)
            }
            override val isTakenDown = megaNode.isTakenDown
            override val isIncomingShare = megaNode.isInShare
            override val fingerprint = megaNode.fingerprint
            override val originalFingerprint = megaNode.originalFingerprint
            override val isNodeKeyDecrypted = megaNode.isNodeKeyDecrypted
            override val hasThumbnail = megaNode.hasThumbnail()
            override val hasPreview = megaNode.hasPreview()
            override val downloadThumbnail = thumbnailFromServerMapper(megaNode)
            override val downloadPreview = previewFromServerMapper(megaNode)
            override val downloadFullImage = fullImageFromServerMapper(megaNode)
            override val latitude = megaNode.latitude
            override val longitude = megaNode.longitude
            override val serializedData = if (requireSerializedData) megaNode.serialize() else null
            override val isAvailableOffline: Boolean = isAvailableOffline
        }
    }
}
