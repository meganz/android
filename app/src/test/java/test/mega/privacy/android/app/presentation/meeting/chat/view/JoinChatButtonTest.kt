package test.mega.privacy.android.app.presentation.meeting.chat.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.JoinChatButton
import mega.privacy.android.app.presentation.meeting.chat.view.TEST_TAG_JOIN_CHAT_BUTTON
import mega.privacy.android.app.presentation.meeting.chat.view.TEST_TAG_JOIN_PROGRESS_BUTTON
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import test.mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class JoinChatButtonTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that join button is shown if it is in preview mode`() {
        initComposeRuleContent(isPreviewMode = true)
        composeRule.onNodeWithTag(TEST_TAG_JOIN_CHAT_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithText(R.string.action_join).assertIsDisplayed()
    }

    @Test
    fun `test that join button is not shown if it is not in preview mode`() {
        initComposeRuleContent(isPreviewMode = false)
        composeRule.onNodeWithTag(TEST_TAG_JOIN_CHAT_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithText(R.string.action_join).assertDoesNotExist()
    }

    @Test
    fun `test that join progress button is shown if it is in preview mode and is joining`() {
        initComposeRuleContent(isPreviewMode = true, isJoining = true)
        composeRule.onNodeWithTag(TEST_TAG_JOIN_PROGRESS_BUTTON).assertIsDisplayed()
    }

    @Test
    fun `test that join progress button is not shown if it is not in preview mode`() {
        initComposeRuleContent(isPreviewMode = false)
        composeRule.onNodeWithTag(TEST_TAG_JOIN_PROGRESS_BUTTON).assertDoesNotExist()
    }

    @Test
    fun `test that join progress button is not shown if it is in preview mode but it is not joining`() {
        initComposeRuleContent(isPreviewMode = true, isJoining = false)
        composeRule.onNodeWithTag(TEST_TAG_JOIN_PROGRESS_BUTTON).assertDoesNotExist()
    }

    private fun initComposeRuleContent(isPreviewMode: Boolean = true, isJoining: Boolean = false) {
        composeRule.setContent {
            JoinChatButton(
                isPreviewMode = isPreviewMode,
                isJoining = isJoining,
                onClick = {},
            )
        }
    }
}