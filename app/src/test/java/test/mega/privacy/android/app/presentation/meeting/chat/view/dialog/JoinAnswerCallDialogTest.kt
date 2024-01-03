package test.mega.privacy.android.app.presentation.meeting.chat.view.dialog

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.JoinAnswerCallDialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.TEST_TAG_JOIN_ANSWER_CALL_DIALOG
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class JoinAnswerCallDialogTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    private val holdPressed = mock<() -> Unit>()
    private val endPressed = mock<() -> Unit>()
    private val cancelPressed = mock<() -> Unit>()

    @Test
    fun `test that correct dialog is shown when chat is a group`() {
        initComposeRuleContent(isGroup = true)
        with(composeRule) {
            onNodeWithTag(TEST_TAG_JOIN_ANSWER_CALL_DIALOG).assertIsDisplayed()
            onNodeWithText(R.string.title_join_call).assertIsDisplayed()
            onNodeWithText(R.string.text_join_another_call).assertIsDisplayed()
            onNodeWithText(R.string.hold_and_join_call_incoming).assertIsDisplayed()
            onNodeWithText(R.string.end_and_join_call_incoming).assertIsDisplayed()
            onNodeWithText(R.string.general_cancel).assertIsDisplayed()
        }
    }

    @Test
    fun `test that correct dialog is shown when chat is not a group`() {
        initComposeRuleContent(isGroup = false)
        with(composeRule) {
            onNodeWithTag(TEST_TAG_JOIN_ANSWER_CALL_DIALOG).assertIsDisplayed()
            onNodeWithText(R.string.title_join_one_to_one_call).assertIsDisplayed()
            onNodeWithText(R.string.text_join_another_call).assertIsDisplayed()
            onNodeWithText(R.string.hold_and_answer_call_incoming).assertIsDisplayed()
            onNodeWithText(R.string.end_and_answer_call_incoming).assertIsDisplayed()
            onNodeWithText(R.string.general_cancel).assertIsDisplayed()
        }
    }

    @Test
    fun `test that hold and join is invoked if button is pressed when chat is group`() {
        initComposeRuleContent(isGroup = true)
        with(composeRule) {
            onNodeWithText(R.string.hold_and_join_call_incoming).apply {
                assertIsDisplayed()
                performClick()
            }
            verify(holdPressed).invoke()
        }
    }

    @Test
    fun `test that hold and answer is invoked if button is pressed when chat is not group`() {
        initComposeRuleContent(isGroup = false)
        with(composeRule) {
            onNodeWithText(R.string.hold_and_answer_call_incoming).apply {
                assertIsDisplayed()
                performClick()
            }
            verify(holdPressed).invoke()
        }
    }

    @Test
    fun `test that end and join is invoked if button is pressed when chat is group`() {
        initComposeRuleContent(isGroup = true)
        with(composeRule) {
            onNodeWithText(R.string.end_and_join_call_incoming).apply {
                assertIsDisplayed()
                performClick()
            }
            verify(endPressed).invoke()
        }
    }

    @Test
    fun `test that end and answer is invoked if button is pressed when chat is not group`() {
        initComposeRuleContent(isGroup = false)
        with(composeRule) {
            onNodeWithText(R.string.end_and_answer_call_incoming).apply {
                assertIsDisplayed()
                performClick()
            }
            verify(endPressed).invoke()
        }
    }

    @Test
    fun `test that cancel is invoked if button is pressed when chat is group`() {
        initComposeRuleContent(isGroup = true)
        with(composeRule) {
            onNodeWithText(R.string.general_cancel).apply {
                assertIsDisplayed()
                performClick()
            }
            verify(cancelPressed).invoke()
        }
    }

    @Test
    fun `test that cancel is invoked if button is pressed when chat is not group`() {
        initComposeRuleContent(isGroup = false)
        with(composeRule) {
            onNodeWithText(R.string.general_cancel).apply {
                assertIsDisplayed()
                performClick()
            }
            verify(cancelPressed).invoke()
        }
    }

    private fun initComposeRuleContent(isGroup: Boolean) {
        composeRule.setContent {
            JoinAnswerCallDialog(
                isGroup = isGroup,
                onHoldAndAnswer = holdPressed,
                onEndAndAnswer = endPressed,
                onDismiss = cancelPressed,
            )
        }
    }
}