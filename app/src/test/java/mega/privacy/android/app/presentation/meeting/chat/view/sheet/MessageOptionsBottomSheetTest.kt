package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.MessageOptionsBottomSheet
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.TEST_TAG_MESSAGE_OPTIONS_PANEL
import mega.privacy.android.shared.original.core.ui.controls.chat.TEST_TAG_EMOJI_PICKER_VIEW
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.TEST_TAG_ADD_REACTIONS_SHEET_ITEM
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageOptionsBottomSheetTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that reactions item is shown`() {
        initComposeRule()
        composeRule.onNodeWithTag(TEST_TAG_ADD_REACTIONS_SHEET_ITEM).assertIsDisplayed()
    }

    @Test
    fun `test that reactions picker is not shown but sheet is`() {
        initComposeRule()
        composeRule.onNodeWithTag(TEST_TAG_EMOJI_PICKER_VIEW).assertDoesNotExist()
        composeRule.onNodeWithTag(TEST_TAG_MESSAGE_OPTIONS_PANEL).assertIsDisplayed()
    }

    private fun initComposeRule() {
        composeRule.setContent {
            MessageOptionsBottomSheet(
                onReactionClicked = {},
                onMoreReactionsClicked = {},
                actions = emptyList(),
                messageId = -1L,
            )
        }
    }
}