package test.mega.privacy.android.app.presentation.mediaplayer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.mediaplayer.Constants.EMPTY_LIST_TEST_TAG
import mega.privacy.android.app.mediaplayer.Constants.EMPTY_TOP_BAR_TEST_TAG
import mega.privacy.android.app.mediaplayer.Constants.PROGRESS_TEST_TAG
import mega.privacy.android.app.mediaplayer.Constants.SELECTED_TOP_BAR_TEST_TAG
import mega.privacy.android.app.mediaplayer.Constants.SUBTITLE_FILES_TEST_TAG
import mega.privacy.android.app.mediaplayer.SelectSubtitleView
import mega.privacy.android.app.mediaplayer.model.SubtitleFileInfoItem
import mega.privacy.android.app.mediaplayer.model.SubtitleLoadState
import mega.privacy.android.core.ui.model.SearchWidgetState
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
internal class SelectSubtitleComposeViewKtTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        uiState: SubtitleLoadState,
        selectedSubtitleFileInfo: SubtitleFileInfo?,
    ) {
        composeTestRule.setContent {
            SelectSubtitleView(
                uiState = uiState,
                searchState = SearchWidgetState.COLLAPSED,
                query = "",
                selectedSubtitleFileInfo = selectedSubtitleFileInfo,
                onSearchTextChange = {},
                onCloseClicked = {},
                onSearchClicked = {},
                itemClicked = {},
                onAddSubtitle = {},
                onBackPressed = {}
            )
        }
    }

    @Test
    fun `test that ui is displayed correctly when the state is loading`() {
        setComposeContent(SubtitleLoadState.Loading, null)

        composeTestRule.onNodeWithTag(PROGRESS_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(EMPTY_TOP_BAR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that ui is displayed correctly when the state is empty`() {
        setComposeContent(SubtitleLoadState.Empty, null)

        composeTestRule.onNodeWithTag(EMPTY_LIST_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(EMPTY_TOP_BAR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that ui is displayed correctly when selected item`() {
        setComposeContent(
            SubtitleLoadState.Success(
                listOf(
                    SubtitleFileInfoItem(
                        false,
                        mock {
                            on { name }.thenReturn("test")
                            on { parentName }.thenReturn("parentTest")
                        }
                    )
                )
            ),
            mock()
        )

        composeTestRule.onNodeWithTag(SUBTITLE_FILES_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SELECTED_TOP_BAR_TEST_TAG).assertIsDisplayed()
    }
}