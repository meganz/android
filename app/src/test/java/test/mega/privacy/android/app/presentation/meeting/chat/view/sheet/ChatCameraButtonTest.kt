package test.mega.privacy.android.app.presentation.meeting.chat.view.sheet

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.ChatCameraButton
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.TEST_TAG_CHAT_CAMERA_BUTTON_ICON
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterialApi::class)
@RunWith(AndroidJUnit4::class)
class ChatCameraButtonTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `test that image showing correctly`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_CHAT_CAMERA_BUTTON_ICON, true).assertIsDisplayed()
    }

    private fun initComposeRuleContent() {
        composeTestRule.setContent {
            ChatCameraButton(
                sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
            )
        }
    }
}