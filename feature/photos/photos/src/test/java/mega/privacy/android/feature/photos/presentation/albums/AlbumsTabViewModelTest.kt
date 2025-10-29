package mega.privacy.android.feature.photos.presentation.albums

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.usecase.media.CreateUserAlbumUseCase
import mega.privacy.android.feature.photos.provider.AlbumsDataProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class for [AlbumsTabViewModel]
 */
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class AlbumsTabViewModelTest {
    private lateinit var underTest: AlbumsTabViewModel

    private val mockAlbumsDataProvider: AlbumsDataProvider = mock()
    private val mockCreateUserAlbumUseCase: CreateUserAlbumUseCase = mock()


    @BeforeEach
    fun setUp() {
        reset(mockAlbumsDataProvider, mockCreateUserAlbumUseCase)
    }

    private fun initViewModel() {
        underTest = AlbumsTabViewModel(
            albumsProvider = setOf(mockAlbumsDataProvider),
            createUserAlbumUseCase = mockCreateUserAlbumUseCase
        )
    }

    @Test
    fun `test that initial state is correct`() = runTest {
        whenever(mockAlbumsDataProvider.order).thenReturn(1)
        whenever(mockAlbumsDataProvider.monitorAlbums()).thenReturn(flowOf(emptyList()))
        initViewModel()

        underTest.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.albums).isEmpty()
        }
    }

    @Test
    fun `test that albums are loaded successfully`() = runTest {
        val mockAlbums = createMockAlbums()
        whenever(mockAlbumsDataProvider.order).thenReturn(1)
        whenever(mockAlbumsDataProvider.monitorAlbums()).thenReturn(flowOf(mockAlbums))
        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.albums).hasSize(2)
            assertThat(state.albums[0]).isInstanceOf(MediaAlbum.User::class.java)
            assertThat((state.albums[0] as MediaAlbum.User).id).isEqualTo(AlbumId(1L))
            assertThat((state.albums[0] as MediaAlbum.User).title).isEqualTo("Album 1")
            assertThat(state.albums[1]).isInstanceOf(MediaAlbum.User::class.java)
            assertThat((state.albums[1] as MediaAlbum.User).id).isEqualTo(AlbumId(2L))
            assertThat((state.albums[1] as MediaAlbum.User).title).isEqualTo("Album 2")
        }
    }

    @Test
    fun `test that multiple providers are combined correctly`() = runTest {
        val mockProvider1: AlbumsDataProvider = mock()
        val mockProvider2: AlbumsDataProvider = mock()

        val albums1 = listOf(createMockUserAlbum(1L, "Album 1"))
        val albums2 = listOf(createMockUserAlbum(2L, "Album 2"))

        whenever(mockProvider1.order).thenReturn(1)
        whenever(mockProvider1.monitorAlbums()).thenReturn(flowOf(albums1))
        whenever(mockProvider2.order).thenReturn(2)
        whenever(mockProvider2.monitorAlbums()).thenReturn(flowOf(albums2))

        underTest = AlbumsTabViewModel(
            albumsProvider = setOf(mockProvider1, mockProvider2),
            createUserAlbumUseCase = mockCreateUserAlbumUseCase
        )

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.albums).hasSize(2)
            // Albums should be sorted by order (provider1 first, then provider2)
            assertThat((state.albums[0] as MediaAlbum.User).title).isEqualTo("Album 1")
            assertThat((state.albums[1] as MediaAlbum.User).title).isEqualTo("Album 2")
        }
    }

    @Test
    fun `test that add new albums calls the use case`() = runTest {
        val expectedName = "New Album"

        initViewModel()

        underTest.addNewAlbum(expectedName)

        verify(mockCreateUserAlbumUseCase).invoke(expectedName)
    }

    private fun createMockAlbums(): List<MediaAlbum> {
        return listOf(
            createMockUserAlbum(1L, "Album 1"),
            createMockUserAlbum(2L, "Album 2")
        )
    }

    private fun createMockUserAlbum(id: Long, title: String): MediaAlbum.User {
        return MediaAlbum.User(
            id = AlbumId(id),
            title = title,
            cover = null,
            creationTime = 0,
            modificationTime = 0,
            isExported = false
        )
    }
}
