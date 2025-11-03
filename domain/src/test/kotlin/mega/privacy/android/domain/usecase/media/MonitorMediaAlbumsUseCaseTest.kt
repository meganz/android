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
    private val getUserAlbumCoverPhotoUseCase: GetUserAlbumCoverPhotoUseCase = mock()

    private val testDispatcher = UnconfinedTestDispatcher()


    @BeforeEach
    fun setUp() {
        reset(albumRepository, photosRepository, getUserAlbumCoverPhotoUseCase)
    }

    private fun initUseCase() {
        underTest = MonitorMediaAlbumsUseCase(
            albumRepository = albumRepository,
            photosRepository = photosRepository,
            getUserAlbumCoverPhotoUseCase = getUserAlbumCoverPhotoUseCase,
            defaultDispatcher = testDispatcher,
            applicationScope = TestScope(testDispatcher)
        )
    }

    @Test
    fun `test that albums are updated when user sets change`() = runTest {
        val allUserSets = createMockUserSets()
        val updatedUserSets = allUserSets.dropLast(1)

        whenever(albumRepository.getAllUserSets())
            .thenReturn(allUserSets) // Initial load - getAllUserSets() called in getUserAlbums
            .thenReturn(allUserSets) // Update flow - getAllUserSets() called in getUserAlbums
        whenever(albumRepository.monitorUserSetsUpdate()).thenReturn(flowOf(updatedUserSets))
        whenever(albumRepository.monitorUserSetsContentUpdate()).thenReturn(flowOf(emptyList()))

        initUseCase()

        // Set up mocks for each album
        allUserSets.forEach { set ->
            val isChanged = updatedUserSets.any { it.id == set.id }

            // Initial load: changedSets = allUserSets, so refresh = true for all
            whenever(
                getUserAlbumCoverPhotoUseCase(
                    albumId = AlbumId(set.id),
                    selectedCoverId = set.cover,
                    refresh = true
                )
            ).thenReturn(null)

            // Update flow: changedSets = updatedUserSets, so refresh = isChanged
            whenever(
                getUserAlbumCoverPhotoUseCase(
                    albumId = AlbumId(set.id),
                    selectedCoverId = set.cover,
                    refresh = isChanged
                )
            ).thenReturn(null)
        }

        underTest().test {
            // Emission 1: Initial load from onStart - emits all albums with refresh = true
            val initialResult = awaitItem()
            assertThat(initialResult).hasSize(allUserSets.size)

            // Emission 2: From monitorUserSetsUpdate() flow - emits all albums, refresh = true only for changed sets
            val updatedResult = awaitItem()
            assertThat(updatedResult).hasSize(allUserSets.size)

            // Emission 3: From monitorUserSetsContentUpdate() flow - emits empty list (no content updates)
            awaitItem()

            awaitComplete()
        }
    }

    @Test
    fun `test that user albums without cover photos are handled correctly`() = runTest {
        val userSets = createMockUserSetsWithoutCover()

        whenever(albumRepository.getAllUserSets())
            .thenReturn(userSets) // Initial load
            .thenReturn(userSets) // Update flow (emptyList() as changedSets)
        whenever(albumRepository.monitorUserSetsUpdate()).thenReturn(flowOf(emptyList()))
        whenever(albumRepository.monitorUserSetsContentUpdate()).thenReturn(flowOf(emptyList()))

        initUseCase()

        // Set up mocks for both scenarios
        userSets.forEach { set ->
            // Initial load: changedSets = userSets, so refresh = true
            whenever(
                getUserAlbumCoverPhotoUseCase(
                    albumId = AlbumId(set.id),
                    selectedCoverId = set.cover,
                    refresh = true
                )
            ).thenReturn(null)

            // Update flow: changedSets = emptyList(), so refresh = false
            whenever(
                getUserAlbumCoverPhotoUseCase(
                    albumId = AlbumId(set.id),
                    selectedCoverId = set.cover,
                    refresh = false
                )
            ).thenReturn(null)
        }

        underTest().test {
            // Emission 1: Initial load from onStart - emits all albums with refresh = true
            assertThat(awaitItem()).hasSize(userSets.size)

            // Emission 2: From monitorUserSetsUpdate() flow - emits all albums with refresh = false (no changes)
            val userAlbums = awaitItem().filterIsInstance<MediaAlbum.User>()
            assertThat(userAlbums).hasSize(userSets.size)

            // Emission 3: From monitorUserSetsContentUpdate() flow - emits all albums with refresh = false
            awaitItem()

            awaitComplete()
        }
    }

    @Test
    fun `test that empty user sets are handled correctly`() = runTest {
        whenever(albumRepository.getAllUserSets())
            .thenReturn(emptyList())
            .thenReturn(emptyList())
        whenever(albumRepository.monitorUserSetsUpdate()).thenReturn(flowOf(emptyList()))
        whenever(albumRepository.monitorUserSetsContentUpdate()).thenReturn(flowOf(emptyList()))

        initUseCase()

        underTest().test {
            // Emission 1: Initial load from onStart - no albums (empty list)
            assertThat(awaitItem()).hasSize(0)

            // Emission 2: From monitorUserSetsUpdate() flow - no albums (empty list)
            assertThat(awaitItem()).hasSize(0)

            // Emission 3: From monitorUserSetsContentUpdate() flow - no albums (empty list)
            assertThat(awaitItem()).hasSize(0)

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

    // No extra repository setup needed as covers are provided by getUserAlbumCoverPhotoUseCase
}
