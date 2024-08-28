package mega.privacy.android.app.presentation.meeting.view.dialog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.view.dialog.FreePlanLimitParticipantsDialog
import mega.privacy.android.app.presentation.meeting.view.dialog.TEST_TAG_FREE_PLAN_LIMIT_PARTICIPANTS_DIALOG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class FreePlanLimitParticipantsDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that dialog is displayed`() {
        composeRule.setContent {
            FreePlanLimitParticipantsDialog(onConfirm = {})
        }
        composeRule.onNodeWithTag(TEST_TAG_FREE_PLAN_LIMIT_PARTICIPANTS_DIALOG).assertIsDisplayed()
    }

    @Test
    fun `test that dialog is clicked`() {
        val onConfirm = mock<() -> Unit>()

        composeRule.setContent {
            FreePlanLimitParticipantsDialog(onConfirm = onConfirm)
        }

        composeRule.onNodeWithTag(TEST_TAG_FREE_PLAN_LIMIT_PARTICIPANTS_DIALOG).performClick()
    }
}