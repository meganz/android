package test.mega.privacy.android.app.presentation.meeting.chat.view.sheet

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.ChatToolbarBottomSheet
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.TEST_TAG_ATTACH_FROM_GALLERY
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.TEST_TAG_GALLERY_LIST
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterialApi::class)
@RunWith(AndroidJUnit4::class)
class ChatToolbarBottomSheetTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `test that gallery list shows`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_GALLERY_LIST).assertExists()
    }

    @Test
    fun `test that gallery list is shown`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACH_FROM_GALLERY).assertIsDisplayed()
    }

    private fun initComposeRuleContent() {
        composeTestRule.setContent {
            ChatToolbarBottomSheet(
                sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
            )
        }
    }
}