package mega.privacy.android.feature.photos.presentation.videos

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.nodecomponents.action.NodeActionHandler
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.list.SORT_ORDER_TAG
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.photos.components.DURATION_FILTER_BUTTON_TEXT_TEST_TAG
import mega.privacy.android.feature.photos.components.LOCATION_FILTER_BUTTON_TEXT_TEST_TAG
import mega.privacy.android.feature.photos.presentation.videos.model.DurationFilterOption
import mega.privacy.android.feature.photos.presentation.videos.model.LocationFilterOption
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity
import mega.privacy.android.navigation.contract.NavigationHandler
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Config(sdk = [34])
@RunWith(AndroidJUnit4::class)
class VideosTabScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    val testVideoType = VideoFileTypeInfo(
        extension = "mp4",
        mimeType = "video/mp4",
        duration = 60.seconds
    )

    private fun setComposeContent(
        uiState: VideosTabUiState = VideosTabUiState.Data(),
        onClick: (item: VideoUiEntity) -> Unit = {},
        onLongClick: (item: VideoUiEntity) -> Unit = {},
        onSortNodes: (NodeSortConfiguration) -> Unit = {},
        modifier: Modifier = Modifier,
        navigationHandler: NavigationHandler = mock(),
        onTransfer: (TransferTriggerEvent) -> Unit = {},
        locationOptionSelected: (LocationFilterOption) -> Unit = {},
        durationOptionSelected: (DurationFilterOption) -> Unit = {},
        onDismissNodeOptionsBottomSheet: () -> Unit = {},
        nodeOptionsActionViewModel: NodeOptionsActionViewModel = mock(),
        nodeActionHandler: NodeActionHandler = mock()
    ) {
        composeTestRule.setContent {
            VideosTabScreen(
                uiState = uiState,
                modifier = modifier,
                onClick = onClick,
                onLongClick = onLongClick,
                onSortNodes = onSortNodes,
                navigationHandler = navigationHandler,
                onTransfer = onTransfer,
                locationOptionSelected = locationOptionSelected,
                durationOptionSelected = durationOptionSelected,
                onDismissNodeOptionsBottomSheet = onDismissNodeOptionsBottomSheet,
                nodeOptionsActionViewModel = nodeOptionsActionViewModel,
                nodeActionHandler = nodeActionHandler
            )
        }
    }

    @Test
    fun `test that loading view is displayed as expected`() {
        setComposeContent(
            uiState = VideosTabUiState.Loading
        )

        VIDEO_TAB_LOADING_VIEW_TEST_TAG.assertIsDisplayedWithTag()
        VIDEO_TAB_VIDEOS_FILTER_BUTTON_VIEW_TEST_TAG.assertIsDisplayedWithTag()

        listOf(
            VIDEO_TAB_EMPTY_VIEW_TEST_TAG,
            VIDEO_TAB_ALL_VIDEOS_VIEW_TEST_TAG,
        ).assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that empty view is displayed as expected`() {
        setComposeContent(
            uiState = VideosTabUiState.Data(
                allVideoEntities = emptyList()
            )
        )

        VIDEO_TAB_EMPTY_VIEW_TEST_TAG.assertIsDisplayedWithTag()
        VIDEO_TAB_VIDEOS_FILTER_BUTTON_VIEW_TEST_TAG.assertIsDisplayedWithTag()

        listOf(
            VIDEO_TAB_LOADING_VIEW_TEST_TAG,
            VIDEO_TAB_ALL_VIDEOS_VIEW_TEST_TAG
        ).assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that all videos view is displayed as expected`() {
        val video = createVideoUiEntity(1L)
        setComposeContent(
            uiState = VideosTabUiState.Data(
                allVideoEntities = listOf(video)
            )
        )

        VIDEO_TAB_ALL_VIDEOS_VIEW_TEST_TAG.assertIsDisplayedWithTag()
        VIDEO_TAB_VIDEOS_FILTER_BUTTON_VIEW_TEST_TAG.assertIsDisplayedWithTag()
        SORT_ORDER_TAG.assertIsDisplayedWithTag()

        listOf(
            VIDEO_TAB_LOADING_VIEW_TEST_TAG,
            VIDEO_TAB_EMPTY_VIEW_TEST_TAG
        ).assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that Location VideosFilterBottomSheet is displayed after location filter button is clicked`() {
        setComposeContent(
            uiState = VideosTabUiState.Data()
        )

        composeTestRule.onNodeWithTag(
            testTag = LOCATION_FILTER_BUTTON_TEXT_TEST_TAG,
            useUnmergedTree = true
        ).performClick()

        VIDEO_TAB_VIDEOS_LOCATION_FILTER_BOTTOM_SHEET_TEST_TAG.assertIsDisplayedWithTag()
    }

    @Test
    fun `test that Duration VideosFilterBottomSheet is displayed after duration filter button is clicked`() {
        setComposeContent(
            uiState = VideosTabUiState.Data()
        )

        composeTestRule.onNodeWithTag(
            testTag = DURATION_FILTER_BUTTON_TEXT_TEST_TAG,
            useUnmergedTree = true
        ).performClick()

        VIDEO_TAB_VIDEOS_DURATION_FILTER_BOTTOM_SHEET_TEST_TAG.assertIsDisplayedWithTag()
    }

    fun `test that SortBottomSheet is displayed correctly`() {
        val video = createVideoUiEntity(1L)
        val onSortNodes = mock<(NodeSortConfiguration) -> Unit>()
        setComposeContent(
            uiState = VideosTabUiState.Data(
                allVideoEntities = listOf(video),
            ),
            onSortNodes = onSortNodes
        )

        with(SORT_ORDER_TAG.getNodeWithTag()) {
            assertIsDisplayed()
            performClick()
        }

        VIDEO_TAB_SORT_BOTTOM_SHEET_TEST_TAG.assertIsDisplayedWithTag()
    }

    private fun String.assertIsDisplayedWithTag() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsDisplayed()


    private fun List<String>.assertIsNotDisplayedWithTag() =
        onEach {
            it.assertIsNotDisplayedWithTag()
        }

    private fun String.assertIsNotDisplayedWithTag() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsNotDisplayed()

    private fun String.getNodeWithTag() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true)

    private fun createVideoUiEntity(
        handle: Long,
        name: String = "Video name $handle",
        size: Long = 1024L,
        duration: Duration = 60.seconds,
        fileTypeInfo: FileTypeInfo = testVideoType,
        parentHandle: Long = 100L,
    ) = mock<VideoUiEntity> {
        on { id }.thenReturn(NodeId(handle))
        on { this.name }.thenReturn(name)
        on { this.size }.thenReturn(size)
        on { this.duration }.thenReturn(duration)
        on { this.fileTypeInfo }.thenReturn(fileTypeInfo)
        on { parentId }.thenReturn(NodeId(parentHandle))
    }
}