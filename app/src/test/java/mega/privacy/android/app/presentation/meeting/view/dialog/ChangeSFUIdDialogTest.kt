package mega.privacy.android.app.presentation.meeting.view.dialog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.view.dialog.ChangeSFUIdDialog
import mega.privacy.android.app.presentation.meeting.view.dialog.SFU_SUBTITLE_TAG
import mega.privacy.android.app.presentation.meeting.view.dialog.SFU_TEXT_FIELD_TAG
import mega.privacy.android.app.presentation.meeting.view.dialog.SFU_TITLE_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChangeSFUIdDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun initComposeRuleContent() {
        composeRule.setContent {
            ChangeSFUIdDialog(onChange = {}, onDismiss = {})
        }
    }

    @Test
    fun `test that all views are displayed`() {
        initComposeRuleContent()
        composeRule.onNodeWithTag(SFU_TITLE_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SFU_SUBTITLE_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SFU_TEXT_FIELD_TAG).assertIsDisplayed()
    }
}
