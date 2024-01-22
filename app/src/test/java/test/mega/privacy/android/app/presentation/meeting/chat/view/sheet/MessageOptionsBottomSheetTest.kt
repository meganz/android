package test.mega.privacy.android.app.presentation.meeting.chat.view.sheet

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.MessageOptionsBottomSheet
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.TEST_TAG_MESSAGE_OPTIONS_PANEL
import mega.privacy.android.core.ui.controls.chat.TEST_TAG_EMOJI_PICKER_VIEW
import mega.privacy.android.core.ui.controls.chat.messages.reaction.TEST_TAG_ADD_REACTIONS_SHEET_ITEM
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
        initComposeRule(showReactionPicker = false)
        composeRule.onNodeWithTag(TEST_TAG_EMOJI_PICKER_VIEW).assertDoesNotExist()
        composeRule.onNodeWithTag(TEST_TAG_MESSAGE_OPTIONS_PANEL).assertIsDisplayed()
    }

    @Test
    fun `test that reactions picker is shown and not sheet options`() {
        initComposeRule(showReactionPicker = true)
        composeRule.onNodeWithTag(TEST_TAG_EMOJI_PICKER_VIEW).assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_MESSAGE_OPTIONS_PANEL).assertDoesNotExist()
    }

    @OptIn(ExperimentalMaterialApi::class)
    private fun initComposeRule(showReactionPicker: Boolean = false) {
        composeRule.setContent {
            MessageOptionsBottomSheet(
                showReactionPicker = showReactionPicker,
                onReactionClicked = {},
                onMoreReactionsClicked = {},
                sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
            )
        }
    }
}