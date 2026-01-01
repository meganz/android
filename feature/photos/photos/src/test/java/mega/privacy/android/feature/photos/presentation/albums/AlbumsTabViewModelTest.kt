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
import mega.privacy.android.domain.usecase.photos.RemoveAlbumsUseCase
import mega.privacy.android.feature.photos.mapper.AlbumNameValidationExceptionMessageMapper
import mega.privacy.android.feature.photos.mapper.AlbumUiStateMapper
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumSelectionAction
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumUiState
import mega.privacy.android.feature.photos.provider.AlbumsDataProvider
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.AlbumGetMultipleLinksNavKey
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
    private val removeAlbumsUseCase: RemoveAlbumsUseCase = mock()
    private val snackbarEventQueue: SnackbarEventQueue = mock()


    @BeforeEach
    fun setUp() {
        reset(
            mockAlbumsDataProvider,
            validateAndCreateUserAlbumUseCase,
            mockAlbumUiStateMapper,
            albumNameValidationExceptionMessageMapper,
            removeAlbumsUseCase,
            snackbarEventQueue
        )
    }

    private fun initViewModel() {
        underTest = AlbumsTabViewModel(
            albumsProvider = setOf(mockAlbumsDataProvider),
            validateAndCreateUserAlbumUseCase = validateAndCreateUserAlbumUseCase,
            albumUiStateMapper = mockAlbumUiStateMapper,
            albumNameValidationExceptionMessageMapper = albumNameValidationExceptionMessageMapper,
            removeAlbumsUseCase = removeAlbumsUseCase,
            snackbarEventQueue = snackbarEventQueue
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
            albumNameValidationExceptionMessageMapper = albumNameValidationExceptionMessageMapper,
            removeAlbumsUseCase = removeAlbumsUseCase,
            snackbarEventQueue = snackbarEventQueue
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

    @Test
    fun `test that toggleAlbumSelection adds album when not selected`() = runTest {
        whenever(mockAlbumsDataProvider.order).thenReturn(1)
        whenever(mockAlbumsDataProvider.monitorAlbums()).thenReturn(flowOf(emptyList()))
        initViewModel()

        val album = createMockUserAlbum(1L, "Album 1")
        underTest.toggleAlbumSelection(album)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.selectedUserAlbums).contains(album)
            assertThat(state.selectedUserAlbums).hasSize(1)
        }
    }

    @Test
    fun `test that toggleAlbumSelection removes album when already selected`() = runTest {
        whenever(mockAlbumsDataProvider.order).thenReturn(1)
        whenever(mockAlbumsDataProvider.monitorAlbums()).thenReturn(flowOf(emptyList()))
        initViewModel()

        val album = createMockUserAlbum(1L, "Album 1")
        // Select first
        underTest.toggleAlbumSelection(album)
        // Toggle again to deselect
        underTest.toggleAlbumSelection(album)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.selectedUserAlbums).doesNotContain(album)
            assertThat(state.selectedUserAlbums).isEmpty()
        }
    }

    @Test
    fun `test that toggleAlbumSelection handles multiple albums correctly`() = runTest {
        whenever(mockAlbumsDataProvider.order).thenReturn(1)
        whenever(mockAlbumsDataProvider.monitorAlbums()).thenReturn(flowOf(emptyList()))
        initViewModel()

        val album1 = createMockUserAlbum(1L, "Album 1")
        val album2 = createMockUserAlbum(2L, "Album 2")

        underTest.toggleAlbumSelection(album1)
        underTest.toggleAlbumSelection(album2)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.selectedUserAlbums).containsExactly(album1, album2)
        }
    }

    @Test
    fun `test that handleSelectionAction with ManageLink triggers navigation event`() = runTest {
        whenever(mockAlbumsDataProvider.order).thenReturn(1)
        whenever(mockAlbumsDataProvider.monitorAlbums()).thenReturn(flowOf(emptyList()))
        initViewModel()

        val album1 = createMockUserAlbum(1L, "Album 1")
        val album2 = createMockUserAlbum(2L, "Album 2")
        underTest.toggleAlbumSelection(album1)
        underTest.toggleAlbumSelection(album2)

        underTest.handleSelectionAction(AlbumSelectionAction.ManageLink)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.navigationEvent).isEqualTo(
                triggered(
                    AlbumGetMultipleLinksNavKey(
                        albumIds = setOf(1L, 2L),
                        hasSensitiveContent = true
                    )
                )
            )
            assertThat(state.selectedUserAlbums).isEmpty()
        }
    }

    @Test
    fun `test that handleSelectionAction with Delete triggers confirmation event`() = runTest {
        whenever(mockAlbumsDataProvider.order).thenReturn(1)
        whenever(mockAlbumsDataProvider.monitorAlbums()).thenReturn(flowOf(emptyList()))
        initViewModel()

        val album = createMockUserAlbum(1L, "Album 1")
        underTest.toggleAlbumSelection(album)

        underTest.handleSelectionAction(AlbumSelectionAction.Delete)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.deleteAlbumsConfirmationEvent).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that deleteAlbums calls use case and clears selection`() = runTest {
        whenever(mockAlbumsDataProvider.order).thenReturn(1)
        whenever(mockAlbumsDataProvider.monitorAlbums()).thenReturn(flowOf(emptyList()))
        initViewModel()

        val album = createMockUserAlbum(1L, "Album 1")
        underTest.toggleAlbumSelection(album)

        underTest.deleteAlbums()

        verify(removeAlbumsUseCase).invoke(listOf(AlbumId(1L)))

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.selectedUserAlbums).isEmpty()
        }
    }

    @Test
    fun `test that deleteAlbums shows singular snackbar message for single album`() = runTest {
        whenever(mockAlbumsDataProvider.order).thenReturn(1)
        whenever(mockAlbumsDataProvider.monitorAlbums()).thenReturn(flowOf(emptyList()))
        initViewModel()

        val album = createMockUserAlbum(1L, "Album 1")
        underTest.toggleAlbumSelection(album)

        underTest.deleteAlbums()

        verify(snackbarEventQueue).queueMessage(
            mega.privacy.android.shared.resources.R.string.delete_singular_album_confirmation_message,
            "Album 1"
        )
    }

    @Test
    fun `test that deleteAlbums shows plural snackbar message for multiple albums`() = runTest {
        whenever(mockAlbumsDataProvider.order).thenReturn(1)
        whenever(mockAlbumsDataProvider.monitorAlbums()).thenReturn(flowOf(emptyList()))
        initViewModel()

        val album1 = createMockUserAlbum(1L, "Album 1")
        val album2 = createMockUserAlbum(2L, "Album 2")
        underTest.toggleAlbumSelection(album1)
        underTest.toggleAlbumSelection(album2)

        underTest.deleteAlbums()

        verify(snackbarEventQueue).queueMessage(
            mega.privacy.android.shared.resources.R.string.albums_multiple_delete_success_message,
            2
        )
    }

    @Test
    fun `test that resetNavigationEvent resets navigation event to consumed`() = runTest {
        whenever(mockAlbumsDataProvider.order).thenReturn(1)
        whenever(mockAlbumsDataProvider.monitorAlbums()).thenReturn(flowOf(emptyList()))
        initViewModel()

        val album = createMockUserAlbum(1L, "Album 1")
        underTest.toggleAlbumSelection(album)
        underTest.handleSelectionAction(AlbumSelectionAction.ManageLink)

        underTest.resetNavigationEvent()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.navigationEvent).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that resetDeleteAlbumsConfirmationEvent resets event to consumed`() = runTest {
        whenever(mockAlbumsDataProvider.order).thenReturn(1)
        whenever(mockAlbumsDataProvider.monitorAlbums()).thenReturn(flowOf(emptyList()))
        initViewModel()

        val album = createMockUserAlbum(1L, "Album 1")
        underTest.toggleAlbumSelection(album)
        underTest.handleSelectionAction(AlbumSelectionAction.Delete)

        underTest.resetDeleteAlbumsConfirmationEvent()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.deleteAlbumsConfirmationEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that selectAllAlbums selects all user albums`() = runTest {
        val mockAlbums = createMockAlbums()
        val expectedAlbumUiState1 = AlbumUiState(
            mediaAlbum = mockAlbums[0],
            title = LocalizedText.Literal("Album 1"),
            isExported = false
        )
        val expectedAlbumUiState2 = AlbumUiState(
            mediaAlbum = mockAlbums[1],
            title = LocalizedText.Literal("Album 2"),
            isExported = false
        )

        whenever(mockAlbumsDataProvider.order).thenReturn(1)
        whenever(mockAlbumsDataProvider.monitorAlbums()).thenReturn(flowOf(mockAlbums))
        whenever(mockAlbumUiStateMapper(mockAlbums[0])).thenReturn(expectedAlbumUiState1)
        whenever(mockAlbumUiStateMapper(mockAlbums[1])).thenReturn(expectedAlbumUiState2)
        initViewModel()

        underTest.selectAllAlbums()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.selectedUserAlbums).hasSize(2)
            assertThat(state.selectedUserAlbums).containsExactlyElementsIn(mockAlbums)
        }
    }

    @Test
    fun `test that clearAlbumsSelection clears all selected albums`() = runTest {
        whenever(mockAlbumsDataProvider.order).thenReturn(1)
        whenever(mockAlbumsDataProvider.monitorAlbums()).thenReturn(flowOf(emptyList()))
        initViewModel()

        val album1 = createMockUserAlbum(1L, "Album 1")
        val album2 = createMockUserAlbum(2L, "Album 2")
        underTest.toggleAlbumSelection(album1)
        underTest.toggleAlbumSelection(album2)

        underTest.clearAlbumsSelection()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.selectedUserAlbums).isEmpty()
        }
    }

    @Test
    fun `test that areAllAlbumsSelected returns true when all user albums are selected`() =
        runTest {
            val mockAlbums = createMockAlbums()
            val expectedAlbumUiState1 = AlbumUiState(
                mediaAlbum = mockAlbums[0],
                title = LocalizedText.Literal("Album 1"),
                isExported = false
            )
            val expectedAlbumUiState2 = AlbumUiState(
                mediaAlbum = mockAlbums[1],
                title = LocalizedText.Literal("Album 2"),
                isExported = false
            )

            whenever(mockAlbumsDataProvider.order).thenReturn(1)
            whenever(mockAlbumsDataProvider.monitorAlbums()).thenReturn(flowOf(mockAlbums))
            whenever(mockAlbumUiStateMapper(mockAlbums[0])).thenReturn(expectedAlbumUiState1)
            whenever(mockAlbumUiStateMapper(mockAlbums[1])).thenReturn(expectedAlbumUiState2)
            initViewModel()

            underTest.selectAllAlbums()

            assertThat(underTest.areAllAlbumsSelected()).isTrue()
        }

    @Test
    fun `test that areAllAlbumsSelected returns false when not all albums are selected`() =
        runTest {
            val mockAlbums = createMockAlbums()
            val expectedAlbumUiState1 = AlbumUiState(
                mediaAlbum = mockAlbums[0],
                title = LocalizedText.Literal("Album 1"),
                isExported = false
            )
            val expectedAlbumUiState2 = AlbumUiState(
                mediaAlbum = mockAlbums[1],
                title = LocalizedText.Literal("Album 2"),
                isExported = false
            )

            whenever(mockAlbumsDataProvider.order).thenReturn(1)
            whenever(mockAlbumsDataProvider.monitorAlbums()).thenReturn(flowOf(mockAlbums))
            whenever(mockAlbumUiStateMapper(mockAlbums[0])).thenReturn(expectedAlbumUiState1)
            whenever(mockAlbumUiStateMapper(mockAlbums[1])).thenReturn(expectedAlbumUiState2)
            initViewModel()

            underTest.toggleAlbumSelection(mockAlbums[0] as MediaAlbum.User)

            assertThat(underTest.areAllAlbumsSelected()).isFalse()
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
