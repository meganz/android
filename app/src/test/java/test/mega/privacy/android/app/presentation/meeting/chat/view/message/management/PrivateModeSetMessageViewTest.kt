package test.mega.privacy.android.app.presentation.meeting.chat.view.message.management

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.PrivateModeSetMessageView
import mega.privacy.android.app.utils.TextUtil
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrivateModeSetMessageViewTest {

    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that text shows correctly when private mode is set`() {
        val ownerActionFullName = "Owner"
        composeTestRule.setContent {
            PrivateModeSetMessageView(
                ownerActionFullName = ownerActionFullName,
            )
        }
        val expected = TextUtil.removeFormatPlaceholder(buildString {
            append(
                composeTestRule.activity.getString(
                    R.string.message_set_chat_private,
                    ownerActionFullName
                )
            )
            appendLine()
            appendLine()
            append(composeTestRule.activity.getString(R.string.subtitle_chat_message_enabled_ERK))
        })
        composeTestRule.onNodeWithText(expected).assertExists()
    }
}