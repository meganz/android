package mega.privacy.android.app.presentation.meeting.view.dialog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class PasteMeetingLinkGuestDialogKtTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that the correct placeholder text is shown given empty link`() {
        with(composeRule) {
            setDialog()

            onNodeWithText(R.string.meeting_link).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the error text is shown when it's not NULL`() {
        val errorText = "errorText"
        with(composeRule) {
            setDialog(errorText = errorText)

            onNodeWithText(errorText).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the meeting link is shown`() {
        val meetingLink = "https//mega.co.nz/meetingLink"
        with(composeRule) {
            setDialog(meetingLink = meetingLink)

            onNodeWithText(meetingLink).assertIsDisplayed()
        }
    }

    private fun ComposeContentTestRule.setDialog(
        meetingLink: String = "",
        onTextChange: (String) -> Unit = {},
        onConfirm: () -> Unit = {},
        onCancel: () -> Unit = {},
        onDismiss: () -> Unit = {},
        errorText: String? = null,
    ) {
        setContent {
            PasteMeetingLinkGuestDialog(
                meetingLink = meetingLink,
                onTextChange = onTextChange,
                onConfirm = onConfirm,
                onCancel = onCancel,
                onDismiss = onDismiss,
                errorText = errorText
            )
        }
    }
}
