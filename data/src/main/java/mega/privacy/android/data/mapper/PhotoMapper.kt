package mega.privacy.android.data.mapper

import mega.privacy.android.data.extensions.getPreviewFileName
import mega.privacy.android.data.extensions.getThumbnailFileName
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.node.label.NodeLabelMapper
import mega.privacy.android.data.wrapper.DateUtilWrapper
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import java.io.File
import java.time.LocalDateTime
import javax.inject.Inject

data class PhotoMapperArgs(
    val id: Long,
    val albumPhotoId: Long? = null,
    val parentId: Long,
    val name: String,
    val isFavourite: Boolean,
    val creationTime: LocalDateTime,
    val modificationTime: LocalDateTime,
    val thumbnailFilePath: String?,
    val previewFilePath: String?,
    val fileTypeInfo: FileTypeInfo,
    val size: Long,
    val isTakenDown: Boolean,
    val isSensitive: Boolean,
    val isSensitiveInherited: Boolean,
    val base64Id: String,
    val restoreId: NodeId? = null,
    val label: Int = 0,
    val nodeLabel: NodeLabel? = null,
    val exportedData: ExportedData? = null,
    val isIncomingShare: Boolean = false,
    val isNodeKeyDecrypted: Boolean = false,
    val serializedData: String? = null,
    val isAvailableOffline: Boolean = false,
    val versionCount: Int = 0,
    val description: String? = null,
    val tags: List<String>? = null,
)

