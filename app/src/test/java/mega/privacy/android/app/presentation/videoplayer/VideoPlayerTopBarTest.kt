package mega.privacy.android.app.presentation.videoplayer

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.videoplayer.view.VIDEO_PLAYER_MORE_ACTIONS_BUTTON_TEST_TAG
import mega.privacy.android.app.presentation.videoplayer.view.VIDEO_PLAYER_TOP_BAR_TEST_TAG
import mega.privacy.android.app.presentation.videoplayer.view.VideoPlayerTopBar
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class VideoPlayerTopBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val onMoreActionsClicked = mock<() -> Unit>()

    private fun setComposeContent(
        title: String = "",
        onBackPressed: () -> Unit = {},
        onMoreActionsClicked: () -> Unit = this.onMoreActionsClicked,
        modifier: Modifier = Modifier,
    ) {
        composeTestRule.setContent {
            VideoPlayerTopBar(
                title = title,
                onBackPressed = onBackPressed,
                onMoreActionsClicked = onMoreActionsClicked,
                modifier = modifier,
            )
        }
    }

    @Test
    fun `test that VideoPlayerTopBar shows the top bar`() {
        setComposeContent()
        composeTestRule.onNodeWithTag(VIDEO_PLAYER_TOP_BAR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that VideoPlayerTopBar shows the more actions button`() {
        setComposeContent()
        composeTestRule.onNodeWithTag(
            testTag = VIDEO_PLAYER_MORE_ACTIONS_BUTTON_TEST_TAG,
            useUnmergedTree = true,
        ).assertIsDisplayed()
    }

    @Test
    fun `test that VideoPlayerTopBar calls onMoreActionsClicked when more actions button is clicked`() {
        setComposeContent()
        composeTestRule.onNodeWithTag(
            testTag = VIDEO_PLAYER_MORE_ACTIONS_BUTTON_TEST_TAG,
            useUnmergedTree = true,
        ).performClick()
        verify(onMoreActionsClicked).invoke()
    }
}
