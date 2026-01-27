package mega.privacy.android.feature.photos.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.core.nodecomponents.action.MultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.model.NodeActionState
import mega.privacy.android.domain.usecase.photos.DownloadPhotoUseCase
import mega.privacy.android.feature.photos.downloader.DownloadPhotoViewModel
import mega.privacy.android.feature.photos.mapper.PhotoMapper
import mega.privacy.android.feature.photos.presentation.albums.AlbumsTabUiState
import mega.privacy.android.feature.photos.presentation.albums.AlbumsTabViewModel
import mega.privacy.android.feature.photos.presentation.handler.MediaSelectionModeType
import mega.privacy.android.feature.photos.presentation.playlists.VideoPlaylistsTabUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineFilterUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabActionUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabSelectionModeActionUiState
import mega.privacy.android.domain.entity.photos.DownloadPhotoResult
import mega.privacy.android.feature.photos.extensions.LocalDownloadPhotoResultMock
import mega.privacy.android.feature.photos.model.PhotoNodeUiState
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.model.PhotosNodeContentType
import mega.privacy.android.feature.photos.presentation.component.PHOTOS_NODE_BODY_IMAGE_NODE_TAG
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabUiState
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotoModificationTimePeriod
import mega.privacy.android.feature.photos.presentation.timeline.model.TimelineSelectionMenuAction
import mega.privacy.android.feature.photos.presentation.videos.VideosTabUiState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.resources.R as sharedResR
import mega.privacy.mobile.analytics.core.event.identifier.EventIdentifier
import mega.privacy.mobile.analytics.event.MediaScreenAlbumsTabEvent
import mega.privacy.mobile.analytics.event.MediaScreenDownloadButtonPressedEvent
import mega.privacy.mobile.analytics.event.MediaScreenLinkButtonPressedEvent
import mega.privacy.mobile.analytics.event.MediaScreenMoreButtonPressedEvent
import mega.privacy.mobile.analytics.event.MediaScreenRespondButtonPressedEvent
import mega.privacy.mobile.analytics.event.MediaScreenShareButtonPressedEvent
import mega.privacy.mobile.analytics.event.MediaScreenTimelineTabEvent
import mega.privacy.mobile.analytics.event.MediaScreenTrashButtonPressedEvent
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
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val downloadPhotoViewModel = mock<DownloadPhotoViewModel>()

    private val viewModelFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                MediaMainViewModel::class.java -> mediaMainViewModel as T
                AlbumsTabViewModel::class.java -> albumsTabViewModel as T
                DownloadPhotoViewModel::class.java -> downloadPhotoViewModel as T
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
        setupViewModels()
        setupViewModelStore()
    }

    private fun setupViewModels() {
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

    private fun setupViewModelStore() {
        // Pre-populate the store with our mocked ViewModels
        ViewModelProvider(viewModelStoreOwner, viewModelFactory).apply {
            get(MediaMainViewModel::class.java)
            get(AlbumsTabViewModel::class.java)
        }
    }

    private fun setComposeContent(
        selectionModeType: MediaSelectionModeType = MediaSelectionModeType.None,
        timelineTabActionUiState: TimelineTabActionUiState = TimelineTabActionUiState(),
        timelineTabUiState: TimelineTabUiState = TimelineTabUiState(),
        selectedPhotosInTypedNode: () -> List<TypedNode> = { emptyList() },
        onTimelinePhotoSelected: (node: PhotoNodeUiState) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner,
                LocalDownloadPhotoResultMock provides DownloadPhotoResult.Idle
            ) {
                MediaMainScreen(
                    albumsTabUiState = AlbumsTabUiState(),
                    timelineTabUiState = timelineTabUiState,
                    timelineTabActionUiState = timelineTabActionUiState,
                    mediaCameraUploadUiState = MediaCameraUploadUiState(),
                    videosTabUiState = VideosTabUiState.Loading,
                    playlistsTabUiState = VideoPlaylistsTabUiState.Loading,
                    nodeActionUiState = NodeActionState(),
                    selectionModeType = selectionModeType,
                    selectedPhotosInTypedNode = selectedPhotosInTypedNode,
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
                    onTimelinePhotoSelected = onTimelinePhotoSelected,
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

    private fun testBottomBarActionTracking(
        action: TimelineSelectionMenuAction,
        expectedEvent: EventIdentifier,
    ) {
        val timelineTabActionUiState = TimelineTabActionUiState(
            selectionModeItem = TimelineTabSelectionModeActionUiState(
                bottomBarActions = listOf(action)
            )
        )
        setComposeContent(
            selectionModeType = MediaSelectionModeType.Timeline,
            timelineTabActionUiState = timelineTabActionUiState
        )

        composeTestRule.onNodeWithTag(action.testTag)
            .performClick()

        assertThat(analyticsRule.events).contains(expectedEvent)
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

    @Test
    fun `test that download button pressed event is tracked when download action is clicked`() {
        testBottomBarActionTracking(
            action = TimelineSelectionMenuAction.Download,
            expectedEvent = MediaScreenDownloadButtonPressedEvent
        )
    }

    @Test
    fun `test that share link button pressed event is tracked when share link action is clicked`() {
        testBottomBarActionTracking(
            action = TimelineSelectionMenuAction.ShareLink,
            expectedEvent = MediaScreenLinkButtonPressedEvent
        )
    }

    @Test
    fun `test that send to chat button pressed event is tracked when send to chat action is clicked`() {
        testBottomBarActionTracking(
            action = TimelineSelectionMenuAction.SendToChat,
            expectedEvent = MediaScreenRespondButtonPressedEvent
        )
    }

    @Test
    fun `test that share button pressed event is tracked when share action is clicked`() {
        testBottomBarActionTracking(
            action = TimelineSelectionMenuAction.Share,
            expectedEvent = MediaScreenShareButtonPressedEvent
        )
    }

    @Test
    fun `test that trash button pressed event is tracked when move to rubbish bin action is clicked`() {
        testBottomBarActionTracking(
            action = TimelineSelectionMenuAction.MoveToRubbishBin,
            expectedEvent = MediaScreenTrashButtonPressedEvent
        )
    }

    @Test
    fun `test that more button pressed event is tracked when more action is clicked`() {
        testBottomBarActionTracking(
            action = TimelineSelectionMenuAction.More,
            expectedEvent = MediaScreenMoreButtonPressedEvent
        )
    }
}
