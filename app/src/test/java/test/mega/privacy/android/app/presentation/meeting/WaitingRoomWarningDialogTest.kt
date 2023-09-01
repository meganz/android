package test.mega.privacy.android.app.presentation.meeting

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.view.CLOSE_DIALOG_TAG
import mega.privacy.android.app.presentation.meeting.view.WAITING_ROOM_WARNING_DIALOG_TAG
import mega.privacy.android.app.presentation.meeting.view.WaitingRoomWarningDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class WaitingRoomWarningDialogTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that warning dialog is shown`() {
        initComposeRuleContent(
            onCloseClicked = {}
        )

        composeRule.onNodeWithTag(WAITING_ROOM_WARNING_DIALOG_TAG).assertExists()
    }

    @Test
    fun `test that close dialog button performs action`() {
        val onCloseClicked = mock<() -> Unit>()
        initComposeRuleContent(
            onCloseClicked = onCloseClicked
        )

        composeRule.onNodeWithTag(CLOSE_DIALOG_TAG).performClick()
        verify(onCloseClicked).invoke()
    }

    private fun initComposeRuleContent(
        onCloseClicked: () -> Unit,
    ) {
        composeRule.setContent {
            WaitingRoomWarningDialog(
                onCloseClicked = onCloseClicked,
            )
        }
    }

}