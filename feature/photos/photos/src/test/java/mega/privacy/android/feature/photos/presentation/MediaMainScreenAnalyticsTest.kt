package mega.privacy.android.feature.photos.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.core.nodecomponents.action.MultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.GetLinkMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.SendToChatMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeActionState
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.photos.model.MediaAppBarAction
import mega.privacy.android.feature.photos.model.PhotoNodeUiState
import mega.privacy.android.feature.photos.presentation.albums.AlbumsTabUiState
import mega.privacy.android.feature.photos.presentation.albums.AlbumsTabViewModel
import mega.privacy.android.feature.photos.presentation.handler.MediaSelectionModeType
import mega.privacy.android.feature.photos.presentation.playlists.VideoPlaylistsTabUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineFilterUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabActionUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabNormalModeActionUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabUiState
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotoModificationTimePeriod
import mega.privacy.android.feature.photos.presentation.videos.VideosTabUiState
import mega.privacy.android.shared.resources.R as sharedResR
import mega.privacy.mobile.analytics.core.event.identifier.EventIdentifier
import mega.privacy.mobile.analytics.event.MediaScreenAlbumsTabEvent
import mega.privacy.mobile.analytics.event.MediaScreenDownloadButtonPressedEvent
import mega.privacy.mobile.analytics.event.MediaScreenFilterMenuToolbarEvent
import mega.privacy.mobile.analytics.event.MediaScreenLinkButtonPressedEvent
import mega.privacy.mobile.analytics.event.MediaScreenMoreButtonPressedEvent
import mega.privacy.mobile.analytics.event.MediaScreenRespondButtonPressedEvent
import mega.privacy.mobile.analytics.event.MediaScreenSearchMenuToolbarEvent
import mega.privacy.mobile.analytics.event.MediaScreenSettingsMenuToolbarEvent
import mega.privacy.mobile.analytics.event.MediaScreenShareButtonPressedEvent
import mega.privacy.mobile.analytics.event.MediaScreenSortByMenuToolbarEvent
import mega.privacy.mobile.analytics.event.MediaScreenTimelineTabEvent
import mega.privacy.mobile.analytics.event.MediaScreenTrashButtonPressedEvent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

