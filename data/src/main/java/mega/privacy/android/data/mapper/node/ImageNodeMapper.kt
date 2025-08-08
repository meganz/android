package mega.privacy.android.data.mapper.node

import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.StringListMapper
import mega.privacy.android.data.mapper.node.label.NodeLabelMapper
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaApiJava
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
    private val offlineAvailabilityMapper: OfflineAvailabilityMapper,
    private val nodeLabelMapper: NodeLabelMapper,
    private val stringListMapper: StringListMapper,
    private val megaApiGateway: MegaApiGateway,
) {
    suspend operator fun invoke(
        megaNode: MegaNode,
        numVersion: suspend (MegaNode) -> Int,
        requireSerializedData: Boolean = false,
        offline: Offline?,
    ) = if (megaNode.isFolder) {
        throw IllegalStateException("Node is a folder")
    } else {
        val version = (numVersion(megaNode) - 1).coerceAtLeast(0)
        val isAvailableOffline = offline?.let { offlineAvailabilityMapper(megaNode, it) } ?: false
        val isSensitiveInherited = megaApiGateway.isSensitiveInherited(megaNode)
        object : ImageNode {
            override val id = NodeId(megaNode.handle)
            override val name = megaNode.name
            override val size = megaNode.size
            override val label = megaNode.label
            override val nodeLabel = nodeLabelMapper(megaNode.label)
            override val parentId = NodeId(megaNode.parentHandle)
            override val base64Id = megaNode.base64Handle
            override val restoreId = NodeId(megaNode.restoreHandle).takeIf {
                it.longValue != MegaApiJava.INVALID_HANDLE
            }
            override val creationTime = megaNode.creationTime
            override val modificationTime = megaNode.modificationTime
            override val thumbnailPath = null
            override val previewPath = null
            override val fullSizePath = null
            override val type = fileTypeInfoMapper(megaNode.name, megaNode.duration)
            override val isFavourite = megaNode.isFavourite
            override val isMarkedSensitive = megaNode.isMarkedSensitive
            override val isSensitiveInherited = isSensitiveInherited
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
            override val versionCount: Int = version
            override val description: String? = megaNode.description
            override val tags: List<String>? = megaNode.tags?.let { stringListMapper(it) }
        }
    }
}
