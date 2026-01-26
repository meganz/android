package mega.privacy.android.feature.photos.presentation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.core.nodecomponents.action.MultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.model.NodeActionState
import mega.privacy.android.feature.photos.presentation.albums.AlbumsTabUiState
import mega.privacy.android.feature.photos.presentation.albums.AlbumsTabViewModel
import mega.privacy.android.feature.photos.presentation.handler.MediaSelectionModeType
import mega.privacy.android.feature.photos.presentation.playlists.VideoPlaylistsTabUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineFilterUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabActionUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabUiState
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotoModificationTimePeriod
import mega.privacy.android.feature.photos.presentation.videos.VideosTabUiState
import mega.privacy.android.shared.resources.R as sharedResR
import mega.privacy.mobile.analytics.event.MediaScreenAlbumsTabEvent
import mega.privacy.mobile.analytics.event.MediaScreenTimelineTabEvent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

/**
 * Test class for MediaMainScreen analytics tracking
 */
@Config(sdk = [33])
@RunWith(AndroidJUnit4::class)
class MediaMainScreenAnalyticsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val analyticsRule = AnalyticsTestRule()

    private val mediaMainViewModel = mock<MediaMainViewModel>()
    private val albumsTabViewModel = mock<AlbumsTabViewModel>()

    init {
        whenever(mediaMainViewModel.uiState).thenReturn(
            MutableStateFlow(
                MediaMainUiState(
                    isMediaRevampPhase2Enabled = false  // Only Timeline and Albums tabs to avoid Hilt dependencies
                )
            )
        )
        whenever(albumsTabViewModel.uiState).thenReturn(
            MutableStateFlow(AlbumsTabUiState())
        )
    }

    private val viewModelFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                MediaMainViewModel::class.java -> mediaMainViewModel as T
                AlbumsTabViewModel::class.java -> albumsTabViewModel as T
                else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }

    private val viewModelStore = ViewModelStore()

    private val viewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore =
            this@MediaMainScreenAnalyticsTest.viewModelStore
    }

    init {
        // Pre-populate the store with our mocked ViewModels
        ViewModelProvider(viewModelStoreOwner, viewModelFactory).apply {
            get(MediaMainViewModel::class.java)
            get(AlbumsTabViewModel::class.java)
        }
    }

    private val context = InstrumentationRegistry
        .getInstrumentation()
        .targetContext

    private fun setComposeContent() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                MediaMainScreen(
                    albumsTabUiState = AlbumsTabUiState(),
                    timelineTabUiState = TimelineTabUiState(),
                    timelineTabActionUiState = TimelineTabActionUiState(),
                    mediaCameraUploadUiState = MediaCameraUploadUiState(),
                    videosTabUiState = VideosTabUiState.Loading,
                    playlistsTabUiState = VideoPlaylistsTabUiState.Loading,
                    nodeActionUiState = NodeActionState(),
                    selectionModeType = MediaSelectionModeType.None,
                    selectedPhotosInTypedNode = { emptyList() },
                    selectedTimePeriod = PhotoModificationTimePeriod.All,
                    multiNodeActionHandler = mock<MultiNodeActionHandler>(),
                    navigationHandler = mock(),
                    timelineFilterUiState = TimelineFilterUiState(),
                    actionHandler = { _, _ -> },
                    setEnableCUPage = {},
                    onTimelineGridSizeChange = {},
                    onTimelineSortOptionChange = {},
                    onTimelineApplyFilterClick = {},
                    setNavigationItemVisibility = {},
                    navigateToMediaSearch = {},
                    onTimelinePhotoSelected = {},
                    onAllTimelinePhotosSelected = {},
                    onClearTimelinePhotosSelection = {},
                    onNavigateToTimelinePhotoPreview = {},
                    onNavigateToAddToAlbum = {},
                    clearCameraUploadsCompletedMessage = {},
                    onNavigateToCameraUploadsSettings = {},
                    handleCameraUploadsPermissionsResult = {},
                    onCUBannerDismissRequest = {},
                    onNavigateToUpgradeAccount = {},
                    onPhotoTimePeriodSelected = {},
                    onNavigateToCameraUploadsProgressScreen = {},
                    onUpdateVideosSearchQuery = {},
                    onUpdatePlaylistSearchQuery = {},
                    onCurrentVideosSearchQueryRequest = { "" },
                    updateSelectionModeAvailableActions = { _, _ -> },
                    onSelectAllVideos = {},
                    onClearVideosSelection = {},
                    onSelectAllPlaylists = {},
                    onClearPlaylistsSelection = {},
                    viewModel = mediaMainViewModel,
                    albumsTabViewModel = albumsTabViewModel
                )
            }
        }
    }

    @Test
    fun `test that timeline tab selected event is tracked when timeline tab is selected`() {
        setComposeContent()

        composeTestRule.onNodeWithText(
            context.getString(sharedResR.string.media_timeline_tab_title)
        ).performClick()

        assertThat(analyticsRule.events).contains(MediaScreenTimelineTabEvent)
    }

    @Test
    fun `test that albums tab selected event is tracked when albums tab is selected`() {
        setComposeContent()

        composeTestRule.onNodeWithText(
            context.getString(sharedResR.string.media_albums_tab_title)
        ).performClick()

        assertThat(analyticsRule.events).contains(MediaScreenAlbumsTabEvent)
    }
}
