package mega.privacy.android.feature.photos.mapper

import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.media.SystemAlbum
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.thumbnail.MediaThumbnailRequest
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumUiState
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AlbumUiStateMapperTest {

    private lateinit var underTest: AlbumUiStateMapper

    @BeforeAll
    fun setup() {
        underTest = AlbumUiStateMapper()
    }

    @Test
    fun `test that system album is mapped correctly without cover`() {
        val albumNameResId = 12345
        val systemAlbum = createMockSystemAlbum(albumNameResId)
        val mediaAlbum = MediaAlbum.System(
            id = systemAlbum,
            cover = null
        )

        val actual = underTest(mediaAlbum)

        val expected = AlbumUiState(
            mediaAlbum = mediaAlbum,
            title = LocalizedText.StringRes(albumNameResId),
            isExported = false,
            cover = null,
            isCoverSensitive = false,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that system album is mapped correctly with cover`() {
        val albumNameResId = 67890
        val systemAlbum = createMockSystemAlbum(albumNameResId)
        val coverNode = createMockNode(
            id = 100L,
            thumbnailPath = "thumb/100",
            previewPath = "preview/100",
            extension = "jpg",
        )
        val mediaAlbum = MediaAlbum.System(
            id = systemAlbum,
            cover = coverNode
        )

        val actual = underTest(mediaAlbum)

        val expected = AlbumUiState(
            mediaAlbum = mediaAlbum,
            title = LocalizedText.StringRes(albumNameResId),
            isExported = false,
            cover = MediaThumbnailRequest(
                id = 100L,
                isPreview = false,
                thumbnailFilePath = "thumb/100",
                previewFilePath = "preview/100",
                isPublicNode = false,
                fileExtension = "jpg",
            ),
            isCoverSensitive = false,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that cover is sensitive when the cover node is marked sensitive`() {
        val systemAlbum = createMockSystemAlbum(1)
        val coverNode = createMockNode(id = 1L, isMarkedSensitive = true)
        val mediaAlbum = MediaAlbum.System(id = systemAlbum, cover = coverNode)

        val actual = underTest(mediaAlbum)

        assertThat(actual.isCoverSensitive).isTrue()
    }

    @Test
    fun `test that cover is sensitive when the cover node is sensitive inherited`() {
        val systemAlbum = createMockSystemAlbum(1)
        val coverNode = createMockNode(id = 1L, isSensitiveInherited = true)
        val mediaAlbum = MediaAlbum.System(id = systemAlbum, cover = coverNode)

        val actual = underTest(mediaAlbum)

        assertThat(actual.isCoverSensitive).isTrue()
    }

    @Test
    fun `test that user album is mapped correctly without cover`() {
        val albumId = AlbumId(123L)
        val title = "My Album"
        val mediaAlbum = MediaAlbum.User(
            id = albumId,
            title = title,
            creationTime = 1000L,
            modificationTime = 2000L,
            isExported = false,
            cover = null
        )

        val actual = underTest(mediaAlbum)

        val expected = AlbumUiState(
            mediaAlbum = mediaAlbum,
            title = LocalizedText.Literal(title),
            isExported = false,
            cover = null,
            isCoverSensitive = false,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that user album is mapped correctly with cover`() {
        val albumId = AlbumId(456L)
        val title = "Vacation Photos"
        val coverNode = createMockNode(
            id = 200L,
            thumbnailPath = "thumb/200",
            previewPath = "preview/200",
            extension = "png",
        )
        val mediaAlbum = MediaAlbum.User(
            id = albumId,
            title = title,
            creationTime = 1500L,
            modificationTime = 2500L,
            isExported = true,
            cover = coverNode
        )

        val actual = underTest(mediaAlbum)

        val expected = AlbumUiState(
            mediaAlbum = mediaAlbum,
            title = LocalizedText.Literal(title),
            isExported = true,
            cover = MediaThumbnailRequest(
                id = 200L,
                isPreview = false,
                thumbnailFilePath = "thumb/200",
                previewFilePath = "preview/200",
                isPublicNode = false,
                fileExtension = "png",
            ),
            isCoverSensitive = false,
        )
        assertThat(actual).isEqualTo(expected)
    }

    private fun createMockSystemAlbum(albumNameResId: Int): SystemAlbum =
        object : SystemAlbum {
            override val albumNameResId = albumNameResId
            override val hideWhenEmpty: Boolean = true
            override val mediaTimelineFilter: MediaTimelineFilter = MediaTimelineFilter(
                granularity = MediaTimelineFilter.Granularity.Day,
                category = MediaTimelineFilter.Category.All,
                location = MediaTimelineFilter.Location.CloudDriveAndVault,
                sensitivity = MediaTimelineFilter.Sensitivity.ShowAll,
            )
        }

    private fun createMockNode(
        id: Long = 0L,
        thumbnailPath: String? = null,
        previewPath: String? = null,
        extension: String = "jpg",
        isMarkedSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
    ): TypedFileNode {
        val fileType = StaticImageFileTypeInfo(mimeType = "image/jpeg", extension = extension)
        return mock {
            on { this.id }.thenReturn(NodeId(id))
            on { this.thumbnailPath }.thenReturn(thumbnailPath)
            on { this.previewPath }.thenReturn(previewPath)
            on { this.type }.thenReturn(fileType)
            on { this.isMarkedSensitive }.thenReturn(isMarkedSensitive)
            on { this.isSensitiveInherited }.thenReturn(isSensitiveInherited)
        }
    }
}
