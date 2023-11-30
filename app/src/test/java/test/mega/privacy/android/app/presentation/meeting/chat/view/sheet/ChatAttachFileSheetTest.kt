package test.mega.privacy.android.app.presentation.meeting.chat.view.sheet

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.ChatAttachFileBottomSheet
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.TEST_TAG_SEND_FROM_CLOUD
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.TEST_TAG_SEND_FROM_LOCAL
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.TEST_TAG_SEND_HEADER
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatAttachFileSheetTest {

    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `test that send header is shown`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_SEND_HEADER).assertExists()
    }

    @Test
    fun `test that attach from cloud option is shown`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_SEND_FROM_CLOUD).assertIsDisplayed()
    }

    @Test
    fun `test that attach from local option is shown`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_SEND_FROM_LOCAL).assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterialApi::class)
    private fun initComposeRuleContent() {
        composeTestRule.setContent {
            ChatAttachFileBottomSheet(
                sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
            )
        }
    }
}