package mega.privacy.android.app.presentation.videoplayer.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.mediaplayer.model.SubtitleFileInfoItem
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class SubtitleFileInfoListViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val subtitleFileInfo = SubtitleFileInfo(
        id = 1L,
        name = "subtitle.srt",
        url = null,
        parentName = "Movies",
        isMarkedSensitive = false,
        isSensitiveInherited = false,
    )

    @Test
    fun `test that item name is displayed when list has items`() {
        composeTestRule.setContent {
            SubtitleFileInfoListView(
                subtitleInfoList = listOf(SubtitleFileInfoItem(subtitleFileInfo = subtitleFileInfo)),
                hiddenNodesEnabled = false,
                onClicked = {},
            )
        }

        composeTestRule.onNodeWithText(subtitleFileInfo.name).assertIsDisplayed()
    }

    @Test
    fun `test that onClicked is invoked when item is clicked`() {
        val onClicked = mock<(SubtitleFileInfo) -> Unit>()
        composeTestRule.setContent {
            SubtitleFileInfoListView(
                subtitleInfoList = listOf(SubtitleFileInfoItem(subtitleFileInfo = subtitleFileInfo)),
                hiddenNodesEnabled = false,
                onClicked = onClicked,
            )
        }

        composeTestRule.onNodeWithText(subtitleFileInfo.name).performClick()
        verify(onClicked).invoke(subtitleFileInfo)
    }

    @Test
    fun `test that checkbox is shown when item is selected`() {
        composeTestRule.setContent {
            SubtitleFileInfoListView(
                subtitleInfoList = listOf(
                    SubtitleFileInfoItem(subtitleFileInfo = subtitleFileInfo, selected = true)
                ),
                hiddenNodesEnabled = false,
                onClicked = {},
            )
        }

        VIDEO_PLAYER_SELECT_SUBTITLE_ITEM_CHECKBOX_TEST_TAG.assertIsDisplayed()
    }

    @Test
    fun `test that checkbox is not shown when item is not selected`() {
        composeTestRule.setContent {
            SubtitleFileInfoListView(
                subtitleInfoList = listOf(
                    SubtitleFileInfoItem(subtitleFileInfo = subtitleFileInfo, selected = false)
                ),
                hiddenNodesEnabled = false,
                onClicked = {},
            )
        }

        composeTestRule.onNodeWithTag(
            VIDEO_PLAYER_SELECT_SUBTITLE_ITEM_CHECKBOX_TEST_TAG,
            useUnmergedTree = true
        ).assertDoesNotExist()
    }

    private fun String.assertIsDisplayed() {
        composeTestRule.onNodeWithTag(this, useUnmergedTree = true).assertIsDisplayed()
    }
}
