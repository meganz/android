package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetDefaultAlbumPhotosTest {
    lateinit var underTest: GetDefaultAlbumPhotos
    private val photosRepository = mock<PhotosRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetDefaultAlbumPhotos(
            photosRepository = photosRepository,
            defaultDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `test that current photos are returned`() = runTest {
        whenever(photosRepository.monitorPhotos()).thenReturn(
            flowOf(
                listOf(
                    createVideo(id = 1L)
                )
            )
        )

        underTest(listOf()).test {
            awaitItem()
            awaitComplete()
        }
    }

    private fun createVideo(
        id: Long,
        parentId: Long = 0L,
        isFavourite: Boolean = false,
        modificationTime: LocalDateTime = LocalDateTime.now(),
    ): Photo {
        return Photo.Video(
            id = id,
            parentId = parentId,
            name = "",
            isFavourite = isFavourite,
            creationTime = LocalDateTime.now(),
            modificationTime = modificationTime,
            thumbnailFilePath = "thumbnailFilePath",
            previewFilePath = "previewFilePath",
            fileTypeInfo = VideoFileTypeInfo("", "", duration = 123)
        )
    }
}
