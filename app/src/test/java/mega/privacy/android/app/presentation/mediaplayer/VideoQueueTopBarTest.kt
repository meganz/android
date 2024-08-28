package mega.privacy.android.app.presentation.mediaplayer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.mediaplayer.queue.model.VideoPlayerMenuAction
import mega.privacy.android.app.mediaplayer.queue.model.VideoPlayerMenuAction.Companion.TEST_TAG_VIDEO_QUEUE_REMOVE_ACTION
import mega.privacy.android.app.mediaplayer.queue.model.VideoPlayerMenuAction.Companion.TEST_TAG_VIDEO_QUEUE_SELECT_ACTION
import mega.privacy.android.app.mediaplayer.queue.view.VIDEO_QUEUE_SEARCH_TOP_BAR_TEST_TAG
import mega.privacy.android.app.mediaplayer.queue.view.VIDEO_QUEUE_SELECTED_MODE_TOP_BAR_TEST_TAG
import mega.privacy.android.app.mediaplayer.queue.view.VideoQueueTopBar
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.android.shared.original.core.ui.controls.menus.TAG_MENU_ACTIONS_SHOW_MORE
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class VideoQueueTopBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        title: String = "",
        isActionMode: Boolean = false,
        selectedSize: Int = 0,
        searchState: SearchWidgetState = SearchWidgetState.COLLAPSED,
        query: String? = null,
        onMenuActionClick: (VideoPlayerMenuAction?) -> Unit = {},
        onSearchTextChange: (String) -> Unit = {},
        onCloseClicked: () -> Unit = {},
        onSearchClicked: () -> Unit = {},
        onBackPressed: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            VideoQueueTopBar(
                title = title,
                isActionMode = isActionMode,
                selectedSize = selectedSize,
                searchState = searchState,
                query = query,
                onMenuActionClick = onMenuActionClick,
                onSearchTextChange = onSearchTextChange,
                onCloseClicked = onCloseClicked,
                onSearchClicked = onSearchClicked,
                onBackPressed = onBackPressed
            )
        }
    }

    @Test
    fun `test that the search top bar is displayed`() {
        setComposeContent()

        composeTestRule.onNodeWithTag(
            testTag = VIDEO_QUEUE_SEARCH_TOP_BAR_TEST_TAG,
            useUnmergedTree = true
        ).assertIsDisplayed()
        composeTestRule.onNodeWithTag(
            testTag = VIDEO_QUEUE_SELECTED_MODE_TOP_BAR_TEST_TAG,
            useUnmergedTree = true
        ).assertIsNotDisplayed()
    }

    @Test
    fun `test that the select mode top bar is displayed`() {
        setComposeContent(isActionMode = true)

        composeTestRule.onNodeWithTag(
            testTag = VIDEO_QUEUE_SEARCH_TOP_BAR_TEST_TAG,
            useUnmergedTree = true
        ).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(
            testTag = VIDEO_QUEUE_SELECTED_MODE_TOP_BAR_TEST_TAG,
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun `test that remove action is pressed`() {
        val onMenuActionClick = mock<(VideoPlayerMenuAction?) -> Unit>()
        setComposeContent(
            isActionMode = true,
            selectedSize = 1,
            onMenuActionClick = onMenuActionClick
        )
        composeTestRule.onNodeWithTag(
            testTag = TEST_TAG_VIDEO_QUEUE_REMOVE_ACTION,
            useUnmergedTree = true
        ).performClick()

        onMenuActionClick.invoke(VideoPlayerMenuAction.VideoQueueRemoveAction)
    }

    @Test
    fun `test that select action is pressed`() {
        val onMenuActionClick = mock<(VideoPlayerMenuAction?) -> Unit>()
        setComposeContent(onMenuActionClick = onMenuActionClick)
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).apply {
            assertIsDisplayed()
            performClick()
        }
        composeTestRule.onNodeWithTag(testTag = TEST_TAG_VIDEO_QUEUE_SELECT_ACTION).apply {
            assertIsDisplayed()
            performClick()
        }

        onMenuActionClick.invoke(VideoPlayerMenuAction.VideoQueueSelectAction)
    }
}