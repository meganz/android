package mega.privacy.android.data.mapper.node

import mega.privacy.android.data.extensions.getFileName
import mega.privacy.android.data.extensions.getPreviewFileName
import mega.privacy.android.data.extensions.getThumbnailFileName
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.model.node.DefaultFileNode
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaNode
import java.io.File
import javax.inject.Inject

/**
 * File node mapper
 *
 * @property cacheGateway
 * @property megaApiGateway
 * @property fileTypeInfoMapper
 * @property megaLocalRoomGateway
 * @property fileGateway
 * @constructor Create empty File node mapper
 */
internal class FileNodeMapper @Inject constructor(
    private val cacheGateway: CacheGateway,
    private val megaApiGateway: MegaApiGateway,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
    private val offlineAvailabilityMapper: OfflineAvailabilityMapper
) {
    /**
     * Invoke
     *
     * @param megaNode
     * @param requireSerializedData
     * @return
     */
    suspend operator fun invoke(
        megaNode: MegaNode,
        requireSerializedData: Boolean,
    ): FileNode = DefaultFileNode(
        id = NodeId(megaNode.handle),
        name = megaNode.name,
        size = megaNode.size,
        label = megaNode.label,
        parentId = NodeId(megaNode.parentHandle),
        base64Id = megaNode.base64Handle,
        creationTime = megaNode.creationTime,
        modificationTime = megaNode.modificationTime,
        hasVersion = megaApiGateway.hasVersion(megaNode),
        thumbnailPath = getThumbnailCacheFilePath(
            megaNode,
            cacheGateway.getThumbnailCacheFolder()
        ),
        previewPath = getPreviewCacheFilePath(
            megaNode,
            cacheGateway.getPreviewCacheFolder()
        ),
        fullSizePath = getFullSizeCacheFilePath(
            megaNode,
            cacheGateway.getFullSizeCacheFolder()
        ),
        type = fileTypeInfoMapper(megaNode),
        isFavourite = megaNode.isFavourite,
        exportedData = megaNode.takeIf { megaNode.isExported }?.let {
            ExportedData(it.publicLink, it.publicLinkCreationTime)
        },
        isTakenDown = megaNode.isTakenDown,
        isIncomingShare = megaNode.isInShare,
        fingerprint = megaNode.fingerprint,
        originalFingerprint = megaNode.originalFingerprint,
        isNodeKeyDecrypted = megaNode.isNodeKeyDecrypted,
        hasThumbnail = megaNode.hasThumbnail(),
        hasPreview = megaNode.hasPreview(),
        serializedData = if (requireSerializedData) megaNode.serialize() else null,
        isAvailableOffline = offlineAvailabilityMapper(megaNode)
    )

    private fun getThumbnailCacheFilePath(megaNode: MegaNode, thumbnailFolder: File?): String? =
        thumbnailFolder?.let {
            "$it${File.separator}${megaNode.getThumbnailFileName()}"
        }?.takeUnless { megaNode.isFolder }

    private fun getPreviewCacheFilePath(megaNode: MegaNode, previewFolder: File?): String? =
        previewFolder?.let {
            "$it${File.separator}${megaNode.getPreviewFileName()}"
        }?.takeUnless { megaNode.isFolder }

    private fun getFullSizeCacheFilePath(megaNode: MegaNode, tempFolder: File?): String? =
        tempFolder?.let {
            "$it${File.separator}${megaNode.getFileName()}"
        }?.takeUnless { megaNode.isFolder }
}
