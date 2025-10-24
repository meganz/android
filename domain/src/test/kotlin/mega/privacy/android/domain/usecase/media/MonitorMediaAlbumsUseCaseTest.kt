package mega.privacy.android.domain.usecase.media

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

/**
 * Test class for [MonitorMediaAlbumsUseCase]
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorMediaAlbumsUseCaseTest {
    private lateinit var underTest: MonitorMediaAlbumsUseCase

    private val albumRepository: AlbumRepository = mock()
    private val photosRepository: PhotosRepository = mock()

    private val testDispatcher = UnconfinedTestDispatcher()


    @BeforeEach
    fun setUp() {
        reset(albumRepository, photosRepository)
    }

    private fun initUseCase() {
        underTest = MonitorMediaAlbumsUseCase(
            albumRepository = albumRepository,
            photosRepository = photosRepository,
            defaultDispatcher = testDispatcher,
            applicationScope = TestScope(testDispatcher)
        )
    }

    @Test
    fun `test that albums are updated when user sets change`() = runTest {
        val initialUserSets = createMockUserSets()
        val updatedUserSets = createMockUserSets().drop(1) // Remove one album

        whenever(albumRepository.getAllUserSets()).thenReturn(initialUserSets)
        whenever(albumRepository.monitorUserSetsUpdate()).thenReturn(flowOf(updatedUserSets))

        initUseCase()

        setupAlbumRepositoryMocks(initialUserSets)
        setupAlbumRepositoryMocks(updatedUserSets)

        underTest().test {
            // Initial emission
            val initialResult = awaitItem()
            assertThat(initialResult).hasSize(3) // 3 user albums

            // Updated emission
            val updatedResult = awaitItem()
            assertThat(updatedResult).hasSize(2) // 2 user albums

            awaitComplete()
        }
    }

    @Test
    fun `test that user albums without cover photos are handled correctly`() = runTest {
        val userSets = createMockUserSetsWithoutCover()

        whenever(albumRepository.getAllUserSets()).thenReturn(userSets)
        whenever(albumRepository.monitorUserSetsUpdate()).thenReturn(flowOf(emptyList()))

        initUseCase()

        setupAlbumRepositoryMocks(userSets)

        underTest().test {
            awaitItem()

            val result = awaitItem()
            val userAlbums = result.filterIsInstance<MediaAlbum.User>()
            userAlbums.forEach { userAlbum ->
                assertThat(userAlbum.cover).isNull()
            }

            awaitComplete()
        }
    }

    @Test
    fun `test that empty user sets are handled correctly`() = runTest {
        whenever(albumRepository.getAllUserSets()).thenReturn(emptyList())
        whenever(albumRepository.monitorUserSetsUpdate()).thenReturn(flowOf(emptyList()))

        initUseCase()

        underTest().test {
            awaitItem()
            val result = awaitItem()
            assertThat(result).hasSize(0) // No user albums

            awaitComplete()
        }
    }


    private fun createMockUserSets(): List<UserSet> {
        return listOf(
            mock<UserSet> {
                on { id }.thenReturn(1L)
                on { name }.thenReturn("Album 1")
                on { cover }.thenReturn(null)
                on { creationTime }.thenReturn(1000L)
                on { modificationTime }.thenReturn(2000L)
                on { isExported }.thenReturn(false)
            },
            mock<UserSet> {
                on { id }.thenReturn(2L)
                on { name }.thenReturn("Album 2")
                on { cover }.thenReturn(null)
                on { creationTime }.thenReturn(1001L)
                on { modificationTime }.thenReturn(2001L)
                on { isExported }.thenReturn(true)
            },
            mock<UserSet> {
                on { id }.thenReturn(3L)
                on { name }.thenReturn("Album 3")
                on { cover }.thenReturn(null)
                on { creationTime }.thenReturn(1002L)
                on { modificationTime }.thenReturn(2002L)
                on { isExported }.thenReturn(false)
            }
        )
    }

    private fun createMockUserSetsWithoutCover(): List<UserSet> {
        return listOf(
            mock<UserSet> {
                on { id }.thenReturn(1L)
                on { name }.thenReturn("Album 1")
                on { cover }.thenReturn(null)
                on { creationTime }.thenReturn(1000L)
                on { modificationTime }.thenReturn(2000L)
                on { isExported }.thenReturn(false)
            },
            mock<UserSet> {
                on { id }.thenReturn(2L)
                on { name }.thenReturn("Album 2")
                on { cover }.thenReturn(null)
                on { creationTime }.thenReturn(1001L)
                on { modificationTime }.thenReturn(2001L)
                on { isExported }.thenReturn(true)
            },
            mock<UserSet> {
                on { id }.thenReturn(3L)
                on { name }.thenReturn("Album 3")
                on { cover }.thenReturn(null)
                on { creationTime }.thenReturn(1002L)
                on { modificationTime }.thenReturn(2002L)
                on { isExported }.thenReturn(false)
            }
        )
    }

    private fun createMockUserSetsWithCover(): List<UserSet> {
        return listOf(
            mock<UserSet> {
                on { id }.thenReturn(1L)
                on { name }.thenReturn("Album 1")
                on { cover }.thenReturn(123L) // Has cover
                on { creationTime }.thenReturn(1000L)
                on { modificationTime }.thenReturn(2000L)
                on { isExported }.thenReturn(false)
            },
            mock<UserSet> {
                on { id }.thenReturn(2L)
                on { name }.thenReturn("Album 2")
                on { cover }.thenReturn(null) // No cover
                on { creationTime }.thenReturn(1001L)
                on { modificationTime }.thenReturn(2001L)
                on { isExported }.thenReturn(true)
            },
            mock<UserSet> {
                on { id }.thenReturn(3L)
                on { name }.thenReturn("Album 3")
                on { cover }.thenReturn(null) // No cover
                on { creationTime }.thenReturn(1002L)
                on { modificationTime }.thenReturn(2002L)
                on { isExported }.thenReturn(false)
            }
        )
    }

    private fun createMockUserAlbums(): List<MediaAlbum.User> {
        return listOf(
            MediaAlbum.User(
                id = AlbumId(1L),
                title = "Album 1",
                cover = null,
                creationTime = 1000L,
                modificationTime = 2000L,
                isExported = false
            ),
            MediaAlbum.User(
                id = AlbumId(2L),
                title = "Album 2",
                cover = null,
                creationTime = 1001L,
                modificationTime = 2001L,
                isExported = true
            ),
            MediaAlbum.User(
                id = AlbumId(3L),
                title = "Album 3",
                cover = null,
                creationTime = 1002L,
                modificationTime = 2002L,
                isExported = false
            )
        )
    }

    private fun createMockPhoto(): Photo.Image {
        return mock<Photo.Image> {
            on { id }.thenReturn(1L)
            on { name }.thenReturn("test_photo.jpg")
            on { modificationTime }.thenReturn(LocalDateTime.now())
        }
    }

    private fun createMockAlbumPhotoId(id: Long, nodeId: Long, albumId: Long = 1L): AlbumPhotoId {
        return AlbumPhotoId(
            id = id,
            nodeId = NodeId(nodeId),
            albumId = AlbumId(albumId)
        )
    }

    private suspend fun setupAlbumRepositoryMocks(userSets: List<UserSet>) {
        userSets.forEach { userSet ->
            whenever(albumRepository.getAlbumElementIDs(AlbumId(userSet.id)))
                .thenReturn(emptyList())
        }
    }
}
