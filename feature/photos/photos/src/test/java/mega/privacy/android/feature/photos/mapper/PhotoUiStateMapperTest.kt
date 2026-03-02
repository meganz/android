package mega.privacy.android.feature.photos.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.feature.photos.model.PhotoUiState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PhotoUiStateMapperTest {

    private lateinit var underTest: PhotoUiStateMapper

    private val durationInSecondsTextMapper: DurationInSecondsTextMapper = mock()

    @BeforeEach
    fun setup() {
        underTest = PhotoUiStateMapper(
            durationInSecondsTextMapper = durationInSecondsTextMapper
        )
    }

    @Test
    fun `test that the image is mapped correctly`() {
        val photo = newImage()

        val actual = underTest(photo = photo)

        val expected = PhotoUiState.Image(
            id = photo.id,
            albumPhotoId = photo.albumPhotoId,
            parentId = photo.parentId,
            name = photo.name,
            isFavourite = photo.isFavourite,
            creationTime = photo.creationTime,
            modificationTime = photo.modificationTime,
            thumbnailFilePath = photo.thumbnailFilePath,
            previewFilePath = photo.previewFilePath,
            fileTypeInfo = photo.fileTypeInfo,
            size = photo.size,
            isTakenDown = photo.isTakenDown,
            isSensitive = photo.isSensitive,
            isSensitiveInherited = photo.isSensitiveInherited,
            base64Id = photo.base64Id,
            restoreId = photo.restoreId,
            label = photo.label,
            nodeLabel = photo.nodeLabel,
            exportedData = photo.exportedData,
            isIncomingShare = photo.isIncomingShare,
            isNodeKeyDecrypted = photo.isNodeKeyDecrypted,
            serializedData = photo.serializedData,
            isAvailableOffline = photo.isAvailableOffline,
            versionCount = photo.versionCount,
            description = photo.description,
            tags = photo.tags
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that the video is mapped correctly`() {
        val photo = newVideo()
        val duration = "02:15"
        whenever(
            durationInSecondsTextMapper(duration = photo.fileTypeInfo.duration)
        ) doReturn duration

        val actual = underTest(photo = photo)

        val expected = PhotoUiState.Video(
            id = photo.id,
            albumPhotoId = photo.albumPhotoId,
            parentId = photo.parentId,
            name = photo.name,
            isFavourite = photo.isFavourite,
            creationTime = photo.creationTime,
            modificationTime = photo.modificationTime,
            thumbnailFilePath = photo.thumbnailFilePath,
            previewFilePath = photo.previewFilePath,
            fileTypeInfo = photo.fileTypeInfo,
            size = photo.size,
            isTakenDown = photo.isTakenDown,
            isSensitive = photo.isSensitive,
            isSensitiveInherited = photo.isSensitiveInherited,
            base64Id = photo.base64Id,
            restoreId = photo.restoreId,
            label = photo.label,
            nodeLabel = photo.nodeLabel,
            exportedData = photo.exportedData,
            isIncomingShare = photo.isIncomingShare,
            isNodeKeyDecrypted = photo.isNodeKeyDecrypted,
            serializedData = photo.serializedData,
            isAvailableOffline = photo.isAvailableOffline,
            versionCount = photo.versionCount,
            description = photo.description,
            tags = photo.tags,
            duration = duration
        )
        assertThat(actual).isEqualTo(expected)
    }

    private fun newImage(
        id: Long = 0L,
        albumPhotoId: Long? = null,
        parentId: Long = 0L,
        name: String = "",
        isFavourite: Boolean = false,
        creationTime: LocalDateTime = LocalDateTime.now(),
        modificationTime: LocalDateTime = LocalDateTime.now(),
        thumbnailFilePath: String? = null,
        previewFilePath: String? = null,
        fileTypeInfo: FileTypeInfo = mock<StaticImageFileTypeInfo>(),
        base64Id: String? = null,
        size: Long = 0L,
        isTakenDown: Boolean = false,
        isSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
        restoreId: NodeId? = null,
        label: Int = 0,
        nodeLabel: NodeLabel? = null,
        exportedData: ExportedData? = null,
        isIncomingShare: Boolean = false,
        isNodeKeyDecrypted: Boolean = false,
        serializedData: String? = null,
        isAvailableOffline: Boolean = false,
        versionCount: Int = 0,
        description: String? = null,
        tags: List<String>? = null,
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
        fileTypeInfo = fileTypeInfo,
        size = size,
        isTakenDown = isTakenDown,
        isSensitive = isSensitive,
        isSensitiveInherited = isSensitiveInherited,
        base64Id = base64Id,
        restoreId = restoreId,
        label = label,
        nodeLabel = nodeLabel,
        exportedData = exportedData,
        isIncomingShare = isIncomingShare,
        isNodeKeyDecrypted = isNodeKeyDecrypted,
        serializedData = serializedData,
        isAvailableOffline = isAvailableOffline,
        versionCount = versionCount,
        description = description,
        tags = tags
    )

    private fun newVideo(
        id: Long = 0L,
        albumPhotoId: Long? = null,
        parentId: Long = 0L,
        name: String = "",
        isFavourite: Boolean = false,
        creationTime: LocalDateTime = LocalDateTime.now(),
        modificationTime: LocalDateTime = LocalDateTime.now(),
        thumbnailFilePath: String? = null,
        previewFilePath: String? = null,
        fileTypeInfo: FileTypeInfo = mock<VideoFileTypeInfo>(),
        base64Id: String? = null,
        size: Long = 0L,
        isTakenDown: Boolean = false,
        isSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
        restoreId: NodeId? = null,
        label: Int = 0,
        nodeLabel: NodeLabel? = null,
        exportedData: ExportedData? = null,
        isIncomingShare: Boolean = false,
        isNodeKeyDecrypted: Boolean = false,
        serializedData: String? = null,
        isAvailableOffline: Boolean = false,
        versionCount: Int = 0,
        description: String? = null,
        tags: List<String>? = null,
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
        fileTypeInfo = fileTypeInfo as VideoFileTypeInfo,
        size = size,
        isTakenDown = isTakenDown,
        isSensitive = isSensitive,
        isSensitiveInherited = isSensitiveInherited,
        base64Id = base64Id,
        restoreId = restoreId,
        label = label,
        nodeLabel = nodeLabel,
        exportedData = exportedData,
        isIncomingShare = isIncomingShare,
        isNodeKeyDecrypted = isNodeKeyDecrypted,
        serializedData = serializedData,
        isAvailableOffline = isAvailableOffline,
        versionCount = versionCount,
        description = description,
        tags = tags
    )
}
