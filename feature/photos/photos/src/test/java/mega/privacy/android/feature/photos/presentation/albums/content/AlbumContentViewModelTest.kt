package mega.privacy.android.feature.photos.presentation.albums.content

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.media.SystemAlbum
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.PhotoPredicate
import mega.privacy.android.domain.exception.account.AlbumNameValidationException
import mega.privacy.android.domain.usecase.GetAlbumPhotosUseCase
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetDefaultAlbumPhotos
import mega.privacy.android.domain.usecase.GetNodeListByIdsUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.ObserveAlbumPhotosAddingProgress
import mega.privacy.android.domain.usecase.ObserveAlbumPhotosRemovingProgress
import mega.privacy.android.domain.usecase.UpdateAlbumPhotosAddingProgressCompleted
import mega.privacy.android.domain.usecase.UpdateAlbumPhotosRemovingProgressCompleted
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.favourites.RemoveFavouritesUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.media.GetUserAlbumCoverPhotoUseCase
import mega.privacy.android.domain.usecase.media.MonitorUserAlbumByIdUseCase
import mega.privacy.android.domain.usecase.media.ValidateAndUpdateUserAlbumUseCase
import mega.privacy.android.domain.usecase.photos.DisableExportAlbumsUseCase
import mega.privacy.android.domain.usecase.photos.GetDefaultAlbumsMapUseCase
import mega.privacy.android.domain.usecase.photos.RemoveAlbumsUseCase
import mega.privacy.android.domain.usecase.photos.RemovePhotosFromAlbumUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.feature.photos.mapper.AlbumNameValidationExceptionMessageMapper
import mega.privacy.android.feature.photos.mapper.AlbumUiStateMapper
import mega.privacy.android.feature.photos.mapper.LegacyMediaSystemAlbumMapper
import mega.privacy.android.feature.photos.mapper.LegacyPhotosSortMapper
import mega.privacy.android.feature.photos.mapper.PhotoUiStateMapper
import mega.privacy.android.feature.photos.model.AlbumSortConfiguration
import mega.privacy.android.feature.photos.model.AlbumSortOption
import mega.privacy.android.feature.photos.model.FilterMediaType
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.model.Sort
import mega.privacy.android.feature.photos.presentation.albums.content.model.AlbumContentSelectionAction
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumUiState
import mega.privacy.android.feature.photos.presentation.albums.model.FavouriteSystemAlbum
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.AlbumContentNavKey
import mega.privacy.android.navigation.destination.AlbumContentPreviewNavKey
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AlbumContentViewModelTest {
    private lateinit var underTest: AlbumContentViewModel

    private val savedStateHandle = mock<androidx.lifecycle.SavedStateHandle>()
    private val getDefaultAlbumPhotos = mock<GetDefaultAlbumPhotos>()
    private val getDefaultAlbumsMapUseCase = mock<GetDefaultAlbumsMapUseCase>()
    private val getUserAlbum = mock<MonitorUserAlbumByIdUseCase>()
    private val getAlbumPhotosUseCase = mock<GetAlbumPhotosUseCase>()
    private val albumUiStateMapper = mock<AlbumUiStateMapper>()
    private val legacyMediaSystemAlbumMapper = mock<LegacyMediaSystemAlbumMapper>()
    private val observeAlbumPhotosAddingProgress = mock<ObserveAlbumPhotosAddingProgress>()
    private val updateAlbumPhotosAddingProgressCompleted =
        mock<UpdateAlbumPhotosAddingProgressCompleted>()
    private val observeAlbumPhotosRemovingProgress = mock<ObserveAlbumPhotosRemovingProgress>()
    private val updateAlbumPhotosRemovingProgressCompleted =
        mock<UpdateAlbumPhotosRemovingProgressCompleted>()
    private val disableExportAlbumsUseCase = mock<DisableExportAlbumsUseCase>()
    private val removeFavouritesUseCase = mock<RemoveFavouritesUseCase>()
    private val removePhotosFromAlbumUseCase = mock<RemovePhotosFromAlbumUseCase>()
    private val getNodeListByIdsUseCase = mock<GetNodeListByIdsUseCase>()
    private val validateAndUpdateUserAlbumUseCase = mock<ValidateAndUpdateUserAlbumUseCase>()
    private val updateNodeSensitiveUseCase = mock<UpdateNodeSensitiveUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val isHiddenNodesOnboardedUseCase = mock<IsHiddenNodesOnboardedUseCase>()
    private val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()
    private val photoUiStateMapper = mock<PhotoUiStateMapper>()
    private val getUserAlbumCoverPhotoUseCase = mock<GetUserAlbumCoverPhotoUseCase>()
    private val removeAlbumsUseCase = mock<RemoveAlbumsUseCase>()
    private val snackbarEventQueue = mock<SnackbarEventQueue>()
    private val analyticsTracker: AnalyticsTracker = mock()
    private val albumNameValidationExceptionMessageMapper: AlbumNameValidationExceptionMessageMapper =
        mock()
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase = mock()
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase = mock()
    private val legacyPhotosSortMapper: LegacyPhotosSortMapper = mock()
    private val themeModeFlow = MutableStateFlow(ThemeMode.System)
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            savedStateHandle,
            getDefaultAlbumPhotos,
            getDefaultAlbumsMapUseCase,
            getUserAlbum,
            getAlbumPhotosUseCase,
            albumUiStateMapper,
            legacyMediaSystemAlbumMapper,
            observeAlbumPhotosAddingProgress,
            updateAlbumPhotosAddingProgressCompleted,
            observeAlbumPhotosRemovingProgress,
            updateAlbumPhotosRemovingProgressCompleted,
            disableExportAlbumsUseCase,
            removeFavouritesUseCase,
            removePhotosFromAlbumUseCase,
            getNodeListByIdsUseCase,
            validateAndUpdateUserAlbumUseCase,
            updateNodeSensitiveUseCase,
            getFeatureFlagValueUseCase,
            monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase,
            isHiddenNodesOnboardedUseCase,
            getBusinessStatusUseCase,
            analyticsTracker,
            photoUiStateMapper,
            getUserAlbumCoverPhotoUseCase,
            removeAlbumsUseCase,
            snackbarEventQueue,
            monitorThemeModeUseCase,
            monitorStorageStateEventUseCase,
            legacyPhotosSortMapper
        )
        stubCommon()
        Analytics.initialise(analyticsTracker)
    }

    private fun stubCommon() {
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(any()) }.thenReturn(false)
        }
        isHiddenNodesOnboardedUseCase.stub {
            onBlocking { invoke() }.thenReturn(false)
        }
        mockMonitorStorageStateEvent(StorageState.Green)
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(emptyFlow())
        whenever(monitorAccountDetailUseCase()).thenReturn(emptyFlow())
        whenever(observeAlbumPhotosAddingProgress(mock())).thenReturn(emptyFlow())
        whenever(observeAlbumPhotosRemovingProgress(mock())).thenReturn(emptyFlow())
        whenever(monitorThemeModeUseCase()).thenReturn(themeModeFlow)
    }

    private fun createViewModel(
        navKey: AlbumContentNavKey = AlbumContentNavKey(id = null, type = null),
    ) {
        underTest = AlbumContentViewModel(
            savedStateHandle = savedStateHandle,
            getDefaultAlbumPhotos = getDefaultAlbumPhotos,
            getDefaultAlbumsMapUseCase = getDefaultAlbumsMapUseCase,
            getUserAlbum = getUserAlbum,
            getAlbumPhotosUseCase = getAlbumPhotosUseCase,
            albumUiStateMapper = albumUiStateMapper,
            legacyMediaSystemAlbumMapper = legacyMediaSystemAlbumMapper,
            observeAlbumPhotosAddingProgress = observeAlbumPhotosAddingProgress,
            updateAlbumPhotosAddingProgressCompleted = updateAlbumPhotosAddingProgressCompleted,
            observeAlbumPhotosRemovingProgress = observeAlbumPhotosRemovingProgress,
            updateAlbumPhotosRemovingProgressCompleted = updateAlbumPhotosRemovingProgressCompleted,
            disableExportAlbumsUseCase = disableExportAlbumsUseCase,
            removeFavouritesUseCase = removeFavouritesUseCase,
            removePhotosFromAlbumUseCase = removePhotosFromAlbumUseCase,
            getNodeListByIdsUseCase = getNodeListByIdsUseCase,
            updateAlbumNameUseCase = validateAndUpdateUserAlbumUseCase,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            photoUiStateMapper = photoUiStateMapper,
            getUserAlbumCoverPhotoUseCase = getUserAlbumCoverPhotoUseCase,
            removeAlbumsUseCase = removeAlbumsUseCase,
            snackbarEventQueue = snackbarEventQueue,
            albumNameValidationExceptionMessageMapper = albumNameValidationExceptionMessageMapper,
            monitorThemeModeUseCase = monitorThemeModeUseCase,
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            legacyPhotosSortMapper = legacyPhotosSortMapper,
            defaultDispatcher = testDispatcher,
            navKey = navKey,
        )
    }

    @Test
    fun `test that deleteAlbum calls removeAlbumsUseCase and updates state on success`() = runTest {
        val albumId = AlbumId(123L)
        whenever(savedStateHandle.get<Long>("id")).thenReturn(albumId.id)
        whenever(removeAlbumsUseCase(listOf(albumId))).thenReturn(Unit)
        createViewModel()

        underTest.deleteAlbum()

        verify(removeAlbumsUseCase).invoke(listOf(albumId))
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.deleteAlbumSuccessEvent).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that deleteAlbum queues snackbar message on success`() = runTest {
        val albumId = AlbumId(123L)
        val albumTitle = "My Album"
        whenever(savedStateHandle.get<Long>("id")).thenReturn(albumId.id)
        whenever(removeAlbumsUseCase(listOf(albumId))).thenReturn(Unit)
        createViewModel()
        underTest.state.value.uiAlbum?.let {
            whenever(it.title).thenReturn(albumTitle)
        }

        underTest.deleteAlbum()

        verify(snackbarEventQueue).queueMessage(any(), any())
    }

    @Test
    fun `test that deleteAlbum does not update state when albumId is null`() = runTest {
        whenever(savedStateHandle.get<Long>("id")).thenReturn(null)
        createViewModel()

        underTest.deleteAlbum()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.deleteAlbumSuccessEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that deleteAlbum does not update state on failure`() = runTest {
        val albumId = AlbumId(123L)
        whenever(savedStateHandle.get<Long>("id")).thenReturn(albumId.id)
        whenever(removeAlbumsUseCase(listOf(albumId))).thenThrow(RuntimeException("Delete failed"))
        createViewModel()

        underTest.deleteAlbum()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.deleteAlbumSuccessEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that resetDeleteAlbumSuccess updates state correctly`() = runTest {
        val albumId = AlbumId(123L)
        whenever(savedStateHandle.get<Long>("id")).thenReturn(albumId.id)
        whenever(removeAlbumsUseCase(listOf(albumId))).thenReturn(Unit)
        createViewModel()

        underTest.deleteAlbum()
        underTest.resetDeleteAlbumSuccess()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.deleteAlbumSuccessEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that showRemoveLinkConfirmation updates state correctly`() = runTest {
        createViewModel()

        underTest.handleAction(AlbumContentSelectionAction.RemoveLink)

        underTest.state.test {
            assertThat(awaitItem().showRemoveLinkConfirmation).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that closeRemoveLinkConfirmation updates state correctly`() = runTest {
        createViewModel()

        underTest.resetRemoveLinkConfirmation()

        underTest.state.test {
            assertThat(awaitItem().showRemoveLinkConfirmation).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that clearSelectedPhotos updates state correctly`() = runTest {
        createViewModel()

        underTest.clearSelectedPhotos()

        underTest.state.test {
            assertThat(awaitItem().selectedPhotos).isEmpty()
        }
    }

    @Test
    fun `test that setHiddenNodesOnboarded updates state correctly`() = runTest {
        createViewModel()

        underTest.state.test {
            underTest.setHiddenNodesOnboarded()
            assertThat(expectMostRecentItem().isHiddenNodesOnboarded).isTrue()
        }
    }

    @Test
    fun `test that togglePhotoSelection adds photo when not selected`() = runTest {
        createViewModel()
        val photo = mock<PhotoUiState.Image>()
        whenever(photo.id).thenReturn(123L)

        underTest.togglePhotoSelection(photo)

        underTest.state.test {
            assertThat(awaitItem().selectedPhotos).contains(photo)
        }
    }

    @Test
    fun `test that togglePhotoSelection removes photo when already selected`() = runTest {
        createViewModel()
        val photo = mock<PhotoUiState.Image>()
        whenever(photo.id).thenReturn(123L)

        // Add photo first
        underTest.togglePhotoSelection(photo)
        // Remove it
        underTest.togglePhotoSelection(photo)

        underTest.state.test {
            assertThat(awaitItem().selectedPhotos).doesNotContain(photo)
        }
    }

    @Test
    fun `test that disableExportAlbum updates state when link removed successfully`() = runTest {
        val albumId = AlbumId(123L)
        val mockUserAlbum = mock<MediaAlbum.User> {
            on { id }.thenReturn(albumId)
        }
        val mockAlbumUiState = mock<AlbumUiState> {
            on { mediaAlbum }.thenReturn(mockUserAlbum)
            on { title }.thenReturn("Album")
            on { cover }.thenReturn(null)
        }
        whenever(disableExportAlbumsUseCase(listOf(albumId))).thenReturn(1)
        whenever(getUserAlbum(any())).thenReturn(flowOf(mockUserAlbum))
        whenever(albumUiStateMapper(mockUserAlbum)).thenReturn(mockAlbumUiState)
        whenever(getAlbumPhotosUseCase(any(), any())).thenReturn(flowOf())
        whenever(observeAlbumPhotosAddingProgress(any())).thenReturn(flowOf())
        whenever(observeAlbumPhotosRemovingProgress(any())).thenReturn(flowOf())

        createViewModel(AlbumContentNavKey(id = mockUserAlbum.id.id, type = "custom"))

        underTest.disableExportAlbum()

        underTest.state.test {
            assertThat(awaitItem().linkRemovedSuccessEvent).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that updatePhotosAddingProgressCompleted calls use case`() = runTest {
        createViewModel()
        val albumId = AlbumId(123L)

        underTest.updatePhotosAddingProgressCompleted(albumId)

        verify(updateAlbumPhotosAddingProgressCompleted).invoke(albumId)
    }

    @Test
    fun `test that updatePhotosRemovingProgressCompleted calls use case`() = runTest {
        createViewModel()
        val albumId = AlbumId(123L)

        underTest.updatePhotosRemovingProgressCompleted(albumId)

        verify(updateAlbumPhotosRemovingProgressCompleted).invoke(albumId)
    }

    @Test
    fun `test that savePhotosToDevice updates state correctly when nodes are fetched`() = runTest {
        createViewModel()
        val node = mock<TypedNode>()
        whenever(getNodeListByIdsUseCase(any())).thenReturn(listOf(node))

        underTest.savePhotosToDevice()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.savePhotosToDeviceEvent).isEqualTo(triggered(listOf(node)))
        }
    }

    @Test
    fun `test that resetSavePhotosToDevice updates state correctly`() = runTest {
        createViewModel()

        underTest.resetSavePhotosToDevice()

        underTest.state.test {
            assertThat(awaitItem().savePhotosToDeviceEvent).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that sharePhotos updates state correctly when nodes are fetched`() = runTest {
        createViewModel()
        val node = mock<TypedNode>()
        whenever(getNodeListByIdsUseCase(any())).thenReturn(listOf(node))

        underTest.sharePhotos()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.sharePhotosEvent).isEqualTo(triggered(listOf(node)))
        }
    }

    @Test
    fun `test that resetSharePhotos updates state correctly`() = runTest {
        createViewModel()

        underTest.resetSharePhotos()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.sharePhotosEvent).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that sendPhotosToChat updates state correctly when nodes are fetched`() = runTest {
        createViewModel()
        val node = mock<TypedNode>()
        whenever(getNodeListByIdsUseCase(any())).thenReturn(listOf(node))

        underTest.sendPhotosToChat()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.sendPhotosToChatEvent).isEqualTo(triggered(listOf(node)))
        }
    }

    @Test
    fun `test that resetSendPhotosToChat updates state correctly`() = runTest {
        createViewModel()

        underTest.resetSendPhotosToChat()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.sendPhotosToChatEvent).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that selectAllPhotos selects all photos when filter is ALL_MEDIA`() = runTest {
        createViewModel()
        val photo1 = mock<PhotoUiState.Image>()
        val photo2 = mock<PhotoUiState.Video>()
        whenever(photo1.id).thenReturn(1L)
        whenever(photo2.id).thenReturn(2L)

        underTest.selectAllPhotos()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.currentMediaType).isEqualTo(FilterMediaType.ALL_MEDIA)
        }
    }

    @Test
    fun `test that hideOrUnhideNodes calls updateNodeSensitiveUseCase with correct parameters`() =
        runTest {
            createViewModel()
            val photo = mock<PhotoUiState.Image>()
            whenever(photo.id).thenReturn(123L)

            underTest.togglePhotoSelection(photo)
            underTest.hideOrUnhideNodes(hide = true)

            verify(updateNodeSensitiveUseCase).invoke(NodeId(123L), true)
        }

    @Test
    fun `test that on update album name update success should reset state`() = runTest {
        val mockUserAlbum = mock<MediaAlbum.User> {
            on { id }.thenReturn(AlbumId(123L))
        }
        val mockAlbumUiState = mock<AlbumUiState> {
            on { mediaAlbum }.thenReturn(mockUserAlbum)
            on { title }.thenReturn("Album")
            on { cover }.thenReturn(null)
        }
        whenever(getUserAlbum(any())).thenReturn(flowOf(mockUserAlbum))
        whenever(albumUiStateMapper(mockUserAlbum)).thenReturn(mockAlbumUiState)
        whenever(getAlbumPhotosUseCase(any(), any())).thenReturn(flowOf())
        whenever(observeAlbumPhotosAddingProgress(any())).thenReturn(flowOf())
        whenever(observeAlbumPhotosRemovingProgress(any())).thenReturn(flowOf())

        createViewModel(AlbumContentNavKey(id = mockUserAlbum.id.id, type = "custom"))

        underTest.updateAlbumName("Album")

        underTest.state.test {
            assertThat(awaitItem().showUpdateAlbumName).isEqualTo(consumed)
        }

        verify(validateAndUpdateUserAlbumUseCase).invoke(mockUserAlbum.id, "Album")
    }

    @Test
    fun `test that on update album name validation exception should update error message`() =
        runTest {
            val mockUserAlbum = mock<MediaAlbum.User> {
                on { id }.thenReturn(AlbumId(123L))
            }
            val mockAlbumUiState = mock<AlbumUiState> {
                on { mediaAlbum }.thenReturn(mockUserAlbum)
                on { title }.thenReturn("Album")
                on { cover }.thenReturn(null)
            }
            val expectedMessage = "expectedMessage"
            whenever(getUserAlbum(any())).thenReturn(flowOf(mockUserAlbum))
            whenever(albumUiStateMapper(mockUserAlbum)).thenReturn(mockAlbumUiState)
            whenever(getAlbumPhotosUseCase(any(), any())).thenReturn(flowOf())
            whenever(observeAlbumPhotosAddingProgress(any())).thenReturn(flowOf())
            whenever(observeAlbumPhotosRemovingProgress(any())).thenReturn(flowOf())
            whenever(validateAndUpdateUserAlbumUseCase(any(), any())).thenAnswer {
                throw AlbumNameValidationException.Exists
            }
            whenever(albumNameValidationExceptionMessageMapper(any())).thenReturn(expectedMessage)

            createViewModel(AlbumContentNavKey(id = mockUserAlbum.id.id, type = "custom"))

            underTest.updateAlbumName("Album")

            underTest.state.test {
                assertThat(awaitItem().updateAlbumNameErrorMessage).isEqualTo(
                    triggered(
                        expectedMessage
                    )
                )
            }

            verify(validateAndUpdateUserAlbumUseCase).invoke(mockUserAlbum.id, "Album")
        }

    @Test
    fun `test that monitor theme mode should update ui state`() = runTest {
        val expected = ThemeMode.Light

        createViewModel()

        themeModeFlow.emit(expected)

        underTest.state.test {
            assertThat(expectMostRecentItem().themeMode).isEqualTo(expected)
        }
    }

    @Test
    fun `test that handleBottomSheetAction with Rename action calls showUpdateAlbumName`() =
        runTest {
            createViewModel()

            underTest.handleAction(AlbumContentSelectionAction.Rename)

            underTest.state.test {
                assertThat(awaitItem().showUpdateAlbumName).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that handleBottomSheetAction with SelectAlbumCover action emits event with album id`() =
        runTest {
            val albumId = AlbumId(123L)
            val mockUserAlbum = mock<MediaAlbum.User> {
                on { id }.thenReturn(albumId)
            }
            val mockAlbumUiState = mock<AlbumUiState> {
                on { mediaAlbum }.thenReturn(mockUserAlbum)
                on { title }.thenReturn("Album")
                on { cover }.thenReturn(null)
            }
            whenever(getUserAlbum(any())).thenReturn(flowOf(mockUserAlbum))
            whenever(albumUiStateMapper(mockUserAlbum)).thenReturn(mockAlbumUiState)
            whenever(getAlbumPhotosUseCase(any(), any())).thenReturn(flowOf())
            whenever(observeAlbumPhotosAddingProgress(any())).thenReturn(flowOf())
            whenever(observeAlbumPhotosRemovingProgress(any())).thenReturn(flowOf())

            createViewModel(AlbumContentNavKey(id = albumId.id, type = "custom"))

            underTest.handleAction(AlbumContentSelectionAction.SelectAlbumCover)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.selectAlbumCoverEvent).isEqualTo(triggered(albumId))
            }
        }

    @Test
    fun `test that handleBottomSheetAction with SelectAlbumCover action emits null when not user album`() =
        runTest {
            createViewModel()

            underTest.handleAction(AlbumContentSelectionAction.SelectAlbumCover)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.selectAlbumCoverEvent).isEqualTo(triggered(null))
            }
        }

    @Test
    fun `test that handleBottomSheetAction with ManageLink action calls manageLink`() = runTest {
        val albumId = AlbumId(123L)
        val mockUserAlbum = mock<MediaAlbum.User> {
            on { id }.thenReturn(albumId)
        }
        val mockAlbumUiState = mock<AlbumUiState> {
            on { mediaAlbum }.thenReturn(mockUserAlbum)
            on { title }.thenReturn("Album")
            on { cover }.thenReturn(null)
        }
        whenever(getUserAlbum(any())).thenReturn(flowOf(mockUserAlbum))
        whenever(albumUiStateMapper(mockUserAlbum)).thenReturn(mockAlbumUiState)
        whenever(getAlbumPhotosUseCase(any(), any())).thenReturn(flowOf())
        whenever(observeAlbumPhotosAddingProgress(any())).thenReturn(flowOf())
        whenever(observeAlbumPhotosRemovingProgress(any())).thenReturn(flowOf())

        createViewModel(AlbumContentNavKey(id = albumId.id, type = "custom"))

        underTest.handleAction(AlbumContentSelectionAction.ManageLink)

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.manageLinkEvent).isNotEqualTo(consumed())
        }
    }

    @Test
    fun `test that handleBottomSheetAction with RemoveLink action calls showRemoveLinkConfirmation`() =
        runTest {
            createViewModel()

            underTest.handleAction(AlbumContentSelectionAction.RemoveLink)

            underTest.state.test {
                assertThat(awaitItem().showRemoveLinkConfirmation).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that handleBottomSheetAction with Delete action calls deleteAlbum when photos are empty`() =
        runTest {
            val albumId = AlbumId(123L)
            whenever(savedStateHandle.get<Long>("id")).thenReturn(albumId.id)
            whenever(removeAlbumsUseCase(listOf(albumId))).thenReturn(Unit)
            createViewModel()

            underTest.handleAction(AlbumContentSelectionAction.Delete)

            verify(removeAlbumsUseCase).invoke(listOf(albumId))
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.deleteAlbumSuccessEvent).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that handleBottomSheetAction with Delete action calls showDeleteAlbumConfirmation when photos are not empty`() =
        runTest {
            val albumId = AlbumId(123L)
            val mockUserAlbum = mock<MediaAlbum.User> {
                on { id }.thenReturn(albumId)
            }
            val mockAlbumUiState = mock<AlbumUiState> {
                on { mediaAlbum }.thenReturn(mockUserAlbum)
                on { title }.thenReturn("Album")
                on { cover }.thenReturn(null)
            }
            val legacyPhoto = mock<Photo.Image> {
                on { id }.thenReturn(1L)
            }
            val photo = mock<PhotoUiState.Image> {
                on { id }.thenReturn(1L)
            }
            whenever(photo.id).thenReturn(1L)
            whenever(getUserAlbum(any())).thenReturn(flowOf(mockUserAlbum))
            whenever(albumUiStateMapper(mockUserAlbum)).thenReturn(mockAlbumUiState)
            whenever(getAlbumPhotosUseCase(any(), any())).thenReturn(flowOf(listOf(legacyPhoto)))
            whenever(photoUiStateMapper(legacyPhoto)).thenReturn(photo)
            whenever(observeAlbumPhotosAddingProgress(any())).thenReturn(flowOf())
            whenever(observeAlbumPhotosAddingProgress(any())).thenReturn(flowOf())
            whenever(observeAlbumPhotosRemovingProgress(any())).thenReturn(flowOf())

            createViewModel(AlbumContentNavKey(id = albumId.id, type = "custom"))

            underTest.handleAction(AlbumContentSelectionAction.Delete)

            underTest.state.test {
                assertThat(awaitItem().showDeleteAlbumConfirmation).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that resetSelectAlbumCoverEvent updates state correctly`() = runTest {
        val albumId = AlbumId(123L)
        val mockUserAlbum = mock<MediaAlbum.User> {
            on { id }.thenReturn(albumId)
        }
        val mockAlbumUiState = mock<AlbumUiState> {
            on { mediaAlbum }.thenReturn(mockUserAlbum)
            on { title }.thenReturn("Album")
            on { cover }.thenReturn(null)
        }
        whenever(getUserAlbum(any())).thenReturn(flowOf(mockUserAlbum))
        whenever(albumUiStateMapper(mockUserAlbum)).thenReturn(mockAlbumUiState)
        whenever(getAlbumPhotosUseCase(any(), any())).thenReturn(flowOf())
        whenever(observeAlbumPhotosAddingProgress(any())).thenReturn(flowOf())
        whenever(observeAlbumPhotosRemovingProgress(any())).thenReturn(flowOf())

        createViewModel(AlbumContentNavKey(id = albumId.id, type = "custom"))

        underTest.handleAction(AlbumContentSelectionAction.SelectAlbumCover)
        underTest.resetSelectAlbumCoverEvent()

        underTest.state.test {
            assertThat(awaitItem().selectAlbumCoverEvent).isEqualTo(consumed())
        }
    }

    @ParameterizedTest
    @MethodSource("provideBottomSheetAction")
    fun `test that paywall event should trigger when handling bottom sheet action and storage state is paywall`(
        action: AlbumContentSelectionAction,
    ) = runTest {
        mockMonitorStorageStateEvent(StorageState.PayWall)

        createViewModel()

        underTest.handleAction(action)

        underTest.state.test {
            assertThat(awaitItem().paywallEvent).isEqualTo(triggered)
        }
    }

    private fun provideBottomSheetAction() = listOf(
        Arguments.of(AlbumContentSelectionAction.Rename),
        Arguments.of(AlbumContentSelectionAction.SelectAlbumCover),
        Arguments.of(AlbumContentSelectionAction.ManageLink),
        Arguments.of(AlbumContentSelectionAction.RemoveLink),
        Arguments.of(AlbumContentSelectionAction.Delete),
    )

    private fun mockMonitorStorageStateEvent(state: StorageState) {
        val storageStateEvent = StorageStateEvent(1L, state)
        whenever(monitorStorageStateEventUseCase.invoke()).thenReturn(
            MutableStateFlow(storageStateEvent)
        )
    }

    @Test
    fun `test that sortPhotos sorts photos correctly by modification time ascending`() =
        runTest {
            val mockUserAlbum = mock<MediaAlbum.User> {
                on { id }.thenReturn(AlbumId(123L))
            }
            val mockAlbumUiState = mock<AlbumUiState> {
                on { mediaAlbum }.thenReturn(mockUserAlbum)
                on { title }.thenReturn("Album")
                on { cover }.thenReturn(null)
            }
            val baseTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0)
            val photo1 = Photo.Image(
                id = 1L,
                albumPhotoId = null,
                parentId = 0L,
                name = "photo1.jpg",
                isFavourite = false,
                creationTime = baseTime,
                modificationTime = baseTime.plusDays(3),
                thumbnailFilePath = null,
                previewFilePath = null,
                fileTypeInfo = mock<StaticImageFileTypeInfo>(),
                base64Id = null,
                size = 0L,
                isTakenDown = false,
                isSensitive = false,
                isSensitiveInherited = false,
            )
            val photo2 = Photo.Image(
                id = 2L,
                albumPhotoId = null,
                parentId = 0L,
                name = "photo2.jpg",
                isFavourite = false,
                creationTime = baseTime,
                modificationTime = baseTime.plusDays(1),
                thumbnailFilePath = null,
                previewFilePath = null,
                fileTypeInfo = mock<StaticImageFileTypeInfo>(),
                base64Id = null,
                size = 0L,
                isTakenDown = false,
                isSensitive = false,
                isSensitiveInherited = false,
            )
            val photo3 = Photo.Image(
                id = 3L,
                albumPhotoId = null,
                parentId = 0L,
                name = "photo3.jpg",
                isFavourite = false,
                creationTime = baseTime,
                modificationTime = baseTime.plusDays(2),
                thumbnailFilePath = null,
                previewFilePath = null,
                fileTypeInfo = mock<StaticImageFileTypeInfo>(),
                base64Id = null,
                size = 0L,
                isTakenDown = false,
                isSensitive = false,
                isSensitiveInherited = false,
            )
            val photos = listOf(photo1, photo2, photo3)
            val photo1UiState = PhotoUiState.Image(
                id = photo1.id,
                albumPhotoId = photo1.albumPhotoId,
                parentId = photo1.parentId,
                name = photo1.name,
                isFavourite = photo1.isFavourite,
                creationTime = photo1.creationTime,
                modificationTime = photo1.modificationTime,
                thumbnailFilePath = photo1.thumbnailFilePath,
                previewFilePath = photo1.previewFilePath,
                fileTypeInfo = photo1.fileTypeInfo,
                base64Id = photo1.base64Id,
                size = photo1.size,
                isTakenDown = photo1.isTakenDown,
                isSensitive = photo1.isSensitive,
                isSensitiveInherited = photo1.isSensitiveInherited,
            )
            val photo2UiState = PhotoUiState.Image(
                id = photo2.id,
                albumPhotoId = photo2.albumPhotoId,
                parentId = photo2.parentId,
                name = photo2.name,
                isFavourite = photo2.isFavourite,
                creationTime = photo2.creationTime,
                modificationTime = photo2.modificationTime,
                thumbnailFilePath = photo2.thumbnailFilePath,
                previewFilePath = photo2.previewFilePath,
                fileTypeInfo = photo2.fileTypeInfo,
                base64Id = photo2.base64Id,
                size = photo2.size,
                isTakenDown = photo2.isTakenDown,
                isSensitive = photo2.isSensitive,
                isSensitiveInherited = photo2.isSensitiveInherited,
            )
            val photo3UiState = PhotoUiState.Image(
                id = photo3.id,
                albumPhotoId = photo3.albumPhotoId,
                parentId = photo3.parentId,
                name = photo3.name,
                isFavourite = photo3.isFavourite,
                creationTime = photo3.creationTime,
                modificationTime = photo3.modificationTime,
                thumbnailFilePath = photo3.thumbnailFilePath,
                previewFilePath = photo3.previewFilePath,
                fileTypeInfo = photo3.fileTypeInfo,
                base64Id = photo3.base64Id,
                size = photo3.size,
                isTakenDown = photo3.isTakenDown,
                isSensitive = photo3.isSensitive,
                isSensitiveInherited = photo3.isSensitiveInherited,
            )
            whenever(getUserAlbum(any())).thenReturn(flowOf(mockUserAlbum))
            whenever(albumUiStateMapper(mockUserAlbum)).thenReturn(mockAlbumUiState)
            whenever(getAlbumPhotosUseCase(any(), any())).thenReturn(flowOf(photos))
            whenever(observeAlbumPhotosAddingProgress(any())).thenReturn(flowOf())
            whenever(observeAlbumPhotosRemovingProgress(any())).thenReturn(flowOf())
            whenever(photoUiStateMapper(photo1)).thenReturn(photo1UiState)
            whenever(photoUiStateMapper(photo2)).thenReturn(photo2UiState)
            whenever(photoUiStateMapper(photo3)).thenReturn(photo3UiState)

            createViewModel(AlbumContentNavKey(id = mockUserAlbum.id.id, type = "custom"))

            // Wait for photos to be loaded
            underTest.state.test {
                val state = awaitItem()
                if (state.photos.isNotEmpty()) {
                    cancelAndIgnoreRemainingEvents()
                }
            }

            val sortConfiguration = AlbumSortConfiguration(
                sortOption = AlbumSortOption.Modified,
                sortDirection = SortDirection.Ascending
            )

            underTest.sortPhotos(sortConfiguration)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.albumSortConfiguration).isEqualTo(sortConfiguration)
                assertThat(state.photos.size).isEqualTo(3)
                // Photos should be sorted by modification time ascending, then by id descending
                // photo2 (day 1), photo3 (day 2), photo1 (day 3)
                assertThat(state.photos[0].id).isEqualTo(photo2UiState.id)
                assertThat(state.photos[1].id).isEqualTo(photo3UiState.id)
                assertThat(state.photos[2].id).isEqualTo(photo1UiState.id)
            }
        }

    @Test
    fun `test that sortPhotos sorts photos correctly by modification time descending`() =
        runTest {
            val mockUserAlbum = mock<MediaAlbum.User> {
                on { id }.thenReturn(AlbumId(123L))
            }
            val mockAlbumUiState = mock<AlbumUiState> {
                on { mediaAlbum }.thenReturn(mockUserAlbum)
                on { title }.thenReturn("Album")
                on { cover }.thenReturn(null)
            }
            val baseTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0)
            val photo1 = Photo.Image(
                id = 1L,
                albumPhotoId = null,
                parentId = 0L,
                name = "photo1.jpg",
                isFavourite = false,
                creationTime = baseTime,
                modificationTime = baseTime.plusDays(1),
                thumbnailFilePath = null,
                previewFilePath = null,
                fileTypeInfo = mock<StaticImageFileTypeInfo>(),
                base64Id = null,
                size = 0L,
                isTakenDown = false,
                isSensitive = false,
                isSensitiveInherited = false,
            )
            val photo2 = Photo.Image(
                id = 2L,
                albumPhotoId = null,
                parentId = 0L,
                name = "photo2.jpg",
                isFavourite = false,
                creationTime = baseTime,
                modificationTime = baseTime.plusDays(3),
                thumbnailFilePath = null,
                previewFilePath = null,
                fileTypeInfo = mock<StaticImageFileTypeInfo>(),
                base64Id = null,
                size = 0L,
                isTakenDown = false,
                isSensitive = false,
                isSensitiveInherited = false,
            )
            val photo3 = Photo.Image(
                id = 3L,
                albumPhotoId = null,
                parentId = 0L,
                name = "photo3.jpg",
                isFavourite = false,
                creationTime = baseTime,
                modificationTime = baseTime.plusDays(2),
                thumbnailFilePath = null,
                previewFilePath = null,
                fileTypeInfo = mock<StaticImageFileTypeInfo>(),
                base64Id = null,
                size = 0L,
                isTakenDown = false,
                isSensitive = false,
                isSensitiveInherited = false,
            )
            val photos = listOf(photo1, photo2, photo3)
            val photo1UiState = PhotoUiState.Image(
                id = photo1.id,
                albumPhotoId = photo1.albumPhotoId,
                parentId = photo1.parentId,
                name = photo1.name,
                isFavourite = photo1.isFavourite,
                creationTime = photo1.creationTime,
                modificationTime = photo1.modificationTime,
                thumbnailFilePath = photo1.thumbnailFilePath,
                previewFilePath = photo1.previewFilePath,
                fileTypeInfo = photo1.fileTypeInfo,
                base64Id = photo1.base64Id,
                size = photo1.size,
                isTakenDown = photo1.isTakenDown,
                isSensitive = photo1.isSensitive,
                isSensitiveInherited = photo1.isSensitiveInherited,
            )
            val photo2UiState = PhotoUiState.Image(
                id = photo2.id,
                albumPhotoId = photo2.albumPhotoId,
                parentId = photo2.parentId,
                name = photo2.name,
                isFavourite = photo2.isFavourite,
                creationTime = photo2.creationTime,
                modificationTime = photo2.modificationTime,
                thumbnailFilePath = photo2.thumbnailFilePath,
                previewFilePath = photo2.previewFilePath,
                fileTypeInfo = photo2.fileTypeInfo,
                base64Id = photo2.base64Id,
                size = photo2.size,
                isTakenDown = photo2.isTakenDown,
                isSensitive = photo2.isSensitive,
                isSensitiveInherited = photo2.isSensitiveInherited,
            )
            val photo3UiState = PhotoUiState.Image(
                id = photo3.id,
                albumPhotoId = photo3.albumPhotoId,
                parentId = photo3.parentId,
                name = photo3.name,
                isFavourite = photo3.isFavourite,
                creationTime = photo3.creationTime,
                modificationTime = photo3.modificationTime,
                thumbnailFilePath = photo3.thumbnailFilePath,
                previewFilePath = photo3.previewFilePath,
                fileTypeInfo = photo3.fileTypeInfo,
                base64Id = photo3.base64Id,
                size = photo3.size,
                isTakenDown = photo3.isTakenDown,
                isSensitive = photo3.isSensitive,
                isSensitiveInherited = photo3.isSensitiveInherited,
            )
            whenever(getUserAlbum(any())).thenReturn(flowOf(mockUserAlbum))
            whenever(albumUiStateMapper(mockUserAlbum)).thenReturn(mockAlbumUiState)
            whenever(getAlbumPhotosUseCase(any(), any())).thenReturn(flowOf(photos))
            whenever(observeAlbumPhotosAddingProgress(any())).thenReturn(flowOf())
            whenever(observeAlbumPhotosRemovingProgress(any())).thenReturn(flowOf())
            whenever(photoUiStateMapper(photo1)).thenReturn(photo1UiState)
            whenever(photoUiStateMapper(photo2)).thenReturn(photo2UiState)
            whenever(photoUiStateMapper(photo3)).thenReturn(photo3UiState)

            createViewModel(AlbumContentNavKey(id = mockUserAlbum.id.id, type = "custom"))

            // Wait for photos to be loaded
            underTest.state.test {
                val state = awaitItem()
                if (state.photos.isNotEmpty()) {
                    cancelAndIgnoreRemainingEvents()
                }
            }

            val sortConfiguration = AlbumSortConfiguration(
                sortOption = AlbumSortOption.Modified,
                sortDirection = SortDirection.Descending
            )

            underTest.sortPhotos(sortConfiguration)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.albumSortConfiguration).isEqualTo(sortConfiguration)
                assertThat(state.photos.size).isEqualTo(3)
                // Photos should be sorted by modification time descending, then by id descending
                // photo2 (day 3), photo3 (day 2), photo1 (day 1)
                assertThat(state.photos[0].id).isEqualTo(photo2UiState.id)
                assertThat(state.photos[1].id).isEqualTo(photo3UiState.id)
                assertThat(state.photos[2].id).isEqualTo(photo1UiState.id)
            }
        }

    @Test
    fun `test that preview photo should trigger event with correct args`() =
        runTest {
            val albumId = AlbumId(123L)
            val mockUserAlbum = mock<MediaAlbum.User> {
                on { id }.thenReturn(albumId)
            }
            val mockAlbumUiState = mock<AlbumUiState> {
                on { mediaAlbum }.thenReturn(mockUserAlbum)
                on { title }.thenReturn("Album")
                on { cover }.thenReturn(null)
            }
            val legacyPhoto = mock<Photo.Image> {
                on { id }.thenReturn(1L)
            }
            val photo = mock<PhotoUiState.Image> {
                on { id }.thenReturn(1L)
            }
            whenever(photo.id).thenReturn(1L)
            whenever(getUserAlbum(any())).thenReturn(flowOf(mockUserAlbum))
            whenever(albumUiStateMapper(mockUserAlbum)).thenReturn(mockAlbumUiState)
            whenever(getAlbumPhotosUseCase(any(), any())).thenReturn(flowOf(listOf(legacyPhoto)))
            whenever(photoUiStateMapper(legacyPhoto)).thenReturn(photo)
            whenever(observeAlbumPhotosAddingProgress(any())).thenReturn(flowOf())
            whenever(observeAlbumPhotosAddingProgress(any())).thenReturn(flowOf())
            whenever(observeAlbumPhotosRemovingProgress(any())).thenReturn(flowOf())
            whenever(legacyPhotosSortMapper(any())).thenReturn(Sort.OLDEST)

            createViewModel(AlbumContentNavKey(id = albumId.id, type = "custom"))

            underTest.previewPhoto(photo)

            underTest.state.test {
                val state = awaitItem()
                val result =
                    (state.previewAlbumContentEvent as StateEventWithContentTriggered<AlbumContentPreviewNavKey>).content
                assertThat(result.albumId).isEqualTo(albumId.id)
                assertThat(result.photoId).isEqualTo(photo.id)
                assertThat(result.albumType).isEqualTo("custom")
                assertThat(result.sortType).isEqualTo(Sort.OLDEST.name)
            }
        }

    @Test
    fun `test that updateBottomBarActionVisibility always includes Download SendToChat and Share`() =
        runTest {
            createViewModel()
            val photo = createMockPhotoUiState(id = 1L, isSensitive = false)

            underTest.togglePhotoSelection(photo)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.visibleBottomBarActions).contains(AlbumContentSelectionAction.Download)
                assertThat(state.visibleBottomBarActions).contains(AlbumContentSelectionAction.SendToChat)
                assertThat(state.visibleBottomBarActions).contains(AlbumContentSelectionAction.Share)
            }
        }

    @Test
    fun `test that updateBottomBarActionVisibility shows Hide when user is not paid`() =
        runTest {
            createViewModel()
            val photo = createMockPhotoUiState(id = 1L, isSensitive = false)

            underTest.togglePhotoSelection(photo)

            underTest.state.test {
                val state = awaitItem()
                // accountType is null (not paid), so Hide should be visible
                assertThat(state.visibleBottomBarActions).contains(AlbumContentSelectionAction.Hide)
                assertThat(state.visibleBottomBarActions).doesNotContain(AlbumContentSelectionAction.Unhide)
            }
        }

    @Test
    fun `test that updateBottomBarActionVisibility shows Hide when has non-sensitive node and is onboarded`() =
        runTest {
            isHiddenNodesOnboardedUseCase.stub {
                onBlocking { invoke() }.thenReturn(true)
            }
            createViewModel()
            val photo =
                createMockPhotoUiState(id = 1L, isSensitive = false, isSensitiveInherited = false)

            underTest.togglePhotoSelection(photo)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.visibleBottomBarActions).contains(AlbumContentSelectionAction.Hide)
            }
        }

    @Test
    fun `test that updateBottomBarActionVisibility shows Unhide when paid and all nodes are sensitive`() =
        runTest {
            isHiddenNodesOnboardedUseCase.stub {
                onBlocking { invoke() }.thenReturn(true)
            }
            createViewModel()

            // Simulate paid account by setting state
            val photo =
                createMockPhotoUiState(id = 1L, isSensitive = true, isSensitiveInherited = false)
            underTest.togglePhotoSelection(photo)

            underTest.state.test {
                assertThat(awaitItem().visibleBottomBarActions).contains(AlbumContentSelectionAction.Hide)
            }
        }

    @Test
    fun `test that updateBottomBarActionVisibility shows Delete for UserAlbum`() =
        runTest {
            val albumId = AlbumId(123L)
            val mockUserAlbum = mock<MediaAlbum.User> {
                on { id }.thenReturn(albumId)
            }
            val mockAlbumUiState = mock<AlbumUiState> {
                on { mediaAlbum }.thenReturn(mockUserAlbum)
                on { title }.thenReturn("Album")
                on { cover }.thenReturn(null)
            }
            whenever(getUserAlbum(any())).thenReturn(flowOf(mockUserAlbum))
            whenever(albumUiStateMapper(mockUserAlbum)).thenReturn(mockAlbumUiState)
            whenever(getAlbumPhotosUseCase(any(), any())).thenReturn(flowOf())
            whenever(observeAlbumPhotosAddingProgress(any())).thenReturn(flowOf())
            whenever(observeAlbumPhotosRemovingProgress(any())).thenReturn(flowOf())

            createViewModel(AlbumContentNavKey(id = albumId.id, type = "custom"))

            val photo = createMockPhotoUiState(id = 1L, isSensitive = false)
            underTest.togglePhotoSelection(photo)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.visibleBottomBarActions).contains(AlbumContentSelectionAction.Delete)
                assertThat(state.visibleBottomBarActions).doesNotContain(AlbumContentSelectionAction.RemoveFavourites)
            }
        }

    @Test
    fun `test that updateBottomBarActionVisibility shows RemoveFavourites for FavouriteAlbum`() =
        runTest {
            val mockFavouriteSystemAlbum = mock<FavouriteSystemAlbum>()
            val mockSystemAlbum = mock<MediaAlbum.System> {
                on { id }.thenReturn(mockFavouriteSystemAlbum)
            }
            val mockAlbumUiState = mock<AlbumUiState> {
                on { mediaAlbum }.thenReturn(mockSystemAlbum)
                on { title }.thenReturn("Favourites")
                on { cover }.thenReturn(null)
            }
            val mockPhotoPredicate: PhotoPredicate = { true }
            whenever(getDefaultAlbumsMapUseCase()).thenReturn(
                mapOf(Album.FavouriteAlbum to mockPhotoPredicate)
            )
            whenever(getDefaultAlbumPhotos(any(), any())).thenReturn(flowOf(emptyList()))
            whenever(legacyMediaSystemAlbumMapper(any(), anyOrNull())).thenReturn(mockSystemAlbum)
            whenever(albumUiStateMapper(mockSystemAlbum)).thenReturn(mockAlbumUiState)

            createViewModel(AlbumContentNavKey(id = null, type = "favourite"))

            val photo = createMockPhotoUiState(id = 1L, isSensitive = false)
            underTest.togglePhotoSelection(photo)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.uiAlbum?.mediaAlbum).isEqualTo(mockSystemAlbum)
                assertThat(state.visibleBottomBarActions).contains(AlbumContentSelectionAction.RemoveFavourites)
                assertThat(state.visibleBottomBarActions).doesNotContain(AlbumContentSelectionAction.Delete)
            }
        }

    @Test
    fun `test that clearSelectedPhotos triggers updateBottomBarActionVisibility with empty list`() =
        runTest {
            createViewModel()
            val photo = createMockPhotoUiState(id = 1L, isSensitive = false)

            // First select a photo
            underTest.togglePhotoSelection(photo)
            // Then clear selection
            underTest.clearSelectedPhotos()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.selectedPhotos).isEmpty()
                assertThat(state.visibleBottomBarActions).isEmpty()
            }
        }

    private fun createMockPhotoUiState(
        id: Long,
        isSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
    ): PhotoUiState.Image {
        return mock {
            on { this.id }.thenReturn(id)
            on { this.isSensitive }.thenReturn(isSensitive)
            on { this.isSensitiveInherited }.thenReturn(isSensitiveInherited)
        }
    }
}

