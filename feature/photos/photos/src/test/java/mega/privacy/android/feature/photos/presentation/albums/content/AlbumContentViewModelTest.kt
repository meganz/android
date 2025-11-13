package mega.privacy.android.feature.photos.presentation.albums.content

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
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
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.exception.account.AlbumNameValidationException
import mega.privacy.android.domain.featuretoggle.ApiFeatures
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
import mega.privacy.android.feature.photos.mapper.PhotoUiStateMapper
import mega.privacy.android.feature.photos.model.FilterMediaType
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.model.Sort
import mega.privacy.android.feature.photos.presentation.albums.content.model.AlbumContentSelectionAction
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumUiState
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import mega.privacy.android.navigation.destination.AlbumContentNavKey
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
    private val themeModeFlow = MutableStateFlow(ThemeMode.System)

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
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
            monitorStorageStateEventUseCase
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

        underTest.showRemoveLinkConfirmation()

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
    fun `test that setCurrentSort updates state correctly`() = runTest {
        createViewModel()
        val sort = Sort.OLDEST

        underTest.setCurrentSort(sort)

        underTest.state.test {
            assertThat(awaitItem().currentSort).isEqualTo(sort)
        }
    }

    @Test
    fun `test that setCurrentMediaType updates state correctly`() = runTest {
        createViewModel()
        val mediaType = FilterMediaType.IMAGES

        underTest.setCurrentMediaType(mediaType)

        underTest.state.test {
            assertThat(awaitItem().currentMediaType).isEqualTo(mediaType)
        }
    }

    @Test
    fun `test that showSortByDialog updates state correctly`() = runTest {
        createViewModel()

        underTest.showSortByDialog(true)

        underTest.state.test {
            assertThat(awaitItem().showSortByDialog).isTrue()
        }
    }

    @Test
    fun `test that showFilterDialog updates state correctly`() = runTest {
        createViewModel()

        underTest.showFilterDialog(true)

        underTest.state.test {
            assertThat(awaitItem().showFilterDialog).isTrue()
        }
    }

    @Test
    fun `test that showRenameDialog updates state correctly`() = runTest {
        createViewModel()

        underTest.showRenameDialog(true)

        underTest.state.test {
            assertThat(awaitItem().showRenameDialog).isTrue()
        }
    }

    @Test
    fun `test that setShowRemovePhotosFromAlbumDialog updates state correctly`() = runTest {
        createViewModel()

        underTest.setShowRemovePhotosFromAlbumDialog(true)

        underTest.state.test {
            assertThat(awaitItem().showRemovePhotosDialog).isTrue()
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
    fun `test that setSnackBarMessage updates state correctly`() = runTest {
        createViewModel()
        val message = "Test message"

        underTest.setSnackBarMessage(message)

        underTest.state.test {
            assertThat(awaitItem().snackBarMessage).isEqualTo(message)
        }
    }

    @Test
    fun `test that setNewAlbumNameValidity updates state correctly`() = runTest {
        createViewModel()

        underTest.setNewAlbumNameValidity(false)

        underTest.state.test {
            assertThat(awaitItem().isInputNameValid).isFalse()
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
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
            false
        )
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
    fun `test that removeFavourites calls use case and clears selection`() = runTest {
        createViewModel()

        underTest.removeFavourites()

        verify(removeFavouritesUseCase).invoke(emptyList())
        underTest.state.test {
            assertThat(awaitItem().selectedPhotos).isEmpty()
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
    fun `test that getSelectedPhotos returns current selected photos`() = runTest {
        createViewModel()

        val result = underTest.getSelectedPhotos()

        assertThat(result).isEqualTo(underTest.state.value.selectedPhotos)
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
    fun `test that hidePhotos updates state correctly when nodes are fetched`() = runTest {
        createViewModel()
        val node = mock<TypedNode>()
        whenever(getNodeListByIdsUseCase(any())).thenReturn(listOf(node))

        underTest.hidePhotos()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.hidePhotosEvent).isEqualTo(triggered(listOf(node)))
        }
    }

    @Test
    fun `test that resetHidePhotos updates state correctly`() = runTest {
        createViewModel()

        underTest.resetHidePhotos()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.hidePhotosEvent).isEqualTo(consumed())
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
    fun `test that selectAllPhotos selects only images when filter is IMAGES`() = runTest {
        createViewModel()
        underTest.setCurrentMediaType(FilterMediaType.IMAGES)

        underTest.selectAllPhotos()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.currentMediaType).isEqualTo(FilterMediaType.IMAGES)
        }
    }

    @Test
    fun `test that selectAllPhotos selects only videos when filter is VIDEOS`() = runTest {
        createViewModel()
        underTest.setCurrentMediaType(FilterMediaType.VIDEOS)

        underTest.selectAllPhotos()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.currentMediaType).isEqualTo(FilterMediaType.VIDEOS)
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
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
            false
        )
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
            whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
                false
            )
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

            underTest.handleBottomSheetAction(AlbumContentSelectionAction.Rename)

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
            whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
                false
            )
            whenever(getUserAlbum(any())).thenReturn(flowOf(mockUserAlbum))
            whenever(albumUiStateMapper(mockUserAlbum)).thenReturn(mockAlbumUiState)
            whenever(getAlbumPhotosUseCase(any(), any())).thenReturn(flowOf())
            whenever(observeAlbumPhotosAddingProgress(any())).thenReturn(flowOf())
            whenever(observeAlbumPhotosRemovingProgress(any())).thenReturn(flowOf())

            createViewModel(AlbumContentNavKey(id = albumId.id, type = "custom"))

            underTest.handleBottomSheetAction(AlbumContentSelectionAction.SelectAlbumCover)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.selectAlbumCoverEvent).isEqualTo(triggered(albumId))
            }
        }

    @Test
    fun `test that handleBottomSheetAction with SelectAlbumCover action emits null when not user album`() =
        runTest {
            createViewModel()

            underTest.handleBottomSheetAction(AlbumContentSelectionAction.SelectAlbumCover)

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
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
            false
        )
        whenever(getUserAlbum(any())).thenReturn(flowOf(mockUserAlbum))
        whenever(albumUiStateMapper(mockUserAlbum)).thenReturn(mockAlbumUiState)
        whenever(getAlbumPhotosUseCase(any(), any())).thenReturn(flowOf())
        whenever(observeAlbumPhotosAddingProgress(any())).thenReturn(flowOf())
        whenever(observeAlbumPhotosRemovingProgress(any())).thenReturn(flowOf())

        createViewModel(AlbumContentNavKey(id = albumId.id, type = "custom"))

        underTest.handleBottomSheetAction(AlbumContentSelectionAction.ManageLink)

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.manageLinkEvent).isNotEqualTo(consumed())
        }
    }

    @Test
    fun `test that handleBottomSheetAction with RemoveLink action calls showRemoveLinkConfirmation`() =
        runTest {
            createViewModel()

            underTest.handleBottomSheetAction(AlbumContentSelectionAction.RemoveLink)

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

            underTest.handleBottomSheetAction(AlbumContentSelectionAction.Delete)

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
            whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
                false
            )
            whenever(getUserAlbum(any())).thenReturn(flowOf(mockUserAlbum))
            whenever(albumUiStateMapper(mockUserAlbum)).thenReturn(mockAlbumUiState)
            whenever(getAlbumPhotosUseCase(any(), any())).thenReturn(flowOf(listOf(legacyPhoto)))
            whenever(photoUiStateMapper(legacyPhoto)).thenReturn(photo)
            whenever(observeAlbumPhotosAddingProgress(any())).thenReturn(flowOf())
            whenever(observeAlbumPhotosAddingProgress(any())).thenReturn(flowOf())
            whenever(observeAlbumPhotosRemovingProgress(any())).thenReturn(flowOf())

            createViewModel(AlbumContentNavKey(id = albumId.id, type = "custom"))

            underTest.handleBottomSheetAction(AlbumContentSelectionAction.Delete)

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
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(
            false
        )
        whenever(getUserAlbum(any())).thenReturn(flowOf(mockUserAlbum))
        whenever(albumUiStateMapper(mockUserAlbum)).thenReturn(mockAlbumUiState)
        whenever(getAlbumPhotosUseCase(any(), any())).thenReturn(flowOf())
        whenever(observeAlbumPhotosAddingProgress(any())).thenReturn(flowOf())
        whenever(observeAlbumPhotosRemovingProgress(any())).thenReturn(flowOf())

        createViewModel(AlbumContentNavKey(id = albumId.id, type = "custom"))

        underTest.handleBottomSheetAction(AlbumContentSelectionAction.SelectAlbumCover)
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

        underTest.handleBottomSheetAction(action)

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
}

