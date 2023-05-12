package mega.privacy.android.data.mapper

import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.extensions.getPreviewFileName
import mega.privacy.android.data.extensions.getThumbnailFileName
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.wrapper.DateUtilWrapper
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.Photo
import nz.mega.sdk.MegaNode
import java.io.File
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * The mapper class for converting the data entity to Photo.Image
 */
typealias ImageMapper = (
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards Long?,
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards String,
    @JvmSuppressWildcards Boolean,
    @JvmSuppressWildcards LocalDateTime,
    @JvmSuppressWildcards LocalDateTime,
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards FileTypeInfo,
) -> @JvmSuppressWildcards Photo.Image

internal fun toImage(
    id: Long,
    albumPhotoId: Long? = null,
    parentId: Long,
    name: String,
    isFavourite: Boolean,
    creationTime: LocalDateTime,
    modificationTime: LocalDateTime,
    thumbnailFilePath: String?,
    previewFilePath: String?,
    fileTypeInfo: FileTypeInfo,
) = Photo.Image(
    id = id,
    albumPhotoId = albumPhotoId,
    parentId = parentId,
    name = name,
    isFavourite = isFavourite,
    creationTime = creationTime,
    modificationTime = modificationTime,
    thumbnailFilePath = thumbnailFilePath,
    previewFilePath = previewFilePath,
    fileTypeInfo = fileTypeInfo
)

/**
 * The mapper class for converting the data entity to Photo.Video
 */
typealias VideoMapper = (
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards Long?,
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards String,
    @JvmSuppressWildcards Boolean,
    @JvmSuppressWildcards LocalDateTime,
    @JvmSuppressWildcards LocalDateTime,
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards FileTypeInfo,
) -> @JvmSuppressWildcards Photo.Video

internal fun toVideo(
    id: Long,
    albumPhotoId: Long? = null,
    parentId: Long,
    name: String,
    isFavourite: Boolean,
    creationTime: LocalDateTime,
    modificationTime: LocalDateTime,
    thumbnailFilePath: String?,
    previewFilePath: String?,
    fileTypeInfo: FileTypeInfo,
) = Photo.Video(
    id = id,
    albumPhotoId = albumPhotoId,
    parentId = parentId,
    name = name,
    isFavourite = isFavourite,
    creationTime = creationTime,
    modificationTime = modificationTime,
    thumbnailFilePath = thumbnailFilePath,
    previewFilePath = previewFilePath,
    fileTypeInfo = fileTypeInfo as VideoFileTypeInfo
)

internal class PhotoMapper @Inject constructor(
    private val imageMapper: ImageMapper,
    private val videoMapper: VideoMapper,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
    private val dateUtilFacade: DateUtilWrapper,
    private val cacheFolderFacade: CacheFolderGateway,
) {
    private val thumbnailFolderPath: String? by lazy {
        cacheFolderFacade.getCacheFolder(CacheFolderConstant.THUMBNAIL_FOLDER)?.path
    }

    private val previewFolderPath: String? by lazy {
        cacheFolderFacade.getCacheFolder(CacheFolderConstant.PREVIEW_FOLDER)?.path
    }

    operator fun invoke(node: MegaNode, albumPhotoId: AlbumPhotoId?): Photo? {
        return when (val fileType = fileTypeInfoMapper(node)) {
            is ImageFileTypeInfo -> {
                imageMapper(
                    node.handle,
                    albumPhotoId?.id,
                    node.parentHandle,
                    node.name,
                    node.isFavourite,
                    dateUtilFacade.fromEpoch(node.creationTime),
                    dateUtilFacade.fromEpoch(node.modificationTime),
                    getThumbnailFilePath(node),
                    getPreviewFilePath(node),
                    fileType,
                )
            }

            is VideoFileTypeInfo -> {
                videoMapper(
                    node.handle,
                    albumPhotoId?.id,
                    node.parentHandle,
                    node.name,
                    node.isFavourite,
                    dateUtilFacade.fromEpoch(node.creationTime),
                    dateUtilFacade.fromEpoch(node.modificationTime),
                    getThumbnailFilePath(node),
                    getPreviewFilePath(node),
                    fileType,
                )
            }

            else -> {
                null
            }
        }
    }

    private fun getThumbnailFilePath(node: MegaNode): String? {
        return thumbnailFolderPath?.let { path ->
            "$path${File.separator}${node.getThumbnailFileName()}"
        }
    }

    private fun getPreviewFilePath(node: MegaNode): String? {
        return previewFolderPath?.let { path ->
            "$path${File.separator}${node.getPreviewFileName()}"
        }
    }
}
