package mega.privacy.android.app.presentation.videosection

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoMenuAction
import mega.privacy.android.app.presentation.videosection.view.videoselected.EMPTY_TOP_BAR_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.videoselected.SEARCH_TOP_BAR_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.videoselected.SELECTED_MODE_TOP_BAR_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.videoselected.VideoSelectedTopBar
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoSelectedTopBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        title: String = "Choose file",
        selectedSize: Int = 0,
        searchState: SearchWidgetState = SearchWidgetState.COLLAPSED,
        query: String? = null,
        isEmpty: Boolean = false,
        onMenuActionClick: (FileInfoMenuAction) -> Unit = {},
        onSearchTextChange: (String) -> Unit = {},
        onCloseClicked: () -> Unit = {},
        onSearchClicked: () -> Unit = {},
        onBackPressed: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            VideoSelectedTopBar(
                title = title,
                selectedSize = selectedSize,
                searchState = searchState,
                query = query,
                isEmpty = isEmpty,
                onMenuActionClick = onMenuActionClick,
                onSearchTextChange = onSearchTextChange,
                onCloseClicked = onCloseClicked,
                onSearchClicked = onSearchClicked,
                onBackPressed = onBackPressed
            )
        }
    }

    @Test
    fun `test that ui is displayed correctly when isEmpty is true`() {
        setComposeContent(isEmpty = true)

        composeTestRule.onNodeWithTag(EMPTY_TOP_BAR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that ui is displayed correctly when selectedSize is not 0`() {
        setComposeContent(selectedSize = 10)

        composeTestRule.onNodeWithTag(SELECTED_MODE_TOP_BAR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that ui is displayed correctly`() {
        setComposeContent()

        composeTestRule.onNodeWithTag(SEARCH_TOP_BAR_TEST_TAG).assertIsDisplayed()
    }
}