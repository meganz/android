package mega.privacy.android.feature.photos.presentation.albums

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.exception.account.AlbumNameValidationException
import mega.privacy.android.domain.usecase.media.ValidateAndCreateUserAlbumUseCase
import mega.privacy.android.feature.photos.mapper.AlbumNameValidationExceptionMessageMapper
import mega.privacy.android.feature.photos.mapper.AlbumUiStateMapper
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumUiState
import mega.privacy.android.feature.photos.provider.AlbumsDataProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
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
    private val validateAndCreateUserAlbumUseCase: ValidateAndCreateUserAlbumUseCase = mock()
    private val mockAlbumUiStateMapper: AlbumUiStateMapper = mock()
    private val albumNameValidationExceptionMessageMapper: AlbumNameValidationExceptionMessageMapper =
        mock()


    @BeforeEach
    fun setUp() {
        reset(
            mockAlbumsDataProvider,
            validateAndCreateUserAlbumUseCase,
            mockAlbumUiStateMapper,
            albumNameValidationExceptionMessageMapper
        )
    }

    private fun initViewModel() {
        underTest = AlbumsTabViewModel(
            albumsProvider = setOf(mockAlbumsDataProvider),
            validateAndCreateUserAlbumUseCase = validateAndCreateUserAlbumUseCase,
            albumUiStateMapper = mockAlbumUiStateMapper,
            albumNameValidationExceptionMessageMapper = albumNameValidationExceptionMessageMapper
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
        val expectedAlbumUiState1 =
            AlbumUiState(
                mediaAlbum = mockAlbums[0],
                title = LocalizedText.Literal("Album 1"),
                isExported = false
            )
        val expectedAlbumUiState2 =
            AlbumUiState(
                mediaAlbum = mockAlbums[1],
                title = LocalizedText.Literal("Album 2"),
                isExported = false
            )

        whenever(mockAlbumsDataProvider.order).thenReturn(1)
        whenever(mockAlbumsDataProvider.monitorAlbums()).thenReturn(flowOf(mockAlbums))
        whenever(mockAlbumUiStateMapper(mockAlbums[0])).thenReturn(expectedAlbumUiState1)
        whenever(mockAlbumUiStateMapper(mockAlbums[1])).thenReturn(expectedAlbumUiState2)
        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.albums).hasSize(2)
            assertThat(state.albums[0]).isEqualTo(expectedAlbumUiState1)
            assertThat(state.albums[1]).isEqualTo(expectedAlbumUiState2)
            verify(mockAlbumUiStateMapper).invoke(mockAlbums[0])
            verify(mockAlbumUiStateMapper).invoke(mockAlbums[1])
        }
    }

    @Test
    fun `test that multiple providers are combined correctly`() = runTest {
        val mockProvider1: AlbumsDataProvider = mock()
        val mockProvider2: AlbumsDataProvider = mock()

        val albums1 = listOf(createMockUserAlbum(1L, "Album 1"))
        val albums2 = listOf(createMockUserAlbum(2L, "Album 2"))
        val expectedAlbumUiState1 =
            AlbumUiState(
                mediaAlbum = albums1[0],
                title = LocalizedText.Literal("Album 1"),
                isExported = false
            )
        val expectedAlbumUiState2 =
            AlbumUiState(
                mediaAlbum = albums2[0],
                title = LocalizedText.Literal("Album 2"),
                isExported = false
            )

        whenever(mockProvider1.order).thenReturn(1)
        whenever(mockProvider1.monitorAlbums()).thenReturn(flowOf(albums1))
        whenever(mockProvider2.order).thenReturn(2)
        whenever(mockProvider2.monitorAlbums()).thenReturn(flowOf(albums2))
        whenever(mockAlbumUiStateMapper(albums1[0])).thenReturn(expectedAlbumUiState1)
        whenever(mockAlbumUiStateMapper(albums2[0])).thenReturn(expectedAlbumUiState2)

        underTest = AlbumsTabViewModel(
            albumsProvider = setOf(mockProvider1, mockProvider2),
            validateAndCreateUserAlbumUseCase = validateAndCreateUserAlbumUseCase,
            albumUiStateMapper = mockAlbumUiStateMapper,
            albumNameValidationExceptionMessageMapper = albumNameValidationExceptionMessageMapper
        )

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.albums).hasSize(2)
            // Albums should be sorted by order (provider1 first, then provider2)
            assertThat(state.albums[0]).isEqualTo(expectedAlbumUiState1)
            assertThat(state.albums[1]).isEqualTo(expectedAlbumUiState2)
            verify(mockAlbumUiStateMapper).invoke(albums1[0])
            verify(mockAlbumUiStateMapper).invoke(albums2[0])
        }
    }

    @Test
    fun `test that add new albums calls the use case and update state on success`() = runTest {
        val expectedName = "New Album"
        whenever(validateAndCreateUserAlbumUseCase(expectedName)).thenReturn(AlbumId(1))

        initViewModel()

        underTest.addNewAlbum(expectedName)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.addNewAlbumErrorMessage).isEqualTo(consumed())
            assertThat(state.addNewAlbumSuccessEvent).isEqualTo(triggered(AlbumId(1)))
        }

        verify(validateAndCreateUserAlbumUseCase).invoke(expectedName)
    }

    @Test
    fun `test that on validation exception should update error message`() = runTest {
        val errorMessage = "errorMessage"
        whenever(albumNameValidationExceptionMessageMapper(any())).thenReturn(errorMessage)
        whenever(validateAndCreateUserAlbumUseCase(any())).thenAnswer {
            throw AlbumNameValidationException.InvalidCharacters("/")
        }

        initViewModel()

        underTest.addNewAlbum("album")

        underTest.uiState.test {
            assertThat(awaitItem().addNewAlbumErrorMessage).isEqualTo(triggered(errorMessage))
        }
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
