package mega.privacy.android.feature.chat.dialog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.shared.resources.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class MeetingHasEndedDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val onDismiss = mock<() -> Unit>()
    private val onShowChat = mock<() -> Unit>()

    @Test
    fun `test that dialog description is shown`() {
        initComposeTestRule(isFromGuest = false)

        composeTestRule.onNodeWithText(
            context.getString(R.string.meeting_has_ended_dialog_title)
        ).assertIsDisplayed()
    }

    @Test
    fun `test that only OK button is shown when isFromGuest is true`() {
        initComposeTestRule(isFromGuest = true)

        with(composeTestRule) {
            onNodeWithText(context.getString(R.string.general_ok)).assertIsDisplayed()
            onNodeWithText(context.getString(R.string.general_dismiss_dialog))
                .assertDoesNotExist()
            onNodeWithText(context.getString(R.string.meeting_has_ended_dialog_view_chat_option))
                .assertDoesNotExist()
        }
    }

    @Test
    fun `test that both View Chat and Dismiss buttons are shown when isFromGuest is false`() {
        initComposeTestRule(isFromGuest = false)

        with(composeTestRule) {
            onNodeWithText(context.getString(R.string.meeting_has_ended_dialog_view_chat_option))
                .assertIsDisplayed()
            onNodeWithText(context.getString(R.string.general_dismiss_dialog))
                .assertIsDisplayed()
            onNodeWithText(context.getString(R.string.general_ok)).assertDoesNotExist()
        }
    }

    @Test
    fun `test that onDismiss is called but not onShowChat when isFromGuest is true and OK button is clicked`() {
        initComposeTestRule(isFromGuest = true)

        composeTestRule.onNodeWithText(context.getString(R.string.general_ok)).performClick()

        verify(onDismiss).invoke()
        verifyNoInteractions(onShowChat)
    }

    @Test
    fun `test that both onDismiss and onShowChat are called when isFromGuest is false and View Chat button is clicked`() {
        initComposeTestRule(isFromGuest = false)

        composeTestRule.onNodeWithText(
            context.getString(R.string.meeting_has_ended_dialog_view_chat_option)
        ).performClick()

        verify(onDismiss).invoke()
        verify(onShowChat).invoke()
    }

    @Test
    fun `test that only onDismiss is called when isFromGuest is false and Dismiss button is clicked`() {
        initComposeTestRule(isFromGuest = false)

        composeTestRule.onNodeWithText(
            context.getString(R.string.general_dismiss_dialog)
        ).performClick()

        verify(onDismiss).invoke()
        verifyNoInteractions(onShowChat)
    }

    private fun initComposeTestRule(isFromGuest: Boolean) {
        composeTestRule.setContent {
            MeetingHasEndedDialog(
                isFromGuest = isFromGuest,
                onDismiss = onDismiss,
                onShowChat = onShowChat
            )
        }
    }
}

