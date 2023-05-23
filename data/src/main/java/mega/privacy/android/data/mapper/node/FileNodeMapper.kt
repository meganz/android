package mega.privacy.android.data.mapper.node

import mega.privacy.android.data.extensions.getFileName
import mega.privacy.android.data.extensions.getPreviewFileName
import mega.privacy.android.data.extensions.getThumbnailFileName
import mega.privacy.android.data.gateway.CacheFolderGateway
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
 * @property cacheFolderGateway
 * @property megaApiGateway
 * @property fileTypeInfoMapper
 * @constructor Create empty File node mapper
 */
internal class FileNodeMapper @Inject constructor(
    private val cacheFolderGateway: CacheFolderGateway,
    private val megaApiGateway: MegaApiGateway,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
) {
    /**
     * Invoke
     *
     * @param megaNode
     * @return
     */
    suspend operator fun invoke(megaNode: MegaNode): FileNode = DefaultFileNode(
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
            cacheFolderGateway.getThumbnailCacheFolder()
        ),
        previewPath = getPreviewCacheFilePath(
            megaNode,
            cacheFolderGateway.getPreviewCacheFolder()
        ),
        fullSizePath = getFullSizeCacheFilePath(
            megaNode,
            cacheFolderGateway.getFullSizeCacheFolder()
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
