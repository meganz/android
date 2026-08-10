package mega.privacy.android.feature.photos.presentation.videos

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity
import mega.privacy.android.feature.photos.presentation.videos.view.VideoRecentlyWatchedClearMenuAction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.minutes

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class VideoRecentlyWatchedScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        uiState: VideoRecentlyWatchedUiState,
        onBack: () -> Unit = {},
        onClear: () -> Unit = {},
        onMenuClick: (NavKey) -> Unit = {},
        onClick: (VideoUiEntity) -> Unit = {},
        modifier: Modifier = Modifier,
    ) {
        composeTestRule.setContent {
            VideoRecentlyWatchedScreen(
                uiState = uiState,
                onBack = onBack,
                onClear = onClear,
                onMenuClick = onMenuClick,
                onClick = onClick,
                modifier = modifier
            )
        }
    }

    @Test
    fun `test that loading view is displayed as expected`() {
        setComposeContent(
            uiState = VideoRecentlyWatchedUiState.Loading
        )

        RECENTLY_WATCHED_LOADING_VIEW_TEST_TAG.assertIsDisplayedWithTag()
        RECENTLY_WATCHED_EMPTY_VIEW_TEST_TAG.assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that empty view is displayed as expected`() {
        setComposeContent(
            uiState = VideoRecentlyWatchedUiState.Data(
                groupedVideoRecentlyWatchedItems = emptyMap()
            )
        )

        RECENTLY_WATCHED_EMPTY_VIEW_TEST_TAG.assertIsDisplayedWithTag()
        VideoRecentlyWatchedClearMenuAction().testTag.assertIsNotDisplayedWithTag()
        RECENTLY_WATCHED_LOADING_VIEW_TEST_TAG.assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that data view is displayed as expected`() {
        val videoItem = mock<VideoUiEntity> {
            on { id }.thenReturn(NodeId(123L))
            on { name }.thenReturn("video")
            on { durationString }.thenReturn("10:00")
            on { duration }.thenReturn(10.minutes)
        }
        setComposeContent(
            uiState = VideoRecentlyWatchedUiState.Data(
                groupedVideoRecentlyWatchedItems = mapOf(
                    0L to listOf(videoItem)
                )
            )
        )

        RECENTLY_WATCHED_HEADER_TAG.assertIsDisplayedWithTag()
        RECENTLY_WATCHED_LOADING_VIEW_TEST_TAG.assertIsNotDisplayedWithTag()
        RECENTLY_WATCHED_EMPTY_VIEW_TEST_TAG.assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that clear menu action is displayed and onClear is invoked as expected`() {
        val onClear: () -> Unit = mock()
        val videoItem = mock<VideoUiEntity> {
            on { id }.thenReturn(NodeId(123L))
            on { name }.thenReturn("video")
            on { durationString }.thenReturn("10:00")
            on { duration }.thenReturn(10.minutes)
        }
        setComposeContent(
            uiState = VideoRecentlyWatchedUiState.Data(
                groupedVideoRecentlyWatchedItems = mapOf(
                    0L to listOf(videoItem)
                )
            ),
            onClear = onClear
        )

        VideoRecentlyWatchedClearMenuAction().testTag.let {
            it.assertIsDisplayedWithTag()
            composeTestRule.onNodeWithTag(it).performClick()
        }
        verify(onClear).invoke()
    }

    @Test
    fun `test that clicking video item invokes onClick with that item`() {
        val videoItem = mock<VideoUiEntity> {
            on { id }.thenReturn(NodeId(123L))
            on { name }.thenReturn("video")
            on { durationString }.thenReturn("10:00")
            on { duration }.thenReturn(10.minutes)
        }
        val onClick: (VideoUiEntity) -> Unit = mock()
        setComposeContent(
            uiState = VideoRecentlyWatchedUiState.Data(
                groupedVideoRecentlyWatchedItems = mapOf(
                    0L to listOf(videoItem)
                )
            ),
            onClick = onClick
        )

        composeTestRule.onNodeWithText("video", true).performClick()
        verify(onClick).invoke(videoItem)
    }

    private fun String.assertIsDisplayedWithTag() =
        composeTestRule.onNodeWithTag(this).assertIsDisplayed()

    private fun String.assertIsNotDisplayedWithTag() =
        composeTestRule.onNodeWithTag(this).assertIsNotDisplayed()
}