/** Test class for MediaMainScreen analytics tracking */
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

    private val viewModelFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                MediaMainViewModel::class.java -> mediaMainViewModel as T
                AlbumsTabViewModel::class.java -> albumsTabViewModel as T
                else -> throw IllegalArgumentException(
                    "Unknown ViewModel class: ${modelClass.name}"
                )
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
                    isMediaRevampPhase2Enabled = false // Only Timeline and Albums tabs to avoid Hilt dependencies
                )
            )
        )
        whenever(albumsTabViewModel.uiState)
            .thenReturn(MutableStateFlow(AlbumsTabUiState()))
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
        mediaCameraUploadUiState: MediaCameraUploadUiState = MediaCameraUploadUiState(),
        nodeActionUiState: NodeActionState = NodeActionState(),
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner,
            ) {
                MediaMainScreen(
                    albumsTabUiState = AlbumsTabUiState(),
                    timelineTabUiState = timelineTabUiState,
                    timelineTabActionUiState = timelineTabActionUiState,
                    mediaCameraUploadUiState = mediaCameraUploadUiState,
                    videosTabUiState = VideosTabUiState.Loading,
                    playlistsTabUiState = VideoPlaylistsTabUiState.Loading,
                    nodeActionUiState = nodeActionUiState,
                    selectionModeType = selectionModeType,
                    selectedPhotoIds = setOf(),
                    selectedPhotosInTypedNode = selectedPhotosInTypedNode,
                    selectedTimePeriod = PhotoModificationTimePeriod.All,
                    multiNodeActionHandler = mock<MultiNodeActionHandler>(),
                    navigationHandler = mock(),
                    timelineFilterUiState = TimelineFilterUiState(),
                    setEnableCUPage = {},
                    onTimelineGridSizeChange = {},
                    onTimelineSortOptionChange = {},
                    onTimelineApplyFilterClick = {},
                    navigateToMediaSearch = {},
                    onTimelinePhotoSelected = onTimelinePhotoSelected,
                    onClearTimelinePhotosSelection = {},
                    onNavigateToTimelinePhotoPreview = {},
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
                    albumsTabViewModel = albumsTabViewModel,
                    showTimelineFilter = false,
                    onTimelineFilterVisibilityChange = {},
                    videoPlaylistsTabViewModel = mock(),
                    handleNotificationPermissionResult = {}
                )
            }
        }
    }

    private fun testBottomBarActionTracking(
        action: MenuActionWithIcon,
        expectedEvent: EventIdentifier,
    ) {
        val nodeActionUiState = NodeActionState(
            availableActions = listOf(action),
            visibleActions = listOf(action)
        )
        setComposeContent(
            selectionModeType = MediaSelectionModeType.Timeline,
            nodeActionUiState = nodeActionUiState
        )

        composeTestRule.onNodeWithTag(action.testTag).performClick()

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

        composeTestRule.onNodeWithText(context.getString(sharedResR.string.media_albums_tab_title))
            .performClick()

        assertThat(analyticsRule.events).contains(MediaScreenAlbumsTabEvent)
    }

    @Test
    fun `test that download button pressed event is tracked when download action is clicked`() {
        testBottomBarActionTracking(
            action = DownloadMenuAction(),
            expectedEvent = MediaScreenDownloadButtonPressedEvent
        )
    }

    @Test
    fun `test that share link button pressed event is tracked when share link action is clicked`() {
        testBottomBarActionTracking(
            action = GetLinkMenuAction(),
            expectedEvent = MediaScreenLinkButtonPressedEvent
        )
    }

    @Test
    fun `test that send to chat button pressed event is tracked when send to chat action is clicked`() {
        testBottomBarActionTracking(
            action = SendToChatMenuAction(),
            expectedEvent = MediaScreenRespondButtonPressedEvent
        )
    }

    @Test
    fun `test that share button pressed event is tracked when share action is clicked`() {
        testBottomBarActionTracking(
            action = ShareMenuAction(),
            expectedEvent = MediaScreenShareButtonPressedEvent
        )
    }

    @Test
    fun `test that trash button pressed event is tracked when move to rubbish bin action is clicked`() {
        testBottomBarActionTracking(
            action = TrashMenuAction(),
            expectedEvent = MediaScreenTrashButtonPressedEvent
        )
    }

    @Test
    fun `test that more button pressed event is tracked when more action is clicked`() {
        testBottomBarActionTracking(
            action = NodeSelectionAction.More,
            expectedEvent = MediaScreenMoreButtonPressedEvent
        )
    }

    @Test
    fun `test that search button pressed event is tracked when search action is clicked`() {
        setComposeContent()

        composeTestRule.onNodeWithTag(MediaAppBarAction.Search.testTag).performClick()

        assertThat(analyticsRule.events).contains(MediaScreenSearchMenuToolbarEvent)
    }

    @Test
    fun `test that filter button pressed event is tracked when filter action is clicked`() {
        val timelineTabActionUiState = TimelineTabActionUiState(
            normalModeItem = TimelineTabNormalModeActionUiState(enableSort = false)
        )
        setComposeContent(timelineTabActionUiState = timelineTabActionUiState)

        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithTag(MediaAppBarAction.FilterSecondary.testTag).performClick()

        assertThat(analyticsRule.events).contains(MediaScreenFilterMenuToolbarEvent)
    }

    @Test
    fun `test that sort button pressed event is tracked when sort action is clicked`() {
        val timelineTabActionUiState = TimelineTabActionUiState(
            normalModeItem = TimelineTabNormalModeActionUiState(enableSort = true)
        )
        setComposeContent(timelineTabActionUiState = timelineTabActionUiState)

        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithTag(MediaAppBarAction.SortBy.testTag).performClick()

        assertThat(analyticsRule.events).contains(MediaScreenSortByMenuToolbarEvent)
    }

    @Test
    fun `test that camera uploads settings button pressed event is tracked when camera uploads settings action is clicked`() {
        val mediaCameraUploadUiState = MediaCameraUploadUiState(status = CUStatusUiState.UpToDate)
        val timelineTabActionUiState = TimelineTabActionUiState(
            normalModeItem = TimelineTabNormalModeActionUiState(enableSort = false)
        )
        setComposeContent(
            timelineTabActionUiState = timelineTabActionUiState,
            mediaCameraUploadUiState = mediaCameraUploadUiState
        )

        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithTag(MediaAppBarAction.CameraUploadsSettings.testTag)
            .performClick()

        assertThat(analyticsRule.events).contains(MediaScreenSettingsMenuToolbarEvent)
    }
}
