package mega.privacy.android.feature.photos.presentation.videos

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@Config(sdk = [34])
@RunWith(AndroidJUnit4::class)
class VideosTabScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        uiState: VideosTabUiState = VideosTabUiState.Data(),
        onClick: (item: VideoUiEntity, index: Int) -> Unit = { _, _ -> },
        onMenuClick: (VideoUiEntity) -> Unit = {},
        onLongClick: (item: VideoUiEntity, index: Int) -> Unit = { _, _ -> },
        modifier: Modifier = Modifier,
        highlightText: String = "",
    ) {
        composeTestRule.setContent {
            VideosTabScreen(
                uiState = uiState,
                modifier = modifier,
                onClick = onClick,
                onMenuClick = onMenuClick,
                onLongClick = onLongClick,
                highlightText = highlightText
            )
        }
    }

    @Test
    fun `test that loading view is displayed as expected`() {
        setComposeContent(
            uiState = VideosTabUiState.Loading
        )

        VIDEO_TAB_LOADING_VIEW_TEST_TAG.assertIsDisplayedWithTag()

        listOf(
            VIDEO_TAB_EMPTY_VIEW_TEST_TAG,
            VIDEO_TAB_ALL_VIDEOS_VIEW_TEST_TAG
        ).assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that empty view is displayed as expected`() {
        setComposeContent(
            uiState = VideosTabUiState.Data(
                allVideos = emptyList()
            )
        )

        VIDEO_TAB_EMPTY_VIEW_TEST_TAG.assertIsDisplayedWithTag()

        listOf(
            VIDEO_TAB_LOADING_VIEW_TEST_TAG,
            VIDEO_TAB_ALL_VIDEOS_VIEW_TEST_TAG
        ).assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that all videos view is displayed as expected`() {
        val video = VideoUiEntity(
            id = NodeId(1L),
            name = "Video 1",
            size = 1024L,
            duration = 60.seconds,
            fileTypeInfo = VideoFileTypeInfo(
                extension = "mp4",
                mimeType = "video/mp4",
                duration = 60.seconds
            ),
            parentId = NodeId(2L)
        )
        setComposeContent(
            uiState = VideosTabUiState.Data(
                allVideos = listOf(video)
            )
        )

        VIDEO_TAB_ALL_VIDEOS_VIEW_TEST_TAG.assertIsDisplayedWithTag()

        listOf(
            VIDEO_TAB_LOADING_VIEW_TEST_TAG,
            VIDEO_TAB_EMPTY_VIEW_TEST_TAG
        ).assertIsNotDisplayedWithTag()
    }

    private fun String.assertIsDisplayedWithTag() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsDisplayed()


    private fun List<String>.assertIsNotDisplayedWithTag() =
        onEach {
            it.assertIsNotDisplayedWithTag()
        }

    private fun String.assertIsNotDisplayedWithTag() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsNotDisplayed()
}