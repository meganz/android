package mega.privacy.android.feature.photos.presentation.albums

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetUserAlbums
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [AlbumsTabViewModel]
 */
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AlbumsTabViewModelTest {
    private lateinit var underTest: AlbumsTabViewModel

    private val getUserAlbums: GetUserAlbums = mock()

    @BeforeEach
    fun setUp() {
        reset(getUserAlbums)
    }

    private fun initViewModel() {
        underTest = AlbumsTabViewModel(
            getUserAlbums = getUserAlbums
        )
    }

    @Test
    fun `test that initial state is correct`() = runTest {
        whenever(getUserAlbums()).thenReturn(flowOf(emptyList()))
        initViewModel()

        underTest.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.albums).isEmpty()
        }
    }

    @Test
    fun `test that albums are loaded successfully`() = runTest {
        val mockAlbums = createMockAlbums()
        whenever(getUserAlbums()).thenReturn(flowOf(mockAlbums))
        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.albums).hasSize(2)
            assertThat(state.albums[0].id).isEqualTo(AlbumId(1L))
            assertThat(state.albums[0].title).isEqualTo("Album 1")
            assertThat(state.albums[1].id).isEqualTo(AlbumId(2L))
            assertThat(state.albums[1].title).isEqualTo("Album 2")
        }
    }

    private fun createMockAlbums(): List<Album.UserAlbum> {
        return listOf(
            Album.UserAlbum(
                id = AlbumId(1L),
                title = "Album 1",
                cover = null,
                creationTime = 0,
                modificationTime = 0,
                isExported = false
            ),
            Album.UserAlbum(
                id = AlbumId(2L),
                title = "Album 2",
                cover = null,
                creationTime = 0,
                modificationTime = 0,
                isExported = false
            )
        )
    }
}
