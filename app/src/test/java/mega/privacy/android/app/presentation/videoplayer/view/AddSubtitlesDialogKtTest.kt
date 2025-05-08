package mega.privacy.android.app.presentation.videoplayer.view

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.videoplayer.model.SUBTITLE_SELECTED_STATE_OFF
import mega.privacy.android.core.test.AnalyticsTestRule
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class AddSubtitlesDialogKtTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val analyticsRule = AnalyticsTestRule()

    private fun setComposeContent(
        isShown: Boolean = true,
        selectOptionState: Int = SUBTITLE_SELECTED_STATE_OFF,
        matchedSubtitleFileUpdate: suspend () -> SubtitleFileInfo? = { null },
        onOffClicked: () -> Unit = {},
        onAddedSubtitleClicked: () -> Unit = {},
        onAutoMatch: (SubtitleFileInfo) -> Unit = {},
        onToSelectSubtitle: () -> Unit = {},
        onDismissRequest: () -> Unit = {},
        modifier: Modifier = Modifier,
        subtitleFileName: String? = null,
    ) {
        composeTestRule.setContent {
            AddSubtitlesDialog(
                isShown = isShown,
                selectOptionState = selectOptionState,
                matchedSubtitleFileUpdate = matchedSubtitleFileUpdate,
                onOffClicked = onOffClicked,
                onAddedSubtitleClicked = onAddedSubtitleClicked,
                onAutoMatch = onAutoMatch,
                onToSelectSubtitle = onToSelectSubtitle,
                onDismissRequest = onDismissRequest,
                modifier = modifier,
                subtitleFileName = subtitleFileName
            )
        }
    }

    @Test
    fun `test that all options are shown`() {
        val subtitleInfo = SubtitleFileInfo(
            id = 1,
            name = "test.srt",
            url = "url",
            parentName = "parentName",
            isMarkedSensitive = false,
            isSensitiveInherited = false,
        )
        setComposeContent(
            matchedSubtitleFileUpdate = { subtitleInfo },
            subtitleFileName = "added subtitle.srt"
        )

        listOf(
            ADD_SUBTITLE_DIALOG_TITLE_TEST_TAG,
            ADD_SUBTITLE_DIALOG_OFF_ROW_TEST_TAG,
            ADD_SUBTITLE_DIALOG_ADDED_SUBTITLE_ROW_TEST_TAG,
            ADD_SUBTITLE_DIALOG_AUTO_MATCHED_SUBTITLE_ROW_TEST_TAG,
            ADD_SUBTITLE_DIALOG_NAVIGATE_TO_SELECT_SUBTITLE_TEST_TAG
        ).onEach {
            it.assertIsDisplayed()
        }
    }

    private fun String.assertIsDisplayed() {
        composeTestRule.onNodeWithTag(this, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that onOffClicked is invoked as expected`() {
        val offClicked = mock<() -> Unit>()
        setComposeContent(onOffClicked = offClicked)

        ADD_SUBTITLE_DIALOG_OFF_ROW_TEST_TAG.assertIsDisplayed()
        ADD_SUBTITLE_DIALOG_OFF_ROW_TEST_TAG.performClick()
        verify(offClicked).invoke()
    }

    @Test
    fun `test that onAddedSubtitleClicked is invoked as expected`() {
        val onAddedSubtitleClicked = mock<() -> Unit>()
        setComposeContent(
            onAddedSubtitleClicked = onAddedSubtitleClicked,
            subtitleFileName = "added subtitle.srt"
        )

        ADD_SUBTITLE_DIALOG_ADDED_SUBTITLE_ROW_TEST_TAG.assertIsDisplayed()
        ADD_SUBTITLE_DIALOG_ADDED_SUBTITLE_ROW_TEST_TAG.performClick()
        verify(onAddedSubtitleClicked).invoke()
    }

    @Test
    fun `test that onToSelectSubtitle is invoked as expected`() {
        val onToSelectSubtitle = mock<() -> Unit>()
        setComposeContent(onToSelectSubtitle = onToSelectSubtitle)

        ADD_SUBTITLE_DIALOG_NAVIGATE_TO_SELECT_SUBTITLE_TEST_TAG.assertIsDisplayed()
        ADD_SUBTITLE_DIALOG_NAVIGATE_TO_SELECT_SUBTITLE_TEST_TAG.performClick()
        verify(onToSelectSubtitle).invoke()
    }

    @Test
    fun `test that onAutoMatch is invoked as expected`() {
        val onAutoMatch = mock<(SubtitleFileInfo) -> Unit>()
        val subtitleInfo = SubtitleFileInfo(
            id = 1,
            name = "test.srt",
            url = "url",
            parentName = "parentName",
            isMarkedSensitive = false,
            isSensitiveInherited = false,
        )
        setComposeContent(
            matchedSubtitleFileUpdate = { subtitleInfo },
            onAutoMatch = onAutoMatch
        )

        ADD_SUBTITLE_DIALOG_AUTO_MATCHED_SUBTITLE_ROW_TEST_TAG.assertIsDisplayed()
        ADD_SUBTITLE_DIALOG_AUTO_MATCHED_SUBTITLE_ROW_TEST_TAG.performClick()
        verify(onAutoMatch).invoke(subtitleInfo)
    }

    private fun String.performClick() {
        composeTestRule.onNodeWithTag(this, useUnmergedTree = true).performClick()
    }
}