package mega.privacy.android.feature.photos.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.feature.photos.model.PhotoUiState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PhotoMapperTest {

    private lateinit var underTest: PhotoMapper

    @BeforeEach
    fun setup() {
        underTest = PhotoMapper()
    }

    @Test
    fun `test that the image is mapped correctly`() {
        val photoUiState = newImage()

        val actual = underTest(photoUiState = photoUiState)

        val expected = Photo.Image(
            id = photoUiState.id,
            albumPhotoId = photoUiState.albumPhotoId,
            parentId = photoUiState.parentId,
            name = photoUiState.name,
            isFavourite = photoUiState.isFavourite,
            creationTime = photoUiState.creationTime,
            modificationTime = photoUiState.modificationTime,
            thumbnailFilePath = photoUiState.thumbnailFilePath,
            previewFilePath = photoUiState.previewFilePath,
            fileTypeInfo = photoUiState.fileTypeInfo,
            size = photoUiState.size,
            isTakenDown = photoUiState.isTakenDown,
            isSensitive = photoUiState.isSensitive,
            isSensitiveInherited = photoUiState.isSensitiveInherited,
            base64Id = photoUiState.base64Id,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that the video is mapped correctly`() {
        val photoUiState = newVideo()

        val actual = underTest(photoUiState = photoUiState)

        val expected = Photo.Video(
            id = photoUiState.id,
            albumPhotoId = photoUiState.albumPhotoId,
            parentId = photoUiState.parentId,
            name = photoUiState.name,
            isFavourite = photoUiState.isFavourite,
            creationTime = photoUiState.creationTime,
            modificationTime = photoUiState.modificationTime,
            thumbnailFilePath = photoUiState.thumbnailFilePath,
            previewFilePath = photoUiState.previewFilePath,
            fileTypeInfo = photoUiState.fileTypeInfo,
            size = photoUiState.size,
            isTakenDown = photoUiState.isTakenDown,
            isSensitive = photoUiState.isSensitive,
            isSensitiveInherited = photoUiState.isSensitiveInherited,
            base64Id = photoUiState.base64Id,
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
    ) = PhotoUiState.Image(
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
    ) = PhotoUiState.Video(
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
    )
}
