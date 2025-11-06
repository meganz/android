package mega.privacy.android.app.presentation.photos.albums.albumcontent

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
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
import org.junit.jupiter.api.AfterAll
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
class LegacyAlbumContentViewModelTest {
    private lateinit var underTest: LegacyAlbumContentViewModel

    private val savedStateHandle = mock<SavedStateHandle>()
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
    private val analyticsTracker: AnalyticsTracker = mock()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        stubCommon()

        underTest = LegacyAlbumContentViewModel(
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
        )
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
            analyticsTracker
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
        whenever(observeAlbumPhotosAddingProgress(any())).thenReturn(emptyFlow())
        whenever(observeAlbumPhotosRemovingProgress(any())).thenReturn(emptyFlow())
    }

    @Test
    fun `test that deleteAlbum updates state correctly`() = runTest {
        underTest.deleteAlbum()

        underTest.state.test {
            assertThat(awaitItem().isDeleteAlbum).isTrue()
        }
    }

    @Test
    fun `test that showRemoveLinkConfirmation updates state correctly`() = runTest {
        underTest.showRemoveLinkConfirmation()

        underTest.state.test {
            assertThat(awaitItem().showRemoveLinkConfirmation).isTrue()
        }
    }

    @Test
    fun `test that closeRemoveLinkConfirmation updates state correctly`() = runTest {
        underTest.closeRemoveLinkConfirmation()

        underTest.state.test {
            assertThat(awaitItem().showRemoveLinkConfirmation).isFalse()
        }
    }

    @Test
    fun `test that setCurrentSort updates state correctly`() = runTest {
        val sort = Sort.OLDEST

        underTest.setCurrentSort(sort)

        underTest.state.test {
            assertThat(awaitItem().currentSort).isEqualTo(sort)
        }
    }

    @Test
    fun `test that setCurrentMediaType updates state correctly`() = runTest {
        val mediaType = FilterMediaType.IMAGES

        underTest.setCurrentMediaType(mediaType)

        underTest.state.test {
            assertThat(awaitItem().currentMediaType).isEqualTo(mediaType)
        }
    }

    @Test
    fun `test that showSortByDialog updates state correctly`() = runTest {
        underTest.showSortByDialog(true)

        underTest.state.test {
            assertThat(awaitItem().showSortByDialog).isTrue()
        }
    }

    @Test
    fun `test that showFilterDialog updates state correctly`() = runTest {
        underTest.showFilterDialog(true)

        underTest.state.test {
            assertThat(awaitItem().showFilterDialog).isTrue()
        }
    }

    @Test
    fun `test that showRenameDialog updates state correctly`() = runTest {
        underTest.showRenameDialog(true)

        underTest.state.test {
            assertThat(awaitItem().showRenameDialog).isTrue()
        }
    }

    @Test
    fun `test that showDeleteAlbumsConfirmation updates state correctly`() = runTest {
        underTest.showDeleteAlbumsConfirmation()

        underTest.state.test {
            assertThat(awaitItem().showDeleteAlbumsConfirmation).isTrue()
        }
    }

    @Test
    fun `test that closeDeleteAlbumsConfirmation updates state correctly`() = runTest {
        underTest.closeDeleteAlbumsConfirmation()

        underTest.state.test {
            assertThat(awaitItem().showDeleteAlbumsConfirmation).isFalse()
        }
    }

    @Test
    fun `test that setShowRemovePhotosFromAlbumDialog updates state correctly`() = runTest {
        underTest.setShowRemovePhotosFromAlbumDialog(true)

        underTest.state.test {
            assertThat(awaitItem().showRemovePhotosDialog).isTrue()
        }
    }

    @Test
    fun `test that clearSelectedPhotos updates state correctly`() = runTest {
        underTest.clearSelectedPhotos()

        underTest.state.test {
            assertThat(awaitItem().selectedPhotos).isEmpty()
        }
    }

    @Test
    fun `test that setSnackBarMessage updates state correctly`() = runTest {
        val message = "Test message"

        underTest.setSnackBarMessage(message)

        underTest.state.test {
            assertThat(awaitItem().snackBarMessage).isEqualTo(message)
        }
    }

    @Test
    fun `test that resetLinkRemoved updates state correctly`() = runTest {
        underTest.resetLinkRemoved()

        underTest.state.test {
            assertThat(awaitItem().isLinkRemoved).isFalse()
        }
    }

    @Test
    fun `test that setNewAlbumNameValidity updates state correctly`() = runTest {
        underTest.setNewAlbumNameValidity(false)

        underTest.state.test {
            assertThat(awaitItem().isInputNameValid).isFalse()
        }
    }

    @Test
    fun `test that setHiddenNodesOnboarded updates state correctly`() = runTest {
        underTest.state.test {
            underTest.setHiddenNodesOnboarded()
            assertThat(expectMostRecentItem().isHiddenNodesOnboarded).isTrue()
        }
    }

    @Test
    fun `test that togglePhotoSelection adds photo when not selected`() = runTest {
        val photo = mock<Photo.Image>()
        whenever(photo.id).thenReturn(123L)

        underTest.togglePhotoSelection(photo)

        underTest.state.test {
            assertThat(awaitItem().selectedPhotos).contains(photo)
        }
    }

    @Test
    fun `test that togglePhotoSelection removes photo when already selected`() = runTest {
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
    fun `test that disableExportAlbum updates state when link removed successfully`() = runTest {
        val albumId = AlbumId(123L)
        whenever(disableExportAlbumsUseCase(listOf(albumId))).thenReturn(1)

        underTest.disableExportAlbum(albumId)

        underTest.state.test {
            assertThat(awaitItem().isLinkRemoved).isTrue()
        }
    }

    @Test
    fun `test that removeFavourites calls use case and clears selection`() = runTest {
        underTest.removeFavourites()

        verify(removeFavouritesUseCase).invoke(any())
        underTest.state.test {
            assertThat(awaitItem().selectedPhotos).isEmpty()
        }
    }

    @Test
    fun `test that updatePhotosAddingProgressCompleted calls use case`() = runTest {
        val albumId = AlbumId(123L)

        underTest.updatePhotosAddingProgressCompleted(albumId)

        verify(updateAlbumPhotosAddingProgressCompleted).invoke(albumId)
    }

    @Test
    fun `test that updatePhotosRemovingProgressCompleted calls use case`() = runTest {
        val albumId = AlbumId(123L)

        underTest.updatePhotosRemovingProgressCompleted(albumId)

        verify(updateAlbumPhotosRemovingProgressCompleted).invoke(albumId)
    }

    @Test
    fun `test that getSelectedPhotos returns current selected photos`() = runTest {
        val result = underTest.getSelectedPhotos()

        assertThat(result).isEqualTo(underTest.state.value.selectedPhotos)
    }

    @Test
    fun `test that selectAllPhotos selects all photos when media type is all`() = runTest {
        underTest.selectAllPhotos()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.selectedPhotos.size).isEqualTo(state.photos.size)
        }
    }

    @Test
    fun `test that getSelectedNodes returns empty list by default`() = runTest {
        whenever(getNodeListByIds(any())).thenReturn(emptyList())

        val result = underTest.getSelectedNodes()

        assertThat(result).isEmpty()
    }

    @Test
    fun `test that hideOrUnhideNodes calls update node sensitive use case`() = runTest {
        underTest.hideOrUnhideNodes(true)

        // Since there are no selected photos, no calls should be made
        verify(updateNodeSensitiveUseCase, org.mockito.kotlin.never()).invoke(any(), any())
    }
}

