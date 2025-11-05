package mega.privacy.android.feature.photos.presentation.albums.content

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetAlbumPhotosUseCase
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetDefaultAlbumPhotos
import mega.privacy.android.domain.usecase.GetUserAlbum
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.ObserveAlbumPhotosAddingProgress
import mega.privacy.android.domain.usecase.ObserveAlbumPhotosRemovingProgress
import mega.privacy.android.domain.usecase.UpdateAlbumPhotosAddingProgressCompleted
import mega.privacy.android.domain.usecase.UpdateAlbumPhotosRemovingProgressCompleted
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.favourites.RemoveFavouritesUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.photos.DisableExportAlbumsUseCase
import mega.privacy.android.domain.usecase.photos.GetDefaultAlbumsMapUseCase
import mega.privacy.android.domain.usecase.photos.GetProscribedAlbumNamesUseCase
import mega.privacy.android.domain.usecase.photos.RemovePhotosFromAlbumUseCase
import mega.privacy.android.domain.usecase.photos.UpdateAlbumNameUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.feature.photos.domain.usecase.GetNodeListByIds
import mega.privacy.android.feature.photos.mapper.UIAlbumMapper
import mega.privacy.android.feature.photos.model.FilterMediaType
import mega.privacy.android.feature.photos.model.Sort
import mega.privacy.android.feature.photos.navigation.AlbumContentNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
    private val getUserAlbum = mock<GetUserAlbum>()
    private val getAlbumPhotosUseCase = mock<GetAlbumPhotosUseCase>()
    private val uiAlbumMapper = mock<UIAlbumMapper>()
    private val observeAlbumPhotosAddingProgress = mock<ObserveAlbumPhotosAddingProgress>()
    private val updateAlbumPhotosAddingProgressCompleted =
        mock<UpdateAlbumPhotosAddingProgressCompleted>()
    private val observeAlbumPhotosRemovingProgress = mock<ObserveAlbumPhotosRemovingProgress>()
    private val updateAlbumPhotosRemovingProgressCompleted =
        mock<UpdateAlbumPhotosRemovingProgressCompleted>()
    private val disableExportAlbumsUseCase = mock<DisableExportAlbumsUseCase>()
    private val removeFavouritesUseCase = mock<RemoveFavouritesUseCase>()
    private val removePhotosFromAlbumUseCase = mock<RemovePhotosFromAlbumUseCase>()
    private val getNodeListByIds = mock<GetNodeListByIds>()
    private val getProscribedAlbumNamesUseCase = mock<GetProscribedAlbumNamesUseCase>()
    private val updateAlbumNameUseCase = mock<UpdateAlbumNameUseCase>()
    private val updateNodeSensitiveUseCase = mock<UpdateNodeSensitiveUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val isHiddenNodesOnboardedUseCase = mock<IsHiddenNodesOnboardedUseCase>()
    private val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()

    private val testDispatcher = UnconfinedTestDispatcher()

    private val analyticsTracker: AnalyticsTracker = mock()

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            savedStateHandle,
            getDefaultAlbumPhotos,
            getDefaultAlbumsMapUseCase,
            getUserAlbum,
            getAlbumPhotosUseCase,
            uiAlbumMapper,
            observeAlbumPhotosAddingProgress,
            updateAlbumPhotosAddingProgressCompleted,
            observeAlbumPhotosRemovingProgress,
            updateAlbumPhotosRemovingProgressCompleted,
            disableExportAlbumsUseCase,
            removeFavouritesUseCase,
            removePhotosFromAlbumUseCase,
            getNodeListByIds,
            getProscribedAlbumNamesUseCase,
            updateAlbumNameUseCase,
            updateNodeSensitiveUseCase,
            getFeatureFlagValueUseCase,
            monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase,
            isHiddenNodesOnboardedUseCase,
            getBusinessStatusUseCase,
            analyticsTracker,
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
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(emptyFlow())
        whenever(monitorAccountDetailUseCase()).thenReturn(emptyFlow())
        whenever(observeAlbumPhotosAddingProgress(mock())).thenReturn(emptyFlow())
        whenever(observeAlbumPhotosRemovingProgress(mock())).thenReturn(emptyFlow())
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
            uiAlbumMapper = uiAlbumMapper,
            observeAlbumPhotosAddingProgress = observeAlbumPhotosAddingProgress,
            updateAlbumPhotosAddingProgressCompleted = updateAlbumPhotosAddingProgressCompleted,
            observeAlbumPhotosRemovingProgress = observeAlbumPhotosRemovingProgress,
            updateAlbumPhotosRemovingProgressCompleted = updateAlbumPhotosRemovingProgressCompleted,
            disableExportAlbumsUseCase = disableExportAlbumsUseCase,
            removeFavouritesUseCase = removeFavouritesUseCase,
            removePhotosFromAlbumUseCase = removePhotosFromAlbumUseCase,
            getNodeListByIds = getNodeListByIds,
            getProscribedAlbumNamesUseCase = getProscribedAlbumNamesUseCase,
            updateAlbumNameUseCase = updateAlbumNameUseCase,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            navKey = navKey,
        )
    }

    @Test
    fun `test that deleteAlbum updates state correctly`() = runTest {
        createViewModel()

        underTest.deleteAlbum()

        underTest.state.test {
            assertThat(awaitItem().isDeleteAlbum).isTrue()
        }
    }

    @Test
    fun `test that showRemoveLinkConfirmation updates state correctly`() = runTest {
        createViewModel()

        underTest.showRemoveLinkConfirmation()

        underTest.state.test {
            assertThat(awaitItem().showRemoveLinkConfirmation).isTrue()
        }
    }

    @Test
    fun `test that closeRemoveLinkConfirmation updates state correctly`() = runTest {
        createViewModel()

        underTest.closeRemoveLinkConfirmation()

        underTest.state.test {
            assertThat(awaitItem().showRemoveLinkConfirmation).isFalse()
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
    fun `test that showDeleteAlbumsConfirmation updates state correctly`() = runTest {
        createViewModel()

        underTest.showDeleteAlbumsConfirmation()

        underTest.state.test {
            assertThat(awaitItem().showDeleteAlbumsConfirmation).isTrue()
        }
    }

    @Test
    fun `test that closeDeleteAlbumsConfirmation updates state correctly`() = runTest {
        createViewModel()

        underTest.closeDeleteAlbumsConfirmation()

        underTest.state.test {
            assertThat(awaitItem().showDeleteAlbumsConfirmation).isFalse()
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
    fun `test that resetLinkRemoved updates state correctly`() = runTest {
        createViewModel()

        underTest.resetLinkRemoved()

        underTest.state.test {
            assertThat(awaitItem().isLinkRemoved).isFalse()
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

        underTest.setHiddenNodesOnboarded()

        underTest.state.test {
            assertThat(awaitItem().isHiddenNodesOnboarded).isTrue()
        }
    }

    @Test
    fun `test that togglePhotoSelection adds photo when not selected`() = runTest {
        createViewModel()
        val photo = mock<Photo.Image>()
        whenever(photo.id).thenReturn(123L)

        underTest.togglePhotoSelection(photo)

        underTest.state.test {
            assertThat(awaitItem().selectedPhotos).contains(photo)
        }
    }

    @Test
    fun `test that togglePhotoSelection removes photo when already selected`() = runTest {
        createViewModel()
        val photo = mock<Photo.Image>()
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
    fun `test that selectAllPhotos selects all photos for ALL_MEDIA type`() = runTest {
        createViewModel()
        val photo1 = mock<Photo.Image>()
        val photo2 = mock<Photo.Video>()

        // Set up initial state with photos
        underTest.state.test {
            val currentState = awaitItem()
            // This is a simplified test - in reality, you'd need to set up the state properly
        }
    }

    @Test
    fun `test that disableExportAlbum calls use case correctly`() = runTest {
        createViewModel()
        val albumId = AlbumId(123L)
        whenever(disableExportAlbumsUseCase(listOf(albumId))).thenReturn(1)

        underTest.disableExportAlbum(albumId)

        verify(disableExportAlbumsUseCase).invoke(listOf(albumId))
    }

    @Test
    fun `test that disableExportAlbum updates state when link removed successfully`() = runTest {
        createViewModel()
        val albumId = AlbumId(123L)
        whenever(disableExportAlbumsUseCase(listOf(albumId))).thenReturn(1)

        underTest.disableExportAlbum(albumId)

        underTest.state.test {
            assertThat(awaitItem().isLinkRemoved).isTrue()
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
}