internal class PhotoMapper @Inject constructor(
    private val fileTypeInfoMapper: FileTypeInfoMapper,
    private val dateUtilFacade: DateUtilWrapper,
    private val thumbnailPreviewRepository: ThumbnailPreviewRepository,
    private val megaApiGateway: MegaApiGateway,
    private val nodeLabelMapper: NodeLabelMapper,
    private val stringListMapper: StringListMapper,
) {
    suspend operator fun invoke(
        node: MegaNode,
        albumPhotoId: AlbumPhotoId?,
        requireSerializedData: Boolean = false,
        isAvailableOffline: Boolean = false,
    ): Photo? {
        return when (val fileType = fileTypeInfoMapper(node.name, node.duration)) {
            is ImageFileTypeInfo -> {
                toImage(
                    args = PhotoMapperArgs(
                        id = node.handle,
                        albumPhotoId = albumPhotoId?.id,
                        parentId = node.parentHandle,
                        name = node.name,
                        isFavourite = node.isFavourite,
                        creationTime = dateUtilFacade.fromEpoch(node.creationTime),
                        modificationTime = dateUtilFacade.fromEpoch(node.modificationTime),
                        thumbnailFilePath = getThumbnailFilePath(node),
                        previewFilePath = getPreviewFilePath(node),
                        fileTypeInfo = fileType,
                        size = node.size,
                        isTakenDown = node.isTakenDown,
                        isSensitive = node.isMarkedSensitive,
                        isSensitiveInherited = megaApiGateway.isSensitiveInherited(node),
                        base64Id = node.base64Handle,
                        restoreId = NodeId(node.restoreHandle).takeIf {
                            it.longValue != MegaApiJava.INVALID_HANDLE
                        },
                        label = node.label,
                        nodeLabel = nodeLabelMapper(node.label),
                        exportedData = node.takeIf { node.isExported }?.let {
                            ExportedData(it.publicLink, it.publicLinkCreationTime)
                        },
                        isIncomingShare = node.isInShare,
                        isNodeKeyDecrypted = node.isNodeKeyDecrypted,
                        serializedData = if (requireSerializedData) node.serialize() else null,
                        isAvailableOffline = isAvailableOffline,
                        versionCount = (megaApiGateway.getNumVersions(node) - 1).coerceAtLeast(0),
                        description = node.description,
                        tags = node.tags?.let { stringListMapper(it) }
                    )
                )
            }

            is VideoFileTypeInfo -> {
                toVideo(
                    args = PhotoMapperArgs(
                        id = node.handle,
                        albumPhotoId = albumPhotoId?.id,
                        parentId = node.parentHandle,
                        name = node.name,
                        isFavourite = node.isFavourite,
                        creationTime = dateUtilFacade.fromEpoch(node.creationTime),
                        modificationTime = dateUtilFacade.fromEpoch(node.modificationTime),
                        thumbnailFilePath = getThumbnailFilePath(node),
                        previewFilePath = getPreviewFilePath(node),
                        fileTypeInfo = fileType,
                        size = node.size,
                        isTakenDown = node.isTakenDown,
                        isSensitive = node.isMarkedSensitive,
                        isSensitiveInherited = megaApiGateway.isSensitiveInherited(node),
                        base64Id = node.base64Handle,
                        restoreId = NodeId(node.restoreHandle).takeIf {
                            it.longValue != MegaApiJava.INVALID_HANDLE
                        },
                        label = node.label,
                        nodeLabel = nodeLabelMapper(node.label),
                        exportedData = node.takeIf { node.isExported }?.let {
                            ExportedData(it.publicLink, it.publicLinkCreationTime)
                        },
                        isIncomingShare = node.isInShare,
                        isNodeKeyDecrypted = node.isNodeKeyDecrypted,
                        serializedData = if (requireSerializedData) node.serialize() else null,
                        isAvailableOffline = isAvailableOffline,
                        versionCount = (megaApiGateway.getNumVersions(node) - 1).coerceAtLeast(0),
                        description = node.description,
                        tags = node.tags?.let { stringListMapper(it) }
                    )
                )
            }

            else -> {
                null
            }
        }
    }

    private fun toImage(args: PhotoMapperArgs) = Photo.Image(
        id = args.id,
        albumPhotoId = args.albumPhotoId,
        parentId = args.parentId,
        name = args.name,
        isFavourite = args.isFavourite,
        creationTime = args.creationTime,
        modificationTime = args.modificationTime,
        thumbnailFilePath = args.thumbnailFilePath,
        previewFilePath = args.previewFilePath,
        fileTypeInfo = args.fileTypeInfo,
        size = args.size,
        isTakenDown = args.isTakenDown,
        isSensitive = args.isSensitive,
        isSensitiveInherited = args.isSensitiveInherited,
        base64Id = args.base64Id,
        restoreId = args.restoreId,
        label = args.label,
        nodeLabel = args.nodeLabel,
        exportedData = args.exportedData,
        isIncomingShare = args.isIncomingShare,
        isNodeKeyDecrypted = args.isNodeKeyDecrypted,
        serializedData = args.serializedData,
        isAvailableOffline = args.isAvailableOffline,
        versionCount = args.versionCount,
        description = args.description,
        tags = args.tags
    )

    private fun toVideo(args: PhotoMapperArgs) = Photo.Video(
        id = args.id,
        albumPhotoId = args.albumPhotoId,
        parentId = args.parentId,
        name = args.name,
        isFavourite = args.isFavourite,
        creationTime = args.creationTime,
        modificationTime = args.modificationTime,
        thumbnailFilePath = args.thumbnailFilePath,
        previewFilePath = args.previewFilePath,
        fileTypeInfo = args.fileTypeInfo as VideoFileTypeInfo,
        size = args.size,
        isTakenDown = args.isTakenDown,
        isSensitive = args.isSensitive,
        isSensitiveInherited = args.isSensitiveInherited,
        base64Id = args.base64Id,
        restoreId = args.restoreId,
        label = args.label,
        nodeLabel = args.nodeLabel,
        exportedData = args.exportedData,
        isIncomingShare = args.isIncomingShare,
        isNodeKeyDecrypted = args.isNodeKeyDecrypted,
        serializedData = args.serializedData,
        isAvailableOffline = args.isAvailableOffline,
        versionCount = args.versionCount,
        description = args.description,
        tags = args.tags
    )

    private suspend fun getThumbnailFilePath(node: MegaNode): String? {
        return thumbnailPreviewRepository.getThumbnailCacheFolderPath()?.let { path ->
            "$path${File.separator}${node.getThumbnailFileName()}"
        }
    }

    private suspend fun getPreviewFilePath(node: MegaNode): String? {
        return thumbnailPreviewRepository.getPreviewCacheFolderPath()?.let { path ->
            "$path${File.separator}${node.getPreviewFileName()}"
        }
    }
}
